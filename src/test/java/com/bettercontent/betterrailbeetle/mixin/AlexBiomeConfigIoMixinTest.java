package com.bettercontent.betterrailbeetle.mixin;

import static org.junit.jupiter.api.Assertions.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.bettercontent.betterrailbeetle.compat.alexscaves.SerializedBiomeConfigIo;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

class AlexBiomeConfigIoMixinTest {
    @Test void directProductionForwarderPreservesAllOpaqueArgumentsResultAndFailureExactlyOnce() {
        var directory = new File("not-accessed-by-this-pure-contract");
        Object defaults = new Object(), type = new Object(), result = new Object();
        Predicate<Object> predicate = value -> false;
        Object[] expected = {directory, "primordial_caves", defaults, type, predicate};
        var calls = new AtomicInteger();
        Operation<Object> original = args -> { calls.incrementAndGet(); assertArrayEquals(expected, args); return result; };
        assertSame(result, SerializedBiomeConfigIo.forward(directory, "primordial_caves", defaults, type, predicate, original));
        assertEquals(1, calls.get());
        var failure = new IllegalArgumentException("unchanged original failure");
        Operation<Object> throwing = args -> { calls.incrementAndGet(); assertArrayEquals(expected, args); throw failure; };
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> SerializedBiomeConfigIo.forward(directory, "primordial_caves", defaults, type, predicate, throwing)));
        assertEquals(2, calls.get());
    }

    @Test void opaqueTypeSlotUsesSupportedCoerceMetadataWithoutReflectionDescriptors() throws Exception {
        var node = new ClassNode();
        try (var bytes = getClass().getClassLoader().getResourceAsStream("com/bettercontent/betterrailbeetle/mixin/AlexBiomeConfigIoMixin.class")) {
            assertNotNull(bytes); new ClassReader(bytes).accept(node, 0);
        }
        var handler = node.methods.stream().filter(m -> m.name.equals("serializeFileTransaction")).findFirst().orElseThrow();
        assertTrue((handler.access & org.objectweb.asm.Opcodes.ACC_PRIVATE) != 0);
        assertTrue((handler.access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0);
        var calls = java.util.Arrays.stream(handler.instructions.toArray())
                .filter(i -> i instanceof org.objectweb.asm.tree.MethodInsnNode)
                .map(i -> (org.objectweb.asm.tree.MethodInsnNode) i).toList();
        assertEquals(1, calls.size());
        assertEquals("com/bettercontent/betterrailbeetle/compat/alexscaves/SerializedBiomeConfigIo", calls.get(0).owner);
        assertEquals("forward", calls.get(0).name);
        assertEquals(handler.desc, calls.get(0).desc);
        assertEquals(java.util.List.of(0, 1, 2, 3, 4, 5), java.util.Arrays.stream(handler.instructions.toArray())
                .filter(i -> i instanceof org.objectweb.asm.tree.VarInsnNode)
                .map(i -> ((org.objectweb.asm.tree.VarInsnNode) i).var).toList());
        assertFalse(handler.desc.contains("java/lang/reflect/"));
        assertEquals("Ljava/lang/Object;", org.objectweb.asm.Type.getArgumentTypes(handler.desc)[3].getDescriptor());
        assertNotNull(handler.invisibleParameterAnnotations);
        assertTrue(handler.invisibleParameterAnnotations[3].stream()
                .anyMatch(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Coerce;")));
    }
}
