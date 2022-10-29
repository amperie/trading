package pablo.trade.algorithms.algorithm_configs;

public class AlgorithmConfigMACD_RSI_PendingLimit extends AlgorithmConfigMACD_RSI_Limit{

    public float histogram_threshold = .03f;
    public int pending_order_minutes_to_expire = 10;

    public AlgorithmConfigMACD_RSI_PendingLimit(String granularity) {
        super(granularity);
    }
}
