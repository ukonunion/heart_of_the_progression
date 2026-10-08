package dev.rpgprogression.gametest;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.IronsCompat;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.MinecraftForge;
import java.util.UUID;
public final class IronsGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);IronsCompat.refresh(p,d);var magic=MagicData.getPlayerMagicData(p);
        h.assertTrue(magic!=null&&d.manaActive,"native magic attached and RPG mana enabled");near(h,magic.getMana(),100,"initial shared mana");
        d.set(RPGAttribute.INTELLIGENCE,12);IronsCompat.refresh(p,d);near(h,d.manaCapacity,120,"intelligence scales native maximum");
        var gear=UUID.randomUUID();var attr=p.getAttribute(AttributeRegistry.MAX_MANA.get());
        attr.addTransientModifier(new AttributeModifier(gear,"test gear",20,AttributeModifier.Operation.ADDITION));IronsCompat.refresh(p,d);near(h,d.manaCapacity,140,"equipment bonus retained");
        magic.setMana(100);var spell=SpellRegistry.FIREBOLT_SPELL.get();int cost=spell.getManaCost(1);
        h.assertTrue(spell.canBeCastedBy(1,CastSource.SPELLBOOK,magic,p).isSuccess(),"native cast predicate sees RPG pool");
        spell.castSpell(p.level(),1,p,CastSource.SPELLBOOK,false);near(h,d.mana,100-cost,"real native firebolt consumes RPG mana exactly once");
        double before=d.mana;h.assertFalse(new MagicManager().regenPlayerMana(p,magic),"native second regeneration disabled");near(h,d.mana,before,"native regen leaves pool unchanged");
        for(int i=0;i<40;i++)IronsCompat.tick(p,d);near(h,d.mana,before,"RPG regen delay after cast");
        for(int i=0;i<20;i++)IronsCompat.tick(p,d);near(h,d.mana,before+5.5,"RPG regeneration per second");
        d.mana=10;magic.addMana(5);near(h,d.mana,15,"native addMana uses current RPG value");
        magic.setMana(0);h.assertFalse(spell.canBeCastedBy(1,CastSource.SPELLBOOK,magic,p).isSuccess(),"native spell rejects empty shared mana");
        java.util.function.Consumer<ChangeManaEvent> veto=e->{if(e.getEntity()==p)e.setCanceled(true);};MinecraftForge.EVENT_BUS.addListener(veto);
        try{magic.setMana(90);near(h,d.mana,0,"native ChangeManaEvent cancellation respected");}finally{MinecraftForge.EVENT_BUS.unregister(veto);}
        magic.setMana(Float.NaN);near(h,d.mana,0,"nonfinite mana rejected");magic.setMana(9999);near(h,d.mana,140,"native mana clamped to gear maximum");
        var mobMagic=new MagicData(false);mobMagic.setMana(12);near(h,mobMagic.getMana(),12,"mob mana unaffected");
        RPGConfig.IRONS_MANA.set(false);
        try{IronsCompat.refresh(p,d);magic.setMana(3);near(h,d.mana,140,"disabled adapter leaves RPG save unchanged");near(h,attr.getValue(),120,"only RPG modifier removed");h.assertFalse(d.manaActive,"mana hidden when disabled");}
        finally{RPGConfig.IRONS_MANA.set(true);attr.removeModifier(gear);}
    }
    private static void near(GameTestHelper h,double actual,double expected,String what){h.assertTrue(Math.abs(actual-expected)<.002,what+": "+actual+" != "+expected);}
}
