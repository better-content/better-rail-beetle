package com.bettercontent.betterrailbeetle.nativefixture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.junit.jupiter.api.Assertions.*;

class WitnessLedgerTest {
    private static final WitnessLedger.Pending PENDING = new WitnessLedger.Pending("DUMMY", 246, -12, 229, false);
    private static final WitnessLedger.Slice CALLERS = new WitnessLedger.Slice(List.of(
            new WitnessLedger.Callsite("net.minecraft.world.level.chunk.LevelChunk", "promotePendingBlockEntity", 521)), false);
    private JsonObject finish(WitnessLedger ledger) {
        return JsonParser.parseString(new String(ledger.finish(UUID.randomUUID().toString()), StandardCharsets.UTF_8)).getAsJsonObject();
    }
    @Test void disabledCapturesNothingAndHasNoOutput() {
        var ledger = new WitnessLedger(false);
        assertEquals(-1, ledger.beginBoundary()); assertFalse(ledger.examinePosition());
        ledger.record(null, 0, null, null, null, null);
        assertEquals(0, ledger.finish("unused").length);
    }
    @Test void scalarsOnlyAndCallerTailIsExplicit() {
        var ledger = new WitnessLedger(true);
        ledger.record("before_promotion", 0, PENDING, "Block{minecraft:air}", "minecraft:full", new WitnessLedger.Slice(CALLERS.frames(), true));
        var root = finish(ledger); var row = root.getAsJsonArray("records").get(0).getAsJsonObject();
        assertEquals(Set.of("phase", "boundary", "id", "x", "y", "z", "keepPacked", "nbt_coordinates_present",
                "block_state", "chunk_status", "callsites", "callsite_slice_limited"), row.keySet());
        assertEquals(Set.of("class", "method", "line"), row.getAsJsonArray("callsites").get(0).getAsJsonObject().keySet());
        assertTrue(row.get("callsite_slice_limited").getAsBoolean());
        assertTrue(root.get("observation_scope_complete").getAsBoolean()); // finite-prefix scope, not a full stack claim
        assertTrue(root.get("truncated_or_missing").getAsBoolean()); // omitted caller tail is never hidden
    }
    @Test void recordAndByteOverflowAreRetainedAsIncomplete() {
        var ledger = new WitnessLedger(true);
        var bigStack = new WitnessLedger.Slice(java.util.Collections.nCopies(WitnessLedger.MAX_CALLSITES,
                new WitnessLedger.Callsite("a".repeat(128), "b".repeat(96), 1)), true);
        for (int i = 0; i < WitnessLedger.MAX_RECORDS + 1; i++)
            ledger.record("before_clear", 1, PENDING, "s".repeat(512), "minecraft:full", bigStack);
        byte[] bytes = ledger.finish(UUID.randomUUID().toString());
        assertTrue(bytes.length <= WitnessLedger.MAX_BYTES);
        var root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(1, root.get("overflow_records").getAsInt());
        assertTrue(root.get("byte_dropped_records").getAsInt() > 0);
        assertFalse(root.get("observation_scope_complete").getAsBoolean());
        assertThrows(IllegalStateException.class, () -> ledger.finish("cannot-reseal"));
    }
    @Test void missingLoadedChunksPositionsAndCoordinatesCannotBecomeComplete() {
        var ledger = new WitnessLedger(true); ledger.missingChunk();
        for (int i = 0; i < WitnessLedger.MAX_EXISTING_POSITIONS; i++) assertTrue(ledger.examinePosition());
        assertFalse(ledger.examinePosition());
        ledger.record("before_promotion", 0, new WitnessLedger.Pending("DUMMY", null, null, null, null),
                "Block{minecraft:air}", "minecraft:full", CALLERS);
        var root = finish(ledger); assertFalse(root.get("observation_scope_complete").getAsBoolean());
        assertEquals(1, root.get("missing_loaded_chunks").getAsInt());
        assertEquals(1, root.get("position_scan_truncated_boundaries").getAsInt());
        assertTrue(root.getAsJsonArray("records").get(0).getAsJsonObject().get("x").isJsonNull());
    }
    @Test void stringsAndBoundariesAreBoundedWithoutHidingTruncation() {
        var ledger = new WitnessLedger(true);
        for (int i = 0; i < WitnessLedger.MAX_BOUNDARIES; i++) assertTrue(ledger.beginBoundary() > 0);
        assertEquals(-1, ledger.beginBoundary());
        ledger.record("before_clear", 1, new WitnessLedger.Pending("invalid/private/id", 1, 2, 3, null),
                "s".repeat(900), "minecraft:full", CALLERS);
        var root = finish(ledger); assertTrue(root.get("truncated_or_missing").getAsBoolean());
        assertFalse(root.toString().contains("invalid/private/id"));
        assertEquals(512, root.getAsJsonArray("records").get(0).getAsJsonObject().get("block_state").getAsString().length());
    }
    @Test void archiveValidatorRejectsFixtureInsteadOfFilteringIt() throws Exception {
        Path base = Path.of(System.getProperty("bc.nativeWitness.testsRoot", "build/native-witness-unit"), UUID.randomUUID().toString());
        Files.createDirectories(base);
        Path good = base.resolve("control-only-not-production.zip"), bad = base.resolve("negative-only-not-production.zip");
        for (Path path : List.of(good, bad)) try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
            zip.putNextEntry(new ZipEntry(path.equals(good) ? "ordinary.txt" : "com/bettercontent/betterrailbeetle/nativefixture/Marker.class"));
            zip.write(1); zip.closeEntry();
        }
        WitnessArchiveGuard.check(good);
        assertThrows(IllegalStateException.class, () -> WitnessArchiveGuard.check(bad));
        assertTrue(Files.exists(bad)); // retained, not cleaned or rewritten
    }
    private ClassNode readClass(String name) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class")) {
            assertNotNull(input); var result = new ClassNode(); new ClassReader(input).accept(result, 0); return result;
        }
    }
    @Test void actualCompiledCaptureHasNoPayloadSerializationNewLoadsOrWorldWrites() throws Exception {
        var capture = readClass("com.bettercontent.betterrailbeetle.nativefixture.NativeWitnessCapture");
        assertTrue(capture.fields.stream().noneMatch(field -> field.desc.contains("net/minecraft/")));
        for (var method : capture.methods) for (var instruction : method.instructions) {
            if (method.name.equals("capture") && instruction instanceof org.objectweb.asm.tree.LdcInsnNode literal && literal.cst instanceof String key)
                assertTrue(Set.of("id", "x", "y", "z", "keepPacked").contains(key), key);
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.owner.equals("net/minecraft/nbt/CompoundTag"))
                assertTrue(Set.of("contains", "getString", "getInt", "getBoolean").contains(call.name), call.name);
            assertFalse(Set.of("getChunk", "getBlockEntity", "setBlock", "setBlockState", "removeBlockEntity", "setBlockEntity", "saveWithFullMetadata").contains(call.name), call.name);
            assertFalse(call.name.equals("getFileName") && call.owner.contains("Stack"));
            if (!method.name.equals("stopped")) assertFalse(call.owner.equals("java/nio/file/Files"));
        }
        var wrapper = readClass("com.bettercontent.betterrailbeetle.nativefixture.mixin.ClearBoundaryWitnessMixin");
        var handler = wrapper.methods.stream().filter(method -> method.name.equals("witnessExpandedBoundary")).findFirst().orElseThrow();
        assertEquals(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, handler.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC));
        int originalCalls = 0;
        for (var instruction : handler.instructions) if (instruction instanceof MethodInsnNode call && call.owner.equals("com/llamalad7/mixinextras/injector/wrapoperation/Operation") && call.name.equals("call")) originalCalls++;
        assertEquals(1, originalCalls); // actual application remains a future native obligation
    }
}
