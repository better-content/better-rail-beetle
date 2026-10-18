package com.bettercontent.betterrailbeetle.worldgen;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.fml.ModList;

/** Geometry is checked against carved terrain in the single-chunk piece. */
public final class SidingStructure extends Structure {
    public static final Codec<SidingStructure> CODEC = simpleCodec(SidingStructure::new);
    public SidingStructure(StructureSettings settings) { super(settings); }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!ModList.get().isLoaded("create")) return Optional.empty();
        BlockPos center = context.chunkPos().getMiddleBlockPosition(0);
        return Optional.of(new GenerationStub(center, pieces -> pieces.addPiece(new SidingPiece(center))));
    }
    @Override public StructureType<?> type() { return SidingWorldgen.STRUCTURE.get(); }
}
