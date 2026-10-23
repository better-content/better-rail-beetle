package com.bettercontent.betterrailbeetle.mixin;

import com.bettercontent.betterrailbeetle.compat.alexscaves.SerializedBiomeConfigIo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.io.File;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** Main loading and the TOML watcher may enter the same JSON helper concurrently.
 * Lock only the original file transaction; argument/config/world work stays outside. */
@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig", remap = false)
public abstract class AlexBiomeConfigIoMixin {
    @WrapOperation(
            method = "getConfigData",
            at = @At(value = "INVOKE", target = "Lcom/github/alexmodguy/alexscaves/server/config/BiomeGenerationConfig;getOrCreateConfigFile(Ljava/io/File;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/reflect/Type;Ljava/util/function/Predicate;)Ljava/lang/Object;", remap = false),
            require = 1, allow = 1, remap = false)
    private static Object serializeFileTransaction(File directory, String name, Object defaults,
            @Coerce Object type, Predicate<?> invalid, Operation<Object> original) {
        return SerializedBiomeConfigIo.forward(directory, name, defaults, type, invalid, original);
    }
}
