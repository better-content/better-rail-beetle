package com.bettercontent.betterrailbeetle.nativeclear;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import com.bettercontent.betterrailbeetle.nativeclear.mixin.ProducerPendingAccess;

/** Observation only. No terrain reads, native factories, pending writes, reflection or hot I/O. */
public final class ProducerCapture {
    private static final ProducerLedger LEDGER = new ProducerLedger();
    private static volatile ProducerPalette palette;
    private ProducerCapture() {}
    public static void initialize(ProducerPalette prepared) { palette = prepared; }
    public static void failed() { LEDGER.failed(); }
    public static ProducerPalette palette() { return palette; }
    public static ProducerLedger.Summary finish() {
        ProducerPalette current = palette;
        return LEDGER.finish(current != null && current.complete());
    }
    public static long enterForce(BlockPos pos, ServerLevel level) {
        return LEDGER.enter("force",Thread.currentThread().getId(),System.identityHashCode(level),
                System.identityHashCode(pos),0,pos.getX(),pos.getY(),pos.getZ(),pos.getX(),pos.getY(),pos.getZ());
    }
    public static long enterClear(BoundingBox box, int floor, ServerLevel level) {
        return LEDGER.enter("clear",Thread.currentThread().getId(),System.identityHashCode(level),
                System.identityHashCode(box),floor,box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ());
    }
    public static void exit(long ticket) { LEDGER.exit(ticket,Thread.currentThread().getId()); }

    /** Called AFTER the actual original section write returned; supplied old/new are never reread. */
    public static void transition(ProducerPendingAccess owner, BlockPos pos, BlockState oldState,
                                  BlockState requested, boolean proto) {
        if (oldState == requested) return;
        CompoundTag pending = owner.railProducerPending().get(pos); // pure field Map.get, NOT native load
        if (pending == null) return;
        if (!pending.contains("id",Tag.TAG_STRING)) { LEDGER.failed(); return; }
        if (!"DUMMY".equals(pending.getString("id"))) return;
        ProducerPalette current = palette;
        ProducerPalette.State old = current == null ? null : current.lookup(oldState);
        ProducerPalette.State next = current == null ? null : current.lookup(requested);
        long ticket = LEDGER.reserve(old == null || next == null);
        if (ticket == 0) return; // no per-candidate allocations/metadata/stack beyond the retained budget
        if (!pending.contains("x",Tag.TAG_INT) || !pending.contains("y",Tag.TAG_INT)
                || !pending.contains("z",Tag.TAG_INT)) LEDGER.failed();
        var keys = new java.util.ArrayList<String>();
        boolean keysTruncated = pending.getAllKeys().size() > 32;
        int scannedKeys = 0;
        for (String key : pending.getAllKeys()) {
            if (scannedKeys++ == 32) break;
            if (key.length() > 64 || !key.matches("[A-Za-z0-9_.:-]+")) { keysTruncated = true; continue; }
            keys.add(key + ":" + pending.getTagType(key));
        }
        keys.sort(String::compareTo);
        if (keysTruncated) LEDGER.failed();
        var scalar = new ProducerLedger.Pending("DUMMY",
                pending.contains("x",Tag.TAG_INT) ? pending.getInt("x") : null,
                pending.contains("y",Tag.TAG_INT) ? pending.getInt("y") : null,
                pending.contains("z",Tag.TAG_INT) ? pending.getInt("z") : null,
                pending.contains("keepPacked",Tag.TAG_BYTE) ? pending.getBoolean("keepPacked") : null,
                keys,keysTruncated);
        var frames = StackWalker.getInstance().walk(stream -> stream
                .filter(frame -> !frame.getClassName().equals(ProducerCapture.class.getName()))
                .limit(ProducerLedger.MAX_FRAMES+1L).map(StackWalker.StackFrame::toStackTraceElement).toList());
        LEDGER.record(ticket,Thread.currentThread().getId(),proto ? "ProtoChunk" : "LevelChunk",
                pos.getX(),pos.getY(),pos.getZ(),old == null ? "UNKNOWN" : old.canonical(),
                next == null ? "UNKNOWN" : next.canonical(),scalar,
                frames.stream().limit(ProducerLedger.MAX_FRAMES).toList(),frames.size()>ProducerLedger.MAX_FRAMES);
    }
}
