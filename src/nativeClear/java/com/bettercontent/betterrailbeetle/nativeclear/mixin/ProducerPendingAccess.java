package com.bettercontent.betterrailbeetle.nativeclear.mixin;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exact inherited native Map field. Getter only; never a loader or a setter. */
@Mixin(value = ChunkAccess.class, remap = false)
public interface ProducerPendingAccess {
    @Accessor("pendingBlockEntities")
    Map<BlockPos, CompoundTag> railProducerPending();
}
