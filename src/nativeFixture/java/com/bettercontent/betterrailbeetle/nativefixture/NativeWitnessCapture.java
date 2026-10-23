package com.bettercontent.betterrailbeetle.nativefixture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Read-only native evidence, opt-in and absent from every shipping artifact and default run. */
@Mod.EventBusSubscriber(modid = "better_rail_beetle")
public final class NativeWitnessCapture {
    private static final WitnessLedger LEDGER = new WitnessLedger(Boolean.getBoolean("bc.rail.nativeWitness.enabled"));
    private static final ClearWitnessScope SCOPES = new ClearWitnessScope(LEDGER);
    private NativeWitnessCapture() {}

    public static ClearWitnessScope.Ticket beginClear(ServerLevel level) { return SCOPES.enter(level); }

    public static void clearBoundary(BoundingBox bounds, ServerLevel level) {
        if (!LEDGER.enabled()) return;
        SCOPES.expanded(level, bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
    }

    /** Both states and the real chunk were already computed by the original Level.setBlock flow. */
    public static void beforeMutation(Level level, LevelChunk chunk, BlockPos pos, BlockState oldState, BlockState newState) {
        if (!LEDGER.enabled() || SCOPES.boundary(level, pos.getX(), pos.getY(), pos.getZ()) < 0) return;
        // Non-BE old state is outside this candidate scope, NOT a pending-map absence proof.
        if (!oldState.hasBlockEntity() || !SCOPES.candidate(level, pos.getX(), pos.getY(), pos.getZ())) return;
        CompoundTag pending = chunk.getBlockEntityNbt(pos); // public Map.get only; never a load/materialization
        if (pending != null) capture("before_clear_mutation", SCOPES.boundary(level, pos.getX(), pos.getY(), pos.getZ()),
                chunk, pending, oldState, newState);
    }

    /** State is the ORIGINAL promotion read's return; the original caller already supplied this tag. */
    public static void promotion(LevelChunk chunk, BlockPos pos, CompoundTag pending, BlockState originalState) {
        if (!LEDGER.enabled()) return;
        // getLevel returns only the chunk's existing owner identity, not terrain/state or a chunk lookup.
        int boundary = SCOPES.boundary(chunk.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        if (boundary > 0) capture("before_promotion", boundary, chunk, pending, originalState, null);
    }

    private static void capture(String phase, int boundary, LevelChunk chunk, CompoundTag pending,
                                BlockState oldState, BlockState newState) {
        if (LEDGER.full()) { LEDGER.droppedRecord(); return; }
        // Never store/copy/serialize the tag, inspect other keys, or materialize a BlockEntity.
        var scalar = new WitnessLedger.Pending(pending.getString("id"),
                pending.contains("x", Tag.TAG_INT) ? pending.getInt("x") : null,
                pending.contains("y", Tag.TAG_INT) ? pending.getInt("y") : null,
                pending.contains("z", Tag.TAG_INT) ? pending.getInt("z") : null,
                pending.contains("keepPacked", Tag.TAG_BYTE) ? pending.getBoolean("keepPacked") : null);
        LEDGER.record(phase, boundary, scalar, oldState.toString(), newState == null ? null : newState.toString(),
                chunk.getStatus().toString(), callers());
    }

    private static WitnessLedger.Slice callers() {
        return StackWalker.getInstance().walk(frames -> {
            var prefix = frames.filter(frame -> !frame.getClassName().startsWith(
                            "com.bettercontent.betterrailbeetle.nativefixture."))
                    .limit(WitnessLedger.MAX_CALLSITES + 1)
                    .map(frame -> new WitnessLedger.Callsite(frame.getClassName(), frame.getMethodName(), frame.getLineNumber()))
                    .toList();
            return new WitnessLedger.Slice(prefix.stream().limit(WitnessLedger.MAX_CALLSITES).toList(),
                    prefix.size() > WitnessLedger.MAX_CALLSITES);
        });
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        if (!LEDGER.enabled()) return;
        String token = System.getProperty("bc.rail.nativeWitness.token", "");
        UUID.fromString(token);
        Path output = Path.of(System.getProperty("bc.rail.nativeWitness.output", "")).toAbsolutePath();
        if (!output.getFileName().toString().equals("native-witness.json")
                || !output.getParent().getFileName().toString().equals(token)
                || Files.isSymbolicLink(output.getParent())
                || !Files.isRegularFile(output.getParent().resolve("source-fixture.json"), LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Native witness requires its fresh owning UUID fixture");
        }
        try {
            Files.write(output, LEDGER.finish(token), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to retain bounded native witness", error);
        }
    }
}
