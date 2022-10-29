package pablo.trade.objects;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import pablo.trade.connections.ToInfluxPoint;
import pablo.trade.connections.ToInfluxPointBase;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Portfolio {
    private HashMap<String, Integer> positions = new HashMap<String, Integer>();
    private float cash;
    private ArrayList<Transaction> txs = new ArrayList<Transaction>();

    public float getCash() {
        return cash;
    }

    public int getPosition(String ticker){
        if(positions.containsKey(ticker)){
            return positions.get(ticker);
        } else {
            return 0;
        }
    }

    public Portfolio(float startingCash){
        this.cash = startingCash;
    }

    public float portfolioValue(String ticker, float price){
        //TODO only supports one ticker this way
        if(positions.containsKey(ticker)){
            return this.cash + positions.get(ticker)*price;
        } else {
            return this.cash;
        }

    }

    //TODO only supports one ticker like this
    public Point toPoint(String ticker, TimeSeries.DataPoint curr_dp, HashMap<String, String> tags){
        Point pt = new Point(ticker);
        if(tags != null) pt.addTags(tags);
        pt.addField("pf_value", portfolioValue(ticker, (float)curr_dp.getValue("close")));
        pt.addField("pf_cash", this.cash);
        pt.addField("pf_" + ticker + "_shares", getPosition(ticker));
        pt.addField("pf_" + ticker + "_shares_value", getPosition(ticker)*(float)curr_dp.getValue("close"));
        pt.time(curr_dp.getDateTime().toEpochSecond(), WritePrecision.S);
        return pt;
    }

    public Transaction buyShares(ZonedDateTime dt,String ticker, int shares_to_buy, float price, TXtype type){
        //TODO: Add transaction costs
        int shares_bought = Math.min(shares_to_buy, (int)(this.cash/price));
        updateHashMap(ticker, shares_bought);
        cash = cash - shares_bought * price;
        Transaction retVal = new Transaction(dt, ticker, shares_bought, price, type);
        txs.add(retVal);
        //System.out.println(retVal.toString());
        return retVal;
    }

    public Transaction sellShares(ZonedDateTime dt, String ticker, int shares_to_sell, float price, TXtype type){
        //TODO add transaction costs
        if(!positions.containsKey(ticker)) return new Transaction(dt, ticker, 0, price, type);
        int shares_sold = Math.min(shares_to_sell, positions.get(ticker));
        updateHashMap(ticker, -1 * shares_sold);
        this.cash = this.cash + shares_sold * price;
        Transaction retVal = new Transaction(dt, ticker, shares_sold, price, type);
        txs.add(retVal);
        //System.out.println(retVal.toString());
        return retVal;
    }

    public LimitOrder buyWithLimitOrder(ZonedDateTime dt, String ticker, int shares_to_buy, float price,
                                  float low_pct, float high_pct, ZonedDateTime expiration){

        Transaction tx = buyShares(dt, ticker, shares_to_buy, price, TXtype.buy_limit);
        LimitOrder lo = new LimitOrder(tx, price*(100-low_pct)/100, price*(100+high_pct)/100, expiration);
        return lo;
    }

    public LimitOrder buyMaxSharesWithLimit(ZonedDateTime dt, String ticker, float price,
                                            float low_pct, float high_pct, ZonedDateTime expiration){
        return buyWithLimitOrder(dt, ticker, 999999999, price, low_pct, high_pct, expiration);
    }

    private void updateHashMap(String key, int add){
        if(positions.containsKey(key)){
            positions.put(key, positions.get(key) + add);
        } else {
            positions.put(key, add);
        }
        if(positions.get(key) == 0) positions.remove(key);
    }

    public ArrayList<Transaction> getTransactionList(){
        return txs;
    }

    public String getTransactionListAsCSV(){
        String retVal = "date,action,shares,price\r\n";
        for(Transaction tx: txs){
            retVal += tx.toCSVRow() + "\r\n";
        }
        return retVal;
    }

    public class Transaction implements ToInfluxPoint {

        public ZonedDateTime getDt() {
            return dt;
        }

        public String getTicker() {
            return ticker;
        }

        public int getShares() {
            return shares;
        }

        public float getPrice() {
            return price;
        }

        private ZonedDateTime dt;
        private String ticker;
        private int shares;
        private float price;
        private TXtype type;

        public Transaction(ZonedDateTime dt, String ticker, int shares, float price, TXtype type){
            this.dt = dt;
            this.ticker = ticker;
            this.shares = shares;
            this.price = price;
            this.type = type;
        }
        public String toString(){
            String retVal = dt.toString() + " " + this.type.toString() + " " +
                    this.shares + " shares at $" + this.price;
            return retVal;
        }

        public String toCSVRow(){
            String retVal = dt.toString() + "," + this.type.toString() + "," +
                    this.shares + "," + this.price;
            return retVal;
        }

        public Point toPoint(HashMap<String, String> tags){
            Point pt = new Point(this.ticker);
            pt.addTag("data_type","Transactions");
            if(tags != null) pt.addTags(tags);
            pt.addField("shares", this.shares);
            pt.addField("price", this.price);
            pt.addField("shares", this.shares);
            pt.addField("type", this.type.toString());
            pt.time(this.dt.toEpochSecond(), WritePrecision.S);
            return pt;
        }
    }

    public class LimitOrder extends OrderBase {
        //TODO: make this more sophisticated to lock shares in when a limit order is placed
        private UUID order_id;
        private ZonedDateTime placed_dt;
        private String ticker;

        public int getShares() {
            return shares;
        }

        private int shares;
        private float low, high;
        private ZonedDateTime expiration;
        private float sold_price = 0;
        private Transaction sale_tx;
        private Transaction buy_tx;
        private boolean Processed;

        public boolean hasProcessed(){
            return this.Processed;
        }

        public LimitOrder(Transaction buy_tx, ZonedDateTime dt, String ticker, int shares, float low,
                          float high, ZonedDateTime expiration){
            this.order_id = UUID.randomUUID();
            this.placed_dt = dt;
            this.ticker = ticker;
            this.shares = shares;
            this.low = low;
            this.high = high;
            this.expiration = expiration;
            this.buy_tx = buy_tx;
            this.Processed = false;
        }

        public LimitOrder(Transaction buy_tx, float low, float high, ZonedDateTime expiration){
            this(buy_tx, buy_tx.getDt(), buy_tx.getTicker(), buy_tx.getShares(),
                    low, high, expiration);
        }

        public void cancelOrder(){
            this.Processed = true;
        }

        public void cancelOrderAndSellAtMarket(Portfolio pf, ZonedDateTime dt, float price){
            cancelOrder();
            this.sale_tx = pf.sellShares(dt, this.ticker, this.shares, price, TXtype.sell_limit);
            this.sold_price = price;
        }

        public boolean processOrder(Portfolio pf, ZonedDateTime dt, float curr_price){
            //return true if order is processed
            //return true if order is expired
            if(!(expiration == null) && dt.isAfter(expiration)) {
                this.Processed = true;
                return true;
            }
            if(curr_price <= this.low || curr_price >= this.high){
                this.sale_tx = pf.sellShares(dt, this.ticker, this.shares, curr_price, TXtype.sell_limit);
                this.sold_price = curr_price;
                this.Processed = true;
                return true;
            } else {
                this.Processed = false;
                return false;
            }
        }

        public String toString(){
            String retVal = this.placed_dt.toString() + " BOUGHT " + this.buy_tx.getShares() +
                    " shares at $" + this.buy_tx.price;
            retVal = retVal + " limits: " + this.low + " to " + this.high + " SOLD for: " +
                    this.sale_tx.price + " at " + this.sale_tx.getDt().toString();
            return retVal;
        }

        @Override
        public Point toPoint(HashMap<String, String> tags){

            Point pt = new Point(this.ticker);
            if(tags != null) pt.addTags(tags);
            pt.addTag("data_type","Limit_Orders");
            pt.addField("placed_dt", this.placed_dt.toEpochSecond());
            pt.addField("shares_sold", this.shares);
            pt.addField("low_limit", this.low);
            pt.addField("high_limit", this.high);
            pt.addField("processed", this.Processed);
            pt.addField("sale_price", this.sold_price);
            pt.addField("buy_price", this.buy_tx.price);
            if(this.sale_tx != null){
                pt.addField("minutes_to_execute",
                        Duration.between(this.placed_dt, this.sale_tx.dt).toMinutes());}
            pt.time(this.placed_dt.toEpochSecond(), WritePrecision.S);
            return pt;
        }

    }

    public enum TXtype{
        buy,
        sell,
        buy_limit,
        sell_limit,
    }

}
