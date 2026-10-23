package com.bettercontent.betterrailbeetle.nativeclear.mixin;
import com.bettercontent.betterrailbeetle.nativeclear.ClearReadiness;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=StructureUtils.class,remap=false)
public abstract class ClearReadinessMixin {
    @WrapMethod(method="clearSpaceForStructure(Lnet/minecraft/world/level/levelgen/structure/BoundingBox;ILnet/minecraft/server/level/ServerLevel;)V",require=1,expect=1,allow=1,remap=false)
    private static void awaitNativeTickingBeforeClear(BoundingBox bounds,int floor,ServerLevel world,Operation<Void> original) {
        var lease=ClearReadiness.prepare(bounds,world);
        Throwable failure=null;
        try {
            original.call(bounds,floor,world);
        } catch(Throwable originalFailure) {
            failure=originalFailure;
            throw originalFailure;
        } finally {
            try { lease.close(); }
            catch(Throwable cleanup) {
                if(failure==null)throw cleanup;
                if(cleanup!=failure)failure.addSuppressed(cleanup);
            }
        }
    }
}
