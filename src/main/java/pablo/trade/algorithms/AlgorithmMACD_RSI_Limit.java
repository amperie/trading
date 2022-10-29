package pablo.trade.algorithms;

import pablo.trade.algorithms.algorithm_configs.AlgorithmConfigMACD_RSI_Limit;
import pablo.trade.objects.TimeSeries;
import pablo.trade.objects.TimeSeries.DataPoint;

public class AlgorithmMACD_RSI_Limit extends Algorithm {

    private int rsi_low, rsi_high;
    private String ticker;
    private float low_pct, high_pct;
    private boolean sell_at_end_of_day;
    private AlgorithmConfigMACD_RSI_Limit cfg;

    public AlgorithmMACD_RSI_Limit(AlgorithmConfigMACD_RSI_Limit cfg) {
        super(cfg);
        this.cfg = cfg;
        this.rsi_low = cfg.rsi_low;
        this.rsi_high = cfg.rsi_high;
        this.ticker = cfg.ticker;
        this.low_pct = cfg.low_pct;
        this.high_pct = cfg.high_pct;
        this.sell_at_end_of_day = cfg.sell_at_end_of_day;
    }

    public void simulate(DataPoint dp, TimeSeries data){

        float curr_hist, curr_rsi, last_hist, close;
        curr_hist = (float)dp.getValue("trend_macd_diff");
        last_hist = (float)data.getDPbyOffset(dp,-1).getValue("trend_macd_diff");
        curr_rsi = (float)dp.getValue("momentum_rsi");
        close = (float)dp.getValue("close");

        if(curr_hist * last_hist < 0){
            //lines crossed, check for buy signal
            if(curr_hist > 0 && curr_rsi < this.rsi_low){
                if(cfg.use_limit_orders) {
                    this.buyMaxSharesWithLimitOrder(this.ticker, close,
                            this.low_pct, this.high_pct, null);
                } else {
                    this.buyShares(this.ticker, 9999999, close);
                }
            }
            //Lines crossed - is it a sell signal? sell only if not using limits
            if(curr_hist<0 && curr_rsi > this.rsi_high && !cfg.use_limit_orders){
                this.sellShares(this.ticker, 9999999, close);
            }
        }

    }

    public void dayChange(String ticker, DataPoint currdp, DataPoint yesterdays_lastdp,
                          DataPoint yesterdays_firstdp, float yesterday_start_of_day_pf_value,
                          float yesterday_end_of_day_pf_value, TimeSeries data){
        if (this.sell_at_end_of_day) {
            //Sell all the positions at the end of the day
            this.cancelLimitOrders();
            this.sellShares(ticker, 9999999, (float) currdp.getValue("close"));
        }
    }
}
