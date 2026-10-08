package dev.rpgprogression.inventory;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.Slot;
import java.util.List;

public final class InventorySlotRules {
    private static List<? extends Integer> cachedConfig;
    private static int[] cachedThresholds;
    public static int[] thresholds() {
        List<? extends Integer> configured=RPGConfig.SLOT_THRESHOLDS.get();
        if(configured==cachedConfig&&cachedThresholds!=null)return cachedThresholds;
        int[] result=new int[27]; int previous=0;
        for(int i=0;i<27;i++) { previous=Math.max(previous,i<configured.size()?configured.get(i):12+(i/3)*2); result[i]=previous; }
        cachedThresholds=result;cachedConfig=configured;return result;
    }
    public static int unlocked(Player p) {
        if(p.isSpectator()||(p.isCreative()&&!RPGConfig.CREATIVE_LOCKS.get()))return 27;
        return p.getCapability(PlayerProgressionProvider.CAPABILITY).map(d->ProgressionMath.unlocked(d.get(RPGAttribute.ENDURANCE),thresholds())).orElse(0);
    }
    public static boolean locked(Player p,int inventoryIndex) { return inventoryIndex>=9&&inventoryIndex<36&&inventoryIndex-9>=unlocked(p); }
    public static boolean locked(Slot slot) {
        return slot.container instanceof Inventory inv && locked(inv.player,slot.getContainerSlot());
    }
    private InventorySlotRules() {}
}
