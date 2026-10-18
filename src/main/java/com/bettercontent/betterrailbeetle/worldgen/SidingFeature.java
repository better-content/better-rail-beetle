package com.bettercontent.betterrailbeetle.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.fml.ModList;

/** Cave-floor decoration: the placed feature finds air above ground after cave carving. */
public final class SidingFeature extends Feature<NoneFeatureConfiguration> {
    public SidingFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!ModList.get().isLoaded("create")) return false;
        var level = context.level();
        BlockPos origin = context.origin();
        if (origin.getY() < -48 || origin.getY() > 64
                || origin.getY() > level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,
                        origin.getX(), origin.getZ()) - 16) return false;

        // Check only this chunk: neither geometry checks nor initialization request neighbours.
        var chunk = level.getChunk(origin);
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (chunk.getBlockState(pos).is(SidingWorldgen.MARKER.get())) return false;
        }
        ChunkPos chunkPos = chunk.getPos();
        var bounds = new BoundingBox(chunkPos.getMinBlockX(), level.getMinBuildHeight(),
                chunkPos.getMinBlockZ(), chunkPos.getMaxBlockX(), level.getMaxBuildHeight() - 1,
                chunkPos.getMaxBlockZ());
        int firstDirection = context.random().nextInt(4);
        for (int d = 0; d < 4; d++) {
            Direction heading = Direction.from2DDataValue((firstDirection + d) % 4);
            if (!SidingPiece.fits(level, bounds, origin, heading)) continue;
            SidingPiece.place(level, origin, heading);
            return true;
        }
        return false;
    }
}
