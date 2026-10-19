package dev.rpgprogression.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rpgprogression.RPGProgression;
import dev.rpgprogression.client.hud.CenteredHud;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=RPGProgression.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientRegistration {
    public static final KeyMapping CHARACTER=new KeyMapping("rpgprogression.key.character",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_J,"rpgprogression.key.category");
    private static boolean viewKeyChecked;
    /** The old default K overlaps both Epic Fight's skill screen and Iris' shader toggle. */
    public static void migrateViewKey(){
        if(viewKeyChecked)return;viewKeyChecked=true;
        var mc=net.minecraft.client.Minecraft.getInstance();var free=InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_J);
        if(CHARACTER.getKey().getValue()==GLFW.GLFW_KEY_K
                &&java.util.Arrays.stream(mc.options.keyMappings).anyMatch(k->k!=CHARACTER&&k.getKey().equals(CHARACTER.getKey()))
                &&java.util.Arrays.stream(mc.options.keyMappings).noneMatch(k->k!=CHARACTER&&k.getKey().equals(free))){
            CHARACTER.setKey(free);KeyMapping.resetMapping();mc.options.save();
        }
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(dev.rpgprogression.skint.SkintSeating.SEAT.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);}
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) {e.register(CHARACTER);}
    @SubscribeEvent public static void hud(RegisterGuiLayersEvent e) {
        // The vanilla layer otherwise hides XP while riding a jumpable animal.
        e.replaceLayer(VanillaGuiLayers.EXPERIENCE_BAR,CenteredHud::renderNativeExperience);
        e.wrapLayer(VanillaGuiLayers.EXPERIENCE_LEVEL,nativeLayer->(graphics,delta)->CenteredHud.renderFadedExperience(graphics,delta,nativeLayer));
        e.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR,ResourceLocation.fromNamespaceAndPath(RPGProgression.MOD_ID,"status"),CenteredHud::render);
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(RPGProgression.MOD_ID,"level_up"),dev.rpgprogression.client.hud.LevelUpHud::render);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent e) {e.enqueueWork(()->{ClientData.install();ClientSkintData.install();});}
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent e) {e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->dev.rpgprogression.client.hud.ManaIconMask.invalidate());}
}
