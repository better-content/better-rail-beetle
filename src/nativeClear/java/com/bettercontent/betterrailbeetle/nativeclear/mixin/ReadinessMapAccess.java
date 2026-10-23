package com.bettercontent.betterrailbeetle.nativeclear.mixin;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=ChunkMap.class,remap=false)
public interface ReadinessMapAccess {
    @Accessor("mainThreadExecutor") BlockableEventLoop<Runnable> railReadinessExecutor();
    @Invoker("getVisibleChunkIfPresent") ChunkHolder railReadinessVisible(long key);
}
