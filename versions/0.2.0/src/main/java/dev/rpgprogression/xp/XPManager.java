package dev.rpgprogression.xp;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import java.util.List;

public final class XPManager {
    public static final TagKey<Block> RARE_ORES=BlockTags.create(new ResourceLocation("rpgprogression","rare_ores"));
    public static final TagKey<EntityType<?>> BOSSES=TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,new ResourceLocation("rpgprogression","bosses"));
    public static final TagKey<Item> TREASURE=ItemTags.create(new ResourceLocation("rpgprogression","fishing_treasure"));
    /** Called only at the successful return of ServerPlayerGameMode.destroyBlock. */
    public static void mined(ServerPlayer p,BlockPos pos,BlockState state) {
        if(state.isAir())return;
        ServerLevel level=p.serverLevel(); boolean placed=PlacedBlockTracker.get(level).remove(pos);
        double hardness=Math.max(0,state.getDestroySpeed(level,pos));
        double xp=RPGConfig.BLOCK_BASE.get()+hardness*RPGConfig.HARDNESS_XP.get();
        if(state.is(RARE_ORES))xp*=RPGConfig.RARE_ORE_XP.get();
        else if(state.is(Tags.Blocks.ORES))xp*=RPGConfig.ORE_XP.get();
        if(placed)xp*=RPGConfig.PLACED_MULTIPLIER.get();
        ProgressionManager.addXP(p,Math.round(xp));
        StaminaManager.spend(p,ProgressionManager.get(p),Math.min(RPGConfig.MINING_MAX_COST.get(),RPGConfig.MINING_BASE_COST.get()+hardness*RPGConfig.MINING_HARDNESS_COST.get()));
    }
    public static void killed(ServerPlayer p,LivingEntity victim) {
        if(!(victim instanceof Mob)||victim==p)return;
        double multiplier=victim.getType().is(BOSSES)?RPGConfig.BOSS_MULT.get():victim instanceof Enemy?RPGConfig.HOSTILE_MULT.get():RPGConfig.PASSIVE_MULT.get();
        ProgressionManager.addXP(p,Math.round(victim.getMaxHealth()*RPGConfig.MOB_HEALTH_XP.get()*multiplier));
    }
    public static void fished(ServerPlayer p,List<ItemStack> drops) {
        long xp=0;
        for(ItemStack stack:drops)xp+= (long)(stack.is(ItemTags.FISHES)?RPGConfig.FISH_XP.get():stack.is(TREASURE)?RPGConfig.TREASURE_XP.get():RPGConfig.JUNK_XP.get())*stack.getCount();
        ProgressionManager.addXP(p,xp);
    }
    public static void running(ServerPlayer p,PlayerProgression d) {
        double x=p.getX(),y=p.getY(),z=p.getZ();
        double dx=x-d.lastX,dz=z-d.lastZ,dy=y-d.lastY;
        boolean valid=d.sampledPosition&&p.isAlive()&&p.isSprinting()&&p.onGround()&&!p.isPassenger()&&!p.isFallFlying()
                &&!p.getAbilities().flying&&!p.isInWaterOrBubble()&&!p.isSpectator()&&!p.isCreative()
                &&!d.exhausted&&Math.abs(dy)<=0.6;
        d.lastX=x; d.lastY=y; d.lastZ=z; d.sampledPosition=true;
        if(++d.runWindowTicks>=1200){d.runWindowTicks=0;d.runWindowXP=0;}
        double squared=dx*dx+dz*dz;
        if(!valid||squared<0.0004||squared>RPGConfig.RUN_MAX_SPEED.get()*RPGConfig.RUN_MAX_SPEED.get())return;
        d.sprintDistance+=Math.sqrt(squared);
        double distance=RPGConfig.RUN_DISTANCE.get();
        if(d.sprintDistance>=distance){
            int gained=(int)Math.min(100,d.sprintDistance/distance); d.sprintDistance%=distance;
            gained=Math.min(gained,Math.max(0,RPGConfig.RUN_WINDOW_CAP.get()-d.runWindowXP));
            d.runWindowXP+=gained; ProgressionManager.addXP(p,gained);
        }
    }
    private XPManager() {}
}
