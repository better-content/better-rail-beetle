package com.bettercontent.betterrailbeetle.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.fml.ModList;

/** One rarity-gated, bounded chunk search for a natural cave mouth backed by excavatable rock. */
public final class SidingFeature extends Feature<NoneFeatureConfiguration> {
    public SidingFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!ModList.get().isLoaded("create")) return false;
        var level = context.level();
        var chunk = level.getChunk(context.origin());
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (chunk.getBlockState(pos).is(SidingWorldgen.MARKER.get())) return false;
        }
        ChunkPos chunkPos = chunk.getPos();
        var bounds = new BoundingBox(chunkPos.getMinBlockX(), level.getMinBuildHeight(),
                chunkPos.getMinBlockZ(), chunkPos.getMaxBlockX(), level.getMaxBuildHeight()-1, chunkPos.getMaxBlockZ());
        var manager = level.getLevel().structureManager();
        if (level instanceof WorldGenRegion region) manager = manager.forWorldGenRegion(region);
        var structures = manager.startsForStructure(chunkPos, structure -> true);
        int firstColumn = context.random().nextInt(256);
        int firstDirection = context.random().nextInt(4);
        int lowest = Math.max(-48, level.getMinBuildHeight()+4);
        // 73 is coprime to 256: visits each column exactly once in a seed-dependent order.
        for (int n = 0; n < 256; n++) {
            int column = (firstColumn + n*73) & 255;
            int x = chunkPos.getMinBlockX() + (column & 15);
            int z = chunkPos.getMinBlockZ() + (column >> 4);
            int top = Math.min(64, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z)-16);
            for (int y = top; y >= lowest; y--) {
                BlockPos mouth = new BlockPos(x, y, z);
                // isAir accepts ordinary air AND cave_air. No random-height/environment-scan lottery.
                if (!level.getBlockState(mouth).isAir() || !level.getBlockState(mouth.below()).isSolid()) continue;
                for (int d = 0; d < 4; d++) {
                    Direction heading = Direction.from2DDataValue((firstDirection+d)%4);
                    BlockPos origin = mouth.relative(heading.getOpposite(), 2);
                    BoundingBox camp = RailCamp.bounds(origin, heading);
                    if (structures.stream().anyMatch(start -> start.getBoundingBox().intersects(camp))) continue;
                    if (!RailCamp.fits(level, bounds, origin, heading)) continue;
                    RailCamp.place(level, origin, heading);
                    return true;
                }
            }
        }
        return false;
    }
}
