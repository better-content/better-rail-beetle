package com.bettercontent.betterrailbeetle.nativeclear;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Uses only the supplied native arguments. Never retain/copy/serialize CompoundTag. */
public final class PromotionWarningCapture {
    private static final PromotionWarningLedger LEDGER = new PromotionWarningLedger();
    private PromotionWarningCapture() {}

    public static void warning(BlockPos pos, CompoundTag pending) {
        LEDGER.observe(() -> {
            var scalar = new PromotionWarningLedger.Pending(pending.getString("id"),
                    pending.contains("x", Tag.TAG_INT) ? pending.getInt("x") : null,
                    pending.contains("y", Tag.TAG_INT) ? pending.getInt("y") : null,
                    pending.contains("z", Tag.TAG_INT) ? pending.getInt("z") : null,
                    pending.contains("keepPacked", Tag.TAG_BYTE) ? pending.getBoolean("keepPacked") : null);
            List<StackTraceElement> prefix = StackWalker.getInstance().walk(frames -> frames
                    .filter(frame -> !frame.getClassName().startsWith(PromotionWarningCapture.class.getName())
                            && !frame.getClassName().equals(PromotionWarningLedger.class.getName()))
                    .limit(PromotionWarningLedger.MAX_FRAMES + 1L)
                    .map(StackWalker.StackFrame::toStackTraceElement).toList());
            return new PromotionWarningLedger.Entry(pos.getX(), pos.getY(), pos.getZ(), scalar,
                    prefix.stream().limit(PromotionWarningLedger.MAX_FRAMES).toList(),
                    prefix.size() > PromotionWarningLedger.MAX_FRAMES);
        });
    }

    public static PromotionWarningLedger.Snapshot finish() { return LEDGER.finish(); }
}
