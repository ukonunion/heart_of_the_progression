package dev.rpgprogression.verification.gametest;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public final class EpicFightGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);
        h.assertTrue(patch!=null,"native Epic Fight capability attached");
        var d=ProgressionManager.get(p);
        near(h,patch.getMaxStamina(),20,"100 RPG to 20 Epic Fight units");
        near(h,patch.getStamina(),20,"shared initial current value");
        h.assertTrue(Skill.Resource.STAMINA.predicate.canExecute(null,patch,3),"native skill predicate");
        Skill.Resource.STAMINA.consumer.consume(null,patch,3);
        near(h,d.stamina,85,"native skill consumer charges shared pool exactly once");
        h.assertTrue(d.regenCooldown==30,"native spending resets RPG regen delay");
        patch.setStaminaRegenAwaitTicks(0);
        patch.preTickServer();
        near(h,d.stamina,85,"Epic Fight tick cannot regenerate a second pool");
        StaminaManager.spend(p,d,10);near(h,patch.getStamina(),15,"RPG spending visible in Epic Fight");
        d.set(RPGAttribute.ENDURANCE,11);near(h,patch.getMaxStamina(),22,"Endurance adjusts native maximum");
        request(p,9999,true);request(p,Float.NaN,true);
        request(p,-1,true);near(h,d.stamina,75,"invalid/untrusted client refills rejected");
        request(p,14,true);near(h,d.stamina,70.05,"native client debit includes one Endurance point");
        patch.setStamina(16);near(h,d.stamina,80,"trusted server skill refund");
        patch.setStamina(0);near(h,d.stamina,.8,"native debit receives one percent reduction");
        StaminaManager.spend(p,d,1000);h.assertFalse(patch.hasStamina(1),"exhaustion denies native skill");
        d.stamina=10;StaminaManager.updateExhaustion(p,d);h.assertFalse(patch.hasStamina(1),"recovery hysteresis shared");
        d.stamina=17;StaminaManager.updateExhaustion(p,d);h.assertTrue(patch.hasStamina(1),"skill restored at shared recovery threshold");
        double old=d.stamina;RPGConfig.EPICFIGHT_COMPAT.set(false);
        try{patch.setStamina(3);near(h,d.stamina,old,"disabled adapter leaves RPG data alone");}
        finally{RPGConfig.EPICFIGHT_COMPAT.set(true);}
        if(ParCoolCompat.enabled())near(h,ParCoolBridge.value(p),d.stamina,"all three mods use the same pool");
        attacks(h,p,patch);
    }
    private static void request(ServerPlayer player,float value,boolean reset){
        yesman.epicfight.network.EpicFightServerBoundPayloadHandler.handleSetStamina(new yesman.epicfight.network.client.CPSetStamina(value,reset),TestPayloadContext.of(player));
    }
    private static void attacks(GameTestHelper h,ServerPlayer p,ServerPlayerPatch patch){
        // FakePlayer never joins the level; native onJoinWorld normally initializes body weight.
        var weight=p.getAttribute(yesman.epicfight.registry.entries.EpicFightAttributes.WEIGHT);weight.setBaseValue(40);
        var d=ProgressionManager.get(p);d.set(RPGAttribute.ENDURANCE,10);d.stamina=d.maxStamina();d.exhausted=false;patch.toEpicFightMode(false);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        var animation=yesman.epicfight.gameasset.Animations.SWORD_AUTO1.get();
        double sword=EpicFightBridge.attackCost(patch,net.minecraft.world.InteractionHand.MAIN_HAND),before=d.stamina;
        near(h,sword,12,"sword category cost");
        patch.onStrike(animation,net.minecraft.world.InteractionHand.MAIN_HAND);
        near(h,d.stamina,before-sword,"native strike charges even without a target");
        var cow=h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW,1,2,1);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(p,cow));
        near(h,d.stamina,before-sword,"a collision does not charge the vanilla attack cost again");
        near(h,EpicFightBridge.categoryCost(patch,"dagger"),6,"light weapon cost");
        near(h,EpicFightBridge.categoryCost(patch,"greatsword"),24,"heavy weapon cost");
        weight.setBaseValue(80);near(h,EpicFightBridge.categoryCost(patch,"sword"),24,"native weight penalty applies to strike cost");weight.setBaseValue(40);
        patch.onStrike(animation,net.minecraft.world.InteractionHand.MAIN_HAND);
        near(h,d.stamina,before-2*sword,"successive native strikes each cost stamina");
        d.stamina=1;patch.onStrike(animation,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertFalse(((EpicAttackAccess)patch).rpg$attackAllowed(),"insufficient stamina blocks the strike");
        near(h,d.stamina,1,"failed strike cannot overdraw the pool");
        try{
            // Resolve the exact native method, avoiding reflection's enumeration of
            // unrelated client-rendering signatures on this common animation class.
            var collision=java.lang.invoke.MethodHandles.privateLookupIn(yesman.epicfight.api.animation.types.AttackAnimation.class,java.lang.invoke.MethodHandles.lookup())
                .findVirtual(yesman.epicfight.api.animation.types.AttackAnimation.class,"hurtCollidingEntities",java.lang.invoke.MethodType.methodType(void.class,
                yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch.class,float.class,float.class,
                yesman.epicfight.api.animation.types.EntityState.class,yesman.epicfight.api.animation.types.EntityState.class,
                yesman.epicfight.api.animation.types.AttackAnimation.Phase.class));
            collision.invoke(animation,patch,0F,0F,null,null,null);
        }catch(Throwable error){throw new AssertionError("blocked strike entered collision damage",error);}
        d.stamina=d.maxStamina();d.exhausted=false;
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
        p.getInventory().setItem(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW,32));before=d.stamina;
        p.getMainHandItem().getItem().releaseUsing(p.getMainHandItem(),p.level(),p,72000-20);
        near(h,d.stamina,before-8,"real bow shot costs stamina");
        h.assertTrue(p.getInventory().getItem(1).getCount()==31,"successful bow shot uses one arrow");
        d.stamina=1;p.tickCount++;
        p.getMainHandItem().getItem().releaseUsing(p.getMainHandItem(),p.level(),p,72000-20);
        near(h,d.stamina,1,"insufficient bow shot denied");
        h.assertTrue(p.getInventory().getItem(1).getCount()==31,"denied bow shot preserves its ammunition");
        d.stamina=d.maxStamina();d.exhausted=false;p.tickCount++;
        var crossbow=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CROSSBOW);
        crossbow.enchant(p.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.MULTISHOT),1);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,crossbow);before=d.stamina;
        crossbow.getItem().releaseUsing(crossbow,p.level(),p,crossbow.getUseDuration(p)-25);
        h.assertTrue(net.minecraft.world.item.CrossbowItem.isCharged(crossbow),"real crossbow successfully charged");
        near(h,d.stamina,before-10,"crossbow Multishot charges once while loading");before=d.stamina;
        ((net.minecraft.world.item.CrossbowItem)crossbow.getItem()).performShooting(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND,crossbow,3.15F,1F,null);
        near(h,d.stamina,before,"crossbow Multishot firing does not charge again");
        patch.toVanillaMode(false);
    }
    private static void near(GameTestHelper h,double value,double expected,String message){h.assertTrue(Math.abs(value-expected)<0.001,message+": "+value+" != "+expected);}
    private EpicFightGameTests(){}
}
