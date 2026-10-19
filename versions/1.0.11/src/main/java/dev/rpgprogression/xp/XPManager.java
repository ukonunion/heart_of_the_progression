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
        xp=XPOverrides.amount(RPGConfig.MINING_OVERRIDES,state.getBlock().builtInRegistryHolder(),net.minecraft.core.registries.Registries.BLOCK,xp)*RPGConfig.MINING_XP_MULTIPLIER.get();
        if(placed)xp*=RPGConfig.PLACED_MULTIPLIER.get();
        ProgressionManager.addXP(p,XPOverrides.reward(xp));
        StaminaManager.spend(p,ProgressionManager.get(p),Math.min(RPGConfig.MINING_MAX_COST.get(),RPGConfig.MINING_BASE_COST.get()+hardness*RPGConfig.MINING_HARDNESS_COST.get()));
    }
    public static void killed(ServerPlayer p,LivingEntity victim) {
        if(!(victim instanceof Mob)||victim==p)return;
        double multiplier=victim.getType().is(BOSSES)?RPGConfig.BOSS_MULT.get():victim instanceof Enemy?RPGConfig.HOSTILE_MULT.get():RPGConfig.PASSIVE_MULT.get();
        double xp=XPOverrides.amount(RPGConfig.COMBAT_OVERRIDES,victim.getType().builtInRegistryHolder(),net.minecraft.core.registries.Registries.ENTITY_TYPE,victim.getMaxHealth()*RPGConfig.MOB_HEALTH_XP.get()*multiplier);
        ProgressionManager.addXP(p,MobXP.reward(XPOverrides.reward(xp*RPGConfig.COMBAT_XP_MULTIPLIER.get()),p.getRandom().nextDouble()));
    }
    public static void fished(ServerPlayer p,List<ItemStack> drops) {
        double xp=0;
        for(ItemStack stack:drops)if(!stack.isEmpty()){
            double base=stack.is(ItemTags.FISHES)?RPGConfig.FISH_XP.get():stack.is(TREASURE)?RPGConfig.TREASURE_XP.get():RPGConfig.JUNK_XP.get();
            xp+=XPOverrides.amount(RPGConfig.FISHING_OVERRIDES,stack.getItem().builtInRegistryHolder(),net.minecraft.core.registries.Registries.ITEM,base)*stack.getCount();
        }
        ProgressionManager.addXP(p,XPOverrides.reward(xp*RPGConfig.FISHING_XP_MULTIPLIER.get()));
    }
    public static void cooked(ServerPlayer p,ItemStack result,boolean campfire){
        if(result.isEmpty())return;
        double xp=XPOverrides.amount(RPGConfig.COOKING_OVERRIDES,result.getItem().builtInRegistryHolder(),net.minecraft.core.registries.Registries.ITEM,campfire?RPGConfig.CAMPFIRE_XP.get():RPGConfig.FURNACE_XP.get());
        ProgressionManager.addXP(p,XPOverrides.reward(xp*result.getCount()*RPGConfig.COOKING_XP_MULTIPLIER.get()));
    }
    public static void running(ServerPlayer p,PlayerProgression d) {
        double x=p.getX(),y=p.getY(),z=p.getZ();
        double dx=x-d.lastX,dz=z-d.lastZ,dy=y-d.lastY;
        boolean swimming=p.isSwimming()&&p.isInWater();
        boolean valid=d.sampledPosition&&p.isAlive()&&p.isSprinting()&&!p.isPassenger()&&!p.isFallFlying()
                &&!p.getAbilities().flying&&!p.isSpectator()&&!p.isCreative()&&!d.exhausted
                &&(swimming||p.onGround()&&!p.isInWaterOrBubble()&&Math.abs(dy)<=0.6);
        d.lastX=x; d.lastY=y; d.lastZ=z; d.sampledPosition=true;
        if(++d.runWindowTicks>=1200){d.runWindowTicks=0;d.runWindowXP=0;}
        double squared=dx*dx+dz*dz+(swimming?dy*dy:0);
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
