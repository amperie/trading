package pablo.trade.utils;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by pablosalanova on 12/4/16.
 */

public class ManagedPool {
    String Name;
    //Everything will be fixed pools
    //ManagedPoolType type;
    ExecutorService pool;
    ArrayList<Future<?>> futures = new ArrayList<>();

    public ManagedPool(String name, int threads){
        pool = Executors.newFixedThreadPool(threads);
        this.Name = name;
    }

    public Future<?> submit(Runnable task){
        Future<?> retVal = pool.submit(task);
        futures.add(retVal);
        return retVal;
    }

    public void waitForAll(){
        for(Future<?> f: futures){
            try {
                f.wait();
            } catch (Exception ex){

            }
        }
    }

}
