package com.bettercontent.betterrailbeetle.diagnostics;

/** Pure projection of values already observed by the owning native decision. */
public final class FeatureObservation {
    private FeatureObservation() {}
    public static String fields(int rails, int supports, int rollbacks) { return PlaytestLog.fields("placed_rails", rails, "placed_supports", supports, "rollback_invocations", rollbacks, "rollback_restoration", "not_independently_observed"); }

}
