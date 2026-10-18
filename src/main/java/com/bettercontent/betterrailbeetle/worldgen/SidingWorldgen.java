package com.bettercontent.betterrailbeetle.worldgen;

import com.bettercontent.betterrailbeetle.RailBeetleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SidingWorldgen {
    private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, RailBeetleMod.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, RailBeetleMod.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RailBeetleMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RailBeetleMod.MOD_ID);
    public static final RegistryObject<StructureType<SidingStructure>> STRUCTURE = TYPES.register("cave_siding", () -> () -> SidingStructure.CODEC);
    public static final RegistryObject<StructurePieceType> PIECE = PIECES.register("cave_siding", () -> SidingPiece::new);
    public static final RegistryObject<Block> MARKER = BLOCKS.register("siding_foundation", () -> new SidingMarkerBlock(Block.Properties.copy(Blocks.COBBLESTONE)));
    public static final RegistryObject<BlockEntityType<SidingMarker>> MARKER_ENTITY = ENTITIES.register("siding_foundation", () -> BlockEntityType.Builder.of(SidingMarker::new, MARKER.get()).build(null));
    private SidingWorldgen() {}
    public static void register(IEventBus bus) {
        TYPES.register(bus); PIECES.register(bus); BLOCKS.register(bus); ENTITIES.register(bus);
    }
}
