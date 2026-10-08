package dev.rpgprogression.client.hud;

import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.gui.GuiTextures;
import dev.rpgprogression.compat.EpicFightCompat;
import dev.rpgprogression.config.RPGConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Set;

/** Reuses vanilla hearts/food/armor, including potion effects, absorption and resource packs. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class CenteredHud {
    private static final Set<String> REPLACED=Set.of("player_health","food_level","armor_level","air_level","experience_bar","mount_health","jump_bar");
    private static final ResourceLocation ICONS=new ResourceLocation("minecraft","textures/gui/icons.png");
    private static Object lastPlayer;
    private static int xpTicks,previousXP,previousLevel;
    private static float previousProgress;
    public static boolean staminaVisible(){var d=ClientData.get();return RPGConfig.SHOW_HUD.get()&&d.initialized&&(d.exhausted||d.stamina<d.maxStamina()-0.01);}
    public static boolean experienceVisible(){return xpTicks>0;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        var p=Minecraft.getInstance().player;
        if(p!=lastPlayer){lastPlayer=p;xpTicks=0;if(p!=null){previousXP=p.totalExperience;previousLevel=p.experienceLevel;previousProgress=p.experienceProgress;}return;}
        if(p==null)return;
        if(xpTicks>0)xpTicks--;
        if(previousXP!=p.totalExperience||previousLevel!=p.experienceLevel||previousProgress!=p.experienceProgress){
            xpTicks=80;previousXP=p.totalExperience;previousLevel=p.experienceLevel;previousProgress=p.experienceProgress;
        }
    }
    @SubscribeEvent public static void original(RenderGuiOverlayEvent.Pre e){
        ResourceLocation id=e.getOverlay().id();
        if(id.getNamespace().equals("minecraft")&&REPLACED.contains(id.getPath()))e.setCanceled(true);
        if(id.getNamespace().equals("epicfight")&&id.getPath().equals("stamina_bar")&&EpicFightCompat.enabled())e.setCanceled(true);
    }
    public static void render(ForgeGui gui,GuiGraphics g,float partial,int width,int height){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.options.hideGui||p.isSpectator()||!gui.shouldDrawSurvivalElements())return;
        gui.setupOverlayRenderState(true,false);
        int bottom=Math.min(height-12,RPGConfig.HUD_BOTTOM.get());
        gui.rightHeight=bottom;
        g.pose().pushPose();g.pose().translate(-50,0,0);gui.renderFood(width,height,g);g.pose().popPose();
        bottom=gui.rightHeight+2;
        gui.leftHeight=bottom;
        g.pose().pushPose();g.pose().translate(51,0,0);gui.renderHealth(width,height,g);g.pose().popPose();
        bottom=gui.leftHeight+2;
        if(p.getArmorValue()>0){
            gui.leftHeight=bottom;g.pose().pushPose();g.pose().translate(51,0,0);
            VanillaGuiOverlay.ARMOR_LEVEL.type().overlay().render(gui,g,partial,width,height);g.pose().popPose();
            bottom=gui.leftHeight+2;
        }
        if(p.getAirSupply()<p.getMaxAirSupply()){
            int bubbles=Math.max(0,Math.min(10,(int)Math.ceil(p.getAirSupply()*10.0/Math.max(1,p.getMaxAirSupply()))));
            for(int i=0;i<10;i++)g.blit(ICONS,width/2-40+i*8,height-bottom,i<bubbles?16:25,18,9,9);
            bottom+=12;
        }
        if(p.getVehicle() instanceof LivingEntity mount&&mount.showVehicleHealth()){
            gui.rightHeight=bottom;g.pose().pushPose();g.pose().translate(-50,0,0);
            VanillaGuiOverlay.MOUNT_HEALTH.type().overlay().render(gui,g,partial,width,height);g.pose().popPose();bottom=gui.rightHeight+2;
        }
        if(staminaVisible()){
            var d=ClientData.get();
            GuiTextures.bar(g,d.exhausted?"exhausted":"stamina",width/2-41,height-bottom+3,82,5,d.stamina/d.maxStamina());
            g.drawCenteredString(mc.font,net.minecraft.network.chat.Component.translatable("rpgprogression.stat.stamina").getString()+" "+(int)Math.ceil(d.stamina),width/2,height-bottom-7,0xFFE8DCBD);
            bottom+=21;
        }
        if(experienceVisible()){
            GuiTextures.bar(g,"xp",width/2-41,height-bottom+3,82,5,p.experienceProgress);
            g.drawCenteredString(mc.font,"XP "+p.experienceLevel,width/2,height-bottom-7,0xFFAAE46D);bottom+=21;
        }
        if(p.getJumpRidingScale()>0){
            GuiTextures.bar(g,"stamina",width/2-41,height-bottom+3,82,5,p.getJumpRidingScale());bottom+=12;
        }
        gui.leftHeight=gui.rightHeight=bottom;
    }
    private CenteredHud(){}
}
