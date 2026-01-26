package com.karlin.decompiler.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class DecompileExecutor {
    private ExecutorService executor;
    private int threadCount;

    public DecompileExecutor(int threadCount) {
        this.threadCount = threadCount;
        this.executor = createExecutor(threadCount);
    }

    public synchronized void setThreadCount(int threadCount) {
        if (this.threadCount == threadCount) {
            return;
        }
        this.threadCount = threadCount;
        executor.shutdownNow();
        executor = createExecutor(threadCount);
    }

    public <T> CompletableFuture<T> submit(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    private ExecutorService createExecutor(int count) {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger index = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "decompile-worker-" + index.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
        return Executors.newFixedThreadPool(Math.max(1, count), factory);
    }
}
