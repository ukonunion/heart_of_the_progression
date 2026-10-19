package dev.rpgprogression.client;

import dev.rpgprogression.RPGProgression;
import dev.rpgprogression.client.gui.CharacterScreen;
import dev.rpgprogression.client.gui.GuiTextures;
import dev.rpgprogression.inventory.InventorySlotRules;
import dev.rpgprogression.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=RPGProgression.MOD_ID,value=Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void replaceParCoolTree(net.neoforged.neoforge.client.event.ScreenEvent.Opening e){
        if(e.getNewScreen()!=null&&e.getNewScreen().getClass().getName().equals("com.alrex.parcool.client.gui.screen.ParCoolSkillTreeScreen")
                &&dev.rpgprogression.skill.SkillTreeManager.parcool())e.setNewScreen(new dev.rpgprogression.client.gui.SkillTreeScreen(new CharacterScreen()));
        if(ClientSkintData.managed(e.getNewScreen())&&!ClientSkintData.allowed())e.setNewScreen(null);
        if(ClientSkintData.active()&&ClientSkintData.managed(e.getCurrentScreen())&&!ClientSkintData.managed(e.getNewScreen()))ClientSkintData.close();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        Minecraft mc=Minecraft.getInstance();ClientRegistration.migrateViewKey();ClientSkintData.tick();if(ClientSkintData.active()&&!ClientSkintData.managed(mc.screen))ClientSkintData.close();if(mc.player==null)return;
        ClientData.applyPending();
        if(mc.player.getVehicle() instanceof dev.rpgprogression.skint.SkintSeat)mc.gui.setOverlayMessage(Component.empty(),false);
        if(dev.rpgprogression.compat.ParCoolCompat.enabled())dev.rpgprogression.compat.ParCoolBridge.mirror(mc.player);
        if(ClientData.get().exhausted&&!mc.player.isCreative()&&!mc.player.isSpectator())mc.player.setSprinting(false);
        while(ClientRegistration.CHARACTER.consumeClick()) {
            if(mc.screen==null)mc.setScreen(new CharacterScreen());
            else if(mc.screen instanceof CharacterScreen||mc.screen instanceof dev.rpgprogression.client.gui.SkillTreeScreen)mc.screen.onClose();
        }
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){ClientData.reset();SkintPreviews.clear();}
    @SubscribeEvent public static void characterMenuHud(net.neoforged.neoforge.client.event.RenderGuiEvent.Pre e){
        if(ClientSkintData.managed(Minecraft.getInstance().screen)){SkintPreviews.capture();e.setCanceled(true);}
        else if(SkintCamera.returning())e.setCanceled(true);
    }
    @SubscribeEvent public static void characterMenuHand(net.neoforged.neoforge.client.event.RenderHandEvent e){
        if(ClientSkintData.managed(Minecraft.getInstance().screen)||SkintCamera.returning())e.setCanceled(true);
    }
    @SubscribeEvent public static void skintHighlight(net.neoforged.neoforge.client.event.RenderHighlightEvent.Block e){if(SkintCamera.active())e.setCanceled(true);}
    @SubscribeEvent public static void locks(ContainerScreenEvent.Render.Foreground e) {
        var screen=e.getContainerScreen();var g=e.getGuiGraphics();
        for(var slot:screen.getMenu().slots)if(InventorySlotRules.locked(slot)) {
            int x=slot.x,y=slot.y;
            g.pose().pushPose();g.pose().translate(0,0,300);
            GuiTextures.draw(g,"locked",x,y,16,16,16,16);
            g.pose().popPose();
            int mx=e.getMouseX()-screen.getGuiLeft(),my=e.getMouseY()-screen.getGuiTop();
            if(mx>=x&&mx<x+16&&my>=y&&my<y+16) {
                int required=InventorySlotRules.thresholds()[slot.getContainerSlot()-9];
                g.renderTooltip(Minecraft.getInstance().font,Component.translatable("rpgprogression.inventory.requires",required),mx,my);
            }
        }
    }
}
