package com.bettercontent.betterrailbeetle.nativefixture;

import java.util.HashSet;
import java.util.Set;

/** Synchronous-only identity/bounds context. No native reference enters the scalar ledger. */
public final class ClearWitnessScope {
    private final WitnessLedger ledger;
    private final ThreadLocal<Scope> active = new ThreadLocal<>();
    private final ThreadLocal<Integer> maskedDepth = new ThreadLocal<>();
    private static final class Scope {
        Object world;
        Scope parent;
        final Thread thread = Thread.currentThread();
        final int boundary, depth;
        final Set<Long> chunks = new HashSet<>();
        int minX, minY, minZ, maxX, maxY, maxZ;
        boolean expanded, completed;
        Scope(Object world, Scope parent, int boundary) {
            this.world = world; this.parent = parent; this.boundary = boundary;
            this.depth = parent == null ? 1 : parent.depth + 1;
        }
    }
    public final class Ticket implements AutoCloseable {
        private Scope scope;
        private boolean masked;
        private Thread ownerThread = Thread.currentThread();
        private Ticket(Scope scope) { this.scope = scope; }
        private Ticket(boolean masked) { this.masked = masked; }
        public void completed() {
            if (scope != null && scope.thread == Thread.currentThread()) scope.completed = true;
        }
        @Override public void close() {
            if (ownerThread == null) return;
            if (ownerThread != Thread.currentThread()) { ledger.incompleteScope(); return; }
            if (masked) {
                int depth = maskedDepth.get();
                if (depth == 1) maskedDepth.remove(); else maskedDepth.set(depth - 1);
                masked = false; ownerThread = null; return;
            }
            Scope current = scope;
            if (current == null) { ownerThread = null; return; }
            try {
                if (!current.completed || !current.expanded) ledger.incompleteScope();
                if (active.get() == current) {
                    if (current.parent == null) active.remove(); else active.set(current.parent);
                } else {
                    // Impossible in the owning try/finally path. Fail closed without masking its exception.
                    ledger.incompleteScope();
                    Scope leaked = active.get();
                    while (leaked != null) {
                        Scope parent = leaked.parent; leaked.world = null; leaked.parent = null;
                        leaked.chunks.clear(); leaked = parent;
                    }
                    active.remove();
                }
            } finally {
                current.world = null; current.parent = null; current.chunks.clear(); scope = null; ownerThread = null;
            }
        }
    }
    public ClearWitnessScope(WitnessLedger ledger) { this.ledger = ledger; }
    public Ticket enter(Object world) {
        if (!ledger.enabled()) return new Ticket((Scope) null);
        Scope parent = active.get();
        if (world == null || maskedDepth.get() != null
                || (parent != null && parent.depth >= WitnessLedger.MAX_BOUNDARIES)) {
            Integer depth = maskedDepth.get(); maskedDepth.set(depth == null ? 1 : depth + 1);
            ledger.incompleteScope(); return new Ticket(true);
        }
        int boundary = ledger.beginBoundary();
        // Keep a rejecting child even when the global cap is reached; never fall through to its parent.
        Scope scope = new Scope(world, parent, boundary); active.set(scope); return new Ticket(scope);
    }
    public void expanded(Object world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        Scope scope = active.get();
        if (maskedDepth.get() != null || scope == null || scope.world != world || scope.boundary < 0) return;
        if (scope.expanded || minX > maxX || minY > maxY || minZ > maxZ) {
            ledger.incompleteScope(); scope.expanded = false; return;
        }
        scope.minX = minX; scope.minY = minY; scope.minZ = minZ;
        scope.maxX = maxX; scope.maxY = maxY; scope.maxZ = maxZ; scope.expanded = true;
    }
    public int boundary(Object world, int x, int y, int z) {
        Scope scope = active.get();
        return maskedDepth.get() == null && scope != null && scope.world == world && scope.expanded && scope.boundary > 0
                && x >= scope.minX && x <= scope.maxX && y >= scope.minY && y <= scope.maxY
                && z >= scope.minZ && z <= scope.maxZ ? scope.boundary : -1;
    }
    /** Counts supplied chunks/eligible native candidate calls, not a voxel scan or pending absence proof. */
    public boolean candidate(Object world, int x, int y, int z) {
        if (boundary(world, x, y, z) < 0) return false;
        Scope scope = active.get(); long chunk = ((long) (x >> 4) << 32) ^ ((z >> 4) & 0xffffffffL);
        if (!scope.chunks.contains(chunk)) {
            if (scope.chunks.size() == WitnessLedger.MAX_CHUNKS_PER_BOUNDARY) {
                ledger.chunkOverflow(1); return false;
            }
            scope.chunks.add(chunk);
        }
        return ledger.examinePosition();
    }
    boolean activeForTest() { return active.get() != null || maskedDepth.get() != null; }
    boolean ticketReleasedForTest(Ticket ticket) { return ticket.scope == null && !ticket.masked; }
}
