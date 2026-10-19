package dev.rpgprogression.compat;
import com.alrex.parcool.api.Effects;
import com.alrex.parcool.common.action.*;
import com.alrex.parcool.common.action.impl.*;
import com.alrex.parcool.common.attachment.Attachments;
import com.alrex.parcool.common.attachment.common.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
/** Server owns debits; ParCool receives a mirror and cannot refill the RPG reservoir. */
public final class ParCoolBridge {
    public static boolean infinite(Player p){return StaminaManager.exempt(p)||p.hasEffect(Effects.INEXHAUSTIBLE);}
    public static boolean fastRunning(Player p){return Parkourability.get(p).get(FastRun.class).isDoing();}
    public static boolean fastSwimming(Player p){return Parkourability.get(p).get(FastSwim.class).isDoing();}
    public static double cost(Player p,Action a){
        if(infinite(p)||!ParCoolCompat.enabled())return 0;
        if(a instanceof Dodge&&net.neoforged.fml.ModList.get().isLoaded("epicparcool")&&EpicFightCompat.battle(p))return yesman.epicfight.registry.entries.EpicFightSkills.ROLL.get().getConsumption()*RPGConfig.EPICFIGHT_UNIT.get();
        // Preserve our 2.0.0 prices despite changed native defaults; honor proportional config overrides.
        double base=switch(String.valueOf(ParCoolSkills.id(a.getClass()))){
            case "parcool:wall_jump","parcool:wall_run","parcool:climb_up","parcool:long_jump","parcool:charge_jump","parcool:dodge","parcool:breakfall","parcool:vault" -> 50;
            case "parcool:fast_run","parcool:fast_swim","parcool:ride_zipline" -> 2;
            case "parcool:hang_on","parcool:horizontal_wall_run" -> 3;
            case "parcool:slide_down","parcool:pole_climb" -> 1;
            default -> 0;
        };
        int standard=Actions.ACTION_REGISTRIES.stream().filter(r->r.getClassInstance()==a.getClass()).mapToInt(Actions.ActionRegistry::getDefaultStaminaConsumption).findFirst().orElse(0);
        // The native unsynchronized client settings use Integer.MAX_VALUE as a sentinel.
        // RPG prices come from the server; client settings must not change the shared reservoir.
        var limitation=p instanceof ServerPlayer server?com.alrex.parcool.common.info.ServerLimitation.get(server):Parkourability.get(p).getActionInfo().getServerLimitation();
        double configured=Math.max(standard,limitation.isSynced()?limitation.getStaminaConsumptionOf(a.getClass()):standard);
        return base*(standard>0?configured/standard:1)*RPGConfig.PARCOOL_COST.get();
    }
    public static boolean blocked(Player p,Action a){
        if(p.isRemoved()||!p.isAlive())return true;
        if(p.level().isClientSide&&!p.isLocalPlayer())return false;
        String id=ParCoolSkills.id(a.getClass());
        if(dev.rpgprogression.skill.SkillTreeManager.parcool()&&id!=null&&(!p.level().isClientSide||p.isLocalPlayer())&&!ProgressionManager.get(p).unlockedSkills.contains(id))return true;
        return ParCoolCompat.enabled()&&!infinite(p)&&cost(p,a)>0&&!StaminaManager.canSpend(p,cost(p,a));
    }
    public static void spend(Player p,Action a){if(ParCoolCompat.enabled()&&p instanceof ServerPlayer){StaminaManager.spend(p,ProgressionManager.get(p),cost(p,a));mirror(p);}}
    public static void mirror(Player p){
        if(!ParCoolCompat.enabled()||p.level().isClientSide&&!p.isLocalPlayer())return;
        var d=PlayerProgressionProvider.get(p).orElse(null);if(d==null||!d.initialized)return;
        double unit=0.001;
        p.setData(Attachments.STAMINA,new ReadonlyStamina(!infinite(p)&&d.exhausted,(int)Math.min(Integer.MAX_VALUE,Math.ceil(d.stamina/unit)),(int)Math.min(Integer.MAX_VALUE,Math.ceil(d.maxStamina()/unit))));
    }
    public static double value(Player p){mirror(p);return p.getData(Attachments.STAMINA).value()/1000.0;}
    public static double maximum(Player p){mirror(p);return p.getData(Attachments.STAMINA).max()/1000.0;}
    public static void working(Player p,Action a){
        if(!(p instanceof ServerPlayer)||!ParCoolCompat.enabled())return;
        if(a.isDoing()&&blocked(p,a)){a.finish(p);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.alrex.parcool.api.unstable.action.ParCoolActionEvent.StopEvent(p,a));}
        else if(a.isDoing()&&(a.getStaminaConsumeTiming()==StaminaConsumeTiming.OnWorking||a instanceof ClimbPoles))spend(p,a);
        mirror(p);
    }
    private ParCoolBridge(){}
}
