package pablo.trade;

import pablo.trade.algorithms.Algorithm;
import pablo.trade.algorithms.AlgorithmMACD_RSI_Limit;
import pablo.trade.algorithms.AlgorithmMACD_RSI_PendingLimit;
import pablo.trade.algorithms.AlgorithmRSI;
import pablo.trade.algorithms.algorithm_configs.AlgorithmConfigMACD_RSI_Limit;
import pablo.trade.algorithms.algorithm_configs.AlgorithmConfigMACD_RSI_PendingLimit;
import pablo.trade.algorithms.algorithm_configs.AlgorithmConfigRSI;
import pablo.trade.connections.influxDBConfig;
import pablo.trade.objects.Portfolio;

import java.time.ZonedDateTime;

public class App {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();


        //runRSI();
        runMACD_RSI_Limit();

        long endTime = System.currentTimeMillis();

        long duration = (endTime - startTime);
        System.out.println("Run time: " + duration);

        System.out.println("");

    }

    public static void runRSI(){

        ZonedDateTime s1 = ZonedDateTime.parse("2022-01-26T02:30:00-05:00");
        ZonedDateTime e1 = ZonedDateTime.parse("2022-10-26T16:35:00-05:00");

        AlgorithmConfigRSI cfg = new AlgorithmConfigRSI("5min");
        cfg.ticker = "SPXU";
        cfg.start = s1;
        cfg.end = e1;
        cfg.pf = new Portfolio(1000);
        cfg.sell_at_end_of_day = false;
        cfg.rsi_high = 70;
        cfg.rsi_low = 40;

        String token = "C-NSNKcEhcdDm_yiXMVWEWiMEFe-vYzZA5WFyipzsFwXdDJDPPKRGMzdIE1ZGkNvSH0z6MB23M4P6bfvJ2aLUA==";
        influxDBConfig iflx_cfg = new influxDBConfig("http://influxdb.lan:8086", token,
                "pablo", "java-results", true,3000);
        cfg.iflx_cfg = iflx_cfg;

        Algorithm al = new AlgorithmRSI(cfg);
        al.run();
        al.writeTransactionListToCSV("D:\\temp\\csvs\\" + cfg.ticker + "-" +
                cfg.granularity + "-txs.csv");
        System.out.println("");

    }

    public static void runMACD_RSI_PendingLimit(){

        ZonedDateTime s1 = ZonedDateTime.parse("2022-09-01T02:30:00-05:00");
        ZonedDateTime e1 = ZonedDateTime.parse("2022-10-28T16:35:00-05:00");

        AlgorithmConfigMACD_RSI_PendingLimit cfg = new AlgorithmConfigMACD_RSI_PendingLimit("12hour");
        cfg.ticker = "SPXU";
        cfg.start = s1;
        cfg.end = e1;
        cfg.pf = new Portfolio(1000);
        cfg.rsi_low = 40;
        cfg.low_pct = 1;
        cfg.high_pct = 1;
        cfg.sell_at_end_of_day = false;

        String token = "C-NSNKcEhcdDm_yiXMVWEWiMEFe-vYzZA5WFyipzsFwXdDJDPPKRGMzdIE1ZGkNvSH0z6MB23M4P6bfvJ2aLUA==";
        influxDBConfig iflx_cfg = new influxDBConfig("http://influxdb.lan:8086", token,
                "pablo", "java-results", true,3000);

        cfg.iflx_cfg = iflx_cfg;

        Algorithm al = new AlgorithmMACD_RSI_PendingLimit(cfg);
        al.run();

    }

    public static void runMACD_RSI_Limit(){

        ZonedDateTime s1 = ZonedDateTime.parse("2021-09-01T02:30:00-05:00");
        ZonedDateTime e1 = ZonedDateTime.parse("2022-10-28T16:35:00-05:00");

        AlgorithmConfigMACD_RSI_Limit cfg = new AlgorithmConfigMACD_RSI_Limit("3hour");
        cfg.ticker = "SPXU";
        cfg.start = s1;
        cfg.end = e1;
        cfg.pf = new Portfolio(1000);
        cfg.rsi_low = 40;
        cfg.rsi_high = 60;
        cfg.low_pct = 1;
        cfg.high_pct = 1;
        cfg.sell_at_end_of_day = false;
        cfg.use_limit_orders = false;

        String token = "C-NSNKcEhcdDm_yiXMVWEWiMEFe-vYzZA5WFyipzsFwXdDJDPPKRGMzdIE1ZGkNvSH0z6MB23M4P6bfvJ2aLUA==";
        influxDBConfig iflx_cfg = new influxDBConfig("http://influxdb.lan:8086", token,
                "pablo", "java-results", true,3000);

        cfg.iflx_cfg = iflx_cfg;

        Algorithm al = new AlgorithmMACD_RSI_Limit(cfg);
        al.run();

        al.writeTransactionListToCSV("D:\\temp\\csvs\\" + cfg.ticker + "-MACD-RSI-" +
                cfg.granularity + "-txs.csv");

    }
}
