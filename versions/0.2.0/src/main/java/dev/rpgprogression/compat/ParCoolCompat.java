package dev.rpgprogression.compat;

import dev.rpgprogression.config.RPGConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/** No ParCool type appears in signatures loaded by the standalone RPG mod. */
public final class ParCoolCompat {
    public static boolean present() {
        return ModList.get().getModContainerById("parcool")
                .map(c->c.getModInfo().getVersion().toString().startsWith("4.")).orElse(false);
    }
    public static boolean enabled() { return present()&&RPGConfig.PARCOOL_COMPAT.get(); }
    public static boolean replacesSprint(Player player) {
        return enabled()&&ParCoolBridge.fastRunning(player);
    }
    private ParCoolCompat() {}
}
