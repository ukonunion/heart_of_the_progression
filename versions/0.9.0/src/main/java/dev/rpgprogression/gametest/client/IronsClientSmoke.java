package dev.rpgprogression.gametest.client;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.compat.IronsCompat;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import java.util.function.BiConsumer;
public final class IronsClientSmoke {
    private static double expected;
    public static void tick(int tick,Minecraft mc,BiConsumer<Boolean,String> check){
        if(tick==360)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var d=ProgressionManager.get(p);d.set(dev.rpgprogression.attribute.RPGAttribute.SPARK_POTENTIAL,50);IronsCompat.refresh(p,d);
            var magic=MagicData.getPlayerMagicData(p);d.mana=8;var spell=SpellRegistry.FIREBOLT_SPELL.get();expected=8-spell.getManaCost(1);
            spell.castSpell(p.level(),1,p,CastSource.SPELLBOOK,false);d.manaRegenCooldown=200;ModNetworking.sync(p);
            check.accept(Math.abs(d.mana-expected)<.01,"integrated server native spell consumes RPG mana once");ModNetworking.resources(p);
        });
        if(tick==390){
            check.accept(Math.abs(ClientData.get().mana-expected)<.01,"native spell cost synchronized to RPG client");
            check.accept(ClientMagicData.getPlayerMana()==(int)ClientData.get().mana,"native client spells read RPG mana");
        }
    }
}
