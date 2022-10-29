package pablo.trade.objects;

import com.influxdb.client.write.Point;
import pablo.trade.connections.ToInfluxPointBase;

import java.time.ZonedDateTime;
import java.util.HashMap;

public abstract class OrderBase extends ToInfluxPointBase {
    public abstract boolean processOrder(Portfolio pf, ZonedDateTime dt, float curr_price);
    public abstract void cancelOrder();
    public abstract Point toPoint(HashMap<String, String> tags);
    public abstract String toString();
    public abstract boolean hasProcessed();
}
