/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Adapted from Wehavecookies56/Bonfires, world/BonfireTeleporter.java (1.20.1).
 * Original author: Wehavecookies56 and Bonfires contributors.
 * Modified 2026-10-15: NeoForge 1.21.1 API, collision/border/hazard checks,
 * no respawn/rest/Estus features, and no unsafe fallback inside the model.
 * See THIRD-PARTY-NOTICES.txt and licenses/LGPL-3.0.txt.
 */
package dev.rpgprogression.skint;

import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

public final class BonfireTravel {
    /** Bonfires' adjacent-cardinal search, with a bounded safe fallback search. */
    public static Optional<Vec3> attemptToPlaceNextToBonfire(BlockPos point,ServerLevel level,ServerPlayer player){
        for(int i=0;i<4;i++){var candidate=point.relative(Direction.from2DDataValue(i));if(safe(candidate,level,player))return Optional.of(Vec3.atBottomCenterOf(candidate));}
        for(int y:new int[]{0,1,-1,2,-2})for(int radius=1;radius<=2;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;
            var candidate=point.offset(x,y,z);if(safe(candidate,level,player))return Optional.of(Vec3.atBottomCenterOf(candidate));
        }return Optional.empty();
    }
    private static boolean hazard(net.minecraft.world.level.block.state.BlockState state){
        return state.is(BlockTags.FIRE)||state.is(BlockTags.CAMPFIRES)||state.is(Blocks.MAGMA_BLOCK)||state.is(Blocks.CACTUS)
            ||state.is(Blocks.POWDER_SNOW)||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.WITHER_ROSE);
    }
    private static boolean safe(BlockPos feet,ServerLevel level,ServerPlayer player){
        if(feet.getY()<=level.getMinBuildHeight()||feet.getY()+player.getBbHeight()>=level.getMaxBuildHeight())return false;
        var bottom=level.getBlockState(feet.below());var lower=level.getBlockState(feet);var upper=level.getBlockState(feet.above());
        if(!bottom.isFaceSturdy(level,feet.below(),Direction.UP)||hazard(bottom)||hazard(lower)||hazard(upper))return false;
        var location=Vec3.atBottomCenterOf(feet);double half=player.getBbWidth()/2;
        var box=new AABB(location.x-half,location.y,location.z-half,location.x+half,location.y+player.getBbHeight(),location.z+half);
        return level.getWorldBorder().isWithinBounds(box)&&level.noCollision(player,box)&&SkintSafety.fluidsSafe(player,level,box);
    }
    public static boolean travelToBonfire(ServerPlayer player,ServerLevel destination,Vec3 position){
        player.teleportTo(destination,position.x,position.y,position.z,player.getYRot(),player.getXRot());
        if(player.serverLevel()!=destination||player.position().distanceToSqr(position)>.01)return false;
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;player.hurtMarked=true;return true;
    }
    private BonfireTravel(){}
}
