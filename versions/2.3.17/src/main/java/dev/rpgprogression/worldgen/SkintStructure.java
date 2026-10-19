package dev.rpgprogression.worldgen;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Small authored shrines. Generation reads base terrain only, so it never loads neighboring chunks. */
public final class SkintStructure extends Structure {
    public static final MapCodec<SkintStructure> CODEC=RecordCodecBuilder.mapCodec(i->i.group(settingsCodec(i),
        ResourceLocation.CODEC.fieldOf("template").forGetter(s->s.template),
        Codec.STRING.fieldOf("placement").forGetter(s->s.placement)).apply(i,SkintStructure::new));
    private final ResourceLocation template;
    private final String placement;
    public SkintStructure(StructureSettings settings,ResourceLocation template,String placement){
        super(settings);this.template=template;this.placement=placement;
        if(!Set.of("surface","cave","nether","end").contains(placement))throw new IllegalArgumentException("Unknown Skint placement: "+placement);
    }
    private static TagKey<Biome> tag(String name){return TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("rpgprogression","has_structure/"+name));}
    public static String surfaceVariant(Holder<Biome> biome){
        if(biome.is(tag("skint_red_sand")))return "skint_main_red_sand";
        if(biome.is(tag("skint_sand")))return "skint_main_sand";
        if(biome.is(tag("skint_mossy")))return "skint_main_mossy";
        return "skint_main_base";
    }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        // Search within this placement chunk instead of discarding a whole 768-block region
        // because its exact center happens to lie on a slope or at a cave wall.
        for(int dx:new int[]{8,4,12})for(int dz:new int[]{8,4,12}){
            var site=findSite(c,c.chunkPos().getMinBlockX()+dx,c.chunkPos().getMinBlockZ()+dz);
            if(site.isPresent())return site;
        }
        return Optional.empty();
    }
    private Optional<GenerationStub> findSite(GenerationContext c,int x,int z){
        var column=c.chunkGenerator().getBaseColumn(x,z,c.heightAccessor(),c.randomState());
        int surface=c.chunkGenerator().getFirstOccupiedHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
        int floor=surface;
        boolean underground=placement.equals("cave")||placement.equals("nether");
        if(underground){
            int min=placement.equals("nether")?33:Math.max(c.heightAccessor().getMinBuildHeight()+8,-56);
            int max=placement.equals("nether")?112:Math.min(56,surface-12);
            var floors=new ArrayList<Integer>();
            for(int y=min;y<=max;y++)if(solid(column.getBlock(y))&&clear(column,y+1,3,false))floors.add(y);
            Collections.shuffle(floors,new java.util.Random(c.random().nextLong()));
            floor=Integer.MIN_VALUE;
            search:for(int y:floors)for(int raise=0;raise<=3;raise++)if(fits(c,x,y+raise,z,false)){floor=y+raise;break search;}
            if(floor==Integer.MIN_VALUE)return Optional.empty();
        }else{
            boolean found=false;
            for(int raise=0;raise<=3;raise++)if(fits(c,x,surface+raise,z,placement.equals("surface"))){floor=surface+raise;found=true;break;}
            if(!found)return Optional.empty();
        }
        var biome=c.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(floor+1),QuartPos.fromBlock(z),c.randomState().sampler());
        if(placement.equals("surface")&&!template.getPath().equals(surfaceVariant(biome)))return Optional.empty();
        var center=new BlockPos(x,floor,z);Rotation rotation=Rotation.getRandom(c.random());
        // Rotate around the center of the 7x7 template; its floor is placed at terrain level.
        var origin=center.offset(-3,0,-3);
        return Optional.of(new GenerationStub(center,b->b.addPiece(new Piece(c.structureTemplateManager(),template,origin,rotation))));
    }
    private static boolean solid(BlockState state){return !state.isAir()&&state.getFluidState().isEmpty()&&state.isFaceSturdy(EmptyBlockGetter.INSTANCE,BlockPos.ZERO,Direction.UP);}
    private static boolean clear(NoiseColumn column,int y,int height,boolean water){
        for(int j=0;j<height;j++){
            var s=column.getBlock(y+j);
            if(!s.isAir()&&!(water&&s.is(Blocks.WATER)))return false;
        }return true;
    }
    private static boolean fits(GenerationContext c,int x,int y,int z,boolean water){
        if(y<c.heightAccessor().getMinBuildHeight()+4||y+4>=c.heightAccessor().getMaxBuildHeight())return false;
        // Check all 49 columns: no buried shrine, cliff platform, lava lake, or End void.
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
            var col=c.chunkGenerator().getBaseColumn(x+dx,z+dz,c.heightAccessor(),c.randomState());
            if(!clear(col,y+1,3,water))return false;
            boolean support=false;for(int drop=0;drop<=3;drop++)if(solid(col.getBlock(y-drop))){support=true;break;}
            if(!support)return false;
        }return true;
    }
    @Override public StructureType<?> type(){return ModStructures.SKINT.get();}
    public static final class Piece extends TemplateStructurePiece {
        private static StructurePlaceSettings settings(Rotation rotation){return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(3,0,3)).setIgnoreEntities(true)
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
            .addProcessor(new RuleProcessor(List.of(new ProcessorRule(new BlockMatchTest(Blocks.AIR),new BlockMatchTest(Blocks.WATER),Blocks.WATER.defaultBlockState()))));}
        public Piece(StructureTemplateManager manager,ResourceLocation id,BlockPos pos,Rotation rotation){super(ModStructures.PIECE.get(),0,manager,id,id.toString(),settings(rotation),pos);}
        public Piece(StructureTemplateManager manager,CompoundTag tag){super(ModStructures.PIECE.get(),tag,manager,id->settings(Rotation.valueOf(tag.getString("Rot"))));}
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){super.addAdditionalSaveData(c,tag);tag.putString("Rot",placeSettings.getRotation().name());}
        @Override protected void handleDataMarker(String name,BlockPos pos,ServerLevelAccessor level,RandomSource random,BoundingBox box){}
        @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox box,ChunkPos chunk,BlockPos pivot){
            super.postProcess(level,manager,generator,random,box,chunk,pivot);
            // Support only authored bottom-layer blocks, preserving the outline of the supplied model.
            BlockState support=(templateName.contains("nether")?Blocks.BLACKSTONE:templateName.contains("end")?Blocks.END_STONE:templateName.contains("red_sand")?Blocks.RED_SANDSTONE:templateName.contains("sand")?Blocks.SANDSTONE:templateName.contains("cave_2")?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();
            for(int dx=0;dx<7;dx++)for(int dz=0;dz<7;dz++){
                BlockPos p=templatePosition.offset(dx,0,dz);if(!box.isInside(p)||!solid(level.getBlockState(p)))continue;
                for(int down=1;down<=4;down++){
                    var below=p.below(down);var state=level.getBlockState(below);
                    if(solid(state))break;
                    if(state.isAir()||!state.getFluidState().isEmpty())level.setBlock(below,support,2);else break;
                }
            }
        }
    }
}
