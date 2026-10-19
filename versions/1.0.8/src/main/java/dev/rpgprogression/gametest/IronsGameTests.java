package dev.rpgprogression.gametest;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.IronsCompat;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.resource.ResourceMath;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.MinecraftForge;
import java.util.UUID;
public final class IronsGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);IronsCompat.refresh(p,d);var magic=MagicData.getPlayerMagicData(p);
        h.assertTrue(magic!=null&&d.manaActive,"native magic attached");near(h,magic.getMana(),0,"new player has zero mana");near(h,d.manaCapacity,0,"new player capacity zero");
        d.set(RPGAttribute.INTELLIGENCE,100);d.set(RPGAttribute.SPARK_POTENTIAL,14);IronsCompat.refresh(p,d);near(h,d.manaCapacity,0,"intelligence and incomplete spark milestone cannot grant mana");
        d.set(RPGAttribute.SPARK_POTENTIAL,15);IronsCompat.refresh(p,d);near(h,d.manaCapacity,1,"first capacity at spark15");near(h,d.mana,0,"capacity gain is not a refill");
        d.set(RPGAttribute.SPARK_POTENTIAL,50);IronsCompat.refresh(p,d);near(h,d.manaCapacity,8,"eight capacity at spark50");
        var attr=p.getAttribute(AttributeRegistry.MAX_MANA.get());var gear=UUID.randomUUID();
        attr.addTransientModifier(new AttributeModifier(gear,"test gear",200,AttributeModifier.Operation.ADDITION));
        var regen=p.getAttribute(AttributeRegistry.MANA_REGEN.get());var regenGear=UUID.randomUUID();regen.addTransientModifier(new AttributeModifier(regenGear,"regen gear",20,AttributeModifier.Operation.ADDITION));
        IronsCompat.refresh(p,d);near(h,d.manaCapacity,8,"gear maximum ignored");near(h,p.getAttributeValue(AttributeRegistry.MAX_MANA.get()),8,"native maximum also uses RPG capacity");
        java.util.Map<io.redspace.ironsspellbooks.api.spells.AbstractSpell,int[]> original=new java.util.HashMap<>();RPGConfig.IRONS_MANA.set(false);
        try{for(var spell:SpellRegistry.REGISTRY.get().getValues())original.put(spell,new int[]{spell.getManaCost(1),spell.getManaCost(5),spell.getManaCost(10)});}
        finally{RPGConfig.IRONS_MANA.set(true);}
        for(var entry:original.entrySet())for(int i=0;i<3;i++)h.assertTrue(entry.getKey().getManaCost(new int[]{1,5,10}[i])==ResourceMath.spellCost(entry.getValue()[i]),"converted cost "+entry.getKey().getSpellId());
        com.mojang.logging.LogUtils.getLogger().info("RPG_MANA_COSTS verified {} spells at three levels",original.size());
        d.mana=8;var spell=SpellRegistry.FIREBOLT_SPELL.get();int cost=spell.getManaCost(1);
        h.assertTrue(spell.canBeCastedBy(1,CastSource.SPELLBOOK,magic,p).isSuccess(),"book checks converted cost");
        spell.castSpell(p.level(),1,p,CastSource.SPELLBOOK,false);near(h,d.mana,8-cost,"real native book spell consumes once");
        double before=d.mana;h.assertFalse(new MagicManager().regenPlayerMana(p,magic),"second native regeneration disabled");
        for(int i=0;i<40;i++)IronsCompat.tick(p,d);near(h,d.mana,before,"regen delay");
        for(int i=0;i<20;i++)IronsCompat.tick(p,d);near(h,d.mana,before+.2,"natural recovery ignores equipment and intelligence");
        magic.addMana(5);magic.setMana(9999);near(h,d.mana,before+.2,"external mana grants blocked");
        d.mana=8;h.assertTrue(CastSource.SCROLL.consumesMana()&&CastSource.SWORD.consumesMana(),"scroll and imbued weapon costs enabled");
        spell.castSpell(p.level(),1,p,CastSource.SCROLL,false);near(h,d.mana,8-cost,"native scroll cast consumes once");
        magic.setMana(0);h.assertFalse(spell.canBeCastedBy(1,CastSource.SCROLL,magic,p).isSuccess(),"empty mana rejects scroll cast");
        h.assertFalse(spell.canBeCastedBy(1,CastSource.SPELLBOOK,magic,p).isSuccess(),"empty mana rejects book cast");
        java.util.function.Consumer<ChangeManaEvent> veto=e->{if(e.getEntity()==p)e.setCanceled(true);};MinecraftForge.EVENT_BUS.addListener(veto);
        try{MobEffectRegistry.INSTANT_MANA.get().applyInstantenousEffect(null,null,p,0,1);near(h,d.mana,0,"potion respects native cancellation");}finally{MinecraftForge.EVENT_BUS.unregister(veto);}
        MobEffectRegistry.INSTANT_MANA.get().applyInstantenousEffect(null,null,p,0,1);near(h,d.mana,1.4,"real instant mana potion converted");
        MobEffectRegistry.INSTANT_MANA.get().applyInstantenousEffect(null,null,p,1,.5);near(h,d.mana,2.8,"strong splash potion scales by strength");
        magic.setMana(Float.NaN);near(h,d.mana,2.8,"nonfinite ignored");
        var saved=d.save();saved.putInt("version",4);saved.putDouble("mana",100);saved.putDouble("manaCapacity",9999);saved.putInt("level",2500);
        var migrated=new PlayerProgression();migrated.load(saved);near(h,migrated.mana,4,"legacy mana converted once");near(h,migrated.manaCapacity,8,"legacy gear capacity discarded");h.assertTrue(migrated.level==2500,"no level cap on load");
        var copy=new PlayerProgression();copy.load(migrated.save());near(h,copy.mana,4,"no repeated conversion on reload");
        var mobMagic=new MagicData(false);mobMagic.setMana(12);near(h,mobMagic.getMana(),12,"mob pool unaffected");
        RPGConfig.IRONS_MANA.set(false);
        try{IronsCompat.refresh(p,d);magic.setMana(3);near(h,d.mana,2.8,"disabled adapter preserves RPG save");near(h,p.getAttributeValue(AttributeRegistry.MAX_MANA.get()),300,"disabled maximum is native");h.assertFalse(d.manaActive,"adapter hidden when disabled");}
        finally{RPGConfig.IRONS_MANA.set(true);attr.removeModifier(gear);regen.removeModifier(regenGear);}
    }
    private static void near(GameTestHelper h,double actual,double expected,String what){h.assertTrue(Math.abs(actual-expected)<.002,what+": "+actual+" != "+expected);}
}
