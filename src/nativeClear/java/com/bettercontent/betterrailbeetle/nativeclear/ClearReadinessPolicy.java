package com.bettercontent.betterrailbeetle.nativeclear;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** JDK-only bounded setup protocol. Native calls belong exclusively to the typed adapter. */
public final class ClearReadinessPolicy {
    public static final long BUDGET_NANOS = 30_000_000_000L;
    public static final int MAX_CHUNKS = 64;
    private static final ThreadLocal<Boolean> ACTIVE=ThreadLocal.withInitial(()->false);
    public record Tile(int x, int z) {}
    public interface Stage { boolean done(); void verify(); }
    public interface Port {
        boolean onThread(); long now(); boolean forced(Tile tile); void force(Tile tile, boolean value);
        void admit(); Stage snapshot(Tile tile); void pump(BooleanSupplier stop);
    }
    public static List<Tile> footprint(int minX, int minZ, int maxX, int maxZ) {
        if (minX > maxX || minZ > maxZ) throw new IllegalArgumentException("Inverted native clear bounds");
        int x0=Math.floorDiv(Math.subtractExact(minX,2),16), x1=Math.floorDiv(Math.addExact(maxX,3),16);
        int z0=Math.floorDiv(Math.subtractExact(minZ,3),16), z1=Math.floorDiv(Math.addExact(maxZ,3),16);
        if ((long)(x1-x0+1)*(z1-z0+1)>MAX_CHUNKS) throw new IllegalArgumentException("Unsupported native clear footprint >64 chunks");
        var tiles=new ArrayList<Tile>();
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)tiles.add(new Tile(x,z));
        return List.copyOf(tiles);
    }
    public static Lease prepare(Port port, List<Tile> tiles) {
        if(!port.onThread())throw new IllegalStateException("Native clear readiness on wrong thread");
        if(tiles.isEmpty()||tiles.size()>MAX_CHUNKS)throw new IllegalArgumentException("Unsupported native clear footprint");
        if(ACTIVE.get())throw new IllegalStateException("Reentrant native clear readiness is unsupported");
        long start=port.now();
        var lease=new Lease(port);
        ACTIVE.set(true);
        try {
            for(var tile:tiles) {
                expired(port,start);
                if(!port.forced(tile)) { lease.added.add(tile); port.force(tile,true); }
                if(!port.forced(tile))throw new IllegalStateException("Native forced ticket not present");
            }
            expired(port,start);
            port.admit(); // real ticket propagation AND holder.updateFutures before selecting futures
            var stages=new ArrayList<Stage>();
            for(var tile:tiles) { expired(port,start); var stage=port.snapshot(tile); stage.done(); stages.add(stage); }
            port.pump(()->port.now()-start>=BUDGET_NANOS || stages.stream().allMatch(Stage::done));
            expired(port,start);
            for(var stage:stages) { if(!stage.done())throw new IllegalStateException("Native pump returned before readiness"); stage.verify(); }
            for(var tile:tiles)if(!port.forced(tile))throw new IllegalStateException("Stale forced admission");
            expired(port,start);
            return lease;
        } catch(Throwable failure) {
            try { lease.close(); } catch(Throwable cleanup) { if(cleanup!=failure)failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    private static void expired(Port port,long start) {
        if(port.now()-start>=BUDGET_NANOS)throw new IllegalStateException("Native clear TICKING readiness exceeded single 30s setup budget");
        if(!port.onThread())throw new IllegalStateException("Native clear readiness changed thread");
    }
    public static final class Lease implements AutoCloseable {
        private final Port port;
        private final List<Tile> added=new ArrayList<>();
        private boolean closed;
        private Lease(Port port){this.port=port;}
        @Override public void close() {
            if(closed)return;
            if(!port.onThread())throw new IllegalStateException("Native clear ticket cleanup on wrong thread");
            closed=true;
            Throwable failure=null;
            for(int i=added.size()-1;i>=0;i--)try {
                var tile=added.get(i);
                if(!port.forced(tile))throw new IllegalStateException("Native readiness ticket ownership lost");
                port.force(tile,false);
                if(port.forced(tile))throw new IllegalStateException("Native readiness ticket release failed");
            } catch(Throwable cleanup) { if(failure==null)failure=cleanup;else if(failure!=cleanup)failure.addSuppressed(cleanup); }
            ACTIVE.remove();
            if(failure instanceof RuntimeException r)throw r;
            if(failure instanceof Error e)throw e;
            if(failure!=null)throw new IllegalStateException("Native readiness cleanup threw checked failure",failure);
        }
    }
    private ClearReadinessPolicy() {}
}
