package dev.rpgprogression.compat;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/** No Iron's class is referenced by common signatures. */
public final class IronsCompat {
    public static boolean present(){return ModList.get().getModContainerById("irons_spellbooks").map(c->c.getModInfo().getVersion().toString().startsWith("1.20.1-3.16.")).orElse(false);}
    public static boolean enabled(){return present()&&RPGConfig.IRONS_MANA.get();}
    public static boolean shares(ServerPlayer p){return p!=null&&enabled()&&p.getCapability(PlayerProgressionProvider.CAPABILITY).map(d->d.initialized&&d.manaInitialized).orElse(false);}
    public static void refresh(ServerPlayer p,PlayerProgression d){if(present())IronsBridge.refresh(p,d);else d.manaActive=false;}
    public static void tick(ServerPlayer p,PlayerProgression d){if(present())IronsBridge.tick(p,d);else d.manaActive=false;}
    private IronsCompat(){}
}
