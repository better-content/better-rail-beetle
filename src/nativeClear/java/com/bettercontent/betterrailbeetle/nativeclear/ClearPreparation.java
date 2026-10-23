package com.bettercontent.betterrailbeetle.nativeclear;

import java.util.function.Supplier;

/** Decode before the arena destroys the old state. Never discard or reconstruct pending NBT. */
public final class ClearPreparation {
    private ClearPreparation() {}

    public static void beforeClear(boolean oldStateHasBlockEntity, Supplier<?> materialize) {
        if (oldStateHasBlockEntity && materialize.get() == null) {
            throw new IllegalStateException("Native arena could not materialize existing block entity before clearing");
        }
    }
}
