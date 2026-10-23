package com.bettercontent.betterrailbeetle.compat.alexscaves;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SerializedBiomeConfigIoTest {
    @Test void twoActualCallersCannotOverlapAndNeitherInvocationIsDropped() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        var secondEntered = new CountDownLatch(1);
        var calls = new AtomicInteger();
        Thread first = new Thread(() -> SerializedBiomeConfigIo.call(() -> {
            calls.incrementAndGet(); entered.countDown(); await(release); return null;
        }), "config-main-contract");
        Thread second = new Thread(() -> {
            secondStarted.countDown();
            SerializedBiomeConfigIo.call(() -> { calls.incrementAndGet(); secondEntered.countDown(); return null; });
        }, "config-watcher-contract");
        var uncaught = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        first.setUncaughtExceptionHandler((thread, failure) -> uncaught.compareAndSet(null, failure));
        second.setUncaughtExceptionHandler((thread, failure) -> uncaught.compareAndSet(null, failure));
        first.setDaemon(true); second.setDaemon(true);
        try {
            first.start(); assertTrue(entered.await(5, TimeUnit.SECONDS));
            second.start(); assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (secondEntered.getCount() != 0 && second.getState() != Thread.State.WAITING
                    && second.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline)
                Thread.yield();
            assertTrue(second.getState() == Thread.State.WAITING || second.getState() == Thread.State.BLOCKED,
                    "second caller must actually contend, not merely remain unscheduled");
            assertEquals(1, secondEntered.getCount(), "original callback must not enter while first holds the lock");
        } finally {
            release.countDown(); first.join(5000); second.join(5000);
        }
        assertFalse(first.isAlive()); assertFalse(second.isAlive()); assertNull(uncaught.get());
        assertEquals(2, calls.get()); assertEquals(0, secondEntered.getCount());
    }

    @Test void everyConcurrentCallerGetsExactlyItsOriginalResult() throws Exception {
        var pool = Executors.newFixedThreadPool(2, r -> { var t = new Thread(r); t.setDaemon(true); return t; });
        var calls = new AtomicInteger();
        var active = new AtomicInteger();
        var maximum = new AtomicInteger();
        var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
        try {
            for (int i = 0; i < 100; i++) {
                int result = i;
                futures.add(pool.submit(() -> SerializedBiomeConfigIo.call(() -> {
                    calls.incrementAndGet(); maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
                    try { Thread.yield(); return result; } finally { active.decrementAndGet(); }
                })));
            }
            for (int i = 0; i < futures.size(); i++) assertEquals(i, futures.get(i).get(5, TimeUnit.SECONDS));
            assertEquals(100, calls.get()); assertEquals(1, maximum.get()); assertEquals(0, active.get());
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS)); }
    }

    @Test void recursiveOriginalIsReentrantAndEachCallRunsOnce() {
        var calls = new AtomicInteger();
        int result = SerializedBiomeConfigIo.call(() -> {
            calls.incrementAndGet();
            return SerializedBiomeConfigIo.call(() -> { calls.incrementAndGet(); return 42; });
        });
        assertEquals(42, result); assertEquals(2, calls.get());
    }

    @Test void originalFailureIsUnchangedAndFinallyReleasesToAnotherThread() throws Exception {
        var failure = new IllegalStateException("original failure");
        var calls = new AtomicInteger();
        assertSame(failure, assertThrows(IllegalStateException.class, () -> SerializedBiomeConfigIo.call(() -> {
            calls.incrementAndGet(); throw failure;
        })));
        var pool = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r); t.setDaemon(true); return t; });
        try {
            assertEquals(42, pool.submit(() -> SerializedBiomeConfigIo.call(() -> {
                calls.incrementAndGet(); return 42;
            })).get(5, TimeUnit.SECONDS));
            assertEquals(2, calls.get());
        } finally { pool.shutdownNow(); }
    }

    private static void await(CountDownLatch latch) {
        try { if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("contract latch timed out"); }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
    }
}
