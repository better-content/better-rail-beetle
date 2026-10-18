package com.bettercontent.betterrailbeetle.worldgen;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Legacy saved-start codec. New discoveries use SidingFeature, not structure starts. */
public final class SidingStructure extends Structure {
    public static final Codec<SidingStructure> CODEC = simpleCodec(SidingStructure::new);
    public SidingStructure(StructureSettings settings) { super(settings); }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return Optional.empty();
    }
    @Override public StructureType<?> type() { return SidingWorldgen.STRUCTURE.get(); }
}
