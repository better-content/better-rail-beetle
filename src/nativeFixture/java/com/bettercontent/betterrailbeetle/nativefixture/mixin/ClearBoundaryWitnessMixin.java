package com.bettercontent.betterrailbeetle.nativefixture.mixin;

import com.bettercontent.betterrailbeetle.nativefixture.NativeWitnessCapture;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = StructureUtils.class, remap = false)
public abstract class ClearBoundaryWitnessMixin {
    @WrapMethod(method = "clearSpaceForStructure", require = 1, allow = 1, remap = false)
    private static void witnessClearScope(BoundingBox originalBounds, int groundHeight, ServerLevel level,
                                          Operation<Void> original) {
        try (var scope = NativeWitnessCapture.beginClear(level)) {
            original.call(originalBounds, groundHeight, level);
            scope.completed();
        }
    }

    @WrapOperation(method = "clearSpaceForStructure", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;betweenClosedStream(Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)Ljava/util/stream/Stream;"),
            require = 1, allow = 1, remap = false)
    private static Stream<BlockPos> witnessExpandedBoundary(BoundingBox actualExpandedBounds,
            Operation<Stream<BlockPos>> original, BoundingBox originalBounds, int groundHeight, ServerLevel level) {
        NativeWitnessCapture.clearBoundary(actualExpandedBounds, level);
        return original.call(actualExpandedBounds);
    }
}
