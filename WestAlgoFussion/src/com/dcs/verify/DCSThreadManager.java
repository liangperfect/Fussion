package com.dcs.verify;


import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.westalgo.factorycamera.debug.Log;

public class DCSThreadManager
{
    private static final Log.Tag TAG = new Log.Tag("DCSThreadManager");
    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 8;
    private static final int KEEP_ALIVE_TIME = 10; // 10 seconds

    /**
     * An object that creates new threads on demand.
     */
    private static final ThreadFactory threadFactory = new ThreadFactory()
    {
        private final AtomicInteger count = new AtomicInteger(1);

        public Thread newThread(Runnable r)
        {
            String name = "thread-pool#" + count.getAndIncrement();
            Log.d(TAG,"thread name: "+ name);
            return new Thread(r, name);
        }
    };

    private final Executor executor ;

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial parameters and ThreadFactory
     *
     * @param corePoolSize the number of threads to keep in the pool
     * @param maxPoolSize the maximum number of threads to allow in the pool
     */
    public DCSThreadManager(int corePoolSize, int maxPoolSize) {
        executor = new ThreadPoolExecutor(
            corePoolSize, maxPoolSize, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
            threadFactory);
    }

    private static DCSThreadManager instance = null;

    /**
     * DCSThreadManager singleton pattern
     */
    public synchronized static DCSThreadManager instance() {
        if (instance == null)
        {
            instance = new DCSThreadManager(CORE_POOL_SIZE,MAX_POOL_SIZE);
        }
        return (instance);
    }


    /**
     * Add Runnable into the thread poll to execute
     *
     * @param item A runnable item
     */
    public void add(Runnable item) {
        executor.execute(item);
    }

}
