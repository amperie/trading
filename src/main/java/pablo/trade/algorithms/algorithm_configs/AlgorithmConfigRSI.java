package pablo.trade.algorithms.algorithm_configs;

public class AlgorithmConfigRSI extends BaseAlgorithmConfig{

    public float rsi_low = 40;
    public float rsi_high = 70;
    public boolean sell_at_end_of_day = false;


    public AlgorithmConfigRSI(String granularity) {
        super(granularity);
    }

    public String getRunName(){
        return "RSI-" + rsi_low + "_to_" + rsi_high + "-"
                + start.toLocalDate().toString() + "-To-" + end.toLocalDate().toString();
    }

}
