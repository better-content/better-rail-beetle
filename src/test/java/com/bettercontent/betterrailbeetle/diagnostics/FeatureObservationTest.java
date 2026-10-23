package com.bettercontent.betterrailbeetle.diagnostics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class FeatureObservationTest {
    @Test public void nativeScalarsHaveExplicitStage() { var fields = FeatureObservation.fields(3, 8, 0); assertTrue(fields.contains("placed_rails=3")); assertTrue(fields.contains("placed_supports=8")); assertTrue(fields.contains("rollback_invocations=0")); }
    @Test public void uncertaintyAndPrivacyStayExplicit() { var fields = FeatureObservation.fields(0, 0, 1); assertTrue(fields.contains("rollback_invocations=1")); assertTrue(fields.contains("rollback_restoration=not_independently_observed")); assertFalse(fields.contains("restored=true")); }
}
