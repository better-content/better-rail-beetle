package com.bettercontent.betterrailbeetle.compat.alexscaves;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.io.File;
import java.util.function.Predicate;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Serializes only Alex's Caves' JSON read/create/read/validate/write operation.
 * No configuration getters, callbacks, world reads or task scheduling belong here. */
public final class SerializedBiomeConfigIo {
    private static final ReentrantLock LOCK = new ReentrantLock();

    private SerializedBiomeConfigIo() {}

    /** Public pure forwarding seam; the actual Mixin handler must remain private static. */
    public static Object forward(File directory, String name, Object defaults, Object type,
            Predicate<?> invalid, Operation<Object> original) {
        return call(() -> original.call(directory, name, defaults, type, invalid));
    }

    public static <T> T call(Supplier<T> original) {
        LOCK.lock();
        try {
            return original.get();
        } finally {
            LOCK.unlock();
        }
    }
}
