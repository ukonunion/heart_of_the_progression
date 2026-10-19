package dev.rpgprogression.requirements;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;

public final class InteractionRequirements {
    public static final TagKey<Item> REDSTONE=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("rpgprogression","redstone_intelligence"));
    public static boolean canTrade(Player player){return player.isCreative()||player.isSpectator()||ItemRequirements.unmet(player,Map.of(RPGAttribute.INTELLIGENCE,RPGConfig.TRADE_INTELLIGENCE.get())).isEmpty();}
    public static ItemStack blockItem(BlockState state){
        if(state.is(Blocks.REDSTONE_WIRE))return new ItemStack(Items.REDSTONE);
        if(state.is(Blocks.REDSTONE_WALL_TORCH))return new ItemStack(Items.REDSTONE_TORCH);
        return new ItemStack(state.getBlock());
    }
    public static ItemStack entityItem(Entity entity){
        return entity instanceof AbstractMinecart||entity instanceof Boat?entity.getPickResult():ItemStack.EMPTY;
    }
    private InteractionRequirements(){}
}
