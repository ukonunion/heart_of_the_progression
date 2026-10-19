package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.EpicAttackAccess;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public final class RequirementsEpicChecks {
    public static void run(ServerPlayer p,JsonArray checks){
        var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);
        if(patch==null)throw new IllegalStateException("Missing native Epic Fight player patch");
        var d=ProgressionManager.get(p);var data=d.save();var held=p.getMainHandItem();boolean battle=patch.isEpicFightMode();
        boolean shared=RPGConfig.EPICFIGHT_COMPAT.get();var weight=p.getAttribute(yesman.epicfight.registry.entries.EpicFightAttributes.WEIGHT);double baseWeight=weight.getBaseValue();
        try{
            weight.setBaseValue(40);patch.toEpicFightMode(false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
            d.set(RPGAttribute.ENDURANCE,14);d.stamina=d.maxStamina();d.exhausted=false;double before=d.stamina;
            patch.onStrike(yesman.epicfight.gameasset.Animations.SWORD_AUTO1.get(),InteractionHand.MAIN_HAND);
            SkintTravelChecks.check(checks,!((EpicAttackAccess)patch).rpg$attackAllowed()&&d.stamina==before,"a real Epic Fight strike below Endurance fifteen is rejected without stamina spending");
            d.set(RPGAttribute.ENDURANCE,15);d.stamina=d.maxStamina();before=d.stamina;
            patch.onStrike(yesman.epicfight.gameasset.Animations.SWORD_AUTO1.get(),InteractionHand.MAIN_HAND);
            SkintTravelChecks.check(checks,((EpicAttackAccess)patch).rpg$attackAllowed()&&d.stamina<before,"a real Epic Fight strike unlocks at Endurance fifteen and retains its stamina cost");
            RPGConfig.EPICFIGHT_COMPAT.set(false);d.set(RPGAttribute.ENDURANCE,14);before=d.stamina;
            patch.onStrike(yesman.epicfight.gameasset.Animations.SWORD_AUTO1.get(),InteractionHand.MAIN_HAND);
            SkintTravelChecks.check(checks,!((EpicAttackAccess)patch).rpg$attackAllowed()&&d.stamina==before,"turning off shared stamina does not bypass weapon attribute requirements");
        }finally{
            RPGConfig.EPICFIGHT_COMPAT.set(shared);p.setItemInHand(InteractionHand.MAIN_HAND,held);d.load(data);weight.setBaseValue(baseWeight);
            if(battle)patch.toEpicFightMode(false);else patch.toVanillaMode(false);
        }
    }
    private RequirementsEpicChecks(){}
}
