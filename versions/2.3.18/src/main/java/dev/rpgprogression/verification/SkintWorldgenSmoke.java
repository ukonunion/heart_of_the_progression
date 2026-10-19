package dev.rpgprogression.verification;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import dev.rpgprogression.block.*;
import dev.rpgprogression.worldgen.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Isolated development-world verification; never included in the distributed jar. */
@EventBusSubscriber(modid="rpgprogression")
public final class SkintWorldgenSmoke {
    private static final JsonArray checks=new JsonArray(),natural=new JsonArray();
    private static final String[] NAMES={"skint_cave_1","skint_cave_2","skint_main_base","skint_main_mossy","skint_main_red_sand","skint_main_sand","skint_end","skint_nether","skint_ocean"};
    private static int phase,index,found;private static boolean done;private static final Set<String> foundKinds=new HashSet<>();
    private static ResourceLocation id(String value){return ResourceLocation.fromNamespaceAndPath("rpgprogression",value);}
    private static void check(boolean ok,String text){SkintTravelChecks.check(checks,ok,text);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(done||!Boolean.getBoolean("rpgprogression.skintWorldgenSmoke"))return;
        var server=event.getServer();
        try{
            if(phase==0){
                Skint2317Checks.run(server.overworld(),checks);Skint2318Checks.run(server.overworld(),checks);templates(server.overworld());spacing(server.overworld());biomes(server.overworld());phase=1;return;
            }
            var dim=phase==1?Level.OVERWORLD:phase==2?Level.NETHER:Level.END;var level=server.getLevel(dim);
            var set=level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(id("skints_"+(phase==3?"end":phase==2?"nether":"overworld")));
            var placement=(RandomSpreadStructurePlacement)set.placement();
            // Distant, previously unexplored regions; load only genuine placement candidates.
            int regionX=Integer.getInteger("rpgprogression.worldgenRegion",80)+index%8,regionZ=Integer.getInteger("rpgprogression.worldgenRegion",80)+index/8;
            var chunkPos=placement.getPotentialStructureChunk(level.getSeed(),regionX*48,regionZ*48);index++;
            var generator=level.getChunkSource().getGenerator();var randomState=level.getChunkSource().randomState();
            boolean possible=false;
            for(var entry:set.structures()){
                var structure=entry.structure().value();
                var start=structure.generate(level.registryAccess(),generator,generator.getBiomeSource(),randomState,level.getStructureManager(),level.getSeed(),chunkPos,0,level,structure.biomes()::contains);
                if(start.isValid()){possible=true;break;}
            }
            if(possible){
                var chunk=level.getChunk(chunkPos.x,chunkPos.z);boolean generated=false;
                for(var entry:chunk.getAllStarts().entrySet()){
                    var key=level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(entry.getKey());
                    if(key==null||!key.getNamespace().equals("rpgprogression")||!entry.getValue().isValid())continue;
                    generated=true;found++;foundKinds.add(key.getPath());var start=entry.getValue();var box=start.getBoundingBox();var center=box.getCenter();
                    var pos=new BlockPos(center.getX(),box.minY()+1,center.getZ());
                    check(level.getBlockState(pos).is(ModBlocks.TRAVELSKINT.get())&&!TravelSkintBlock.active(level.getBlockState(pos)),"natural "+key+" places an inactive central skint in "+dim.location()+" at "+pos.toShortString());
                    check(level.getBlockState(pos).canSurvive(level,pos)&&level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty(),"natural "+key+" has a solid floor and usable space above it: floor="+level.getBlockState(pos.below())+", head="+level.getBlockState(pos.above()));
                    var item=new JsonObject();item.addProperty("structure",key.toString());item.addProperty("dimension",dim.location().toString());item.addProperty("x",pos.getX());item.addProperty("y",pos.getY());item.addProperty("z",pos.getZ());natural.add(item);
                }
                check(generated,"a valid shared-set candidate actually produces a saved structure start at "+chunkPos+" in "+dim.location());
            }
            com.mojang.logging.LogUtils.getLogger().info("SKINT_WORLDGEN phase={} candidates={} found={} kinds={}",phase,index,found,foundKinds);
            boolean enough=phase==1?found>=3&&foundKinds.stream().anyMatch(s->s.contains("cave"))&&foundKinds.contains("skint_ocean"):found>=2;
            if(enough||index>=64){check(enough,"natural "+dim.location()+" generation produces "+(phase==1?"surface and cave":"two dimension-specific")+" shrines within the sampled terrain");phase++;index=found=0;}
            if(phase>3)finish(server,null);
        }catch(Throwable error){com.mojang.logging.LogUtils.getLogger().error("SKINT_WORLDGEN failed",error);finish(server,error);}
    }
    private static void templates(ServerLevel level){
        int i=0;
        for(String name:NAMES){
            var t=level.getStructureManager().get(id(name)).orElseThrow();check(t.getSize().equals(new Vec3i(7,3,7)),name+" retains the author's 7x3x7 template");
            for(var rotation:Rotation.values()){
                var origin=new BlockPos(300+(i%8)*16,180,300+(i/8)*16);i++;
                for(int x=0;x<7;x++)for(int z=0;z<7;z++)level.setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
                var settings=new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(3,0,3)).setIgnoreEntities(true).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
                check(t.placeInWorld(level,origin,origin,settings,level.random,2),name+" places through native StructureTemplate with "+rotation);
                var pos=origin.offset(3,1,3);var entity=level.getBlockEntity(pos);
                check(entity instanceof TravelSkintBlockEntity e&&e.activationStarted()==-1&&!TravelSkintBlock.active(e.getBlockState())&&e.getBlockState().canSurvive(level,pos),name+" keeps a usable, inactive skint at the rotated center");
            }
        }
        var wetOrigin=new BlockPos(480,180,300);
        for(int x=0;x<7;x++)for(int z=0;z<7;z++){
            level.setBlock(wetOrigin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            for(int y=0;y<3;y++)level.setBlock(wetOrigin.offset(x,y,z),Blocks.WATER.defaultBlockState(),2);
        }
        var piece=new SkintStructure.Piece(level.getStructureManager(),id("skint_main_base"),wetOrigin,Rotation.CLOCKWISE_90);
        piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),level.random,piece.getBoundingBox(),new ChunkPos(wetOrigin),wetOrigin);
        var wet=wetOrigin.offset(3,1,3);
        check(level.getBlockState(wet).getValue(TravelSkintBlock.WATERLOGGED)&&level.getBlockState(wet.above()).is(Blocks.WATER),"underwater template placement waterlogs the skint and preserves water in the authored air spaces");
        var tag=piece.createTag(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level));
        var loaded=new SkintStructure.Piece(level.getStructureManager(),tag);
        check(loaded.getRotation()==Rotation.CLOCKWISE_90&&loaded.getBoundingBox().equals(piece.getBoundingBox()),"saved custom structure pieces restore their template position, rotation and bounding box");
    }
    private static void spacing(ServerLevel level){
        for(String dim:List.of("overworld","nether","the_end")){
            var set=level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(id("skints_"+(dim.equals("the_end")?"end":dim)));
            var p=(RandomSpreadStructurePlacement)set.placement();check(p.spacing()==48&&p.separation()==32,"the "+dim+" structure set uses a 768-block grid with a minimum candidate gap of 528 blocks");
            double min=Double.MAX_VALUE,max=0;
            for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++){
                var a=p.getPotentialStructureChunk(level.getSeed(),x*48,z*48);double nearest=Double.MAX_VALUE;
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(dx!=0||dz!=0){var b=p.getPotentialStructureChunk(level.getSeed(),(x+dx)*48,(z+dz)*48);nearest=Math.min(nearest,16*Math.hypot(a.x-b.x,a.z-b.z));}
                min=Math.min(min,nearest);max=Math.max(max,nearest);
            }
            check(min>=528&&max<=1008,"sampled "+dim+" candidate nearest-neighbor distances fit the requested approximate range: "+Math.round(min)+"–"+Math.round(max)+" blocks");
        }
        var ow=level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(id("skints_overworld"));check(ow.structures().size()==7,"surface and cave shrines share a single spacing grid, preventing close independent spawns");
    }
    private static void biomes(ServerLevel level){
        var r=level.registryAccess().registryOrThrow(Registries.BIOME);
        for(String ocean:List.of("ocean","deep_ocean","warm_ocean","cold_ocean","deep_frozen_ocean")){
            var biome=r.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace(ocean)));
            check(SkintStructure.surfaceVariant(biome).equals("skint_ocean"),ocean+" selects the new ocean template instead of main_base");
        }
        for(var pair:Map.of("desert","sand","beach","sand","snowy_beach","sand","badlands","red_sand","wooded_badlands","red_sand","swamp","mossy","mangrove_swamp","mossy","jungle","mossy","bamboo_jungle","mossy","plains","base").entrySet()){
            var biome=r.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace(pair.getKey())));
            check(SkintStructure.surfaceVariant(biome).equals("skint_main_"+pair.getValue()),pair.getKey()+" selects the requested surface template");
        }
    }
    private static void finish(net.minecraft.server.MinecraftServer server,Throwable error){
        done=true;var report=new JsonObject();report.addProperty("version","2.3.18");report.add("checks",checks);report.add("naturalStructures",natural);if(error!=null)report.addProperty("error",error.toString());
        report.addProperty("result",error==null&&checks.asList().stream().allMatch(c->c.getAsJsonObject().get("passed").getAsBoolean())?"PASSED":"FAILED");
        try{Files.writeString(Path.of("skint-2.3.18-worldgen-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new RuntimeException(e);}
        server.halt(false);
    }
}
