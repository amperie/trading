package pablo.trade.connections;

import com.influxdb.client.write.Point;

import java.util.HashMap;

public interface ToInfluxPoint {
    public Point toPoint(HashMap<String, String> tags);
}
