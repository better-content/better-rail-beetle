package com.bettercontent.betterrailbeetle.nativefixture;

import com.bettercontent.betterrailbeetle.nativeclear.ClearReadinessPolicy;
import com.bettercontent.betterrailbeetle.nativeclear.ClearReadinessPolicy.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

class ClearReadinessTest {
    static final List<Tile> TILES=ClearReadinessPolicy.footprint(0,0,0,0);
    static class Fake implements Port {
        Set<Tile> forced=new HashSet<>(List.of(TILES.get(0))); List<String> events=new ArrayList<>();
        boolean thread=true,admitted,cleanupFault,forceFault;long time;String mode="ok";int pumps;
        final Error originalForceError=new AssertionError("native FULL force failure");
        public boolean onThread(){return thread;}public long now(){return time;}
        public boolean forced(Tile t){return forced.contains(t);}
        public void force(Tile t,boolean v){events.add("force:"+v);if(v){forced.add(t);if(forceFault)throw originalForceError;}else{if(cleanupFault){cleanupFault=false;throw new IllegalStateException("cleanup");}forced.remove(t);}}
        public void admit(){events.add("admit");admitted=true;}
        final List<CompletableFuture<Integer>> futures=new ArrayList<>();
        public Stage snapshot(Tile t){assertTrue(admitted);events.add("snapshot");var f=new CompletableFuture<Integer>();futures.add(f);if(mode.equals("right"))f.complete(-1);return new Stage(){
            public boolean done(){if(!f.isDone())return false;int value=f.getNow(0);if(value<0)throw new IllegalStateException("initial Right UNLOADED");return true;}
            public void verify(){if(!done()||mode.equals("stale"))throw new IllegalStateException("stale holder/future/world/key/status");}
        };}
        public void pump(BooleanSupplier stop){pumps++;events.add("pump");
            if(mode.equals("early"))return;
            if(mode.equals("timeout"))time=ClearReadinessPolicy.BUDGET_NANOS;
            else for(var f:futures)if(mode.equals("exception"))f.completeExceptionally(new IllegalStateException("native future failure"));else f.complete(mode.equals("right")?-1:1);
            assertTrue(stop.getAsBoolean());
        }
    }
    @Test void exactInflatedInclusiveFootprintNegativeCoordinatesAndMissingForceEdges(){
        var camp=ClearReadinessPolicy.footprint(197,30,244,77);
        assertEquals(20,camp.size());assertTrue(camp.contains(new Tile(15,5)));
        assertFalse(new HashSet<>(camp).stream().allMatch(t->t.x()>=11&&t.x()<=15&&t.z()>=0&&t.z()<=4));
        assertEquals(Set.of(new Tile(-1,-1),new Tile(-1,0),new Tile(0,-1),new Tile(0,0)),new HashSet<>(TILES));
        assertThrows(IllegalArgumentException.class,()->ClearReadinessPolicy.footprint(1,0,0,0));
        assertThrows(IllegalArgumentException.class,()->ClearReadinessPolicy.footprint(0,0,1000,1000));
        assertThrows(ArithmeticException.class,()->ClearReadinessPolicy.footprint(Integer.MIN_VALUE,0,1,1));
    }
    @Test void forceMissingOnlyAdmitBeforeSnapshotPumpAndPreserveOriginalForcedSet(){
        var p=new Fake();var original=Set.copyOf(p.forced);
        try(var lease=ClearReadinessPolicy.prepare(p,TILES)){assertEquals(new HashSet<>(TILES),p.forced);assertEquals(3,p.events.stream().filter("force:true"::equals).count());assertTrue(p.events.indexOf("admit")<p.events.indexOf("snapshot"));assertEquals(1,p.pumps);}
        assertEquals(original,p.forced);assertEquals(3,p.events.stream().filter("force:false"::equals).count());
    }
    @Test void initialRightExceptionalStaleEarlyAndTimeoutAllRejectWithoutRetry(){
        for(String mode:List.of("right","exception","stale","early","timeout")) {
            var p=new Fake();p.mode=mode;var original=Set.copyOf(p.forced);
            assertThrows(RuntimeException.class,()->{try(var lease=ClearReadinessPolicy.prepare(p,TILES)){fail("original clear must not run");}});
            assertEquals(original,p.forced,mode);assertEquals(mode.equals("right")?0:1,p.pumps,mode);
        }
    }
    @Test void oneBudgetStartsBeforeForcingAndWrongThreadDoesNotMutate(){
        var p=new Fake(){@Override public void force(Tile t,boolean v){super.force(t,v);if(v)time+=ClearReadinessPolicy.BUDGET_NANOS;}};
        assertThrows(IllegalStateException.class,()->ClearReadinessPolicy.prepare(p,TILES));assertFalse(p.admitted);assertEquals(Set.of(TILES.get(0)),p.forced);
        var wrong=new Fake();wrong.thread=false;assertThrows(IllegalStateException.class,()->ClearReadinessPolicy.prepare(wrong,TILES));assertTrue(wrong.events.isEmpty());
    }
    @Test void partialAdmissionFullFailureJournalRestoresAndPreservesThrowable(){
        var p=new Fake();p.forceFault=true;
        assertSame(p.originalForceError,assertThrows(Error.class,()->ClearReadinessPolicy.prepare(p,TILES)));
        assertEquals(Set.of(TILES.get(0)),p.forced);assertFalse(p.admitted);
    }
    @Test void originalThrowableIdentityAndCleanupSuppressedNotHidden(){
        var p=new Fake();var original=new AssertionError("original clear");
        assertSame(original,assertThrows(Error.class,()->{try(var lease=ClearReadinessPolicy.prepare(p,TILES)){throw original;}}));
        assertEquals(Set.of(TILES.get(0)),p.forced);
        var q=new Fake();var failure=new IllegalStateException("original clear failure");
        assertSame(failure,assertThrows(IllegalStateException.class,()->{try(var lease=ClearReadinessPolicy.prepare(q,TILES)){q.cleanupFault=true;throw failure;}}));
        assertEquals(1,failure.getSuppressed().length);assertEquals(3,q.events.stream().filter("force:false"::equals).count());
    }
    @Test void reentrantSetupRejectsAndLeaseIsIdempotent(){
        var p=new Fake();var lease=ClearReadinessPolicy.prepare(p,TILES);
        assertThrows(IllegalStateException.class,()->ClearReadinessPolicy.prepare(new Fake(),TILES));
        lease.close();lease.close();assertEquals(3,p.events.stream().filter("force:false"::equals).count());
        try(var next=ClearReadinessPolicy.prepare(new Fake(),TILES)){} // guard reset even after preceding cleanup
    }
}
