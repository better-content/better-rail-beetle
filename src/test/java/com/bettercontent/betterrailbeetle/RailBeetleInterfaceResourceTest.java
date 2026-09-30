package com.bettercontent.betterrailbeetle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RailBeetleInterfaceResourceTest {
    @Test void primaryControlsUsePlainLanguageWithoutRequiringTooltips() throws IOException {
        String lang = languageFile();
        assertTranslation(lang, "screen.better_rail_beetle.stop.button", "STOP");
        assertTranslation(lang, "screen.better_rail_beetle.reverse.button", "REVERSE");
        assertTranslation(lang, "screen.better_rail_beetle.half.button", "2 b/s");
        assertTranslation(lang, "screen.better_rail_beetle.normal.button", "4 b/s");
        assertTranslation(lang, "screen.better_rail_beetle.double.button", "8 b/s");
        assertTranslation(lang, "screen.better_rail_beetle.triple.button", "12 b/s");
        assertTranslation(lang, "screen.better_rail_beetle.neutral.off", "Free-roll: OFF");
        assertTranslation(lang, "screen.better_rail_beetle.brake.auto", "Brake: AUTO");
        assertTranslation(lang, "screen.better_rail_beetle.searchlight.off", "Light: OFF");
    }

    @Test void screenExplainsMachineryStateAndDestructiveCommands() throws IOException {
        String lang = languageFile();
        for (String key : new String[]{
                "screen.better_rail_beetle.machinery_locked.help.one",
                "screen.better_rail_beetle.machinery_locked.help.two",
                "screen.better_rail_beetle.machinery_ready.help.one",
                "screen.better_rail_beetle.machinery_ready.help.two",
                "screen.better_rail_beetle.current_mode",
                "screen.better_rail_beetle.remote.current_mode",
                "screen.better_rail_beetle.governor_hint.one",
                "screen.better_rail_beetle.governor_hint.two",
                "screen.better_rail_beetle.remote.governor_hint"
        }) {
            assertTrue(lang.contains("\"" + key + "\""), "missing explanatory UI copy: " + key);
        }
        assertTrue(lang.contains("This clears any active route"), "reverse must explain that it clears a route");
        assertTrue(lang.contains("Free-roll clears the active route"), "free-roll must explain its side effects");
    }

    @Test void routeBlockerMessagesNameLocationAndNextAction() throws IOException {
        String lang = languageFile();
        assertTranslation(lang, "message.better_rail_beetle.route_blocked.unloaded",
                "Rail Beetle stopped at %s: terrain is unloaded. Move closer.");
        assertTranslation(lang, "message.better_rail_beetle.route_blocked.geometry",
                "Rail Beetle stopped at %s: route geometry changed. Clear it or replan.");
        assertTranslation(lang, "message.better_rail_beetle.route_blocked.materials",
                "Rail Beetle stopped at %s: missing route materials. Restock it.");
    }

    @Test void routeHudPromptsAcceptTheCurrentKeyBinding() throws IOException {
        String lang = languageFile();
        assertTranslation(lang, "hud.better_rail_beetle.follow", "[%s] Follow route");
        assertTranslation(lang, "hud.better_rail_beetle.follow_missing", "[%s] Follow · Missing %s");
        assertTranslation(lang, "hud.better_rail_beetle.stop", "[%s] Stop");
        assertTranslation(lang, "hud.better_rail_beetle.clear", "[%s] Clear route & replan");
        assertTrue(!lang.contains("[G]"), "route HUD must not name a stale fixed key");
    }

    private static void assertTranslation(String lang, String key, String value) {
        assertTrue(lang.contains("\"" + key + "\": \"" + value + "\""),
                () -> "expected explicit label for " + key);
    }

    private String languageFile() throws IOException {
        try (var stream = getClass().getResourceAsStream("/assets/better_rail_beetle/lang/en_us.json")) {
            assertNotNull(stream, "missing English language file");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
