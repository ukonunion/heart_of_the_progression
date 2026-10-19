package dev.rpgprogression.client;

import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ToastAddEvent;

/** Also catches mods which enqueue a RecipeToast directly instead of calling addOrUpdate. */
@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class RecipeNotifications {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void toast(ToastAddEvent event){if(event.getToast() instanceof RecipeToast)event.setCanceled(true);}
    private RecipeNotifications(){}
}
