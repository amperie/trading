package pablo.trade.objects;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TimeSeries implements Iterable<TimeSeries.DataPoint> {

    private ArrayList<DataPoint> list = new ArrayList<DataPoint>();
    private boolean sorted = true;
    private String[] schema;
    private boolean schemaInitialized = false;

    public TimeSeries(){
        this.Initialize();
    }
    public TimeSeries(String csvFilePath, ZonedDateTime start, ZonedDateTime end){
        this.loadFromCSV(csvFilePath, start, end);
        this.Initialize();
    }

    private void Initialize(){

    }

    public void addDateTimeNode(ZonedDateTime dt, HashMap<String, Object> data){
        addDateTimeNode(new DataPoint(dt, data));
    }

    private void addDateTimeNode(DataPoint node) {
        list.add(node);
        this.sorted = false;
    }

    public HashMap<String, Object> getDTdata(ZonedDateTime dt){
        //Before accessing make sure list is sorted
        if (!this.sorted){
            this.sortList();
        }
        int index = Collections.binarySearch(list, new DataPoint(dt,null), new NodeComparator());
        return list.get(index).data;
    }

    public int getIndexOfDTOrLesser(ZonedDateTime dt){
        int idx = getIndexOfDTOrHigher(dt);
        if(idx == -1) return 0;
        if(idx == list.size()-1) return -1;
        //if dt is found in ts
        if(list.get(idx).timestamp == dt.toEpochSecond())
        //Found the exact timestamp
        {return idx;}
            else
        {return idx+1;}
    }

    public int getIndexOfDTOrHigher(ZonedDateTime dt){
        long ts = dt.toEpochSecond();
        this.sortList();
        int size = list.size();
        int idx = size;
        //cases where dt is outside the range of points
        //dt is later than the latest date in ts
        if(ts>list.get(0).timestamp) return -1;
        //dt is before the earliest date in ts
        if(ts<list.get(size-1).timestamp) return size-1;
        for(idx=size-1; idx>=0; idx--){
            if(ts <= list.get(idx).timestamp) break;
        }
        return idx;
    }

    public DataPoint getDPbyOffset(DataPoint dp, int offset){
        //negative offset to go back in time
        int idx = getIndexOfDTOrHigher(dp.getDateTime());
        //if it's at the edge of the list, return the original dp param
        if(idx-offset<0 || idx-offset >= list.size()){
            return dp;
        } else {
            return list.get(idx-offset);
        }

    }

    public DataPoint getByIndex(int index){
        return list.get(index);
    }

    private void sortList(){
        if(!this.sorted) list.sort(new NodeComparator());
        this.sorted = true;
    }

    public void loadFromCSV(String path, ZonedDateTime start, ZonedDateTime end){
        try {
            String[] fields;
            Scanner rs = new Scanner(new File(path));
            schema = rs.nextLine().split(",");
            this.schemaInitialized = true;

            while (rs.hasNextLine()) {
                fields = rs.nextLine().split(",");

                HashMap<String, Object> data = new HashMap<String, Object>();
                //Expect that the date is the first field
                ZonedDateTime dt = (ZonedDateTime) inferTypeFromString(fields[0]);
                //If the current data point is not in between the start or end dates,
                // don't insert it, break out of this iteration
                if (start != null && dt.isBefore(start)) continue;
                if (end != null && dt.isAfter(end)) continue;
                data.put(schema[0], dt);

                for (int i = 1; i < schema.length; i++) {
                    Object val = inferTypeFromString(fields[i]);
                    data.put(schema[i], val);
                }
                //Insert into our list
                this.addDateTimeNode(new DataPoint(dt, data));
            }

            this.sortList();
            System.out.println("");
        } catch (FileNotFoundException ex) {
            //TODO
        }
    }

    private Object inferTypeFromString(String in){
        try {
            return Integer.parseInt(in);
        } catch (Exception ex) {}
        try {
            return Long.parseLong(in);
        } catch (Exception ex) {}

        try {
            return Float.parseFloat(in);
        } catch (Exception ex) {}

        try {
            DateTimeFormatter formatter
                    = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssz");
            return ZonedDateTime.parse(in, formatter);
        } catch (Exception ex) {}

        if (in.toLowerCase(Locale.ROOT)=="true" || in.toLowerCase(Locale.ROOT)=="false"){
            return Boolean.parseBoolean(in);
        }
        //Who knows what this is - return a string
        return in;
    }

    public TimeSeriesIterator iterator() {
        return new TimeSeriesIterator(this, 0,
                this.list.size()-1, false);
    }

    public TimeSeriesIterator iterator(ZonedDateTime start,
                                       ZonedDateTime end, boolean descending){
        return new TimeSeriesIterator(this, start, end, descending);
    }

    public class DataPoint {
        private long timestamp;
        private ZonedDateTime dt;
        private HashMap<String, Object> data;

        public DataPoint(ZonedDateTime TzDateTime) {
            this.dt = TzDateTime;
            this.timestamp = TzDateTime.toEpochSecond();
            data = new HashMap<String, Object>();
        }

        public DataPoint(ZonedDateTime TzDateTime, HashMap<String, Object> data) {
            this(TzDateTime);
            this.data = data;
        }

        public void addDataValue(String key, Object value){
            data.put(key, value);
        }

        public void setData(HashMap<String, Object> value){
            data = value;
        }

        public long getTimestamp(){return this.timestamp;}

        public ZonedDateTime getDateTime(){return this.dt;}

        public Object getValue(String key){return data.get(key);}

        public HashMap<String, Object> getData(){return data;}
    }

    public class NodeComparator implements Comparator<DataPoint> {

        public int compare(DataPoint o1, DataPoint o2) {
            if (o1.timestamp == o2.timestamp) return 0;
            if (o1.timestamp > o2.timestamp) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public class TimeSeriesIterator implements Iterator<DataPoint> {

        private ZonedDateTime start, end;
        private int start_idx, end_idx;
        private int index = 0;
        private TimeSeries ts;
        //descending means we iterate from 0 to size()
        //not descending is from size() to 0
        private boolean descending = false;

        public TimeSeriesIterator(TimeSeries ts, ZonedDateTime start,
                                  ZonedDateTime end, boolean descending){
            this.start = start;
            this.end = end;
            this.ts = ts;
            this.start_idx = ts.getIndexOfDTOrHigher(start);
            this.end_idx = ts.getIndexOfDTOrLesser(end);
            if(descending) {
                this.index = this.end_idx - 1;
            } else {
                this.index = this.start_idx + 1;
            }
            this.descending = descending;
        }

        public TimeSeriesIterator(TimeSeries ts, int start_idx,
                                  int end_idx, boolean descending){
            this.start = ts.getByIndex(start_idx).dt;
            this.end = ts.getByIndex(end_idx).dt;
            this.ts = ts;
            this.start_idx = start_idx;
            this.end_idx = end_idx;
            this.index = start_idx;
            if(descending) {
                this.index = this.end_idx - 1;
            } else {
                this.index = this.start_idx + 1;
            }
            this.descending = descending;
        }

        public boolean hasNext() {
            if(descending) {
                return this.index < this.start_idx;
            } else {
                return this.index > this.end_idx;
            }
        }

        public DataPoint next() {
            if(this.hasNext()){
                if(descending) {
                    this.index++;
                } else {
                    this.index--;
                }
                return ts.getByIndex(index);
            } else {
                return null;
            }
        }

        public DataPoint current() {
            return ts.getByIndex(index);
        }

        public DataPoint peekNext() {
            if(this.hasNext()){
                if(descending) {
                    return ts.getByIndex(index+1);
                } else {
                    return ts.getByIndex(index-1);
                }
            } else {
                return null;
            }
        }
    }
}
