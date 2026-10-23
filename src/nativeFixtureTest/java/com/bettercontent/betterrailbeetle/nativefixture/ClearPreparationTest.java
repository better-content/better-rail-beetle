package com.bettercontent.betterrailbeetle.nativefixture;

import com.bettercontent.betterrailbeetle.nativeclear.ClearPreparation;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ClearPreparationTest {
    @Test void nonBlockEntityDoesNotReadOrConsumePendingData() {
        ClearPreparation.beforeClear(false, () -> { fail("Must not read non-BE pending data"); return null; });
    }
    @Test void materializesExactlyOnceBeforeCallerMutation() {
        var calls = new AtomicInteger();
        ClearPreparation.beforeClear(true, () -> { calls.incrementAndGet(); return new Object(); });
        assertEquals(1, calls.get());
    }
    @Test void missingEntityFailsClosed() {
        assertThrows(IllegalStateException.class, () -> ClearPreparation.beforeClear(true, () -> null));
    }
    @Test void shippingGuardRejectsClearClassesSourcesAndMixinResource() throws Exception {
        var root = java.nio.file.Path.of(System.getProperty("bc.nativeWitness.testsRoot", "build/native-witness-unit"),
                java.util.UUID.randomUUID().toString());
        java.nio.file.Files.createDirectories(root);
        int index = 0;
        for (String name : new String[]{"com/bettercontent/betterrailbeetle/nativeclear/ClearPreparation.class",
                "com/bettercontent/betterrailbeetle/nativeclear/ClearPreparation.java",
                "com/bettercontent/betterrailbeetle/nativeclear/PromotionWarningCapture.class",
                "com/bettercontent/betterrailbeetle/nativeclear/PromotionWarningLedger.java",
                "com/bettercontent/betterrailbeetle/nativeclear/PromotionWarningFlush.class",
                "com/bettercontent/betterrailbeetle/nativeclear/mixin/PromotionWarningMixin.class",
                "better_rail_beetle_native_clear.mixins.json"}) {
            var archive = root.resolve("negative-" + index++ + ".zip");
            try (var zip = new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(archive))) {
                zip.putNextEntry(new java.util.zip.ZipEntry(name));
                zip.write(1);
                zip.closeEntry();
            }
            assertThrows(IllegalStateException.class, () -> WitnessArchiveGuard.check(archive));
            assertTrue(java.nio.file.Files.exists(archive));
        }
    }
    @Test void helperIsOutsideTheTransformersReservedMixinPackage() throws Exception {
        var config = java.nio.file.Files.readString(java.nio.file.Path.of(
            "src/nativeClear/resources/better_rail_beetle_native_clear.mixins.json"));
        var matcher = java.util.regex.Pattern.compile("\\\"package\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(config);
        assertTrue(matcher.find());
        String mixinPackage = matcher.group(1);
        for (String helper : new String[]{ClearPreparation.class.getName(),
                "com.bettercontent.betterrailbeetle.nativeclear.PromotionWarningCapture",
                "com.bettercontent.betterrailbeetle.nativeclear.PromotionWarningLedger",
                "com.bettercontent.betterrailbeetle.nativeclear.PromotionWarningFlush"}) {
            assertFalse(helper.startsWith(mixinPackage + "."),
                "Mixin reserves its package; directly called helpers must live outside it");
            assertTrue(java.nio.file.Files.exists(java.nio.file.Path.of("src/nativeClear/java", helper.replace('.', '/') + ".java")));
        }
        assertTrue(java.nio.file.Files.exists(java.nio.file.Path.of("src/nativeClear/java",
            mixinPackage.replace('.', '/'), "ClearPreparationMixin.java")));
    }
    @Test void originalDecodeExceptionIsNotSwallowedOrRetried() {
        var failure = new IllegalArgumentException("decode failed");
        var calls = new AtomicInteger();
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> ClearPreparation.beforeClear(true, () -> { calls.incrementAndGet(); throw failure; })));
        assertEquals(1, calls.get());
    }
}
