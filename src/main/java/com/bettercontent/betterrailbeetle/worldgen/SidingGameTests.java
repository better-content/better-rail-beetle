package com.bettercontent.betterrailbeetle.worldgen;

import com.bettercontent.betterrailbeetle.RailBeetleMod;
import com.bettercontent.betterrailbeetle.compat.CreateCompat;
import com.bettercontent.betterrailbeetle.entity.RailBeetleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(RailBeetleMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SidingGameTests {
    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "empty", timeoutTicks = 300)
    public static void north(GameTestHelper helper) { verify(helper, Direction.NORTH); }
    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "empty", timeoutTicks = 300)
    public static void south(GameTestHelper helper) { verify(helper, Direction.SOUTH); }
    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "empty", timeoutTicks = 300)
    public static void east(GameTestHelper helper) { verify(helper, Direction.EAST); }
    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "empty", timeoutTicks = 300)
    public static void west(GameTestHelper helper) { verify(helper, Direction.WEST); }

    @GameTest(templateNamespace = RailBeetleMod.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void caveFloorFeature(GameTestHelper helper) {
        var level = helper.getLevel();
        var chunk = new net.minecraft.world.level.ChunkPos(helper.absolutePos(new BlockPos(8, 0, 8)));
        // An authored underground pocket away from the centre column (which stays solid).
        // This tests the production placement pipeline, not natural-terrain frequency.
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            for (int y = -3; y <= 20; y++) {
                boolean cave = z >= 1 && z <= 5 && y >= 0 && y <= 4;
                level.setBlock(new BlockPos(chunk.getMinBlockX()+x, y, chunk.getMinBlockZ()+z),
                        (cave ? Blocks.AIR : Blocks.STONE).defaultBlockState(), 2);
            }
        }
        var placed = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE)
                .get(new net.minecraft.resources.ResourceLocation(RailBeetleMod.MOD_ID, "cave_siding"));
        helper.assertTrue(placed != null, "Production siding placed feature must load");
        var generator = level.getChunkSource().getGenerator();
        var feature = SidingWorldgen.FEATURE.get();
        var config = net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE;
        var random = net.minecraft.util.RandomSource.create(123);
        BlockPos centre = new BlockPos(chunk.getMinBlockX()+8, 0, chunk.getMinBlockZ()+8);
        helper.assertTrue(!feature.place(config, level, generator, random, centre),
                "Solid rock must reject without excavation");
        BlockPos floor = new BlockPos(chunk.getMinBlockX()+8, 0, chunk.getMinBlockZ()+3);
        level.setBlock(floor, Blocks.WATER.defaultBlockState(), 2);
        helper.assertTrue(!feature.place(config, level, generator, random, floor),
                "Flooded floor must reject the feature");
        level.setBlock(floor, Blocks.AIR.defaultBlockState(), 2);
        helper.assertTrue(!feature.place(config, level, generator, random,
                        new BlockPos(chunk.getMinBlockX()+1, 0, chunk.getMinBlockZ()+3)),
                "A siding crossing the chunk boundary must reject");
        BlockPos start = chunk.getWorldPosition();
        boolean success = false;
        for (int seed = 0; seed < 512 && !success; seed++) {
            success = placed.placeWithBiomeCheck(level, generator, net.minecraft.util.RandomSource.create(seed), start);
        }
        helper.assertTrue(success, "Production feature must find an off-centre cave floor");
        long markers = level.getChunk(start).getBlockEntitiesPos().stream()
                .filter(pos -> level.getBlockState(pos).is(SidingWorldgen.MARKER.get())).count();
        helper.assertTrue(markers == 1, "Multiple attempts must leave only one siding in the chunk");
        helper.assertTrue(!placed.placeWithBiomeCheck(level, generator, net.minecraft.util.RandomSource.create(42), start),
                "Further attempts must not create another siding in the same chunk");
        helper.assertTrue(level.getBlockState(new BlockPos(chunk.getMinBlockX()+8, 0, chunk.getMinBlockZ()+8)).is(Blocks.STONE),
                "Feature must not excavate the solid centre column");
        helper.succeed();
    }

    private static void verify(GameTestHelper helper, Direction heading) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 3, 8));
        for (int along = -6; along <= 6; along++) for (int side = -1; side <= 1; side++) {
            BlockPos p = SidingPiece.position(origin, heading, along, side);
            level.setBlockAndUpdate(p.below(), Blocks.COBBLESTONE.defaultBlockState());
            for (int h = 0; h < 3; h++) level.setBlockAndUpdate(p.above(h), Blocks.AIR.defaultBlockState());
        }
        var bounds = new net.minecraft.world.level.levelgen.structure.BoundingBox(origin.getX()-6, origin.getY()-3, origin.getZ()-6, origin.getX()+6, origin.getY()+3, origin.getZ()+6);
        helper.assertTrue(SidingPiece.fits(level, bounds, origin, heading), "Dry supported cave must accept siding");
        BlockPos exit = SidingPiece.position(origin, heading, 6, 0);
        level.setBlockAndUpdate(exit, Blocks.STONE.defaultBlockState());
        helper.assertTrue(!SidingPiece.fits(level, bounds, origin, heading), "Blocked departure must reject entire site");
        level.setBlockAndUpdate(exit, Blocks.WATER.defaultBlockState());
        helper.assertTrue(!SidingPiece.fits(level, bounds, origin, heading), "Flooded departure must reject entire site");
        level.setBlockAndUpdate(exit, Blocks.AIR.defaultBlockState());
        SidingPiece.place(level, origin, heading);
        BlockPos markerPos = SidingPiece.position(origin, heading, -5, 1).below();
        var marker = (SidingMarker) level.getBlockEntity(markerPos);
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(marker.complete(), "Siding must initialize and couple exactly once");
            var beetle = (RailBeetleEntity) level.getEntity(marker.vehicleId(level, 0));
            var passenger = (AbstractMinecart) level.getEntity(marker.vehicleId(level, 1));
            var freight = (MinecartChest) level.getEntity(marker.vehicleId(level, 2));
            helper.assertTrue(beetle != null && passenger != null && freight != null, "All three vehicles must exist");
            helper.assertTrue(CreateCompat.isInSameConsist(beetle, passenger) && CreateCompat.isInSameConsist(beetle, freight), "Both trailing carts must be coupled");
            helper.assertTrue(beetle.noseHeading() == heading && beetle.brakeApplied(), "Found Beetle must face the exit with brakes on");
            helper.assertTrue(beetle.railCount() == 64 && beetle.supportCount() == 64 && beetle.inventory().getStackInSlot(2).getCount() == 16 && beetle.fuelTicks() == 0,
                    "Unclaimed Beetle must not burn its working supplies");
            helper.assertTrue(freight.getItem(0).is(Items.RAIL) && freight.getItem(0).getCount() == 64, "Freight must contain reserves");
            var savedBeetle = new net.minecraft.nbt.CompoundTag();
            beetle.saveWithoutId(savedBeetle);
            beetle.load(savedBeetle);
            helper.assertTrue(beetle.noseHeading() == heading, "Heading must survive reload");
            var start = beetle.position();
            var passengerStart = passenger.position();
            var freightStart = freight.position();
            var player = helper.makeMockPlayer();
            player.setPos(beetle.getX(), beetle.getY()+1, beetle.getZ());
            beetle.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(beetle.proposals().stream().anyMatch(route -> route.steps().size() >= 3), "Opened discovery must offer a usable first route"))
                    .thenExecute(() -> {
                        var route = beetle.proposals().stream().filter(candidate -> candidate.steps().size() >= 3).findFirst().orElseThrow();
                        var first = route.steps().get(0).railPos();
                        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                                new net.minecraft.world.phys.Vec3(first.getX()+0.5, first.getY()+0.2, first.getZ()+0.5));
                        beetle.selectRoute(player, route.generation(), route.id());
                        helper.assertTrue(beetle.activeRoute() != null, "Discovery route must be selectable without preparation");
                    })
                    .thenWaitUntil(() -> helper.assertTrue(beetle.position().distanceTo(start) >= 2
                            && passenger.position().distanceTo(passengerStart) > 0.5
                            && freight.position().distanceTo(freightStart) > 0.5, "Entire supplied consist must depart"))
                    .thenExecute(() -> {
                        helper.assertTrue(beetle.railCount() < 64, "First departure must build using supplied rails");
                        marker.load(marker.saveWithFullMetadata());
                        beetle.discard(); passenger.discard(); freight.discard();
                    })
                    .thenIdle(45)
                    .thenExecute(() -> helper.assertTrue(level.getEntity(marker.vehicleId(level, 0)) == null
                            && level.getEntity(marker.vehicleId(level, 1)) == null
                            && level.getEntity(marker.vehicleId(level, 2)) == null, "Committed site must never replace removed vehicles"))
                    .thenSucceed();
        });
    }
}
