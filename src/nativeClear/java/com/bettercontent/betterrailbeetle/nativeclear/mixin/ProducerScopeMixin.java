package com.bettercontent.betterrailbeetle.nativeclear.mixin;

import com.bettercontent.betterrailbeetle.nativeclear.ProducerCapture;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;

/** Bounded outer setup scopes; WrapMethod balances even when the original throws. */
@Mixin(value = StructureUtils.class, remap = false)
public abstract class ProducerScopeMixin {
    @WrapMethod(method = "forceLoadChunks(Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;)V",
            require = 1, expect = 1, allow = 1, remap = false)
    private static void forceScope(BlockPos position, ServerLevel level, Operation<Void> original) {
        long ticket = 0;
        try { ticket = ProducerCapture.enterForce(position,level); }
        catch (Throwable observerFailure) { try { ProducerCapture.failed(); } catch (Throwable ignored) {} }
        try { original.call(position,level); }
        finally {
            try { ProducerCapture.exit(ticket); }
            catch (Throwable observerFailure) { try { ProducerCapture.failed(); } catch (Throwable ignored) {} }
        }
    }
    @WrapMethod(method = "clearSpaceForStructure(Lnet/minecraft/world/level/levelgen/structure/BoundingBox;ILnet/minecraft/server/level/ServerLevel;)V",
            require = 1, expect = 1, allow = 1, remap = false)
    private static void clearScope(BoundingBox bounds, int floor, ServerLevel level, Operation<Void> original) {
        long ticket = 0;
        try { ticket = ProducerCapture.enterClear(bounds,floor,level); }
        catch (Throwable observerFailure) { try { ProducerCapture.failed(); } catch (Throwable ignored) {} }
        try { original.call(bounds,floor,level); }
        finally {
            try { ProducerCapture.exit(ticket); }
            catch (Throwable observerFailure) { try { ProducerCapture.failed(); } catch (Throwable ignored) {} }
        }
    }
}
