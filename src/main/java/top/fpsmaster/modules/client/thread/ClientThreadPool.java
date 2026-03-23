package top.fpsmaster.modules.client.thread;

import top.fpsmaster.modules.logger.ClientLogger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientThreadPool {
    private static final int DEFAULT_QUEUE_CAPACITY = 256;

    private final ThreadPoolExecutor executorService;

    public ClientThreadPool(int threadCount) {
        int normalizedThreadCount = Math.max(1, threadCount);
        AtomicInteger threadIndex = new AtomicInteger(1);
        executorService = new ThreadPoolExecutor(
                normalizedThreadCount,
                normalizedThreadCount,
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(DEFAULT_QUEUE_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(runnable, "FPSMaster-Async-" + threadIndex.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                },
                (runnable, executor) -> ClientLogger.warn("Async task rejected because the queue is full")
        );
        executorService.allowCoreThreadTimeOut(true);
    }

    public <T> Future<T> execute(Callable<T> task) {
        return executorService.submit(task);
    }

    public Future<?> runnable(Runnable task) {
        return executorService.submit(task);
    }

    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}



