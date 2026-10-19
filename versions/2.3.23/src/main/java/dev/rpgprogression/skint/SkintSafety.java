package dev.rpgprogression.skint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

/** Water is a valid arrival/rest volume only while underwater breathing is actually available. */
public final class SkintSafety {
    public static boolean canBreatheWater(ServerPlayer player){
        return player.hasEffect(MobEffects.WATER_BREATHING)||player.hasEffect(MobEffects.CONDUIT_POWER);
    }
    public static boolean fluidsSafe(ServerPlayer player,ServerLevel level,AABB box){
        boolean breathing=canBreatheWater(player);
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(Math.nextDown(box.maxX),Math.nextDown(box.maxY),Math.nextDown(box.maxZ)))){
            var fluid=level.getFluidState(pos);
            if(fluid.isEmpty()||pos.getY()+fluid.getHeight(level,pos)<=box.minY)continue;
            if(!fluid.is(FluidTags.WATER)||!breathing)return false;
        }
        return true;
    }
    private SkintSafety(){}
}
