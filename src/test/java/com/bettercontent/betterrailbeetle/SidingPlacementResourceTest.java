package com.bettercontent.betterrailbeetle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SidingPlacementResourceTest {
    private static final String ROOT = "/data/better_rail_beetle/";

    @Test void discoveriesUsePostCarvingCaveFloorPlacementInsteadOfStructureCandidates() throws IOException {
        var modifier = json("forge/biome_modifier/add_cave_siding.json");
        assertEquals("forge:add_features", modifier.get("type").getAsString());
        assertEquals("#minecraft:is_overworld", modifier.get("biomes").getAsString());
        assertEquals("underground_decoration", modifier.get("step").getAsString());
        assertEquals("better_rail_beetle:cave_siding", modifier.get("features").getAsString());
        var configured = json("worldgen/configured_feature/cave_siding.json");
        assertEquals("better_rail_beetle:cave_siding", configured.get("type").getAsString());
        var placed = json("worldgen/placed_feature/cave_siding.json");
        assertEquals("better_rail_beetle:cave_siding", placed.get("feature").getAsString());
        var placement = placed.getAsJsonArray("placement");
        var types = new ArrayList<String>();
        placement.forEach(value -> types.add(value.getAsJsonObject().get("type").getAsString()));
        assertEquals(List.of("minecraft:rarity_filter", "minecraft:biome"), types);
        assertEquals(16, placement.get(0).getAsJsonObject().get("chance").getAsInt(),
                "One bounded cave-wall search per selected chunk, not 16 random-height guesses");
        assertNull(getClass().getResource(ROOT + "worldgen/structure_set/cave_sidings.json"),
                "No new phantom structure starts should be scheduled");
        assertNotNull(getClass().getResource(ROOT + "worldgen/structure/cave_siding.json"),
                "Keep the legacy structure definition for existing saves");
    }

    private JsonObject json(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(ROOT + path)) {
            assertNotNull(stream, path);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
