package com.bettercontent.betterrailbeetle.nativefixture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/** Task-only check of actual staged runtime AND sources archives; never filters an archive. */
public final class WitnessArchiveGuard {
    private WitnessArchiveGuard() {}
    public static void check(Path archive) throws IOException {
        try (var zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.contains("/nativeclear/") || name.contains("native_clear") || name.contains("nativeClear")
                        || name.contains("/nativefixture/") || name.contains("native_witness")
                        || name.contains("nativeFixture") || name.contains(".native-fixture-inputs")
                        || name.endsWith("native-witness.json") || name.endsWith("source-fixture.json")) {
                    throw new IllegalStateException("Nonshipping native witness leaked into archive entry: " + name);
                }
            }
        }
    }
    public static void main(String[] args) throws IOException {
        if (args.length != 2) throw new IllegalArgumentException("Expected actual staged runtime and sources JAR");
        for (String arg : args) check(Path.of(arg));
        System.out.println("Native witness absent from both actual staged runtime and sources JARs");
    }
}
