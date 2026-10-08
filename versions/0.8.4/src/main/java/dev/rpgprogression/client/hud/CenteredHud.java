package dev.rpgprogression.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.gui.GuiTextures;
import dev.rpgprogression.compat.EpicFightCompat;
import dev.rpgprogression.config.RPGConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Set;

/** Centered player/animal health and stamina; auxiliary meters in the bottom-right corner. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class CenteredHud {
    private static final Set<String> REPLACED=Set.of("player_health","food_level","armor_level","air_level","mount_health","jump_bar","experience_bar");
    private static final ResourceLocation ICONS=new ResourceLocation("minecraft","textures/gui/icons.png");
    public static boolean staminaVisible(){var d=ClientData.get();return RPGConfig.SHOW_HUD.get()&&d.initialized&&(d.exhausted||d.stamina<d.maxStamina()-0.01);}
    public static HealthStyle healthStyle(){var p=Minecraft.getInstance().player;return p==null?HealthStyle.NORMAL:HealthStyle.select(p.hasEffect(MobEffects.WITHER),p.hasEffect(MobEffects.POISON),p.isFullyFrozen(),p.hasEffect(MobEffects.ABSORPTION)||p.getAbsorptionAmount()>0);}
    public static boolean hungerEffect(){var p=Minecraft.getInstance().player;return p!=null&&p.hasEffect(MobEffects.HUNGER);}
    public static LivingEntity riddenAnimal(){var p=Minecraft.getInstance().player;return p!=null&&p.getVehicle() instanceof LivingEntity mount&&(mount instanceof Animal||mount.showVehicleHealth())?mount:null;}
    @SubscribeEvent public static void original(RenderGuiOverlayEvent.Pre e){
        ResourceLocation id=e.getOverlay().id();
        if(id.getNamespace().equals("minecraft")&&REPLACED.contains(id.getPath()))e.setCanceled(true);
        if(id.getNamespace().equals("epicfight")&&id.getPath().equals("stamina_bar")&&EpicFightCompat.enabled())e.setCanceled(true);
    }
    public static void render(ForgeGui gui,GuiGraphics g,float partial,int width,int height){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.options.hideGui||p.isSpectator())return;
        gui.setupOverlayRenderState(true,false);
        if(RPGConfig.SHOW_HUD.get()&&dev.rpgprogression.compat.IronsCompat.enabled())IronsManaHud.render(g);
        if(!gui.shouldDrawSurvivalElements())return;
        // Nine-pixel HP frame ends at the hotbar's top edge, centered over the old XP strip.
        int bottom=Math.min(height-12,Math.max(31,RPGConfig.HUD_BOTTOM.get()));
        HealthStyle health=healthStyle();
        var absorption=p.getEffect(MobEffects.ABSORPTION);
        double shieldMaximum=Math.max(p.getAbsorptionAmount(),absorption==null?0:4.0*(absorption.getAmplifier()+1));
        double hp=(p.getHealth()+p.getAbsorptionAmount())/Math.max(1,p.getMaxHealth()+shieldMaximum);
        TexturedMeters.draw(g,health.texture(p.level().getLevelData().isHardcore()),width/2,height-bottom,9,hp,health.color);
        bottom+=12;
        if(staminaVisible()){
            var d=ClientData.get();
            TexturedMeters.draw(g,"stamina/base_stamina",width/2,height-bottom,TexturedMeters.STAMINA_HEIGHT,d.stamina/d.maxStamina(),TexturedMeters.STAMINA);
            bottom+=13;
        }
        LivingEntity mount=riddenAnimal();
        if(mount!=null){
            // Keep the existing stack order; lower only the animal frame by three GUI pixels.
            TexturedMeters.draw(g,"hp/survival/anim_health",width/2,height-bottom+3,TexturedMeters.HEALTH_HEIGHT,mount.getHealth()/Math.max(1,mount.getMaxHealth()),TexturedMeters.ANIMAL_HEALTH);
            bottom+=12;
        }
        int cornerBottom=renderCorner(gui,g,partial,width,height);
        gui.leftHeight=bottom;gui.rightHeight=Math.max(bottom,cornerBottom);
    }
    private static int renderCorner(ForgeGui gui,GuiGraphics g,float partial,int width,int height){
        var mc=Minecraft.getInstance();var p=mc.player;
        int right=width-8,left=right-82,center=right-41;
        // On narrow windows the corner column sits above the hotbar instead of covering its slots.
        int bottom=left<=width/2+97?39:17;
        if(p.getArmorValue()>0){
            gui.leftHeight=bottom;g.pose().pushPose();g.pose().translate(right-81-(width/2-91),0,0);
            VanillaGuiOverlay.ARMOR_LEVEL.type().overlay().render(gui,g,partial,width,height);g.pose().popPose();
            bottom=gui.leftHeight+2;
        }
        if(p.getAirSupply()<p.getMaxAirSupply()){
            int bubbles=Math.max(0,Math.min(10,(int)Math.ceil(p.getAirSupply()*10.0/Math.max(1,p.getMaxAirSupply()))));
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            for(int i=0;i<10;i++)g.blit(ICONS,right-81+i*8,height-bottom,i<bubbles?16:25,18,9,9);
            RenderSystem.disableBlend();bottom+=12;
        }
        if(p.getJumpRidingScale()>0){
            GuiTextures.bar(g,"stamina",left,height-bottom+3,82,5,p.getJumpRidingScale());bottom+=12;
        }
        return bottom;
    }
    private CenteredHud(){}
}
