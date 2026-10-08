package dev.rpgprogression.compat;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.PlayerProgressionProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/** Safe to load with Epic Fight absent and on a dedicated server. */
public final class EpicFightCompat {
    public static boolean present(){return ModList.get().getModContainerById("epicfight")
            .map(c->c.getModInfo().getVersion().toString().startsWith("20.14.")).orElse(false);}
    public static boolean enabled(){return present()&&RPGConfig.EPICFIGHT_COMPAT.get();}
    public static boolean shares(Player p){return p!=null&&enabled()&&(!p.level().isClientSide||p.isLocalPlayer())
            &&p.getCapability(PlayerProgressionProvider.CAPABILITY).map(d->d.initialized).orElse(false);}
    private EpicFightCompat(){}
}
