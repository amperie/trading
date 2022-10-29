package pablo.trade.algorithms.algorithm_configs;

public class AlgorithmConfigMACD_RSI_Limit extends BaseAlgorithmConfig{
    public int rsi_low;
    public int rsi_high;
    public float low_pct;
    public float high_pct;
    public boolean sell_at_end_of_day;
    public String granularity;
    public boolean use_limit_orders = true;

    public AlgorithmConfigMACD_RSI_Limit(String granularity){
        super(granularity);
        this.granularity = granularity;
    }

    public String getRunName(){
        return "MACD_RSI_Limit-" + this.granularity + "-RSI" + this.rsi_low + "_LowPct_" +
                low_pct + "_HighPct_" + high_pct + "__"
                + start.toLocalDate().toString() + "-To-" + end.toLocalDate().toString();
    }

}
