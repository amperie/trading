package pablo.trade.algorithms;

import pablo.trade.algorithms.Algorithm;
import pablo.trade.algorithms.algorithm_configs.AlgorithmConfigMACD_RSI_Limit;
import pablo.trade.algorithms.algorithm_configs.AlgorithmConfigRSI;
import pablo.trade.objects.TimeSeries;

public class AlgorithmRSI extends Algorithm {

    private AlgorithmConfigRSI cfg;

    public AlgorithmRSI(AlgorithmConfigRSI cfg) {
        super(cfg);
        this.cfg = cfg;
    }

    public void simulate(TimeSeries.DataPoint dp, TimeSeries data){

        float curr_hist, curr_rsi, last_rsi, close;
        curr_hist = (float)dp.getValue("trend_macd_diff");
        last_rsi = (float)data.getDPbyOffset(dp,-1).getValue("momentum_rsi");
        curr_rsi = (float)dp.getValue("momentum_rsi");
        close = (float)dp.getValue("close");

        //buy when rsi less than threshold but also try to find the local minimum
        if(last_rsi < cfg.rsi_low && curr_rsi > last_rsi){
            this.buyShares(cfg.ticker, 9999999, close);
        }

        //sell when rsi is above threshold, also look for the local max
        if(last_rsi > cfg.rsi_high && curr_rsi < last_rsi){
            this.sellShares(cfg.ticker, 9999999, close);
        }

    }

    public void dayChange(String ticker, TimeSeries.DataPoint currdp, TimeSeries.DataPoint yesterdays_lastdp,
                          TimeSeries.DataPoint yesterdays_firstdp, float yesterday_start_of_day_pf_value,
                          float yesterday_end_of_day_pf_value, TimeSeries data){
        if (cfg.sell_at_end_of_day) {
            //Sell all the positions at the end of the day
            //this.cancelLimitOrders();
            this.sellShares(ticker, 9999999, (float) currdp.getValue("close"));
        }
    }
}
