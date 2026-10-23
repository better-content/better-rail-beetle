package com.bettercontent.betterrailbeetle.nativeclear.mixin;

import com.bettercontent.betterrailbeetle.nativeclear.PromotionWarningCapture;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Observes the original DUMMY warning, not a precondition or inferred failure. */
@Mixin(value = LevelChunk.class, remap = false)
public abstract class PromotionWarningMixin {
    @WrapOperation(method = "promotePendingBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", ordinal = 0),
            require = 1, allow = 1, remap = false)
    private void observeOriginalDummyWarning(Logger logger, String format, Object first, Object second,
                                            Operation<Void> original, BlockPos pos, CompoundTag pending) {
        try {
            // Same receiver/arguments, exactly once; never cancel, replace, suppress or retry.
            original.call(logger, format, first, second);
        } finally {
            try {
                PromotionWarningCapture.warning(pos, pending);
            } catch (Throwable observerFailure) {
                // Observer failure cannot replace the original logger result/exception.
                // The bounded ledger separately counts failures once observation has begun.
            }
        }
    }
}
