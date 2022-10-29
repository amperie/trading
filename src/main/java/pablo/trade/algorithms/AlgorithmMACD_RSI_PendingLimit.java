package pablo.trade.algorithms;

import pablo.trade.algorithms.algorithm_configs.AlgorithmConfigMACD_RSI_PendingLimit;
import pablo.trade.objects.TimeSeries;
import pablo.trade.objects.TimeSeries.DataPoint;

public class AlgorithmMACD_RSI_PendingLimit extends Algorithm {

    private int rsi;
    private String ticker;
    private float low_pct, high_pct;
    private boolean sell_at_end_of_day;
    private float hist_threshold;
    private int minutes_to_expire;

    public AlgorithmMACD_RSI_PendingLimit(AlgorithmConfigMACD_RSI_PendingLimit cfg) {
        super(cfg);
        this.rsi = cfg.rsi_low;
        this.ticker = cfg.ticker;
        this.low_pct = cfg.low_pct;
        this.high_pct = cfg.high_pct;
        this.sell_at_end_of_day = cfg.sell_at_end_of_day;
        this.hist_threshold = cfg.histogram_threshold;
        this.minutes_to_expire = cfg.pending_order_minutes_to_expire;
    }

    public void simulate(DataPoint dp, TimeSeries data){

        float curr_hist, curr_rsi, last_hist;
        curr_hist = (float)dp.getValue("trend_macd_diff");
        last_hist = (float)data.getDPbyOffset(dp,-1).getValue("trend_macd_diff");
        curr_rsi = (float)dp.getValue("momentum_rsi");

        if(curr_hist * last_hist < 0){
            if(curr_hist > 0 && curr_rsi < this.rsi){
                this.addPendingOrder(dp, data, minutes_to_expire);
            }
        }

    }

    public boolean decideToExecutePendingOrder(PendingOrder ord,
                                               DataPoint dp, TimeSeries data){

        float curr_hist = (float)dp.getValue("trend_macd_diff");
        if(curr_hist > hist_threshold) {
            return true;
        } else {
            return false;
        }

    }

    public void executePendingOrder(PendingOrder ord,
                                    DataPoint dp, TimeSeries data){

        float close = (float)dp.getValue("close");
        this.buyMaxSharesWithLimitOrder(this.ticker, close,
                this.low_pct, this.high_pct, null);

    }

    public void dayChange(String ticker, DataPoint currdp, DataPoint yesterdays_lastdp,
                          DataPoint yesterdays_firstdp, float yesterday_start_of_day_pf_value,
                          float yesterday_end_of_day_pf_value, TimeSeries data){
        if (this.sell_at_end_of_day) {
            //Sell all the positions at the end of the day
            this.cancelLimitOrdersAndSellAtMarket();
            this.sellShares(ticker, 9999999, (float) currdp.getValue("close"));
        }
    }
}
