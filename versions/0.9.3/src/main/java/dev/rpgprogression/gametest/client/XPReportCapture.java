package dev.rpgprogression.gametest.client;

import com.google.gson.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionMath;
import dev.rpgprogression.xp.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.common.Tags;
import java.nio.file.*;

/** Development-only registry capture used to substantiate the XP report; excluded from the JAR. */
public final class XPReportCapture {
    public static void write(ServerPlayer p){
        var report=new JsonObject();var levels=new JsonArray();long total=0;
        for(int level=1;level<=1000;level++){
            var row=new JsonObject();row.addProperty("level",level);row.addProperty("total",total);
            long cost=ProgressionMath.required(level,RPGConfig.BASE_XP.get(),RPGConfig.XP_MULTIPLIER.get());row.addProperty("next",cost);levels.add(row);total+=cost;
        }
        report.add("levels",levels);var blocks=new JsonArray();
        for(var block:BuiltInRegistries.BLOCK){
            var state=block.defaultBlockState();double hardness=state.getDestroySpeed(p.level(),p.blockPosition());
            if(state.isAir()||hardness<0)continue;
            double mult=state.is(XPManager.RARE_ORES)?RPGConfig.RARE_ORE_XP.get():state.is(Tags.Blocks.ORES)?RPGConfig.ORE_XP.get():1;
            var row=new JsonObject();row.addProperty("id",BuiltInRegistries.BLOCK.getKey(block).toString());row.addProperty("hardness",hardness);row.addProperty("oreMultiplier",mult);
            row.addProperty("xp",XPOverrides.reward(XPOverrides.amount(RPGConfig.MINING_OVERRIDES,block.builtInRegistryHolder(),net.minecraft.core.registries.Registries.BLOCK,(RPGConfig.BLOCK_BASE.get()+hardness*RPGConfig.HARDNESS_XP.get())*mult)*RPGConfig.MINING_XP_MULTIPLIER.get()));blocks.add(row);
        }
        report.add("blocks",blocks);var mobs=new JsonArray();
        for(var type:BuiltInRegistries.ENTITY_TYPE){
            var entity=type.create(p.serverLevel());if(!(entity instanceof Mob mob))continue;
            double mult=type.is(XPManager.BOSSES)?RPGConfig.BOSS_MULT.get():mob instanceof Enemy?RPGConfig.HOSTILE_MULT.get():RPGConfig.PASSIVE_MULT.get();
            var row=new JsonObject();row.addProperty("id",BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());row.addProperty("maxHealth",mob.getMaxHealth());row.addProperty("categoryMultiplier",mult);
            row.addProperty("xp",XPOverrides.reward(XPOverrides.amount(RPGConfig.COMBAT_OVERRIDES,type.builtInRegistryHolder(),net.minecraft.core.registries.Registries.ENTITY_TYPE,mob.getMaxHealth()*RPGConfig.MOB_HEALTH_XP.get()*mult)*RPGConfig.COMBAT_XP_MULTIPLIER.get()));mobs.add(row);
        }
        report.add("mobs",mobs);
        report.addProperty("base",RPGConfig.BASE_XP.get());report.addProperty("multiplier",RPGConfig.XP_MULTIPLIER.get());report.addProperty("rulesVersion",RPGConfig.RULES_VERSION.get());
        try{var file=Path.of("xp-report-capture-0.9.3-"+(dev.rpgprogression.compat.IronsCompat.enabled()?"combined":"standalone")+".json");Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
        catch(Exception error){throw new IllegalStateException("Cannot write XP report capture",error);}
    }
    private XPReportCapture(){}
}
