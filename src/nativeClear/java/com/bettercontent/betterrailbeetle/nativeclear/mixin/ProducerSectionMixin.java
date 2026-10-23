package com.bettercontent.betterrailbeetle.nativeclear.mixin;

import com.bettercontent.betterrailbeetle.nativeclear.ProducerCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {LevelChunk.class, ProtoChunk.class}, remap = false)
public abstract class ProducerSectionMixin {
    @Redirect(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"),
            require = 1, expect = 1, allow = 1, remap = false)
    private BlockState observeActualSectionWrite(LevelChunkSection section, int x, int y, int z, BlockState requested,
                                                BlockPos suppliedPosition, BlockState suppliedState, boolean moving) {
        // Typed exact native forwarding FIRST: no varargs array, boxing, new position or lambda.
        BlockState actualOld = section.setBlockState(x,y,z,requested);
        try {
            ProducerCapture.transition((ProducerPendingAccess)(Object)this,suppliedPosition,actualOld,requested,
                    (Object)this instanceof ProtoChunk);
        } catch (Throwable observerFailure) {
            try { ProducerCapture.failed(); } catch (Throwable ignored) { /* preserve the native result */ }
        }
        return actualOld;
    }
}
