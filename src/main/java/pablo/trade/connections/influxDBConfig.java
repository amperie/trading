package pablo.trade.connections;

public class influxDBConfig {
    public String token;
    public String url;
    public String bucket;
    public String org;
    public boolean automatic_write;
    public int buffer_size;

    public influxDBConfig(String url, String token, String org, String bucket, boolean automatic_write, int bufferSize){
        this.url = url;
        this.token = token;
        this.org = org;
        this.bucket = bucket;
        this.buffer_size = bufferSize;
        this.automatic_write = automatic_write;
    }
}
