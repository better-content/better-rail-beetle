package com.bettercontent.betterrailbeetle.nativefixture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.junit.jupiter.api.Assertions.*;

class ClearWitnessScopeTest {
    private JsonObject finish(WitnessLedger ledger) {
        return JsonParser.parseString(new String(ledger.finish("pure-only-not-native"), StandardCharsets.UTF_8)).getAsJsonObject();
    }
    @Test void rejectsForeignWorldAndThreadWithoutNativeReads() throws Exception {
        var scopes = new ClearWitnessScope(new WitnessLedger(true)); var world = new Object();
        try (var ticket = scopes.enter(world)) {
            scopes.expanded(world, 0, 0, 0, 16, 16, 16);
            assertTrue(scopes.boundary(world, 1, 1, 1) > 0);
            assertEquals(-1, scopes.boundary(new Object(), 1, 1, 1));
            var other = new AtomicInteger();
            var thread = new Thread(() -> other.set(scopes.boundary(world, 1, 1, 1)));
            thread.start(); thread.join(5000); assertFalse(thread.isAlive()); assertEquals(-1, other.get());
            ticket.completed();
        }
        assertFalse(scopes.activeForTest());
    }
    @Test void bindsActualExpandedBoundsNotUnexpandedInputOrAllTerrain() {
        var scopes = new ClearWitnessScope(new WitnessLedger(true)); var world = new Object();
        try (var ticket = scopes.enter(world)) {
            assertEquals(-1, scopes.boundary(world, 1, 1, 1));
            scopes.expanded(world, -2, -3, -3, 6, 7, 7);
            assertTrue(scopes.boundary(world, -2, -3, -3) > 0);
            assertTrue(scopes.boundary(world, 6, 7, 7) > 0);
            assertEquals(-1, scopes.boundary(world, -3, -3, -3));
            assertEquals(-1, scopes.boundary(world, 6, 8, 7)); ticket.completed();
        }
    }
    @Test void finallyClearsContextAndPreservesOriginalExceptionIdentity() {
        var ledger = new WitnessLedger(true); var scopes = new ClearWitnessScope(ledger); var world = new Object();
        var original = new IllegalStateException("original-only");
        var ticket = scopes.enter(world);
        assertSame(original, assertThrows(IllegalStateException.class, () -> {
            try (ticket) { scopes.expanded(world, 0, 0, 0, 1, 1, 1); throw original; }
        }));
        assertFalse(scopes.activeForTest()); assertTrue(scopes.ticketReleasedForTest(ticket));
        assertFalse(finish(ledger).get("observation_scope_complete").getAsBoolean());
    }
    @Test void nestedScopesRestoreParentAndReleaseClosedTicketReferences() {
        var scopes = new ClearWitnessScope(new WitnessLedger(true)); var a = new Object(); var b = new Object();
        var outer = scopes.enter(a); scopes.expanded(a, 0, 0, 0, 1, 1, 1);
        var inner = scopes.enter(b); scopes.expanded(b, 0, 0, 0, 1, 1, 1);
        assertEquals(-1, scopes.boundary(a, 0, 0, 0)); inner.completed(); inner.close();
        assertTrue(scopes.ticketReleasedForTest(inner)); assertTrue(scopes.boundary(a, 0, 0, 0) > 0);
        outer.completed(); outer.close(); assertFalse(scopes.activeForTest());
        assertTrue(scopes.ticketReleasedForTest(outer)); outer.close();
    }
    @Test void chunkAndRepeatedCandidateBudgetsCannotBeRenewed() {
        var ledger = new WitnessLedger(true); var scopes = new ClearWitnessScope(ledger); var world = new Object();
        try (var ticket = scopes.enter(world)) {
            scopes.expanded(world, 0, 0, 0, 2000, 0, 0);
            for (int i = 0; i < WitnessLedger.MAX_CHUNKS_PER_BOUNDARY; i++) assertTrue(scopes.candidate(world, i * 16, 0, 0));
            assertFalse(scopes.candidate(world, WitnessLedger.MAX_CHUNKS_PER_BOUNDARY * 16, 0, 0));
            for (int i = WitnessLedger.MAX_CHUNKS_PER_BOUNDARY; i < WitnessLedger.MAX_EXISTING_POSITIONS; i++)
                assertTrue(scopes.candidate(world, 0, 0, 0));
            assertFalse(scopes.candidate(world, 0, 0, 0)); ticket.completed();
        }
        try (var next = scopes.enter(world)) {
            scopes.expanded(world, 0, 0, 0, 1, 1, 1); assertFalse(scopes.candidate(world, 0, 0, 0)); next.completed();
        }
        var json = finish(ledger); assertEquals(WitnessLedger.MAX_EXISTING_POSITIONS, json.get("examined_existing_positions").getAsInt());
        assertFalse(json.get("observation_scope_complete").getAsBoolean());
    }
    @Test void depthAndGlobalBoundaryCapsMaskRatherThanLeakParentScope() {
        var ledger = new WitnessLedger(true); var scopes = new ClearWitnessScope(ledger); var world = new Object();
        var tickets = new ArrayList<ClearWitnessScope.Ticket>();
        for (int i = 0; i < WitnessLedger.MAX_BOUNDARIES; i++) {
            tickets.add(scopes.enter(world)); scopes.expanded(world, 0, 0, 0, 1, 1, 1);
        }
        try (var rejected = scopes.enter(world)) { assertEquals(-1, scopes.boundary(world, 0, 0, 0)); }
        assertTrue(scopes.boundary(world, 0, 0, 0) > 0);
        for (int i = tickets.size() - 1; i >= 0; i--) { tickets.get(i).completed(); tickets.get(i).close(); }
        try (var rejected = scopes.enter(world)) {
            scopes.expanded(world, 0, 0, 0, 1, 1, 1); assertEquals(-1, scopes.boundary(world, 0, 0, 0)); rejected.completed();
        }
        assertFalse(scopes.activeForTest()); assertFalse(finish(ledger).get("observation_scope_complete").getAsBoolean());
    }
    @Test void disabledMissingAndDuplicateBoundsAreExplicitNotCompleteObservations() {
        var disabled = new ClearWitnessScope(new WitnessLedger(false));
        try (var token = disabled.enter(new Object())) { assertFalse(disabled.activeForTest()); }
        var ledger = new WitnessLedger(true); var scopes = new ClearWitnessScope(ledger); var world = new Object();
        try (var ticket = scopes.enter(world)) {
            scopes.expanded(world, 0, 0, 0, 1, 1, 1);
            try (var missing = scopes.enter(null)) { assertEquals(-1, scopes.boundary(world, 0, 0, 0)); }
            scopes.expanded(world, 0, 0, 0, 1, 1, 1); assertEquals(-1, scopes.boundary(world, 0, 0, 0)); ticket.completed();
        }
        assertFalse(scopes.activeForTest()); assertFalse(finish(ledger).get("observation_scope_complete").getAsBoolean());
    }
    @Test void proposedStateIsBoundedScalarAndDoesNotAlterLegacyRowSchema() {
        var ledger = new WitnessLedger(true);
        ledger.record("before_clear_mutation", 1, new WitnessLedger.Pending("DUMMY", 1, 2, 3, null),
                "Block{minecraft:campfire}", "s".repeat(900), "minecraft:full", new WitnessLedger.Slice(List.of(), false));
        var root = finish(ledger); var row = root.getAsJsonArray("records").get(0).getAsJsonObject();
        assertEquals(512, row.get("proposed_block_state").getAsString().length());
        assertFalse(root.get("observation_scope_complete").getAsBoolean());
    }
    private ClassNode readClass(String name) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class")) {
            assertNotNull(in, name); var node = new ClassNode(); new ClassReader(in).accept(node, 0); return node;
        }
    }
    @Test void actualSdkMutationPointHasAcquiredChunkAndExactlyOldStateOrdinalOne() throws Exception {
        var level = readClass("net.minecraft.world.level.Level");
        String state = "Lnet/minecraft/world/level/block/state/BlockState;";
        var method = level.methods.stream().filter(m -> m.name.equals("setBlock") && m.desc.equals(
                "(Lnet/minecraft/core/BlockPos;" + state + "II)Z")).findFirst().orElseThrow();
        var calls = new ArrayList<MethodInsnNode>();
        for (var instruction : method.instructions) if (instruction instanceof MethodInsnNode call
                && call.owner.equals("net/minecraft/world/level/chunk/LevelChunk") && call.name.equals("setBlockState")) calls.add(call);
        assertEquals(1, calls.size()); int at = method.instructions.indexOf(calls.get(0));
        var locals = method.localVariables.stream().filter(l -> l.desc.equals(state)
                && method.instructions.indexOf(l.start) <= at && method.instructions.indexOf(l.end) > at)
                .map(l -> l.index).sorted().toList();
        assertEquals(List.of(2, 8), locals);
        assertTrue(method.localVariables.stream().anyMatch(l -> l.index == 5
                && l.desc.equals("Lnet/minecraft/world/level/chunk/LevelChunk;")
                && method.instructions.indexOf(l.start) <= at && method.instructions.indexOf(l.end) > at));
    }
    @Test void compiledWrappersForwardOnceAndCaptureAddsNoNativeStateReadsOrScans() throws Exception {
        String prefix = "com.bettercontent.betterrailbeetle.nativefixture.";
        for (String name : List.of("ClearBoundaryWitnessMixin", "ClearMutationWitnessMixin", "PendingPromotionWitnessMixin")) {
            var cls = readClass(prefix + "mixin." + name);
            for (var method : cls.methods) if (method.name.startsWith("witness")) {
                assertTrue((method.access & Opcodes.ACC_PRIVATE) != 0); int original = 0;
                for (var insn : method.instructions) if (insn instanceof MethodInsnNode call
                        && call.owner.equals("com/llamalad7/mixinextras/injector/wrapoperation/Operation") && call.name.equals("call")) original++;
                assertEquals(1, original, name + ":" + method.name);
                if (method.name.equals("witnessClearScope")) assertFalse(method.tryCatchBlocks.isEmpty());
            }
        }
        var capture = readClass(prefix + "NativeWitnessCapture");
        for (var method : capture.methods) for (var insn : method.instructions) if (insn instanceof MethodInsnNode call) {
            assertFalse(Set.of("getChunk", "getChunkAt", "getChunkNow", "getChunkSource", "getBlockEntitiesPos", "getBlockState",
                    "betweenClosedStream", "getBlockEntity", "copy", "putString", "putInt", "setBlock", "setBlockState").contains(call.name), call.name);
            if (call.owner.equals("java/nio/file/Files")) assertEquals("stopped", method.name);
        }
        var mutation = readClass(prefix + "mixin.ClearMutationWitnessMixin").methods.stream()
                .filter(m -> m.name.equals("witnessBeforeClearMutation")).findFirst().orElseThrow();
        var annotations = new ArrayList<org.objectweb.asm.tree.AnnotationNode>();
        for (var table : java.util.Arrays.asList(mutation.visibleParameterAnnotations, mutation.invisibleParameterAnnotations))
            if (table != null) for (var entry : table) if (entry != null) annotations.addAll(entry);
        assertTrue(annotations.stream().anyMatch(a -> a.desc.equals("Lcom/llamalad7/mixinextras/sugar/Local;")
                && a.values.equals(List.of("ordinal", 1))));
    }
}
