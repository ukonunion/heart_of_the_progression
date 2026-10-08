package dev.rpgprogression.compat;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.MinecraftForge;
import java.util.UUID;

/** Native spell checks, refunds, potions and ChangeManaEvent all operate on the RPG pool. */
public final class IronsBridge {
    private static final UUID MAXIMUM=UUID.fromString("efb9a68e-5b72-4af6-a7dd-31f0ae9b9b78");
    private static final ThreadLocal<Boolean> REGENERATING=ThreadLocal.withInitial(()->false);
    public static void refresh(ServerPlayer p,PlayerProgression d){
        var attr=p.getAttribute(AttributeRegistry.MAX_MANA.get());if(attr==null){d.manaActive=false;return;}
        double amount=IronsCompat.enabled()?d.baseMana()-100:0;
        var old=attr.getModifier(MAXIMUM);
        if(old!=null&&(old.getAmount()!=amount||amount==0)){attr.removeModifier(MAXIMUM);old=null;}
        if(amount!=0&&old==null)attr.addTransientModifier(new AttributeModifier(MAXIMUM,"RPG mana",amount,AttributeModifier.Operation.ADDITION));
        d.manaActive=IronsCompat.enabled();
        if(!d.manaActive)return;
        d.manaCapacity=Math.max(0,attr.getValue());
        if(!d.manaInitialized){d.mana=d.manaCapacity;d.manaInitialized=true;d.dirty=true;}
        d.mana=Math.max(0,Math.min(d.mana,d.manaCapacity));
    }
    public static void tick(ServerPlayer p,PlayerProgression d){
        refresh(p,d);if(!d.manaActive||!p.isAlive())return;
        if(d.manaRegenCooldown>0){d.manaRegenCooldown--;return;}
        double intelligence=Math.max(0,d.get(RPGAttribute.INTELLIGENCE)-RPGConfig.START.get());
        double rate=(RPGConfig.MANA_REGEN.get()+intelligence*RPGConfig.MANA_REGEN_PER_INTELLIGENCE.get())*Math.max(0,p.getAttributeValue(AttributeRegistry.MANA_REGEN.get()))/20;
        if(rate<=0||d.mana>=d.manaCapacity)return;
        REGENERATING.set(true);
        try{MagicData.getPlayerMagicData(p).setMana((float)Math.min(d.manaCapacity,d.mana+rate));}
        finally{REGENERATING.remove();}
    }
    public static float set(ServerPlayer p,MagicData magic,float value){
        var d=ProgressionManager.get(p);double old=d.mana;
        if(!Float.isFinite(value))return (float)old;
        var e=new ChangeManaEvent(p,magic,(float)old,value);
        if(MinecraftForge.EVENT_BUS.post(e)||!Float.isFinite(e.getNewMana()))return (float)old;
        d.mana=Math.max(0,Math.min(d.manaCapacity,e.getNewMana()));
        if(old-d.mana>0.0001)d.manaRegenCooldown=(int)Math.ceil(RPGConfig.MANA_REGEN_DELAY.get()*20);
        if(!REGENERATING.get()&&Math.abs(d.mana-old)>0.0001)ModNetworking.resources(p);
        return (float)d.mana;
    }
    private IronsBridge(){}
}
