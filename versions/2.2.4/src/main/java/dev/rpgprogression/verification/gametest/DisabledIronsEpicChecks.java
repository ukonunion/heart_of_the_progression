package dev.rpgprogression.verification.gametest;

import dev.rpgprogression.compat.*;
import dev.rpgprogression.progression.ProgressionManager;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class DisabledIronsEpicChecks {
    public static void run(GameTestHelper h,ServerPlayer p){
        RangedEpicSetup.mode(p,true);var data=ProgressionManager.get(p);data.stamina=100;
        var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(p,yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch.class);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ItemRegistry.MAGEHUNTER.get()));
        patch.onStrike(yesman.epicfight.gameasset.Animations.SWORD_AUTO1.get(),InteractionHand.MAIN_HAND);
        h.assertFalse(((EpicAttackAccess)patch).rpg$attackAllowed(),"native Epic Fight strike denied");
        h.assertTrue(data.stamina==100,"blocked strike consumes no stamina");
        h.assertFalse(EpicFightBridge.strike(patch,InteractionHand.MAIN_HAND),"direct Epic strike helper denied");
        RangedEpicSetup.mode(p,false);
    }
    private DisabledIronsEpicChecks(){}
}
