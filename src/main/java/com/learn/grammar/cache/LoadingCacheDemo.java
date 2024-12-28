package com.learn.grammar.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.*;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class LoadingCacheDemo {
    public static void main(String[] args) {
        final AtomicReference<Integer> loadCount = new AtomicReference<>(0);

        LoadingCache<Integer, Integer> squareCache = CacheBuilder.newBuilder().maximumSize(100).build(
                new CacheLoader<Integer, Integer>() {
                    @Override
                    public Integer load(Integer key) throws Exception {
                        System.out.println("Loading...");
                        loadCount.updateAndGet(count -> count + 1);
                        return key * key;
                    }
                });
        try {
            // Access some values using get method
            System.out.println("Square of 3: " + squareCache.get(3)); // Loads value
            System.out.println("Square of 4: " + squareCache.get(4)); // Loads value

            // Prepare a set of keys to retrieve
            Set<Integer> keys = new HashSet<>(Arrays.asList(10, 100, 1000));
            // Use getAll to retrieve multiple values at once
            Map<Integer, Integer> squares = squareCache.getAll(keys); // Loads value for key 5
            System.out.println("Squares: " + squares);

            // Access the load count via AtomicReference
            System.out.println("Cache load invoked: " + loadCount.get() + " times");

        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
}
