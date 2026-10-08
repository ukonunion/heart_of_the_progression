package dev.rpgprogression.client;

import dev.rpgprogression.RPGProgression;
import dev.rpgprogression.client.gui.CharacterScreen;
import dev.rpgprogression.inventory.InventorySlotRules;
import dev.rpgprogression.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=RPGProgression.MOD_ID,value=Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
        ClientData.applyPending();
        if(ClientData.get().exhausted&&!mc.player.isCreative()&&!mc.player.isSpectator())mc.player.setSprinting(false);
        while(ClientRegistration.CHARACTER.consumeClick()) {
            if(mc.screen==null) {ModNetworking.CHANNEL.sendToServer(new ModNetworking.RequestData());mc.setScreen(new CharacterScreen());}
            else if(mc.screen instanceof CharacterScreen)mc.setScreen(null);
        }
    }
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){ClientData.reset();}
    @SubscribeEvent public static void characterMenuHud(net.minecraftforge.client.event.RenderGuiEvent.Pre e){
        if(Minecraft.getInstance().screen instanceof CharacterScreen)e.setCanceled(true);
    }
    @SubscribeEvent public static void characterMenuHand(net.minecraftforge.client.event.RenderHandEvent e){
        if(Minecraft.getInstance().screen instanceof CharacterScreen)e.setCanceled(true);
    }
    @SubscribeEvent public static void locks(ContainerScreenEvent.Render.Foreground e) {
        var screen=e.getContainerScreen();var g=e.getGuiGraphics();
        for(var slot:screen.getMenu().slots)if(InventorySlotRules.locked(slot)) {
            int x=slot.x,y=slot.y;
            g.pose().pushPose();g.pose().translate(0,0,300);
            dev.rpgprogression.client.gui.GuiTextures.draw(g,"slot_locked",x,y,16,16,16,16);g.pose().popPose();
            int mx=e.getMouseX()-screen.getGuiLeft(),my=e.getMouseY()-screen.getGuiTop();
            if(mx>=x&&mx<x+16&&my>=y&&my<y+16) {
                int required=InventorySlotRules.thresholds()[slot.getContainerSlot()-9];
                g.renderTooltip(Minecraft.getInstance().font,Component.translatable("rpgprogression.inventory.requires",required),mx,my);
            }
        }
    }
}
