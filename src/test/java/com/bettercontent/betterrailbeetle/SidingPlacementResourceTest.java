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
        assertEquals(List.of("minecraft:count", "minecraft:in_square", "minecraft:height_range",
                "minecraft:environment_scan", "minecraft:random_offset", "minecraft:biome"), types);
        assertEquals(16, placement.get(0).getAsJsonObject().get("count").getAsInt());
        var height = placement.get(2).getAsJsonObject().getAsJsonObject("height");
        assertEquals(-48, height.getAsJsonObject("min_inclusive").get("absolute").getAsInt());
        assertEquals(64, height.getAsJsonObject("max_inclusive").get("absolute").getAsInt());
        var scan = placement.get(3).getAsJsonObject();
        assertEquals("down", scan.get("direction_of_search").getAsString());
        assertEquals(8, scan.get("max_steps").getAsInt());
        assertEquals("minecraft:air", scan.getAsJsonObject("allowed_search_condition").get("blocks").getAsString());
        assertEquals("minecraft:solid", scan.getAsJsonObject("target_condition").get("type").getAsString());
        assertEquals(1, placement.get(4).getAsJsonObject().get("y_spread").getAsInt());
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
