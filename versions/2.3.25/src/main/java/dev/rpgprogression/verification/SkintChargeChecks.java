package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.block.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.effect.ModEffects;
import dev.rpgprogression.mob.*;
import dev.rpgprogression.xp.XPManager;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Exercises native effects, damage hooks, entity NBT, boss immunity and support predicates. Not shipped. */
public final class SkintChargeChecks {
    private static void check(JsonArray checks,boolean ok,String text){SkintTravelChecks.check(checks,ok,text);}
    private static boolean close(double a,double b){return Math.abs(a-b)<.001;}
    private static void join(Mob mob,boolean loaded){NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(mob,mob.level(),loaded));}
    private static long lowRollSeed(){for(long seed=0;;seed++){if(RandomSource.create(seed).nextDouble()<.1)return seed;}}
    private static long highRollSeed(){for(long seed=0;;seed++){if(RandomSource.create(seed).nextDouble()>=.1)return seed;}}
    public static JsonArray run(ServerLevel level,BlockPos pos){
        var checks=new JsonArray();var zombie=EntityType.ZOMBIE.create(level);float normal=zombie.getMaxHealth();
        zombie.setHealth(normal/2);SkintCharge.charge(zombie);
        check(checks,!zombie.hasCustomName()&&!zombie.isCustomNameVisible(),"charge adds no mob name or HP label");
        var named=EntityType.COW.create(level);named.setCustomName(net.minecraft.network.chat.Component.literal("Player-named cow"));SkintCharge.charge(named);
        check(checks,named.getCustomName().getString().equals("Player-named cow")&&!named.isCustomNameVisible(),"charge preserves an existing player-given name and visibility");
        check(checks,SkintCharge.charged(zombie)&&close(zombie.getMaxHealth(),normal*1.5)&&close(zombie.getHealth(),normal*.75),"charge multiplies HP by 1.5 and preserves the current health fraction");
        var effect=zombie.getEffect(ModEffects.SKINT_CHARGED);
        check(checks,effect.isInfiniteDuration()&&effect.isVisible()&&effect.showIcon()&&effect.getCures().isEmpty(),"charge is infinite, visible and not removable by normal effect cures");
        check(checks,effect.getParticleOptions().equals(dev.rpgprogression.particle.ModParticles.SKINT_CHARGED.get()),"the synchronized effect emits the supplied custom particle type");
        float wounded=zombie.getHealth();SkintCharge.charge(zombie);
        check(checks,close(zombie.getMaxHealth(),normal*1.5)&&zombie.getHealth()==wounded,"reapplying charge neither stacks health nor heals a wounded mob");
        var nbt=zombie.saveWithoutId(new CompoundTag());var loaded=EntityType.ZOMBIE.create(level);loaded.load(nbt);join(loaded,true);
        check(checks,SkintCharge.charged(loaded)&&close(loaded.getMaxHealth(),normal*1.5)&&close(loaded.getHealth(),wounded),"entity NBT restores the infinite effect and health modifier exactly once");
        loaded.removeEffect(ModEffects.SKINT_CHARGED);
        check(checks,close(loaded.getMaxHealth(),normal),"removing the effect removes its native health modifier");

        int charged=0;var seeds=RandomSource.create(731013);var sample=EntityType.ZOMBIE.create(level);
        for(int i=0;i<1000;i++){
            sample.removeEffect(ModEffects.SKINT_CHARGED);sample.getPersistentData().remove(SkintCharge.CHECKED);sample.setHealth(normal);
            long seed=seeds.nextLong();sample.getRandom().setSeed(seed);join(sample,false);boolean expected=RandomSource.create(seed).nextDouble()<.1;
            if(SkintCharge.charged(sample))charged++;
            if(SkintCharge.charged(sample)!=expected)throw new AssertionError("Spawn probability differs from 10% at sample "+i);
            boolean first=SkintCharge.charged(sample);sample.getRandom().setSeed(first?highRollSeed():lowRollSeed());join(sample,true);join(sample,false);
            if(SkintCharge.charged(sample)!=first)throw new AssertionError("Repeat join rerolled a mob");
        }
        check(checks,charged>=70&&charged<=130,"1000 deterministic native spawn events use exactly the 10% threshold; charged="+charged);
        check(checks,true,"repeated joins, reloads and transfers never reroll the saved spawn result");
        var old=EntityType.COW.create(level);old.getRandom().setSeed(lowRollSeed());join(old,true);join(old,false);
        check(checks,!SkintCharge.charged(old)&&old.getPersistentData().getBoolean(SkintCharge.CHECKED),"ordinary mobs loaded from an older save are marked without a retroactive random roll");
        for(var type:new EntityType<?>[]{EntityType.COW,EntityType.WITHER,EntityType.ENDER_DRAGON,EntityType.WARDEN}){
            var mob=(Mob)type.create(level);float hp=mob.getMaxHealth();SkintCharge.charge(mob);
            check(checks,SkintCharge.charged(mob)&&close(mob.getMaxHealth(),hp*1.5)&&close(mob.getHealth(),hp*1.5),"passive mobs and effect-immune bosses receive charge: "+type);
        }
        var data=new SkintChargeWorldData();check(checks,data.claimFirstDragon(false)&&!data.claimFirstDragon(false),"only the first dragon can claim the guaranteed charge");
        var restored=SkintChargeWorldData.load(data.save(new CompoundTag(),level.registryAccess()),level.registryAccess());
        check(checks,!restored.claimFirstDragon(false),"the first-dragon decision survives world SavedData serialization");
        var oldWorld=new SkintChargeWorldData();check(checks,!oldWorld.claimFirstDragon(true)&&!oldWorld.claimFirstDragon(false),"an older world with a previously killed dragon does not regain a first-dragon guarantee");
        var worldData=SkintChargeWorldData.get(level);
        try{
            level.getDataStorage().set("rpgprogression_skint_charge",new SkintChargeWorldData());
            var first=EntityType.ENDER_DRAGON.create(level);first.getRandom().setSeed(highRollSeed());join(first,false);
            check(checks,SkintCharge.charged(first),"the first dragon is charged even when its random roll would fail");
            var next=EntityType.ENDER_DRAGON.create(level);next.getRandom().setSeed(highRollSeed());join(next,false);
            check(checks,!SkintCharge.charged(next),"a later dragon with a roll outside 10% remains ordinary");
            var nextLucky=EntityType.ENDER_DRAGON.create(level);nextLucky.getRandom().setSeed(lowRollSeed());join(nextLucky,false);
            check(checks,SkintCharge.charged(nextLucky),"a later dragon with a roll inside 10% receives charge");
        }finally{level.getDataStorage().set("rpgprogression_skint_charge",worldData);}

        var plain=EntityType.ZOMBIE.create(level);var strong=EntityType.ZOMBIE.create(level);SkintCharge.charge(strong);
        var cow=EntityType.COW.create(level);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);
        cow.hurt(plain.damageSources().mobAttack(plain),10);float ordinaryDamage=100-cow.getHealth();cow.invulnerableTime=0;cow.setHealth(100);
        cow.hurt(strong.damageSources().mobAttack(strong),10);float chargedDamage=100-cow.getHealth();
        check(checks,close(ordinaryDamage,10)&&close(chargedDamage,13),"actual melee damage passes through the native damage pipeline exactly 1.3 times");
        cow.invulnerableTime=0;cow.setHealth(100);var arrow=EntityType.ARROW.create(level);arrow.setOwner(strong);
        cow.hurt(strong.damageSources().arrow(arrow,strong),10);
        check(checks,close(100-cow.getHealth(),13),"a projectile attributed to a charged mob also deals 1.3 times damage");
        cow.invulnerableTime=0;cow.setHealth(100);cow.hurt(cow.damageSources().fall(),10);
        check(checks,close(100-cow.getHealth(),10),"environmental damage is unaffected");

        var otherHP=new AttributeModifier(ResourceLocation.parse("rpgprogression:verification_health"),7,AttributeModifier.Operation.ADD_VALUE);
        plain.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(otherHP);strong.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(otherHP);
        for(double roll:new double[]{0,.01,.25,.5,.75,.99,1})check(checks,XPManager.mobReward(strong,roll)==XPManager.mobReward(plain,roll)*2,"XP is exactly doubled without counting bonus health, roll="+roll);
        check(checks,close(SkintCharge.ordinaryMaxHealth(strong),27),"XP baseline preserves health bonuses from other mods");
        for(double hp:new double[]{13.333333,42.4999999,1001.25,1024,2000}){
            var fractional=EntityType.ZOMBIE.create(level);fractional.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);long before=XPManager.mobReward(fractional,.5);SkintCharge.charge(fractional);
            check(checks,XPManager.mobReward(fractional,.5)==before*2,"XP retains native HP rounding and attribute caps, base="+hp);
        }
        var overrides=RPGConfig.COMBAT_OVERRIDES.get();
        try{
            RPGConfig.COMBAT_OVERRIDES.set(java.util.List.of("minecraft:zombie=13"));
            check(checks,XPManager.mobReward(strong,.5)==26&&XPManager.mobReward(plain,.5)==13,"an odd custom XP override is doubled after reward rounding");
            RPGConfig.COMBAT_OVERRIDES.set(java.util.List.of("minecraft:zombie=0"));check(checks,XPManager.mobReward(strong,.5)==0,"zero XP overrides remain zero for charged mobs");
        }finally{RPGConfig.COMBAT_OVERRIDES.set(overrides);}
        int mobs=0;
        for(var type:net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE){
            if(!(type.create(level) instanceof Mob mob))continue;
            float hp=mob.getMaxHealth();long before=XPManager.mobReward(mob,.5);SkintCharge.charge(mob);
            check(checks,SkintCharge.charged(mob)&&mob.getMaxHealth()>=hp&&XPManager.mobReward(mob,.5)==before*2,"registered mob supports charge and doubled XP: "+type);mobs++;
        }
        check(checks,mobs>=50,"all registered vanilla and optional mod mobs checked: "+mobs);
        SkintTravelChecks.point(level,pos);var block=ModBlocks.TRAVELSKINT.get();
        for(var facing:Direction.Plane.HORIZONTAL){
            var state=block.defaultBlockState().setValue(dev.rpgprogression.block.TravelSkintBlock.ACTIVE,true).setValue(HorizontalDirectionalBlock.FACING,facing);level.setBlock(pos,state,3);
            check(checks,!Shapes.joinIsNotEmpty(state.getShape(level,pos),Shapes.block(),BooleanOp.NOT_SAME)&&!Shapes.joinIsNotEmpty(state.getCollisionShape(level,pos),Shapes.block(),BooleanOp.NOT_SAME),"selection and collision are exactly one cube facing "+facing);
            boolean supports=false;for(var face:Direction.values())for(var support:SupportType.values())supports|=state.isFaceSturdy(level,pos,face,support);
            check(checks,!supports&&state.getBlockSupportShape(level,pos).isEmpty(),"the full cube offers no attachment support facing "+facing);
        }
        for(var attachment:new Block[]{Blocks.TORCH,Blocks.SOUL_TORCH,Blocks.REDSTONE_TORCH,Blocks.REDSTONE_WIRE,Blocks.RAIL,Blocks.STONE_PRESSURE_PLATE,Blocks.LANTERN,Blocks.WHITE_CARPET,Blocks.SNOW})
            check(checks,!attachment.defaultBlockState().canSurvive(level,pos.above()),"unsupported block cannot stand on a Skint: "+attachment);
        for(var attachment:new Block[]{Blocks.WALL_TORCH,Blocks.LADDER,Blocks.LEVER,Blocks.STONE_BUTTON}){
            var state=attachment.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);
            if(state.hasProperty(BlockStateProperties.ATTACH_FACE))state=state.setValue(BlockStateProperties.ATTACH_FACE,AttachFace.WALL);
            check(checks,!state.canSurvive(level,pos.north()),"unsupported block cannot attach to a Skint side: "+attachment);
        }
        var state=level.getBlockState(pos);
        check(checks,state.getLightEmission(level,pos)==15&&state.getDestroySpeed(level,pos)==-1,"the Skint keeps maximum block light and unbreakable hardness");
        level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),3);check(checks,level.getBlockState(pos).isAir(),"the full-cube Skint still disappears when its solid floor is removed");
        return checks;
    }
    public static void persistedFixtures(ServerLevel level,JsonArray checks){
        var mobs=level.getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(62,199,-2,79,208,15)).stream().filter(m->m.getTags().contains("heart-charge-fixture")).toList();
        check(checks,java.util.stream.Stream.of(EntityType.ZOMBIE,EntityType.COW,EntityType.WITHER).allMatch(type->mobs.stream().anyMatch(m->m.getType()==type)),"a complete game restart restores saved charged fixtures for every tested mob type");
        for(var mob:mobs){
            double expected=mob.getType()==EntityType.ZOMBIE?30:mob.getType()==EntityType.COW?15:450;
            check(checks,SkintCharge.charged(mob)&&mob.getEffect(ModEffects.SKINT_CHARGED).isInfiniteDuration()&&close(mob.getMaxHealth(),expected)&&mob.getPersistentData().getBoolean(SkintCharge.CHECKED),"a saved mob retains infinite charge, scaled HP and its once-only spawn marker after game restart: "+mob.getType());
        }
    }
    public static boolean persistedFixturesReady(ServerLevel level){
        level.getChunkAt(new BlockPos(70,200,7));
        var mobs=level.getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(62,199,-2,79,208,15)).stream().filter(m->m.getTags().contains("heart-charge-fixture")).toList();
        return java.util.stream.Stream.of(EntityType.ZOMBIE,EntityType.COW,EntityType.WITHER).allMatch(type->mobs.stream().anyMatch(m->m.getType()==type));
    }
    private SkintChargeChecks(){}
}
