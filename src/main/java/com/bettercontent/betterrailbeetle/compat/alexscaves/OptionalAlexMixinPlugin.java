package com.bettercontent.betterrailbeetle.compat.alexscaves;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Bytecode-only optional-presence gate: never defines/initializes a native class. */
public final class OptionalAlexMixinPlugin implements IMixinConfigPlugin {
    static final String TARGET = "com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig";
    static final String CITADEL = "com.github.alexthe666.citadel.Citadel";
    static final String MIXIN = "com.bettercontent.betterrailbeetle.mixin.AlexBiomeConfigIoMixin";
    private final Predicate<String> present;

    public OptionalAlexMixinPlugin() { this(OptionalAlexMixinPlugin::hasBytecode); }
    OptionalAlexMixinPlugin(Predicate<String> present) { this.present = present; }

    private static boolean hasBytecode(String name) {
        // ModLauncher's legacy Mixin service cannot supply untransformed ClassNodes.
        // Locate the named class resource without defining/initializing the class.
        // Lookup failures propagate; only actual missing resources mean absence.
        return OptionalAlexMixinPlugin.class.getClassLoader().getResource(name.replace('.', '/') + ".class") != null;
    }

    @Override public boolean shouldApplyMixin(String target, String mixin) {
        return TARGET.equals(target) && MIXIN.equals(mixin) && present.test(TARGET) && present.test(CITADEL);
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
