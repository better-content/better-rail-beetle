package com.bettercontent.betterrailbeetle.nativefixture;

import com.bettercontent.betterrailbeetle.nativeclear.PromotionWarningLedger;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PromotionWarningTest {
    private PromotionWarningLedger.Entry entry() {
        return new PromotionWarningLedger.Entry(213, -41, 67,
                new PromotionWarningLedger.Pending("DUMMY", 213, -41, 67, false),
                List.of(new StackTraceElement("native.Caller", "clear", "Caller.java", 17)), true);
    }
    @Test void fourIncidentBudgetCountsFailuresAndNeverRequeriesBeyondBudget() {
        var ledger = new PromotionWarningLedger();
        var calls = new AtomicInteger();
        for (int i = 0; i < 20; i++) ledger.observe(() -> { calls.incrementAndGet(); return entry(); });
        var result = ledger.finish();
        assertEquals(4, calls.get()); assertEquals(20, result.warnings());
        assertEquals(4, result.entries().size()); assertEquals(0, result.captureFailures());
        assertTrue(result.entries().get(0).stackTruncated());
        assertNull(ledger.finish());
        ledger.observe(() -> { fail("Closed ledger must not capture"); return null; });
    }
    @Test void observerFailuresAreBoundedAndCannotEscape() {
        var ledger = new PromotionWarningLedger();
        var calls = new AtomicInteger();
        for (int i = 0; i < 10; i++) assertDoesNotThrow(() -> ledger.observe(() -> {
            calls.incrementAndGet(); throw new AssertionError("observer fault");
        }));
        var result = ledger.finish();
        assertEquals(4, calls.get()); assertEquals(4, result.captureFailures());
        assertEquals(10, result.warnings()); assertTrue(result.entries().isEmpty());
    }
    @Test void scalarAndStackBoundsHaveNegativeControls() {
        assertEquals(24, PromotionWarningLedger.MAX_FRAMES);
        assertThrows(IllegalArgumentException.class, () -> new PromotionWarningLedger.Entry(1, 2, 3,
                entry().pending(), java.util.Collections.nCopies(25, entry().stack().get(0)), false));
        assertThrows(IllegalArgumentException.class, () -> new PromotionWarningLedger.Entry(1, 2, 3,
                new PromotionWarningLedger.Pending("x".repeat(129), null, null, null, null), List.of(), false));
        var mutable = new java.util.ArrayList<>(entry().stack());
        var copied = new PromotionWarningLedger.Entry(1, 2, 3, entry().pending(), mutable, false);
        mutable.clear(); assertEquals(1, copied.stack().size());
        assertThrows(UnsupportedOperationException.class, () -> copied.stack().clear());
    }
}
