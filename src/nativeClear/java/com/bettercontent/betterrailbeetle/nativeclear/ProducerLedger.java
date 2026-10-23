package com.bettercontent.betterrailbeetle.nativeclear;

import java.util.ArrayList;
import java.util.List;

/** All retained data are scalar/immutable; no native owner, argument, state or tag is retained. */
public final class ProducerLedger {
    public static final int MAX_RECORDS = 8, MAX_FRAMES = 24, MAX_DEPTH = 16, MAX_SCOPES = 256;
    public record Scope(String kind, long sequence, long parent, long thread, int depth,
                        int worldIdentityHash, int argumentIdentityHash, int floor,
                        int x0, int y0, int z0, int x1, int y1, int z1) {}
    public record Pending(String id, Integer x, Integer y, Integer z, Boolean keepPacked,
                          List<String> keyTypes, boolean keysTruncated) {
        public Pending { keyTypes = List.copyOf(keyTypes); if (keyTypes.size() > 32) throw new IllegalArgumentException("keys cap"); }
    }
    public record Entry(long sequence, long thread, String ownerKind, int x, int y, int z,
                        String oldState, String requestedState, Pending pending,
                        Scope activeForce, Scope activeClear, Scope lastCompletedForce,
                        List<StackTraceElement> stack, boolean stackTruncated) {
        public Entry { stack = List.copyOf(stack); if (stack.size() > MAX_FRAMES) throw new IllegalArgumentException("stack cap"); }
    }
    public record Summary(long candidates, long budgetDropped, long unknownStates, long captureFailures,
                          int scopes, int scopeOverflow, int crossThreadScopes, int balanceFailures,
                          int danglingDepth, long unboundTransitions, boolean observationComplete, List<Entry> entries) {}
    private final Scope[] stack = new Scope[MAX_DEPTH];
    private final List<Entry> entries = new ArrayList<>(MAX_RECORDS);
    private long ownerThread, sequence, candidates, unknownStates, captureFailures, unboundTransitions;
    private int depth, scopes, scopeOverflow, crossThreadScopes, balanceFailures;
    private Scope lastForce;
    private boolean closed;

    public synchronized long enter(String kind, long thread, int worldHash, int argumentHash, int floor,
                                   int x0, int y0, int z0, int x1, int y1, int z1) {
        if (closed) return 0;
        if (ownerThread == 0) ownerThread = thread;
        if (ownerThread != thread) { crossThreadScopes++; return 0; }
        if (depth >= MAX_DEPTH || scopes >= MAX_SCOPES) { scopeOverflow++; return 0; }
        long id = ++sequence;
        stack[depth] = new Scope(kind, id, depth == 0 ? 0 : stack[depth-1].sequence(), thread,
                depth+1, worldHash, argumentHash, floor, x0, y0, z0, x1, y1, z1);
        depth++; scopes++;
        return id;
    }
    public synchronized void exit(long ticket, long thread) {
        if (ticket == 0 || closed) return;
        if (thread != ownerThread || depth == 0 || stack[depth-1].sequence() != ticket) { balanceFailures++; return; }
        Scope popped = stack[--depth]; stack[depth] = null;
        if (popped.kind().equals("force")) lastForce = popped;
    }
    public synchronized long reserve(boolean unknown) {
        if (closed) return 0;
        if (candidates < Long.MAX_VALUE) candidates++;
        if (unknown && unknownStates < Long.MAX_VALUE) unknownStates++;
        return candidates <= MAX_RECORDS ? ++sequence : 0;
    }
    public synchronized void failed() { if (!closed && captureFailures < Long.MAX_VALUE) captureFailures++; }
    public synchronized void record(long ticket, long thread, String ownerKind, int x, int y, int z,
                                    String oldState, String requested, Pending pending,
                                    List<StackTraceElement> frames, boolean truncated) {
        if (closed || ticket == 0 || entries.size() >= MAX_RECORDS) return;
        Scope force = null, clear = null;
        if (thread == ownerThread) for (int i=0; i<depth; i++) {
            if (stack[i].kind().equals("force")) force = stack[i];
            if (stack[i].kind().equals("clear")) clear = stack[i];
        }
        if (force == null && clear == null) unboundTransitions++;
        entries.add(new Entry(ticket, thread, ownerKind, x,y,z,oldState,requested,pending,
                force,clear,thread == ownerThread ? lastForce : null,frames,truncated));
    }
    public synchronized Summary finish(boolean paletteComplete) {
        if (closed) return null;
        closed = true;
        return new Summary(candidates,Math.max(0,candidates-MAX_RECORDS),unknownStates,captureFailures,
                scopes,scopeOverflow,crossThreadScopes,balanceFailures,depth,unboundTransitions,
                paletteComplete && scopes>0 && unboundTransitions==0 && candidates <= MAX_RECORDS && unknownStates==0 && captureFailures==0
                        && scopeOverflow==0 && crossThreadScopes==0 && balanceFailures==0 && depth==0,
                List.copyOf(entries));
    }
}
