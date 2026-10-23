package com.bettercontent.betterrailbeetle.nativefixture.mixin;

import com.bettercontent.betterrailbeetle.nativefixture.NativeWitnessCapture;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelChunk.class, remap = false)
public abstract class PendingPromotionWitnessMixin {
    @WrapOperation(method = "promotePendingBlockEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            require = 1, allow = 1, remap = false)
    private BlockState witnessBeforePromotion(LevelChunk chunk, BlockPos readPosition, Operation<BlockState> original,
                                               BlockPos originalPosition, CompoundTag pending) {
        BlockState originalState = original.call(chunk, readPosition);
        NativeWitnessCapture.promotion(chunk, originalPosition, pending, originalState);
        return originalState;
    }
}
