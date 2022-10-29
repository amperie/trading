package pablo.trade.algorithms.algorithm_configs;

import pablo.trade.connections.influxDBConfig;
import pablo.trade.objects.Portfolio;
import pablo.trade.objects.TimeSeries;

import java.time.ZonedDateTime;
import java.util.HashMap;

public class BaseAlgorithmConfig {
    public Portfolio pf = null;
    public ZonedDateTime start, end;
    public TimeSeries data;
    public String ticker;
    public influxDBConfig iflx_cfg;
    public String granularity;
    public String data_path = "D:\\Programming\\trading\\data\\";

    public BaseAlgorithmConfig(String granularity){
        this.granularity = granularity;
    }

    public String getRunName(){
        return "BaseAlgorithm-" + start.toLocalDate().toString() + "-To-" + end.toLocalDate().toString();
    }

    public HashMap<String, String> getInfluxTags(String algorithm_name, HashMap<String, String> tags){
        HashMap<String, String> retVal = new HashMap<>();
        retVal.put("run_name", getRunName());
        retVal.put("granularity", this.granularity);
        retVal.put("algo", algorithm_name);
        if(tags != null) retVal.putAll(tags);
        return retVal;
    }
}
