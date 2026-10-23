package com.bettercontent.betterrailbeetle.nativefixture.mixin;

import com.bettercontent.betterrailbeetle.nativefixture.NativeWitnessCapture;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Level.class, remap = false)
public abstract class ClearMutationWitnessMixin {
    @WrapOperation(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"),
            require = 1, allow = 1, remap = false)
    private BlockState witnessBeforeClearMutation(LevelChunk originalChunk, BlockPos pos, BlockState newState,
                                                  boolean moving, Operation<BlockState> original,
                                                  @Local(ordinal = 1) BlockState oldState) {
        NativeWitnessCapture.beforeMutation((Level) (Object) this, originalChunk, pos, oldState, newState);
        return original.call(originalChunk, pos, newState, moving);
    }
}
