package pablo.trade.algorithms;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pablo.trade.algorithms.algorithm_configs.BaseAlgorithmConfig;
import pablo.trade.connections.influxDB;
import pablo.trade.connections.influxDBConfig;
import pablo.trade.objects.Portfolio;
import pablo.trade.objects.TimeSeries;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import pablo.trade.objects.TimeSeries.DataPoint;
import pablo.trade.objects.TimeSeries.TimeSeriesIterator;
import pablo.trade.objects.Portfolio.LimitOrder;
import pablo.trade.objects.Portfolio.Transaction;
import pablo.trade.utils.Utils;

public class Algorithm implements Runnable {

    private TimeSeries ts;
    private ZonedDateTime start, end;
    private Portfolio pf;
    private String ticker;
    private DataPoint currdp;
    private ArrayList<PendingOrder> pendingOrders = new ArrayList<>();
    private ArrayList<LimitOrder> limitOrders = new ArrayList<LimitOrder>();
    private ArrayList<Point> pf_value = new ArrayList<Point>();
    private ArrayList<Point> daily_returns = new ArrayList<>();
    private boolean resultsWrittenToInflux = false;
    private BaseAlgorithmConfig cfg;
    private static final Logger log = LogManager.getLogger(Algorithm.class);

    public Algorithm(BaseAlgorithmConfig cfg){
        this.pf = cfg.pf;
        this.ts = cfg.data;
        this.start = cfg.start;
        this.end = cfg.end;
        this.ticker = cfg.ticker;
        this.cfg = cfg;
    }

    public void loadData(){
        ts = new TimeSeries();
        ts.loadFromCSV(cfg.data_path + cfg.ticker + "\\" +
                cfg.ticker + "_TA-" + cfg.granularity + ".csv",
                cfg.start.minusWeeks(2), cfg.end);
    }

    final public Transaction buyShares(String ticker, int shares_to_buy, float price){
        return pf.buyShares(currdp.getDateTime(), ticker, shares_to_buy, price,
                Portfolio.TXtype.buy);
    }

    final public Transaction sellShares(String ticker, int shares_to_sell, float price){
        return pf.sellShares(currdp.getDateTime(), ticker, shares_to_sell, price,
                Portfolio.TXtype.sell);
    }

    final public LimitOrder buyWithLimitOrder(String ticker, int shares_to_buy, float price,
                                        float low_pct, float high_pct, ZonedDateTime expiration){
        LimitOrder lo = pf.buyWithLimitOrder(currdp.getDateTime(), ticker, shares_to_buy,
                price, low_pct, high_pct, expiration);
        if(lo.getShares() > 0) limitOrders.add(lo);
        return lo;
    }

    final public LimitOrder buyMaxSharesWithLimitOrder(String ticker, float price, float low_pct,
                                                 float high_pct, ZonedDateTime expiration){
        return buyWithLimitOrder(ticker, 99999999, price, low_pct, high_pct, expiration);
    }

    final private void processLimitOrders(){
        for(LimitOrder lo: this.limitOrders){
            if(!lo.hasProcessed()){
                boolean processed = lo.processOrder(pf, currdp.getDateTime(),
                        (float)currdp.getData().get("close"));
            }
        }
    }

    final public void cancelLimitOrdersAndSellAtMarket(){
        for(LimitOrder lo: this.limitOrders){
            if(!lo.hasProcessed()){
                lo.cancelOrderAndSellAtMarket(pf, currdp.getDateTime(),
                        (float)currdp.getData().get("close"));
            }
        }
    }

    final public void cancelLimitOrders(){
        for(LimitOrder lo: this.limitOrders){
            lo.cancelOrder();
        }
    }

    public String limitOrdersToString(){
        String retVal="";
        for(LimitOrder lo: limitOrders){
            retVal = retVal + lo.toString() + "\r\n";
        }
        return retVal;
    }

    final public PendingOrder addPendingOrder(DataPoint curr_dp, TimeSeries data,
                                              int minutes_to_expiration){
        PendingOrder ord = new PendingOrder(curr_dp.getDateTime(), curr_dp,
                data, minutes_to_expiration);
        pendingOrders.add(ord);
        return ord;
    }

    final private void processPendingOrders(DataPoint dp, TimeSeries data){
        for(PendingOrder ord: pendingOrders){
            //only active orders
            if(ord.isActive(dp.getDateTime())){
                //is the order triggering?
                if(decideToExecutePendingOrder(ord, dp, data)){
                    executePendingOrder(ord, dp, data);
                    //pendingOrders.remove(ord);
                    ord.markInactive();
                }
            }
        }
    }

    public boolean decideToExecutePendingOrder(PendingOrder ord,
                                               DataPoint dp, TimeSeries data){
        //override this method to put the logic in of when a pennding
        //order should be executed
        return false;
    }

    public void executePendingOrder(PendingOrder ord,
                                    DataPoint dp, TimeSeries data){
        //Override this method with the logic needed to execute the order
    }

    //TODO only supports single ticker
    final public void run(){
        //load data if not already loaded
        if(ts == null){loadData();}

        log.log(Level.INFO, "Running algorithm " + this.getClass().getName() + " for ticker " + ticker);
        this.resultsWrittenToInflux = false;
        TimeSeriesIterator ti = ts.iterator(start, end, false);
        DataPoint lastdp = null;
        DataPoint first_dp_of_day = null;
        float pf_value_start_of_day = -1.0f;

        //Get starting value
        float start_value = pf.portfolioValue(this.ticker, (Float)ti.peekNext().getData().get("close"));

        while (ti.hasNext()){
            currdp = ti.next();
            //if first_dp_of_day is uninitialized, initialize it with the first dp of the simulation
            //this will only happen the first iteration of this loop
            if(first_dp_of_day == null) {
                first_dp_of_day = currdp;
                pf_value_start_of_day = pf.portfolioValue(ticker,
                        (float)currdp.getData().get("close"));
            }

            //Check if there is a day change so we can store daily performance
            //for calculating daily returns
            if(lastdp != null && lastdp.getDateTime().getDayOfYear()
                != currdp.getDateTime().getDayOfYear()){
                //get pf value from last dt yesterday and send it in with the pf value from the start of day
                float pf_value_end_of_day = pf.portfolioValue(ticker, (float)lastdp.getData().get("close"));
                //Day changed, call the event function
                dayChangeEvent(ticker, currdp, lastdp, first_dp_of_day,
                        pf_value_start_of_day, pf_value_end_of_day, this.ts);
                //update the first_dp_of_day to be the currdp since we just changed ove to a new day
                first_dp_of_day = currdp;
                pf_value_start_of_day = pf.portfolioValue(ticker, (float)currdp.getData().get("close"));
            }

            //call simulate method which is where the algorithm is implemented
            //call it with the current dt's data point
            //include all the data in case the method needs it
            simulate(currdp, this.ts);

            //Process pending orders to see if any of them trigger
            processPendingOrders(currdp, this.ts);

            //Process limit orders after any buys from the simulate method
            processLimitOrders();

            //Track portfolio value for writing to influx later
            this.pf_value.add(this.pf.toPoint(ticker, currdp, null));

            //store the data point we just used to compare in the next loop
            lastdp = currdp;
        }

        float end_value = pf.portfolioValue(ticker, (float)lastdp.getData().get("close"));
        log.log(Level.INFO, "Done with algorithm " + this.getClass().getName() + " for ticker " +
                ticker + " starting value: " + start_value + " End: " + end_value);
        System.out.println("Starting value: " + start_value + " End: " + end_value);

        if(this.cfg.iflx_cfg != null && this.cfg.iflx_cfg.automatic_write){
            writeResultsToInflux(cfg.iflx_cfg, cfg.getInfluxTags(this.getClass().getSimpleName(), null));
        }

    }

    public void simulate(DataPoint dp, TimeSeries data){
        //override this to try out pablo.trade.algorithms
        LimitOrder tx = buyMaxSharesWithLimitOrder("SPXU", (float)dp.getData().get("close"),
                1f, 1f, null);

        if(tx.getShares()>0) System.out.println(tx.toString() +
                "   Cash:" + pf.getCash() + " SPXU: " + pf.getPosition("SPXU"));
    }

    final private void dayChangeEvent(String ticker, DataPoint currdp, DataPoint yesterdays_lastdp,
                                      DataPoint yesterdays_firstdp, float yesterday_start_of_day_pf_value,
                                      float yesterday_end_of_day_pf_value, TimeSeries data){

        float ret = (yesterday_end_of_day_pf_value - yesterday_start_of_day_pf_value)/
                        yesterday_start_of_day_pf_value;
        Point pt = new Point(ticker);
        pt.time(yesterdays_lastdp.getDateTime().toEpochSecond(), WritePrecision.S);
        pt.addField("daily_return", ret);
        pt.addTag("data_type", "Returns");
        this.daily_returns.add(pt);

        //call function that is overridable
        dayChange(ticker, currdp, yesterdays_lastdp, yesterdays_firstdp,
                yesterday_start_of_day_pf_value, yesterday_end_of_day_pf_value,
                data);

    }

    public void dayChange(String ticker, DataPoint currdp, DataPoint yesterdays_lastdp,
                                      DataPoint yesterdays_firstdp, float yesterday_start_of_day_pf_value,
                                      float yesterday_end_of_day_pf_value, TimeSeries data){
        //Override this to process this event
    }

    public void writeResultsToInflux(influxDBConfig cfg, HashMap<String, String> tags){

        if(this.resultsWrittenToInflux) return;
        this.resultsWrittenToInflux = true;
        influxDB iflx = new influxDB(cfg.url, cfg.token, cfg.bucket, cfg.org);
        iflx.setAutoFlushBuffer(cfg.buffer_size);
        LocalDateTime run_time = LocalDateTime.now();
        //Default tags
        if(tags != null) iflx.addDefaultTags(tags);
        iflx.addDefaultTag("run_datetime", run_time.toString());
        iflx.addDefaultTag("simulator", this.getClass().getName());
        //write portfolio values
        HashMap<String, String> pf_tags = new HashMap<>();
        pf_tags.put("data_type", "Portfolio");
        iflx.addPointsToBuffer(this.pf_value, pf_tags);
        //write limit orders
        iflx.addLimitOrdersToBuffer(this.limitOrders, null);
        //write transactions
        iflx.addTxsToBuffer(pf.getTransactionList(), null);
        //add daily returns to buffer
        iflx.addPointsToBuffer(this.daily_returns, null);
        iflx.writeBufferToInfluxDB();
    }

    public void writeTransactionListToCSV(String file){
        Utils.writeToTextFile(file, pf.getTransactionListAsCSV());
    }

    public class PendingOrder{
        public ZonedDateTime dt_created;
        public DataPoint dp_at_creation;
        public TimeSeries data;
        public ZonedDateTime expiration;
        private boolean isActive = true;

        public PendingOrder(ZonedDateTime dt_created, DataPoint dp_at_creation,
                            TimeSeries data, int minutes_to_expiration){
            this.dt_created = dt_created;
            this.dp_at_creation = dp_at_creation;
            this.data = data;
            this.expiration = dt_created.plusMinutes(minutes_to_expiration);
        }

        public boolean isExpired(ZonedDateTime dt_curr){
            if(expiration.isBefore(dt_curr)) this.markInactive();
            return expiration.isBefore(dt_curr);
        }

        public boolean isActive(ZonedDateTime dt_curr){
            if(!isActive) return false;
            return !isExpired(dt_curr);
        }

        public void markInactive(){
            this.isActive = false;
        }
    }

}
