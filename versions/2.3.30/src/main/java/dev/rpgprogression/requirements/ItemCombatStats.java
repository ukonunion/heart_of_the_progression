package dev.rpgprogression.requirements;

import dev.rpgprogression.compat.EpicFightCompat;
import dev.rpgprogression.stamina.StaminaManager;
import dev.rpgprogression.stamina.RangedStaminaManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** Read-only item information, using the same costs as the server's attack handlers. */
public final class ItemCombatStats {
    public static boolean ranged(ItemStack stack){return stack.getItem() instanceof BowItem||stack.getItem() instanceof CrossbowItem;}
    public static boolean melee(ItemStack stack){
        boolean[] damage={false};stack.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)->{if(attribute.equals(Attributes.ATTACK_DAMAGE))damage[0]=true;});
        return damage[0]||EpicFightCompat.weapon(stack);
    }
    public static boolean weapon(ItemStack stack){return !stack.isEmpty()&&(ranged(stack)||melee(stack));}
    public static double attackCost(Player player,ItemStack stack){
        if(player!=null&&StaminaManager.exempt(player))return 0;
        double base=EpicFightCompat.battle(player)?EpicFightCompat.attackCost(player,stack):StaminaManager.regularAttackCost();
        return StaminaManager.cost(player,base);
    }
    public static double rangedCost(Player player,ItemStack stack){
        String category=stack.getItem() instanceof CrossbowItem?"crossbow":stack.getItem() instanceof TridentItem?"spear":"bow";
        return RangedStaminaManager.cost(player,category);
    }
    private ItemCombatStats(){}
}
