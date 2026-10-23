package com.bettercontent.betterrailbeetle.compat.alexscaves;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

class OptionalAlexMixinPluginTest {
    @Test void allOptionalAbsenceCombinationsSkipAndBothPresentApply() {
        for (var present : Set.of(Set.<String>of(), Set.of(OptionalAlexMixinPlugin.TARGET), Set.of(OptionalAlexMixinPlugin.CITADEL))) {
            var plugin = new OptionalAlexMixinPlugin(present::contains);
            assertFalse(plugin.shouldApplyMixin(OptionalAlexMixinPlugin.TARGET, OptionalAlexMixinPlugin.MIXIN));
        }
        var both = new OptionalAlexMixinPlugin(Set.of(OptionalAlexMixinPlugin.TARGET, OptionalAlexMixinPlugin.CITADEL)::contains);
        assertTrue(both.shouldApplyMixin(OptionalAlexMixinPlugin.TARGET, OptionalAlexMixinPlugin.MIXIN));
        var probes = new AtomicInteger();
        var unexpected = new OptionalAlexMixinPlugin(name -> { probes.incrementAndGet(); return true; });
        assertFalse(unexpected.shouldApplyMixin("unrelated.Target", OptionalAlexMixinPlugin.MIXIN));
        assertFalse(unexpected.shouldApplyMixin(OptionalAlexMixinPlugin.TARGET, "unrelated.Mixin"));
        assertEquals(0, probes.get());
    }

    @Test void realCaveFreeUnitClasspathLinksMixinAndPseudoMetadataWithoutNativeClasses() throws Exception {
        var loader = getClass().getClassLoader();
        assertNull(loader.getResource(OptionalAlexMixinPlugin.TARGET.replace('.', '/') + ".class"));
        assertNull(loader.getResource(OptionalAlexMixinPlugin.CITADEL.replace('.', '/') + ".class"));
        var absent = new OptionalAlexMixinPlugin(name -> loader.getResource(name.replace('.', '/') + ".class") != null);
        assertFalse(absent.shouldApplyMixin(OptionalAlexMixinPlugin.TARGET, OptionalAlexMixinPlugin.MIXIN));
        assertFalse(new OptionalAlexMixinPlugin().shouldApplyMixin(OptionalAlexMixinPlugin.TARGET, OptionalAlexMixinPlugin.MIXIN));
        var node = new ClassNode();
        try (var bytes = loader.getResourceAsStream(OptionalAlexMixinPlugin.MIXIN.replace('.', '/') + ".class")) {
            assertNotNull(bytes); new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE);
        }
        var annotations = new java.util.ArrayList<org.objectweb.asm.tree.AnnotationNode>();
        if (node.visibleAnnotations != null) annotations.addAll(node.visibleAnnotations);
        if (node.invisibleAnnotations != null) annotations.addAll(node.invisibleAnnotations);
        assertTrue(annotations.stream().anyMatch(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Pseudo;")),
                "Sponge must be allowed to skip the absent soft target before the plugin runs");
        assertEquals(OptionalAlexMixinPlugin.MIXIN.replace('.', '/'), node.name);
    }

    @Test void lookupFailureIsNotSilentlyTurnedIntoOptionalAbsence() {
        var original = new IllegalStateException("bytecode lookup failed");
        var plugin = new OptionalAlexMixinPlugin(name -> { throw original; });
        assertSame(original, assertThrows(IllegalStateException.class,
                () -> plugin.shouldApplyMixin(OptionalAlexMixinPlugin.TARGET, OptionalAlexMixinPlugin.MIXIN)));
    }

}
