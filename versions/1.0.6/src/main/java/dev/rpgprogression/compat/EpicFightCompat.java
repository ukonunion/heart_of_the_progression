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
    public static boolean battle(Player p){return shares(p)&&EpicFightBridge.battle(p);}
    public static boolean weapon(net.minecraft.world.item.ItemStack stack){return enabled()&&EpicFightBridge.weapon(stack);}
    public static double attackCost(Player p,net.minecraft.world.item.ItemStack stack){return battle(p)?EpicFightBridge.previewAttackCost(p,stack):dev.rpgprogression.stamina.StaminaManager.regularAttackCost();}
    public static double rangedCost(Player p,String category){return battle(p)?EpicFightBridge.previewRangedCost(p,category):0;}
    public static double maximum(Player p,double base){return shares(p)?EpicFightBridge.maximum(p,base):base;}
    public static double regeneration(Player p){return shares(p)?EpicFightBridge.regeneration(p):1;}
    public static int regenerationDelay(Player p,int regular){return shares(p)?EpicFightBridge.regenerationDelay(p,regular):regular;}
    private EpicFightCompat(){}
}
