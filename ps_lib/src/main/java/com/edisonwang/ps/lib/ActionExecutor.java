package com.edisonwang.ps.lib;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author edi
 */
public class ActionExecutor {

    public static final String DEFAULT = "default";

    static class LimitedExecutor {
        private final ThreadPoolExecutor service;
        private final PriorityBlockingQueue<Runnable> queue;

        public LimitedExecutor(int limit) {
            queue = new PriorityBlockingQueue<>(2, new PrioritizedRunnable.PrioritizedRunnableComparator());
            service = new ThreadPoolExecutor(limit, limit,
                    0L, TimeUnit.MILLISECONDS, queue);
        }

        public void execute(PrioritizedRunnable runnable) {
            service.execute(runnable);
        }
    }

    private final ExecutorService mFullParallelExecutor = Executors.newCachedThreadPool();

    private final HashMap<Integer, HashMap<String, LimitedExecutor>> mLimitedExecutors = new HashMap<>();

    public void executeOnNewThread(Runnable runnable) {
        mFullParallelExecutor.execute(runnable);
    }

    public void execute(Runnable runnable, int queueLimit, String queueTag, int queuePriority) {
        //A queue is identified by a type.
        //With in a type, you can have multiple queues (except full parallel queues)
        //Within a queue, you can have multiple tags
        synchronized (mLimitedExecutors) {
            HashMap<String, LimitedExecutor> limitedQueue = mLimitedExecutors.get(queueLimit);
            if (limitedQueue == null) {
                limitedQueue = new HashMap<>();
                mLimitedExecutors.put(queueLimit, limitedQueue);
            }
            LimitedExecutor executor = limitedQueue.get(queueTag);
            if (executor == null) {
                executor = new LimitedExecutor(queueLimit);
                limitedQueue.put(queueTag, executor);
            }
            executor.execute(new PrioritizedRunnable(queuePriority, runnable));
        }
    }

}
