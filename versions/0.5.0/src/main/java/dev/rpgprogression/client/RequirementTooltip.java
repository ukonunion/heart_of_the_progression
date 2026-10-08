package dev.rpgprogression.client;

import dev.rpgprogression.requirements.ItemRequirements;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class RequirementTooltip {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent e){
        if(e.getEntity()==null)return;
        var rules=ItemRequirements.required(e.getItemStack());if(rules.isEmpty())return;
        e.getToolTip().add(Component.translatable("rpgprogression.requirements.title").withStyle(ChatFormatting.GOLD));
        rules.forEach((attribute,minimum)->{
            int current=ClientData.get().get(attribute);
            e.getToolTip().add(Component.translatable("rpgprogression.requirements.entry",Component.translatable(attribute.key()),current,minimum)
                    .withStyle(current>=minimum?ChatFormatting.GREEN:ChatFormatting.RED));
        });
    }
    private RequirementTooltip(){}
}
