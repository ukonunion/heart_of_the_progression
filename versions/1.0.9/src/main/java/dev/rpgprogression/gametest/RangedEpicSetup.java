package dev.rpgprogression.gametest;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
public final class RangedEpicSetup {
    public static void mode(ServerPlayer player,boolean battle){
        var patch=EpicFightCapabilities.getEntityPatch(player,PlayerPatch.class);
        player.getAttribute(yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes.WEIGHT.get()).setBaseValue(40);
        if(battle)patch.toEpicFightMode(false);else patch.toVanillaMode(false);
    }
    private RangedEpicSetup(){}
}
