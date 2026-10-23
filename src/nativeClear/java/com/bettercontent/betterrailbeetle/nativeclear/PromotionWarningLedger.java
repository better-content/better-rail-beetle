package com.bettercontent.betterrailbeetle.nativeclear;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Scalar-only, bounded warning evidence. No world access, logging or I/O. */
public final class PromotionWarningLedger {
    public static final int MAX_RECORDS = 4;
    public static final int MAX_FRAMES = 24;
    public record Pending(String id, Integer x, Integer y, Integer z, Boolean keepPacked) {}
    public record Entry(int x, int y, int z, Pending pending, List<StackTraceElement> stack, boolean stackTruncated) {
        public Entry {
            stack = List.copyOf(stack);
            if (stack.size() > MAX_FRAMES) throw new IllegalArgumentException("Unbounded warning stack");
            if (pending.id().length() > 128) throw new IllegalArgumentException("Unbounded scalar id");
        }
    }
    public record Snapshot(long warnings, long captureFailures, long budgetDropped, boolean closed, List<Entry> entries) {}
    private long warnings;
    private long captureFailures;
    private boolean closed;
    private final List<Entry> entries = new ArrayList<>(MAX_RECORDS);

    /** Never allow observer failures to change an original logger result/exception. */
    public synchronized void observe(Supplier<Entry> capture) {
        if (closed) return;
        if (warnings < Long.MAX_VALUE) warnings++;
        // Includes failed attempts: at most four scalar/stack acquisitions, not retry-until-full.
        if (warnings > MAX_RECORDS) return;
        try {
            entries.add(java.util.Objects.requireNonNull(capture.get()));
        } catch (Throwable observerFailure) {
            captureFailures++;
        }
    }

    /** Called only after native setup/test loops, at ServerStoppedEvent. */
    public synchronized Snapshot finish() {
        if (closed) return null;
        closed = true;
        return new Snapshot(warnings, captureFailures, Math.max(0, warnings - MAX_RECORDS), true, List.copyOf(entries));
    }
}
