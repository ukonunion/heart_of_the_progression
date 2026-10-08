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
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Set;

/** Supplied health, food and stamina frames; one effect skin and symmetric fill behind each PNG. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class CenteredHud {
    private static final Set<String> REPLACED=Set.of("player_health","food_level","armor_level","air_level","experience_bar","mount_health","jump_bar");
    private static final ResourceLocation ICONS=new ResourceLocation("minecraft","textures/gui/icons.png");
    private static Object lastPlayer;
    private static int xpTicks,previousXP,previousLevel;
    private static float previousProgress;
    public static boolean staminaVisible(){var d=ClientData.get();return RPGConfig.SHOW_HUD.get()&&d.initialized&&(d.exhausted||d.stamina<d.maxStamina()-0.01);}
    public static boolean experienceVisible(){return xpTicks>0;}
    public static HealthStyle healthStyle(){var p=Minecraft.getInstance().player;return p==null?HealthStyle.NORMAL:HealthStyle.select(p.hasEffect(MobEffects.WITHER),p.hasEffect(MobEffects.POISON),p.isFullyFrozen(),p.hasEffect(MobEffects.ABSORPTION)||p.getAbsorptionAmount()>0);}
    public static boolean hungerEffect(){var p=Minecraft.getInstance().player;return p!=null&&p.hasEffect(MobEffects.HUNGER);}
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
        boolean hungry=hungerEffect();
        TexturedMeters.draw(g,hungry?"hunger/hunger_effect":"hunger/hunger",width/2,height-bottom,TexturedMeters.FOOD_HEIGHT,p.getFoodData().getFoodLevel()/20.0,hungry?TexturedMeters.HUNGER_EFFECT:TexturedMeters.HUNGER);
        bottom+=12;
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
        if(p.getVehicle() instanceof LivingEntity mount&&mount.showVehicleHealth()){
            gui.rightHeight=bottom;g.pose().pushPose();g.pose().translate(right-(width/2+91),0,0);
            VanillaGuiOverlay.MOUNT_HEALTH.type().overlay().render(gui,g,partial,width,height);g.pose().popPose();bottom=gui.rightHeight+2;
        }
        var resources=ClientData.get();
        if(resources.spark<resources.maxSpark()-0.01){
            GuiTextures.bar(g,"spark",left,height-bottom+3,82,5,resources.spark/resources.maxSpark());
            g.drawCenteredString(mc.font,net.minecraft.network.chat.Component.translatable("rpgprogression.stat.spark_short").getString()+" "+(int)Math.floor(resources.spark),center,height-bottom-7,0xFFE1C58B);bottom+=21;
        }
        if(resources.manaActive&&resources.mana<resources.manaCapacity-0.01){
            GuiTextures.bar(g,"mana",left,height-bottom+3,82,5,resources.mana/Math.max(1,resources.manaCapacity));
            g.drawCenteredString(mc.font,net.minecraft.network.chat.Component.translatable("rpgprogression.stat.mana").getString()+" "+(int)Math.floor(resources.mana),center,height-bottom-7,0xFF80D5E8);bottom+=21;
        }
        if(experienceVisible()){
            GuiTextures.bar(g,"xp",left,height-bottom+3,82,5,p.experienceProgress);
            g.drawCenteredString(mc.font,"XP "+p.experienceLevel,center,height-bottom-7,0xFFAAE46D);bottom+=21;
        }
        if(p.getJumpRidingScale()>0){
            GuiTextures.bar(g,"stamina",left,height-bottom+3,82,5,p.getJumpRidingScale());bottom+=12;
        }
        return bottom;
    }
    private CenteredHud(){}
}
