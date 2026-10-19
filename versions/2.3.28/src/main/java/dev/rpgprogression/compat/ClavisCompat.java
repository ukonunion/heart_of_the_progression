package dev.rpgprogression.compat;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ClavisMath;
import dev.rpgprogression.progression.PlayerProgressionProvider;
import net.minecraft.world.entity.player.Player;

/** No Clavis or client classes are linked when the optional mod is absent. */
public final class ClavisCompat {
    public static double lifeSaveChance(Player player) {
        if(player==null)return 0;
        return PlayerProgressionProvider.get(player)
                .map(d->ClavisMath.lifeSaveChance(d.get(RPGAttribute.AGILITY),RPGConfig.CLAVIS_LIFE_SAVE.get())).orElse(0.0);
    }
    private ClavisCompat(){}
}
