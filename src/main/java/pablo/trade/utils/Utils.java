package pablo.trade.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pablo.trade.algorithms.Algorithm;

import java.io.PrintWriter;

public class Utils {

    private static final Logger log = LogManager.getLogger(Utils.class);

    public static void writeToTextFile(String file, String contents){
        try {
            PrintWriter f = new PrintWriter(file);
            f.print(contents);
            f.close();
        } catch (Exception ex) {
            //TODO fix this shit
            log.error(ex);
        }

    }
}
