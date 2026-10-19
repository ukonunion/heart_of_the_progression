package dev.rpgprogression.block;

import dev.rpgprogression.RPGProgression;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(RPGProgression.MOD_ID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(RPGProgression.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,RPGProgression.MOD_ID);
    public static final DeferredBlock<TravelSkintBlock> TRAVELSKINT=BLOCKS.register("travelskint",()->new TravelSkintBlock(
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE)
            .strength(-1,3600000).noLootTable().noOcclusion().pushReaction(PushReaction.BLOCK)
            .isValidSpawn((state,level,pos,type)->false)));
    public static final DeferredItem<BlockItem> TRAVELSKINT_ITEM=ITEMS.registerSimpleBlockItem(TRAVELSKINT);
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> MAIN_TAB=TABS.register("main",()->CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.rpgprogression"))
        .icon(()->new ItemStack(TRAVELSKINT_ITEM.get()))
        .displayItems((parameters,output)->output.accept(TRAVELSKINT_ITEM.get())).build());
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);TABS.register(bus);}
    private ModBlocks(){}
}
