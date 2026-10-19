package dev.rpgprogression.compat;

import com.alrex.parcool.api.ParCoolMobEffects;
import com.alrex.parcool.api.action.*;
import com.alrex.parcool.api.stamina.IReadableStamina;
import com.alrex.parcool.common.Parkourability;
import com.alrex.parcool.common.action.ParCoolActions;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Only reached while ParCool 4 is installed. Costs are computed from its server config. */
public final class ParCoolBridge {
    public static boolean infinite(Player player) {
        return StaminaManager.exempt(player)||player.hasEffect(ParCoolMobEffects.INEXHAUSTIBLE);
    }
    public static boolean blocked(Parkourability parkour,ActionEntry<?> entry) {
        var player=parkour.player();
        if(dev.rpgprogression.skill.SkillTreeManager.parcool()&&entry.option().needLearning()
                &&(!player.level().isClientSide||player.isLocalPlayer())){
            var d=PlayerProgressionProvider.get(player).orElse(null);
            if(d==null||!d.unlockedSkills.contains(entry.id().toString()))return true;
        }
        return ParCoolCompat.enabled()&&!infinite(player)&&!entry.option().availableWhileExhausted()
                &&ProgressionManager.get(player).exhausted;
    }
    public static void spend(Parkourability parkour,ActionEntry<?> entry,StaminaConsumption.Type phase) {
        if(!ParCoolCompat.enabled()||!(parkour.player() instanceof ServerPlayer player)||infinite(player))return;
        var data=ProgressionManager.get(player);data.initialize();
        StaminaManager.spend(player,data,parkour.getCost(entry,phase)*RPGConfig.PARCOOL_COST.get());
    }
    public static boolean fastRunning(Player player) {
        return Parkourability.get(player).get(ParCoolActions.FAST_RUN).isDoing();
    }
    public static boolean fastSwimming(Player player){return Parkourability.get(player).get(ParCoolActions.FAST_SWIM).isDoing();}
    public static IReadableStamina view(Player player) {
        return player.isLocalPlayer()?new RPGLocalStamina(player):new ServerView(player);
    }
    private record ServerView(Player player) implements IReadableStamina {
        public double max(){return ProgressionManager.get(player).maxStamina();}
        public double value(){return ProgressionManager.get(player).stamina;}
        public boolean isExhausted(){return !infinite(player)&&ProgressionManager.get(player).exhausted;}
        // RPG applies its own exhaustion penalties; ParCool still checks isExhausted for actions.
        public boolean imposePenalty(){return false;}
    }
    private ParCoolBridge() {}
}
