package com.bettercontent.betterrailbeetle.nativefixture;

import com.bettercontent.betterrailbeetle.nativeclear.PromotionWarningCapture;
import com.bettercontent.betterrailbeetle.nativeclear.mixin.PromotionWarningMixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class PromotionWarningHookTest {
    private static final String WARN_DESC = "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V";
    private static final String TARGET = "promotePendingBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/block/entity/BlockEntity;";
    private ClassNode read(String path) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path + ".class")) {
            assertNotNull(input, path); var node = new ClassNode(); new ClassReader(input).accept(node, 0); return node;
        }
    }
    private MethodNode hook(ClassNode node) {
        return node.methods.stream().filter(m -> m.name.equals("observeOriginalDummyWarning")).findFirst().orElseThrow();
    }
    private List<MethodInsnNode> calls(MethodNode method) {
        var result = new ArrayList<MethodInsnNode>();
        for (var insn : method.instructions) if (insn instanceof MethodInsnNode call) result.add(call);
        return result;
    }
    private void forwardingContract(MethodNode method) {
        var calls = calls(method);
        assertEquals(1, calls.stream().filter(c -> c.owner.equals("com/llamalad7/mixinextras/injector/wrapoperation/Operation") && c.name.equals("call")).count());
        assertTrue(calls.stream().noneMatch(c -> c.owner.equals("org/slf4j/Logger")));
        int forward = -1, capture = -1;
        for (int i = 0; i < calls.size(); i++) {
            if (calls.get(i).owner.equals("com/llamalad7/mixinextras/injector/wrapoperation/Operation")) forward = i;
            if (capture < 0 && calls.get(i).owner.endsWith("/PromotionWarningCapture")) capture = i;
        }
        assertTrue(forward >= 0 && capture > forward, "Original warning must execute before observation");
        assertTrue(method.tryCatchBlocks.stream().anyMatch(block -> "java/lang/Throwable".equals(block.type)));
    }
    @Test void compiledHookPinsExactWarningAbiAndSingleOriginalForward() throws Exception {
        var method = hook(read("com/bettercontent/betterrailbeetle/nativeclear/mixin/PromotionWarningMixin"));
        forwardingContract(method);
        var wrap = method.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/WrapOperation;")).findFirst().orElseThrow();
        var values = values(wrap);
        assertEquals(List.of(TARGET), values.get("method"));
        assertEquals(1, values.get("require")); assertEquals(1, values.get("allow")); assertEquals(false, values.get("remap"));
        var sites = (List<?>) values.get("at");
        assertEquals(1, sites.size());
        var at = (AnnotationNode) sites.get(0);
        assertEquals(0, values(at).get("ordinal"));
        assertEquals("Lorg/slf4j/Logger;warn" + WARN_DESC, values(at).get("target"));
        // Deliberate duplicate forwarding must be rejected by the same oracle.
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "com/llamalad7/mixinextras/injector/wrapoperation/Operation", "call", "([Ljava/lang/Object;)Ljava/lang/Object;", true));
        assertThrows(AssertionError.class, () -> forwardingContract(method));
    }
    private java.util.Map<String, Object> values(AnnotationNode annotation) {
        var result = new java.util.HashMap<String, Object>();
        for (int i = 0; i < annotation.values.size(); i += 2) result.put((String) annotation.values.get(i), annotation.values.get(i + 1));
        return result;
    }
    @Test void actualNativeMethodHasExactlyPinnedDummyThenFailureWarnings() throws Exception {
        var node = read("net/minecraft/world/level/chunk/LevelChunk");
        var method = node.methods.stream().filter(m -> (m.name + m.desc).equals(TARGET)).findFirst().orElseThrow();
        var warnings = calls(method).stream().filter(c -> c.owner.equals("org/slf4j/Logger") && c.name.equals("warn")).toList();
        assertEquals(2, warnings.size()); assertTrue(warnings.stream().allMatch(c -> c.desc.equals(WARN_DESC)));
        var literals = new ArrayList<String>();
        for (var insn : method.instructions) if (insn instanceof LdcInsnNode literal && literal.cst instanceof String text && text.startsWith("Tried to load")) literals.add(text);
        assertEquals(List.of("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location",
                "Tried to load a block entity for block {} but failed at location {}"), literals);
    }
    private void privacyContract(ClassNode node) {
        assertTrue(node.fields.stream().noneMatch(f -> f.desc.contains("net/minecraft/")));
        for (var method : node.methods) for (var call : calls(method)) {
            if (call.owner.startsWith("net/minecraft/")) assertTrue(
                    (call.owner.equals("net/minecraft/core/BlockPos") && Set.of("getX", "getY", "getZ").contains(call.name)) ||
                    (call.owner.equals("net/minecraft/nbt/CompoundTag") && Set.of("contains", "getString", "getInt", "getBoolean").contains(call.name)), call.owner + "." + call.name);
            assertFalse(call.owner.startsWith("java/io/") || call.owner.startsWith("java/nio/file/") || call.owner.equals("org/slf4j/Logger"));
        }
    }
    @Test void compiledCaptureHasNoExtraNativeQueriesPayloadIoOrLoopLogging() throws Exception {
        var node = read("com/bettercontent/betterrailbeetle/nativeclear/PromotionWarningCapture");
        privacyContract(node);
        for (var method : node.methods) for (var insn : method.instructions)
            if (insn instanceof LdcInsnNode literal && literal.cst instanceof String key)
                assertTrue(Set.of("id", "x", "y", "z", "keepPacked").contains(key), key);
        node.methods.get(0).instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/level/Level", "getBlockEntity", "()V", false));
        assertThrows(AssertionError.class, () -> privacyContract(node));
    }
    @Test void compiledFlushLogsOnlyAtBoundShutdownEventAndHelpersAreNotMixins() throws Exception {
        var node = read("com/bettercontent/betterrailbeetle/nativeclear/PromotionWarningFlush");
        assertTrue(node.visibleAnnotations.stream().anyMatch(a -> a.desc.endsWith("Mod$EventBusSubscriber;")));
        int outputCalls = 0;
        for (var method : node.methods) for (var call : calls(method)) {
            if (call.owner.equals("org/slf4j/Logger")) {
                outputCalls++;
                assertEquals("stopped", method.name);
                assertEquals("(Lnet/minecraftforge/event/server/ServerStoppedEvent;)V", method.desc);
                assertEquals("info", call.name);
                assertTrue(method.visibleAnnotations.stream().anyMatch(a -> a.desc.endsWith("/SubscribeEvent;")));
            }
            assertFalse(call.owner.startsWith("java/io/") || call.owner.startsWith("java/nio/file/"));
        }
        assertEquals(1, outputCalls);
        for (String helper : List.of("PromotionWarningCapture", "PromotionWarningLedger", "PromotionWarningFlush"))
            assertFalse(read("com/bettercontent/betterrailbeetle/nativeclear/" + helper).name.contains("/mixin/"));
    }
    private static final class HookInstance extends PromotionWarningMixin {}
    @Test void actualCompiledHookForwardsIdentityOnceAndPreservesOriginalExceptionsDespiteCaptureFault() throws Exception {
        var method = java.util.Arrays.stream(PromotionWarningMixin.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("observeOriginalDummyWarning")).findFirst().orElseThrow();
        method.setAccessible(true);
        Object first = new Object(), second = new Object(); String format = new String("original format");
        var receiver = (org.slf4j.Logger) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{org.slf4j.Logger.class}, (proxy, called, args) -> { throw new AssertionError("Do not call substitute logger methods"); });
        var count = new AtomicInteger(); var instance = new HookInstance();
        for (Throwable failure : new Throwable[]{null, new IllegalStateException("native logger failure"), new AssertionError("native logger error")}) {
            Operation<Void> original = args -> {
                count.incrementAndGet(); assertSame(receiver, args[0]); assertSame(format, args[1]);
                assertSame(first, args[2]); assertSame(second, args[3]);
                if (failure instanceof RuntimeException runtime) throw runtime;
                if (failure instanceof Error error) throw error;
                return null;
            };
            if (failure == null) assertDoesNotThrow(() -> method.invoke(instance, receiver, format, first, second, original, null, null));
            else assertSame(failure, assertThrows(InvocationTargetException.class,
                    () -> method.invoke(instance, receiver, format, first, second, original, null, null)).getCause());
        }
        assertEquals(3, count.get());
        var observed = PromotionWarningCapture.finish();
        assertEquals(3, observed.warnings()); assertEquals(3, observed.captureFailures());
        assertTrue(observed.entries().isEmpty()); // null supplied tag deliberately faults observer, not original
    }
}
