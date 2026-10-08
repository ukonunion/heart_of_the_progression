package dev.rpgprogression.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rpgprogression.RPGProgression;
import dev.rpgprogression.client.hud.StaminaHud;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=RPGProgression.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientRegistration {
    public static final KeyMapping CHARACTER=new KeyMapping("rpgprogression.key.character",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_K,"rpgprogression.key.category");
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) {e.register(CHARACTER);}
    @SubscribeEvent public static void hud(RegisterGuiOverlaysEvent e) {e.registerAboveAll("stamina",StaminaHud::render);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent e) {e.enqueueWork(ClientData::install);}
}
