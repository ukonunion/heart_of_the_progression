package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.IronsCompat;
import dev.rpgprogression.progression.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;

/** Actual Iron's capacity, casting and old-save checks; never shipped in the JAR. */
public final class IronsIntelligenceChecks {
    public static void run(ServerPlayer p,JsonArray checks){
        var d=ProgressionManager.get(p);var original=d.save();var magic=MagicData.getPlayerMagicData(p);
        try{
            d.set(RPGAttribute.SPARK_POTENTIAL,100);d.mana=0;
            for(int[] row:new int[][]{{10,0},{14,0},{15,1},{49,7},{50,8},{100,8}}){
                d.set(RPGAttribute.INTELLIGENCE,row[0]);IronsCompat.refresh(p,d);
                SkintTravelChecks.check(checks,d.manaActive&&d.manaCapacity==row[1]&&p.getAttributeValue(AttributeRegistry.MAX_MANA)==row[1],"real Iron's capacity at Intelligence "+row[0]+" is "+row[1]+" even with Potential 100");
            }
            d.set(RPGAttribute.SPARK_POTENTIAL,10);d.set(RPGAttribute.INTELLIGENCE,50);IronsCompat.refresh(p,d);
            SkintTravelChecks.check(checks,d.manaCapacity==8&&d.maxSpark()==25,"starting Spark Potential no longer limits the eight-unit native mana pool");
            d.mana=8;d.set(RPGAttribute.INTELLIGENCE,15);IronsCompat.refresh(p,d);
            SkintTravelChecks.check(checks,d.mana==1&&magic.getMana()==1,"lower Intelligence immediately clamps stored and native mana to its new capacity");
            d.set(RPGAttribute.INTELLIGENCE,50);IronsCompat.refresh(p,d);
            SkintTravelChecks.check(checks,d.manaCapacity==8&&d.mana==1,"raising Intelligence increases capacity without refilling mana");
            d.mana=8;var spell=SpellRegistry.FIREBOLT_SPELL.get();int cost=spell.getManaCost(1);
            SkintTravelChecks.check(checks,spell.canBeCastedBy(1,CastSource.SPELLBOOK,magic,p).isSuccess(),"an actual book spell accepts mana obtained through Intelligence");
            spell.castSpell(p.level(),1,p,CastSource.SPELLBOOK,false);
            SkintTravelChecks.check(checks,Math.abs(d.mana-(8-cost))<.001,"actual book casting spends its converted mana cost once");
            var saved=d.save();saved.putInt("intelligence",15);saved.putInt("spark_potential",50);saved.putDouble("manaCapacity",8);saved.putDouble("mana",6);
            int level=d.level,points=d.points;var discovered=java.util.Set.copyOf(d.discoveredSkints);
            d.load(saved);IronsCompat.refresh(p,d);
            SkintTravelChecks.check(checks,d.manaCapacity==1&&d.mana==1&&d.level==level&&d.points==points&&d.discoveredSkints.equals(discovered),"loading a 2.3.7-era save recalculates capacity from Intelligence without resetting levels, points or discoveries");
        }finally{d.load(original);IronsCompat.refresh(p,d);}
    }
    private IronsIntelligenceChecks(){}
}
