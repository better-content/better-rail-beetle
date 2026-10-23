package com.bettercontent.betterrailbeetle.nativeclear.mixin;

import com.bettercontent.betterrailbeetle.nativeclear.ClearPreparation;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the source GameTest RunConfig loads this mixin, never the production mod. */
@Mixin(value = StructureUtils.class, remap = false)
public abstract class ClearPreparationMixin {
    @Inject(method = "clearBlock(ILnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;)V",
            at = @At("HEAD"), require = 1)
    private static void materializeBeforeArenaClear(int floor, BlockPos pos, ServerLevel level, CallbackInfo ci) {
        // Native setBlockState replaces the state before old-block onRemove callbacks
        // can read its entity (e.g. ChestBlock). Decode lazy DUMMY against the old block.
        // Native decoding failures remain fatal; no pending-map removal or log filtering.
        ClearPreparation.beforeClear(level.getBlockState(pos).hasBlockEntity(), () -> level.getBlockEntity(pos));
    }
}
