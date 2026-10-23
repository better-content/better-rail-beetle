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
    @Test void originalDecodeExceptionIsNotSwallowedOrRetried() {
        var failure = new IllegalArgumentException("decode failed");
        var calls = new AtomicInteger();
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> ClearPreparation.beforeClear(true, () -> { calls.incrementAndGet(); throw failure; })));
        assertEquals(1, calls.get());
    }
}
