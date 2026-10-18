package com.bettercontent.betterrailbeetle.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Everything, including the departure runway, stays inside the owning chunk. */
public final class SidingPiece extends StructurePiece {
    private final BlockPos center;
    public SidingPiece(BlockPos center) {
        super(SidingWorldgen.PIECE.get(), 0, new BoundingBox(center.getX()-6, -64, center.getZ()-6, center.getX()+6, 96, center.getZ()+6));
        this.center = center.immutable();
    }
    public SidingPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        this(BlockPos.of(tag.getLong("Center")));
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("Center", center.asLong());
    }
    public static BlockPos position(BlockPos origin, Direction heading, int along, int side) {
        return origin.relative(heading, along).relative(heading.getClockWise(), side);
    }
    @Override public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
            RandomSource random, BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        // No partial placement: validate the full footprint before changing any blocks.
        int top = Math.min(64, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, center.getX(), center.getZ()) - 16);
        int firstDirection = random.nextInt(4);
        for (int y = top; y >= Math.max(-48, level.getMinBuildHeight()+4); y--) {
            BlockPos origin = new BlockPos(center.getX(), y, center.getZ());
            for (int d = 0; d < 4; d++) {
                Direction heading = Direction.from2DDataValue((firstDirection+d)%4);
                if (!fits(level, box, origin, heading)) continue;
                place(level, origin, heading);
                return;
            }
        }
    }
    public static boolean fits(net.minecraft.world.level.LevelReader level, BoundingBox box, BlockPos origin, Direction heading) {
        for (int along = -6; along <= 6; along++) for (int side = -1; side <= 1; side++) {
            BlockPos p = position(origin, heading, along, side);
            if (!box.isInside(p.below(3)) || !box.isInside(p.above(2))) return false;
            for (int h = 0; h < 3; h++) if (!level.getBlockState(p.above(h)).isAir()) return false;
            // Only shallow foundation repairs; never a platform over a deep ravine or water.
            boolean supported = false;
            for (int depth = 1; depth <= 3; depth++) {
                var state = level.getBlockState(p.below(depth));
                if (!state.getFluidState().isEmpty() || state.hasBlockEntity()) return false;
                if (state.isFaceSturdy(level, p.below(depth), Direction.UP)) { supported = true; break; }
                if (!state.isAir()) return false;
            }
            if (!supported) return false;
        }
        return true;
    }
    static void place(net.minecraft.world.level.LevelAccessor level, BlockPos origin, Direction heading) {
        for (int along = -6; along <= 1; along++) for (int side = -1; side <= 1; side++) {
            BlockPos p = position(origin, heading, along, side).below();
            for (int depth = 0; depth < 3; depth++) {
                BlockPos support = p.below(depth);
                if (depth > 0 && !level.getBlockState(support).isAir()) break;
                level.setBlock(support, Blocks.COBBLESTONE.defaultBlockState(), 2);
            }
        }
        var rail = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE,
                heading.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH);
        for (int along = -6; along <= 1; along++) level.setBlock(position(origin, heading, along, 0), rail, 2);
        BlockPos marker = position(origin, heading, -5, 1).below();
        level.setBlock(marker, SidingWorldgen.MARKER.get().defaultBlockState(), 2);
        if (level.getBlockEntity(marker) instanceof SidingMarker initializer) initializer.configure(origin, heading);
    }
}
