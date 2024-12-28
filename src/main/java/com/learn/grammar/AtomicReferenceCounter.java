package com.learn.grammar;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public class AtomicReferenceCounter {
    private final AtomicReference<Integer> counter;


    public AtomicReferenceCounter() {
        counter = new AtomicReference<>(0);
    }

    public void increment() {
        while (true) {
            Integer current = counter.get();
            Integer updated = current + 1;

            if (counter.compareAndSet(current, updated)) {
                break;
            }
        }
    }

    public void decrement() {
        while (true) {
            Integer current = counter.get();
            int updated = current - 1;
            if (counter.compareAndSet(current, updated)) {
                break;
            }

        }
    }

    public int getValue() {
        return counter.get();
    }

    public void update(UnaryOperator<Integer> updateFunction) {
        while (true) {
            Integer current = counter.get();
            Integer updated = updateFunction.apply(current);
            if (counter.compareAndSet(current, updated)) {
                break;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        AtomicReferenceCounter counter = new AtomicReferenceCounter();
        int numberOfThreads = 10;
        int incrementsPerThread = 1000;

        Thread[] threads = new Thread[numberOfThreads * 2];

        for (int i = 0; i < numberOfThreads * 2; i++) {
            threads[i] = new Thread(()->{
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.decrement();
                }
            });
        }

        for (Thread thead : threads) {
            thead.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("Final counter value: " + counter.getValue());
    }
}
