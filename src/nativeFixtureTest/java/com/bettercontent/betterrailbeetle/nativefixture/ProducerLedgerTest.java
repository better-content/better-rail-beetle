package com.bettercontent.betterrailbeetle.nativefixture;

import com.bettercontent.betterrailbeetle.nativeclear.ProducerLedger;
import com.bettercontent.betterrailbeetle.nativeclear.ProducerPalette;
import java.util.IdentityHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProducerLedgerTest {
    @Test void immutableIdentityPaletteIncludesWholeSelectionAndSchemaInDigest() {
        Object a = new Object(), b = new Object();
        var map = new IdentityHashMap<Object,ProducerPalette.State>();
        map.put(a,new ProducerPalette.State(0,"minecraft:sign[rotation=3]"));
        var palette = new ProducerPalette(map,List.of("minecraft:sign","other:entity_block"),true);
        map.clear(); assertNotNull(palette.lookup(a)); assertNull(palette.lookup(b));
        assertEquals(64,palette.digest().length()); assertEquals(64,palette.schemaDigest().length());
        assertEquals(2,palette.selectedCount());
        var changed = new IdentityHashMap<Object,ProducerPalette.State>();
        changed.put(a,new ProducerPalette.State(0,"minecraft:sign[rotation=4]"));
        assertNotEquals(palette.digest(),new ProducerPalette(changed,List.of("minecraft:sign","other:entity_block"),true).digest());
        assertNotEquals(palette.digest(),new ProducerPalette(changed,List.of("minecraft:sign"),true).digest());
    }
    @Test void nestedScopesBindSuppliedScalarsParentAndBalance() {
        var ledger = new ProducerLedger();
        long force=ledger.enter("force",7,91,92,0,220,-60,136,220,-60,136);
        long clear=ledger.enter("clear",7,91,93,-60,218,-63,133,270,7,186);
        long seq=ledger.reserve(false);
        ledger.record(seq,7,"LevelChunk",259,-30,165,"old","air",pending(),List.of(),false);
        ledger.exit(clear,7); ledger.exit(force,7);
        var summary=ledger.finish(true); var entry=summary.entries().get(0);
        assertEquals(force,entry.activeForce().sequence()); assertEquals(force,entry.activeClear().parent());
        assertEquals(93,entry.activeClear().argumentIdentityHash()); assertEquals(91,entry.activeClear().worldIdentityHash());
        assertEquals(218,entry.activeClear().x0()); assertEquals(186,entry.activeClear().z1());
        assertTrue(summary.observationComplete()); assertNull(ledger.finish(true));
    }
    @Test void crossThreadDoesNotInventScopeAssociation() {
        var ledger = new ProducerLedger(); long scope=ledger.enter("force",1,1,2,0,0,0,0,0,0,0);
        long ticket=ledger.reserve(false); ledger.record(ticket,2,"ProtoChunk",1,2,3,"old","air",pending(),List.of(),false);
        assertNull(ledger.finish(true).entries().get(0).activeForce());
        assertTrue(scope>0);
    }
    @Test void balanceFailureAndDanglingCannotBecomeComplete() {
        var ledger = new ProducerLedger(); long a=ledger.enter("force",1,1,2,0,0,0,0,0,0,0);
        ledger.enter("clear",1,1,3,0,0,0,0,0,0,0); ledger.exit(a,1);
        var result=ledger.finish(true); assertEquals(1,result.balanceFailures()); assertEquals(2,result.danglingDepth()); assertFalse(result.observationComplete());
    }
    @Test void boundedDepthAndScopePopulationFailExplicitly() {
        var ledger = new ProducerLedger();
        for(int i=0;i<ProducerLedger.MAX_DEPTH;i++) assertTrue(ledger.enter("force",1,1,2,0,0,0,0,0,0,0)>0);
        assertEquals(0,ledger.enter("force",1,1,2,0,0,0,0,0,0,0)); assertFalse(ledger.finish(true).observationComplete());
        var other=new ProducerLedger();
        for(int i=0;i<ProducerLedger.MAX_SCOPES;i++) {long s=other.enter("force",1,1,2,0,0,0,0,0,0,0);other.exit(s,1);}
        assertEquals(0,other.enter("force",1,1,2,0,0,0,0,0,0,0)); assertEquals(1,other.finish(true).scopeOverflow());
    }
    @Test void candidatesFailuresUnknownAndPaletteTruncationFailCompleteness() {
        var ledger=new ProducerLedger();
        for(int i=0;i<9;i++) {long seq=ledger.reserve(i==0); if(seq!=0)ledger.record(seq,1,"LevelChunk",1,2,3,"UNKNOWN","air",pending(),List.of(),false);}
        ledger.failed(); var result=ledger.finish(false);
        assertEquals(8,result.entries().size()); assertEquals(1,result.budgetDropped()); assertEquals(1,result.unknownStates()); assertEquals(1,result.captureFailures()); assertFalse(result.observationComplete());
        var fresh=new ProducerLedger(); assertFalse(fresh.finish(false).observationComplete());
    }
    @Test void recordShapeAndFrameCapsRejectInvalidMetadata() {
        assertThrows(IllegalArgumentException.class,()->new ProducerLedger.Pending("DUMMY",1,2,3,null,java.util.Collections.nCopies(33,"x:3"),false));
        assertThrows(IllegalArgumentException.class,()->new ProducerLedger.Entry(1,1,"LevelChunk",1,2,3,"old","new",pending(),null,null,null,java.util.Collections.nCopies(25,new StackTraceElement("A","b","A.java",1)),false));
    }
    private ProducerLedger.Pending pending() {return new ProducerLedger.Pending("DUMMY",259,-30,165,null,List.of("id:8","x:3","y:3","z:3"),false);}
}
