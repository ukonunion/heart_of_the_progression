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
    @SubscribeEvent public static void replaceParCoolTree(net.minecraftforge.client.event.ScreenEvent.Opening e){
        if(e.getNewScreen()!=null&&e.getNewScreen().getClass().getName().equals("com.alrex.parcool.client.gui.screen.ParCoolSkillTreeScreen")
                &&dev.rpgprogression.skill.SkillTreeManager.parcool())e.setNewScreen(new dev.rpgprogression.client.gui.SkillTreeScreen(new CharacterScreen(),1));
    }
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
        if(Minecraft.getInstance().screen instanceof CharacterScreen||Minecraft.getInstance().screen instanceof dev.rpgprogression.client.gui.SkillTreeScreen)e.setCanceled(true);
    }
    @SubscribeEvent public static void characterMenuHand(net.minecraftforge.client.event.RenderHandEvent e){
        if(Minecraft.getInstance().screen instanceof CharacterScreen||Minecraft.getInstance().screen instanceof dev.rpgprogression.client.gui.SkillTreeScreen)e.setCanceled(true);
    }
    @SubscribeEvent public static void locks(ContainerScreenEvent.Render.Foreground e) {
        var screen=e.getContainerScreen();var g=e.getGuiGraphics();
        for(var slot:screen.getMenu().slots)if(InventorySlotRules.locked(slot)) {
            int x=slot.x,y=slot.y;
            g.pose().pushPose();g.pose().translate(0,0,300);
            g.fill(x,y,x+16,y+16,0xB0181611);
            g.renderOutline(x+5,y+3,6,6,0xFFAB9262);g.fill(x+4,y+7,x+12,y+13,0xFFAB9262);
            g.fill(x+7,y+9,x+9,y+12,0xFF29251D);g.pose().popPose();
            int mx=e.getMouseX()-screen.getGuiLeft(),my=e.getMouseY()-screen.getGuiTop();
            if(mx>=x&&mx<x+16&&my>=y&&my<y+16) {
                int required=InventorySlotRules.thresholds()[slot.getContainerSlot()-9];
                g.renderTooltip(Minecraft.getInstance().font,Component.translatable("rpgprogression.inventory.requires",required),mx,my);
            }
        }
    }
}
