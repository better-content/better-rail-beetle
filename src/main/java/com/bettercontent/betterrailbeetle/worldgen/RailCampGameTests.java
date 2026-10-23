package com.bettercontent.betterrailbeetle.worldgen;

import com.bettercontent.betterrailbeetle.RailBeetleMod;
import com.bettercontent.betterrailbeetle.compat.CreateCompat;
import com.bettercontent.betterrailbeetle.entity.RailBeetleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(RailBeetleMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RailCampGameTests {
    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "rail_camp", timeoutTicks = 300)
    public static void northFacesNaturalCave(GameTestHelper helper) { orientation(helper, Direction.NORTH); }
    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "rail_camp", timeoutTicks = 300)
    public static void southFacesNaturalCave(GameTestHelper helper) { orientation(helper, Direction.SOUTH); }
    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "rail_camp", timeoutTicks = 300)
    public static void eastFacesNaturalCave(GameTestHelper helper) { orientation(helper, Direction.EAST); }
    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "rail_camp", timeoutTicks = 300)
    public static void westFacesNaturalCave(GameTestHelper helper) { orientation(helper, Direction.WEST); }

    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "rail_camp", timeoutTicks = 200)
    public static void nativeNoiseTerrainHasPlaceableCaveWalls(GameTestHelper helper) {
        var level = helper.getLevel();
        var preset = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                .getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL);
        var generator = (net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator) preset.createWorldDimensions().overworld();
        var noise = net.minecraft.world.level.levelgen.RandomState.create(generator.generatorSettings().value(),
                level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.NOISE), 12345L);
        var destination = new ChunkPos(helper.absolutePos(new BlockPos(24, 20, 24)));
        var feature = SidingWorldgen.FEATURE.get();
        var config = net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE;
        var destinationChunk = level.getChunk(destination.getWorldPosition());
        var originalReferences = new java.util.HashMap<>(destinationChunk.getAllReferences());
        destinationChunk.setAllReferences(java.util.Map.of());
        int found = 0;
        // Reproducible native noise-column samples, not authored rectangular pockets. This is
        // deliberately not a claim about full-pack carvers, decorations or the rarity-filtered rate.
        try {
            for (int sample = 0; sample < 16; sample++) {
                var source = new ChunkPos(400 + sample*17, 700 + sample*29);
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                    var column = generator.getBaseColumn(source.getMinBlockX()+dx, source.getMinBlockZ()+dz, level, noise);
                    for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                        level.setBlock(new BlockPos(destination.getMinBlockX()+dx, y, destination.getMinBlockZ()+dz), column.getBlock(y), 2);
                    }
                }
                net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(destinationChunk,
                        java.util.EnumSet.of(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG));
                if (feature.place(config, level, level.getChunkSource().getGenerator(),
                        net.minecraft.util.RandomSource.create(sample), destination.getWorldPosition())) found++;
            }
        } finally {
            destinationChunk.setAllReferences(originalReferences);
        }
        com.mojang.logging.LogUtils.getLogger().info("Rail camp native noise probe: {} / 16 selected chunks accepted, seed 12345 (before rarity/carvers/decorations)", found);
        helper.assertTrue(found > 0, "Camp must accept at least one reproducible native cave-wall sample, not only hand-built test rooms");
        helper.succeed();
    }

    private static BlockPos fixture(GameTestHelper helper, Direction heading) {
        var level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(24, 20, 24));
        var chunk = new ChunkPos(anchor);
        int centre = heading == Direction.NORTH || heading == Direction.WEST ? 7 : 8;
        BlockPos origin = new BlockPos(chunk.getMinBlockX()+centre, anchor.getY(), chunk.getMinBlockZ()+centre);
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = origin.getY()-3; y <= origin.getY()+20; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        // Only the natural cave is open. The train's entire future parking space starts as rock.
        for (int along = 2; along <= 7; along++) for (int side = -7; side <= 7; side++) {
            for (int h = 0; h < 4; h++) {
                level.setBlock(RailCamp.position(origin, heading, along, side).above(h), Blocks.CAVE_AIR.defaultBlockState(), 2);
            }
        }
        return origin;
    }

    private static BoundingBox chunkBounds(BlockPos origin) {
        var chunk = new ChunkPos(origin);
        return new BoundingBox(chunk.getMinBlockX(), -64, chunk.getMinBlockZ(), chunk.getMaxBlockX(), 319, chunk.getMaxBlockZ());
    }

    private static void orientation(GameTestHelper helper, Direction heading) {
        var level = helper.getLevel();
        BlockPos origin = fixture(helper, heading);
        helper.assertTrue(level.getBlockState(origin).is(Blocks.STONE), "Future siding must initially be rock, not a perfect natural corridor");
        helper.assertTrue(RailCamp.fits(level, chunkBounds(origin), origin, heading), "Cave wall must accept camp in requested direction");
        helper.assertTrue(!RailCamp.fits(level, chunkBounds(origin), origin, heading.getOpposite()), "Reversed nose into rock must reject");
        RailCamp.place(level, origin, heading);
        var marker = (SidingMarker) level.getBlockEntity(RailCamp.position(origin, heading, -5, 1).below());
        helper.assertTrue(level.getBlockState(RailCamp.position(origin, heading, -3, 3)).is(Blocks.CRAFTING_TABLE), "Camp must contain its workbench");
        helper.assertTrue(level.getBlockState(RailCamp.position(origin, heading, -1, 3)).is(Blocks.CAMPFIRE), "Camp must contain its abandoned hearth");
        helper.assertTrue(level.getBlockState(RailCamp.position(origin, heading, 2, 0)).is(Blocks.CAVE_AIR), "Natural cave mouth must remain unexcavated");
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(marker.complete(), "Camp consist must initialize");
            var beetle = (RailBeetleEntity) level.getEntity(marker.vehicleId(level, 0));
            var passenger = level.getEntity(marker.vehicleId(level, 1));
            var freight = level.getEntity(marker.vehicleId(level, 2));
            helper.assertTrue(beetle.noseHeading() == heading && beetle.brakeApplied(), "Nose must face the open cave, not the camp's back wall");
            helper.assertTrue(CreateCompat.isInSameConsist(beetle, passenger) && CreateCompat.isInSameConsist(beetle, freight), "Found consist must already be coupled");
            helper.assertTrue(beetle.railCount() == 64 && beetle.inventory().getStackInSlot(2).getCount() == 16, "Undiscovered camp must preserve supplies");
            var start = beetle.position();
            var freightStart = freight.position();
            var player = helper.makeMockPlayer();
            player.setPos(beetle.getX(), beetle.getY()+1, beetle.getZ());
            beetle.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(beetle.proposals().stream().anyMatch(route -> route.steps().size() >= 2), "Nose must offer a route into the natural cave"))
                    .thenExecute(() -> {
                        var route = beetle.proposals().stream().filter(candidate -> candidate.steps().size() >= 2).findFirst().orElseThrow();
                        BlockPos first = route.steps().get(0).railPos();
                        helper.assertTrue(first.equals(RailCamp.position(origin, heading, 2, 0)), "First built rail must be outside the camp, in front of the nose");
                        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                                new net.minecraft.world.phys.Vec3(first.getX()+0.5, first.getY()+0.2, first.getZ()+0.5));
                        beetle.selectRoute(player, route.generation(), route.id());
                        helper.assertTrue(beetle.activeRoute() != null, "Camp route must select normally");
                    })
                    .thenWaitUntil(() -> helper.assertTrue(beetle.position().distanceTo(start) >= 1.5
                            && freight.position().distanceTo(freightStart) > 0.5, "Consist must leave the excavated camp toward the cave"))
                    .thenExecute(() -> helper.assertTrue(beetle.railCount() < 64, "Departure must use found supplies"))
                    .thenSucceed();
        });
    }

    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "rail_camp", timeoutTicks = 100)
    public static void unsafeExcavationRejectsWithoutWrites(GameTestHelper helper) {
        var level = helper.getLevel();
        Direction heading = Direction.EAST;
        BlockPos origin = fixture(helper, heading);
        var bounds = chunkBounds(origin);
        BlockPos shell = RailCamp.position(origin, heading, -4, 5).above();
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.WATER, Blocks.LAVA, Blocks.CHEST, Blocks.STONE_BRICKS}) {
            level.setBlock(shell, block.defaultBlockState(), 2);
            helper.assertTrue(!RailCamp.fits(level, bounds, origin, heading), "Fluid, block entity or built structure must veto excavation");
            helper.assertTrue(level.getBlockState(origin).is(Blocks.STONE), "Rejected site must not carve or place rails");
            level.setBlock(shell, Blocks.STONE.defaultBlockState(), 2);
        }
        helper.assertTrue(!RailCamp.fits(level, bounds, origin.relative(heading, -1), heading), "Safety shell outside owning chunk must reject");
        // Remove the roof above one side: the room must not open a hole to the surface.
        BlockPos roof = RailCamp.position(origin, heading, -3, 3);
        for (int h = 1; h <= 20; h++) level.setBlock(roof.above(h), Blocks.AIR.defaultBlockState(), 2);
        helper.assertTrue(!RailCamp.fits(level, bounds, origin, heading), "Insufficient overhead cover must reject");
        // Deterministic regression for arena clearing of a lazy worldgen block entity.
        // No read/materialization between installing the DUMMY and the real native clear.
        BlockPos lazy = origin.above(2);
        level.setBlock(lazy, Blocks.CHEST.defaultBlockState(), 2);
        var chunk = level.getChunkAt(lazy);
        chunk.removeBlockEntity(lazy);
        var pending = new net.minecraft.nbt.CompoundTag();
        pending.putString("id", "DUMMY");
        pending.putInt("x", lazy.getX());
        pending.putInt("y", lazy.getY());
        pending.putInt("z", lazy.getZ());
        chunk.setBlockEntityNbt(pending);
        helper.assertTrue(!chunk.getBlockEntities().containsKey(lazy) && chunk.getBlockEntityNbt(lazy) != null,
                "Regression must start with genuinely pending, unmaterialized chest data");
        net.minecraft.gametest.framework.StructureUtils.clearSpaceForStructure(
                new BoundingBox(lazy), lazy.getY(), level);
        helper.assertTrue(level.getBlockState(lazy).isAir() && chunk.getBlockEntityNbt(lazy) == null
                        && !chunk.getBlockEntities().containsKey(lazy),
                "Native clear must decode before replacing the state, then remove the real entity without orphan pending data");
        helper.succeed();
    }

    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "rail_camp", timeoutTicks = 100)
    public static void existingStructureVetoesCampBeforeExcavation(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = fixture(helper, Direction.SOUTH);
        var chunk = level.getChunk(origin);
        var structure = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .get(new net.minecraft.resources.ResourceLocation(RailBeetleMod.MOD_ID, "cave_siding"));
        var starts = new java.util.HashMap<>(chunk.getAllStarts());
        var references = new java.util.HashMap<>(chunk.getAllReferences());
        try {
            var start = new net.minecraft.world.level.levelgen.structure.StructureStart(structure, chunk.getPos(), 0,
                    new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(new SidingPiece(origin))));
            chunk.setStartForStructure(structure, start);
            chunk.addReferenceForStructure(structure, chunk.getPos().toLong());
            helper.assertTrue(!SidingWorldgen.FEATURE.get().place(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
                    level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(123), chunk.getPos().getWorldPosition()),
                    "Existing structure bounds must veto a camp even if its blocks are ordinary stone");
            helper.assertTrue(level.getBlockState(origin).is(Blocks.STONE), "Structure veto must leave terrain unchanged");
        } finally {
            chunk.setAllStarts(starts);
            chunk.setAllReferences(references);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "rail_camp", timeoutTicks = 100)
    public static void productionFeatureFindsCaveAirWallInOneSearch(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = fixture(helper, Direction.SOUTH);
        var feature = SidingWorldgen.FEATURE.get();
        var config = net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE;
        var generator = level.getChunkSource().getGenerator();
        BlockPos start = new ChunkPos(origin).getWorldPosition();
        helper.assertTrue(feature.place(config, level, generator, net.minecraft.util.RandomSource.create(123), start),
                "One selected chunk search must find a cave_air wall without hundreds of retry seeds");
        long markers = level.getChunk(start).getBlockEntitiesPos().stream()
                .filter(pos -> level.getBlockState(pos).is(SidingWorldgen.MARKER.get())).count();
        helper.assertTrue(markers == 1, "One chunk must have one camp");
        helper.assertTrue(!feature.place(config, level, generator, net.minecraft.util.RandomSource.create(456), start), "Further searches must not carve another camp");
        helper.succeed();
    }
}
