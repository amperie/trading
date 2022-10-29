package pablo.trade.connections;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import pablo.trade.objects.Portfolio;
import pablo.trade.objects.Portfolio.Transaction;
import pablo.trade.objects.Portfolio.LimitOrder;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class influxDB {
    //TODO make .toInfluxPoint methods in structures I want to write to influx (ie: transactions)

    private String token;
    private String bucket;
    private String org;
    private String url;
    private InfluxDBClient clnt;
    private WriteApiBlocking iflx_w;
    private HashMap<String, String> def_tags = new HashMap<>();
    private ArrayList<Point> pt_buffer = new ArrayList<>();
    private Object sync = new Object();
    private int AutoFlushBuffer = 0; //auto flush off if 0

    public influxDB(String url, String token, String bucket, String org) {
        this.token = token;
        this.bucket = bucket;
        this.org = org;
        this.url = url;
        this.clnt = InfluxDBClientFactory.create(url,
                token.toCharArray(), org, bucket);
        this.iflx_w = clnt.getWriteApiBlocking();
    }

    public void setAutoFlushBuffer(int record_count){
        this.AutoFlushBuffer = record_count;
    }

    public int getAutoFlushBuffer(){
        return this.getAutoFlushBuffer();
    }

    public void addDefaultTag(String tag, String value) {
        def_tags.put(tag, value);
    }

    public void addDefaultTags(HashMap<String, String> tags){
        def_tags.putAll(tags);
    }

    public Point addPointToBuffer(ZonedDateTime dt, String measurement,
                           HashMap<String, String> tags,
                           HashMap<String, Object> fields) {
        Point pt = new Point(measurement)
                .addTags(def_tags)
                .addFields(fields)
                .time(dt.toEpochSecond(), WritePrecision.S);
        if(def_tags != null) {pt.addTags(def_tags);}
        if(tags != null) {pt.addTags(tags);}
        addPointToBuffer(pt);

        return pt;

    }

    //TODO use interface instead
    public void addLimitOrdersToBuffer(ArrayList<LimitOrder> pts, HashMap<String, String> tags){
        for(ToInfluxPointBase pt: pts){
            addPointToBuffer(pt.toPoint(tags));
        }
    }

    //TODO use interface instead
    public void addTxsToBuffer(ArrayList<Transaction> pts, HashMap<String, String> tags){
        for(Transaction pt: pts){
            addPointToBuffer(pt.toPoint(tags));
        }
    }

    public void addPointToBuffer(Point pt){

        pt.addTags(def_tags);

        synchronized (sync) {
            pt_buffer.add(pt);
        }
        checkBufferWrite();
    }

    public void addPointsToBuffer(ArrayList<Point> pts, HashMap<String, String> tags){
        for (Point pt : pts) {
            if (tags != null) {
                pt.addTags(tags);
            }
            if(def_tags != null) {
                pt.addTags(def_tags);
            }
            synchronized (sync){
                pt_buffer.add(pt);
            }
            checkBufferWrite();
        }

        //synchronized (sync){
        //    pt_buffer.addAll(pts);
        //}
        checkBufferWrite();
    }

    private void checkBufferWrite(){
        if(this.AutoFlushBuffer > 0 && pt_buffer.size() >= this.AutoFlushBuffer){
            writeBufferToInfluxDB();
        }
    }

    public int writeBufferToInfluxDB(){
        int size = pt_buffer.size();
        synchronized (sync) {
            writePoints(pt_buffer);
            pt_buffer.clear();
        }
        //System.out.println("Influx wrote "+ size + " data points");
        return size;
    }

    public void writePoint(Point pt){
        iflx_w.writePoint(pt);
    }

    public void writePoints(ArrayList<Point> pts){
        iflx_w.writePoints(pts);
    }

}
