package com.consistency;


import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class AsyncThread {

    private static ExecutorService executor = Executors.newFixedThreadPool(30,
            new BasicThreadFactory.Builder()
                    .namingPattern("AsyncThread-%d")
                    .daemon(true)
                    .build());

    public static CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }
}

