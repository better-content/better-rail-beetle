package com.bettercontent.betterrailbeetle;

import com.google.gson.JsonParser;
import com.bettercontent.betterrailbeetle.upgrade.EngineKind;
import com.bettercontent.betterrailbeetle.upgrade.ModuleKind;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

final class RailBeetleResourceTest {
    @Test void entityMaterialAtlasIsPresentAtMinecraftTextureResolution() throws IOException {
        try (var stream = getClass().getResourceAsStream(
                "/assets/better_rail_beetle/textures/entity/better_rail_beetle.png")) {
            assertNotNull(stream, "missing Rail Beetle entity texture atlas");
            var image = ImageIO.read(stream);
            assertNotNull(image, "Rail Beetle entity texture atlas must be a readable image");
            assertEquals(64, image.getWidth());
            assertEquals(64, image.getHeight());
        }
    }

    @Test void everyMachineryItemHasANameModelAndTooltip() throws IOException {
        String lang = resource("/assets/better_rail_beetle/lang/en_us.json");
        for (EngineKind kind : EngineKind.values()) {
            assertTrue(lang.contains("\"engine.better_rail_beetle." + kind.id() + "\""));
            if (kind.builtIn()) continue;
            assertNotNull(getClass().getResource("/assets/better_rail_beetle/models/item/" + kind.id() + ".json"));
            assertTrue(lang.contains("\"item.better_rail_beetle." + kind.id() + "\""));
            assertTrue(lang.contains("\"tooltip.better_rail_beetle.engine." + kind.id() + "\""));
        }
        for (ModuleKind kind : ModuleKind.values()) {
            assertNotNull(getClass().getResource("/assets/better_rail_beetle/models/item/" + kind.id() + ".json"));
            assertTrue(lang.contains("\"item.better_rail_beetle." + kind.id() + "\""));
            assertTrue(lang.contains("\"tooltip.better_rail_beetle.module." + kind.id() + "\""));
        }
    }

    @Test void craftableEntryPointsRetainTheirActualIngredientRoots() throws IOException {
        var beetle = JsonParser.parseString(resource("/data/better_rail_beetle/recipes/better_rail_beetle.json"))
                .getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", beetle.get("type").getAsString());
        assertEquals("better_rail_beetle:better_rail_beetle",
                beetle.getAsJsonObject("result").get("item").getAsString());
        assertEquals("[\" C \",\"RFR\",\" M \"]", beetle.getAsJsonArray("pattern").toString());
        var beetleKey = beetle.getAsJsonObject("key");
        assertEquals("minecraft:compass", beetleKey.getAsJsonObject("C").get("item").getAsString());
        assertEquals("minecraft:rail", beetleKey.getAsJsonObject("R").get("item").getAsString());
        assertEquals("minecraft:furnace", beetleKey.getAsJsonObject("F").get("item").getAsString());
        assertEquals("minecraft:minecart", beetleKey.getAsJsonObject("M").get("item").getAsString());

        var beacon = JsonParser.parseString(resource("/data/better_rail_beetle/recipes/route_beacon.json"))
                .getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", beacon.get("type").getAsString());
        assertEquals("better_rail_beetle:route_beacon",
                beacon.getAsJsonObject("result").get("item").getAsString());
        assertEquals("[\" C \",\"IRI\",\" P \"]", beacon.getAsJsonArray("pattern").toString());
        var beaconKey = beacon.getAsJsonObject("key");
        assertEquals("minecraft:compass", beaconKey.getAsJsonObject("C").get("item").getAsString());
        assertEquals("minecraft:copper_ingot", beaconKey.getAsJsonObject("I").get("item").getAsString());
        assertEquals("minecraft:redstone_torch", beaconKey.getAsJsonObject("R").get("item").getAsString());
        assertEquals("minecraft:planks", beaconKey.getAsJsonObject("P").get("tag").getAsString());
    }

    @Test void allPowerProvidersRemainOptional() throws IOException {
        String metadata = resource("/META-INF/mods.toml");
        for (String mod : new String[]{"create", "powergrid", "ars_nouveau", "bloodmagic",
                "pneumaticcraft", "goety", "malum", "sodiumdynamiclights"}) {
            int start = metadata.indexOf("modId=\"" + mod + "\"");
            assertTrue(start >= 0, "missing optional metadata for " + mod);
            assertTrue(metadata.substring(start, Math.min(metadata.length(), start + 180)).contains("mandatory=false"),
                    mod + " must be optional");
        }
    }

    private String resource(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
