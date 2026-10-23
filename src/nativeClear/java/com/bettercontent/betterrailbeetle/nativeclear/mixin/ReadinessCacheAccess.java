package com.bettercontent.betterrailbeetle.nativeclear.mixin;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=ServerChunkCache.class,remap=false)
public interface ReadinessCacheAccess {
    @Invoker("runDistanceManagerUpdates") boolean railReadinessAdmit();
}
