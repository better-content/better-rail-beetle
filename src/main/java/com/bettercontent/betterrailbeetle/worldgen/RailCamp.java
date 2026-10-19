package com.bettercontent.betterrailbeetle.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** An excavated work-camp alcove. Positive along always points OUT into the natural cave. */
public final class RailCamp {
    private RailCamp() {}

    public static BlockPos position(BlockPos origin, Direction heading, int along, int side) {
        return SidingPiece.position(origin, heading, along, side);
    }

    /** Includes an untouched one-block safety shell, not just the carved room. */
    public static BoundingBox bounds(BlockPos origin, Direction heading) {
        BlockPos a = position(origin, heading, -8, -3).below(3);
        BlockPos b = position(origin, heading, 3, 5).above(5);
        return new BoundingBox(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), b.getY(), Math.max(a.getZ(), b.getZ()));
    }

    private static boolean natural(BlockState state) {
        return state.isAir() || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT) || state.is(Blocks.GRAVEL);
    }

    public static boolean fits(LevelReader level, BoundingBox chunkBounds, BlockPos origin, Direction heading) {
        BoundingBox box = bounds(origin, heading);
        if (!chunkBounds.isInside(new BlockPos(box.minX(), box.minY(), box.minZ()))
                || !chunkBounds.isInside(new BlockPos(box.maxX(), box.maxY(), box.maxZ()))) return false;
        // The nose is at +1. The mouth and first two steps are existing cave, never excavated.
        for (int along = 2; along <= 3; along++) for (int side = -1; side <= 1; side++) {
            BlockPos p = position(origin, heading, along, side);
            for (int h = 0; h < 3; h++) if (!level.getBlockState(p.above(h)).isAir()) return false;
            if (!hasGround(level, p)) return false;
        }
        // A real wall behind the train, not an arbitrary platform in the middle of a cave.
        if (!level.getBlockState(position(origin, heading, -6, 0).above()).is(BlockTags.BASE_STONE_OVERWORLD)) return false;
        // Check before any writes. Fluids, block entities, ores and manufactured blocks are not excavated.
        for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
            if (origin.getY() > level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 16) return false;
            for (int y = box.minY(); y <= box.maxY(); y++) {
                var state = level.getBlockState(new BlockPos(x, y, z));
                if (!state.getFluidState().isEmpty() || state.hasBlockEntity()
                        || (!natural(state) && !state.is(net.minecraftforge.common.Tags.Blocks.ORES))) return false;
            }
        }
        for (int along = -7; along <= 1; along++) for (int side = -2; side <= maxSide(along); side++) {
            BlockPos p = position(origin, heading, along, side);
            if (!hasGround(level, p) || !natural(level.getBlockState(p.below()))) return false;
            int height = along == -7 || side == 4 ? 3 : 4;
            for (int h = 0; h < height; h++) if (!natural(level.getBlockState(p.above(h)))) return false;
            // Explicit overburden as well as the generation heightmap: never excavate a sky shaft.
            int cover = 0;
            for (int h = 4; h <= 16; h++) if (level.getBlockState(p.above(h)).isSolid()) cover++;
            if (cover < 4) return false;
        }
        return true;
    }

    private static boolean hasGround(LevelReader level, BlockPos p) {
        for (int depth = 1; depth <= 3; depth++) {
            BlockPos floor = p.below(depth);
            var state = level.getBlockState(floor);
            if (!state.getFluidState().isEmpty() || state.hasBlockEntity()) return false;
            if (state.isFaceSturdy(level, floor, Direction.UP)) return true;
            if (!state.isAir()) return false;
        }
        return false;
    }

    private static int maxSide(int along) { return along >= -5 && along <= -1 ? 4 : 2; }

    public static void place(LevelAccessor level, BlockPos origin, Direction heading) {
        for (int along = -7; along <= 1; along++) for (int side = -2; side <= maxSide(along); side++) {
            BlockPos p = position(origin, heading, along, side);
            int height = along == -7 || side == 4 ? 3 : 4;
            for (int h = 0; h < height; h++) level.setBlock(p.above(h), Blocks.CAVE_AIR.defaultBlockState(), 2);
            for (int depth = 1; depth <= 3; depth++) {
                BlockPos floor = p.below(depth);
                if (depth > 1 && !level.getBlockState(floor).isAir()) break;
                level.setBlock(floor, ((along + side) % 5 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState(), 2);
            }
        }
        // Broken timber canopy: all posts and debris stay outside the vehicle/rail clearance.
        for (int along : new int[]{-6, -2}) {
            for (int h = 0; h < (along == -6 ? 3 : 2); h++) {
                set(level, origin, heading, along, -2, h, Blocks.STRIPPED_OAK_LOG.defaultBlockState());
            }
        }
        for (int side = -2; side <= 2; side++) {
            set(level, origin, heading, -6, side, 3, Blocks.STRIPPED_OAK_LOG.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, heading.getClockWise().getAxis()));
        }
        set(level, origin, heading, -6, 2, 2, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(level, origin, heading, -3, 3, 0, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, origin, heading, -1, 3, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        // Materialize its empty block entity during worldgen rather than leaving a DUMMY NBT entry.
        level.getBlockEntity(position(origin, heading, -1, 3));
        set(level, origin, heading, -4, -2, 0, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        set(level, origin, heading, -4, 4, 0, Blocks.OAK_SLAB.defaultBlockState());
        SidingPiece.place(level, origin, heading);
    }

    private static void set(LevelAccessor level, BlockPos origin, Direction heading, int along, int side, int up, BlockState state) {
        level.setBlock(position(origin, heading, along, side).above(up), state, 2);
    }
}
