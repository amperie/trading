package pablo.trade.connections;

import com.influxdb.client.write.Point;

import java.util.HashMap;

public class ToInfluxPointBase {
    public Point toPoint(HashMap<String, String> tags){return null;}
}
