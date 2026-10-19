package dev.rpgprogression.gametest;
import dev.rpgprogression.compat.*;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import java.util.function.BiConsumer;
public final class AttributeBalanceEpicChecks {
    private static void near(BiConsumer<Boolean,String> check,double value,double expected,String message){check.accept(Math.abs(value-expected)<.001,message+": "+value+" == "+expected);}
    public static void run(ServerPlayer p,BiConsumer<Boolean,String> check){
        RangedEpicSetup.mode(p,true);var d=ProgressionManager.get(p);var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(p,yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch.class);
        d.stamina=7;d.exhausted=false;check.accept(patch.hasStamina(2),"native Epic Fight skill can afford a discounted ten-unit price with seven stamina");
        yesman.epicfight.skill.Skill.Resource.STAMINA.consumer.consume(null,patch,2);near(check,d.stamina,0,"native negative target preserves the full raw price and discounts once");
        d.stamina=6.9;d.exhausted=false;check.accept(!patch.hasStamina(2),"native skill rejects less than the discounted price");
        d.stamina=7;d.exhausted=false;EpicFightBridge.clientRequest(patch,-.6F,true);near(check,d.stamina,0,"affordable native client debit applies the same discount");
        d.stamina=6;d.exhausted=false;EpicFightBridge.clientRequest(patch,-.8F,true);near(check,d.stamina,6,"unaffordable native client debit cannot overdraw");
        d.stamina=100;d.exhausted=false;yesman.epicfight.skill.Skill.Resource.STAMINA.consumer.consume(null,patch,3);near(check,d.stamina,89.5,"native Epic Fight skill receives exactly one thirty percent discount");
        var sword=yesman.epicfight.world.item.EpicFightItems.IRON_GREATSWORD.get();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(sword));
        near(check,dev.rpgprogression.requirements.ItemCombatStats.attackCost(p,p.getMainHandItem()),16.8,"heavy weapon tooltip has the discounted native price");
        d.stamina=16.8;d.exhausted=false;patch.onStrike(yesman.epicfight.gameasset.Animations.GREATSWORD_AUTO1.get(),InteractionHand.MAIN_HAND);
        check.accept(((EpicAttackAccess)patch).rpg$attackAllowed(),"native heavy strike permits exactly the discounted price");near(check,d.stamina,0,"native heavy strike pays once rather than double-discounting");
        RangedEpicSetup.mode(p,false);
    }
    private AttributeBalanceEpicChecks(){}
}
