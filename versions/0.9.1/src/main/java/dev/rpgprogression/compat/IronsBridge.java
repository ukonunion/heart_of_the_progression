package dev.rpgprogression.compat;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import java.util.UUID;

/** RPG capacity/recovery; external refunds and equipment cannot mint mana. */
public final class IronsBridge {
    private static final UUID MAXIMUM=UUID.fromString("efb9a68e-5b72-4af6-a7dd-31f0ae9b9b78");
    private static final ThreadLocal<Boolean> REGENERATING=ThreadLocal.withInitial(()->false);
    public static void refresh(ServerPlayer p,PlayerProgression d){
        var attr=p.getAttribute(AttributeRegistry.MAX_MANA.get());if(attr==null){d.manaActive=false;return;}
        attr.removeModifier(MAXIMUM);
        d.manaActive=IronsCompat.enabled();
        if(!d.manaActive)return;
        d.manaCapacity=d.baseMana();
        if(!d.manaInitialized){d.mana=0;d.manaInitialized=true;d.dirty=true;}
        d.mana=Math.max(0,Math.min(d.mana,d.manaCapacity));
    }
    public static void tick(ServerPlayer p,PlayerProgression d){
        refresh(p,d);if(!d.manaActive||!p.isAlive())return;
        if(d.manaRegenCooldown>0){d.manaRegenCooldown--;return;}
        double rate=RPGConfig.MANA_REGEN.get()/20;
        if(rate<=0||d.mana>=d.manaCapacity)return;
        recover(p,Math.min(d.manaCapacity,d.mana+rate));
    }
    private static void recover(ServerPlayer p,double value){
        boolean prior=REGENERATING.get();REGENERATING.set(true);
        try{MagicData.getPlayerMagicData(p).setMana((float)value);}
        finally{if(prior)REGENERATING.set(true);else REGENERATING.remove();}
    }
    public static void potion(ServerPlayer p,int amplifier,double strength){
        if(!Double.isFinite(strength)||strength<=0)return;
        var d=ProgressionManager.get(p);
        // Native instant mana: 25 + 5% of maximum per amplifier, converted to new units.
        recover(p,d.mana+(1+Math.max(0,amplifier))*(1+d.manaCapacity*.05)*strength);
        ModNetworking.resources(p);
    }
    public static float set(ServerPlayer p,MagicData magic,float value){
        var d=ProgressionManager.get(p);double old=d.mana;
        if(!Float.isFinite(value)||value>old&&!REGENERATING.get())return (float)old;
        var e=new ChangeManaEvent(p,magic,(float)old,value);
        if(MinecraftForge.EVENT_BUS.post(e)||!Float.isFinite(e.getNewMana()))return (float)old;
        d.mana=Math.max(0,Math.min(d.manaCapacity,Math.min(value,e.getNewMana())));
        if(old-d.mana>0.0001)d.manaRegenCooldown=(int)Math.ceil(RPGConfig.MANA_REGEN_DELAY.get()*20);
        if(!REGENERATING.get()&&Math.abs(d.mana-old)>0.0001)ModNetworking.resources(p);
        return (float)d.mana;
    }
    private IronsBridge(){}
}
