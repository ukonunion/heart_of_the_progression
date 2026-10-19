package dev.rpgprogression.worldgen;

import dev.rpgprogression.RPGProgression;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ModStructures {
    private static final DeferredRegister<StructureType<?>> TYPES=DeferredRegister.create(Registries.STRUCTURE_TYPE,RPGProgression.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,RPGProgression.MOD_ID);
    public static final DeferredHolder<StructureType<?>,StructureType<SkintStructure>> SKINT=TYPES.register("skint",()->()->SkintStructure.CODEC);
    public static final DeferredHolder<StructurePieceType,StructurePieceType> PIECE=PIECES.register("skint",()->(context,tag)->new SkintStructure.Piece(context.structureTemplateManager(),tag));
    public static void register(IEventBus bus){TYPES.register(bus);PIECES.register(bus);}
    private ModStructures(){}
}
