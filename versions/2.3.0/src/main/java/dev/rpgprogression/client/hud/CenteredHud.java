package dev.rpgprogression.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.compat.EpicFightCompat;
import dev.rpgprogression.config.RPGConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.Set;

/** Centered survival meters and a temporary native XP overlay after vanilla XP changes. */
@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class CenteredHud {
    private static final Set<String> REPLACED=Set.of("player_health","food_level","armor_level","air_level","vehicle_health","jump_meter");
    private static final ExperienceChangeTracker EXPERIENCE=new ExperienceChangeTracker();
    private static final HudVisibilityAnimation HEALTH=new HudVisibilityAnimation(),STAMINA=new HudVisibilityAnimation(),ANIMAL=new HudVisibilityAnimation(),AIR=new HudVisibilityAnimation(),XP=new HudVisibilityAnimation(),MANA=new HudVisibilityAnimation();
    private static Object session;
    private static double lastAnimalHealth;
    private static void reset(){EXPERIENCE.reset();for(var animation:new HudVisibilityAnimation[]{HEALTH,STAMINA,ANIMAL,AIR,XP,MANA})animation.reset();lastAnimalHealth=0;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.getConnection()==null){reset();session=null;return;}
        if(session!=mc.getConnection()){reset();session=mc.getConnection();}
        if(mc.isPaused())return;
        EXPERIENCE.update(session,p.totalExperience,p.experienceLevel,p.experienceProgress);
        boolean survival=mc.gameMode!=null&&mc.gameMode.canHurtPlayer()&&!p.isSpectator();
        HEALTH.tick(survival);STAMINA.tick(survival&&staminaVisible());ANIMAL.tick(survival&&riddenAnimal()!=null);AIR.tick(survival&&oxygenVisible());XP.tick(survival&&experienceVisible());
        MANA.tick(!p.isSpectator()&&RPGConfig.SHOW_HUD.get()&&dev.rpgprogression.compat.IronsCompat.enabled()&&ClientData.get().manaActive&&IronsManaHud.visible(p));
    }
    public static boolean experienceVisible(){return EXPERIENCE.visible();}
    public static boolean experienceRendered(){return XP.active();}
    public static float experienceOpacity(){return XP.value(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));}
    public static boolean staminaVisible(){var d=ClientData.get();return RPGConfig.SHOW_HUD.get()&&d.initialized&&(d.exhausted||d.stamina<d.maxStamina()-0.01);}
    public static HealthStyle healthStyle(){var p=Minecraft.getInstance().player;return p==null?HealthStyle.NORMAL:HealthStyle.select(p.hasEffect(MobEffects.WITHER),p.hasEffect(MobEffects.POISON),p.isFullyFrozen(),p.hasEffect(MobEffects.ABSORPTION)||p.getAbsorptionAmount()>0);}
    public static boolean oxygenVisible(){var p=Minecraft.getInstance().player;return p!=null&&p.getAirSupply()<p.getMaxAirSupply();}
    public static LivingEntity riddenAnimal(){var p=Minecraft.getInstance().player;return p!=null&&p.getVehicle() instanceof LivingEntity mount&&(mount instanceof Animal||mount.showVehicleHealth())?mount:null;}
    private static boolean experienceLayer(ResourceLocation id){return id.equals(VanillaGuiLayers.EXPERIENCE_BAR)||id.equals(VanillaGuiLayers.EXPERIENCE_LEVEL);}
    @SubscribeEvent public static void original(RenderGuiLayerEvent.Pre e){
        ResourceLocation id=e.getName();
        if(id.getNamespace().equals("minecraft")&&REPLACED.contains(id.getPath()))e.setCanceled(true);
        if(experienceLayer(id)&&!experienceRendered())e.setCanceled(true);
        if(id.getNamespace().equals("epicfight")&&id.getPath().equals("stamina_bar")&&EpicFightCompat.enabled())e.setCanceled(true);
    }
    public static void renderFadedExperience(GuiGraphics g,DeltaTracker tracker,LayeredDraw.Layer nativeLayer){
        float opacity=XP.value(tracker.getGameTimeDeltaPartialTick(false));if(opacity<=0)return;
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,opacity);
        try{nativeLayer.render(g,tracker);}finally{g.flush();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();}
    }
    public static void renderNativeExperience(GuiGraphics g,DeltaTracker tracker){
        var mc=Minecraft.getInstance();
        if(mc.player!=null&&!mc.player.isSpectator()&&mc.gameMode!=null&&mc.gameMode.hasExperience())
            renderFadedExperience(g,tracker,(graphics,delta)->((dev.rpgprogression.mixin.client.ExperienceHudMixin)mc.gui).rpg$renderExperienceBar(graphics,graphics.guiWidth()/2-91));
    }
    public static void render(GuiGraphics g,DeltaTracker tracker){
        float partial=tracker.getGameTimeDeltaPartialTick(false);int width=g.guiWidth(),height=g.guiHeight();
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.options.hideGui||p.isSpectator())return;
        if(dev.rpgprogression.compat.IronsCompat.present()&&MANA.active())IronsManaHud.render(g,MANA.value(partial));
        if(mc.gameMode==null||!mc.gameMode.canHurtPlayer())return;
        boolean xp=experienceRendered()&&mc.gameMode!=null&&mc.gameMode.hasExperience();
        // Make room for native XP and its level text only during the temporary display window.
        float bottom=Math.min(height-12,Math.max(31,RPGConfig.HUD_BOTTOM.get())+(xp?16*XP.value(partial):0));
        HealthStyle health=healthStyle();
        var absorption=p.getEffect(MobEffects.ABSORPTION);
        double shieldMaximum=Math.max(p.getAbsorptionAmount(),absorption==null?0:4.0*(absorption.getAmplifier()+1));
        double hp=(p.getHealth()+p.getAbsorptionAmount())/Math.max(1,p.getMaxHealth()+shieldMaximum);
        meter(g,health.texture(p.level().getLevelData().isHardcore()),width/2,height-bottom,9,hp,health.color,HEALTH.value(partial));
        bottom+=12;
        if(STAMINA.active()){
            var d=ClientData.get();
            meter(g,"stamina/base_stamina",width/2,height-bottom,TexturedMeters.STAMINA_HEIGHT,d.stamina/d.maxStamina(),TexturedMeters.STAMINA,STAMINA.value(partial));
        }
        bottom+=13*STAMINA.value(partial);
        LivingEntity mount=riddenAnimal();
        if(mount!=null)lastAnimalHealth=mount.getHealth()/Math.max(1,mount.getMaxHealth());
        if(ANIMAL.active()){
            // Keep the existing stack order; lower only the animal frame by three GUI pixels.
            meter(g,"hp/survival/anim_health",width/2,height-bottom+3,TexturedMeters.HEALTH_HEIGHT,lastAnimalHealth,TexturedMeters.ANIMAL_HEALTH,ANIMAL.value(partial));
        }
        bottom+=12*ANIMAL.value(partial);
        if(AIR.active()){
            meter(g,"hp/survival/oxygen",width/2,height-bottom+4,TexturedMeters.HEALTH_HEIGHT,Math.max(0,p.getAirSupply())/(double)Math.max(1,p.getMaxAirSupply()),TexturedMeters.OXYGEN,AIR.value(partial));
        }
        bottom+=12*AIR.value(partial);
        mc.gui.leftHeight=(int)Math.ceil(bottom);mc.gui.rightHeight=mc.gui.leftHeight;
    }
    private static void meter(GuiGraphics g,String texture,int center,float y,int height,double ratio,int color,float opacity){g.pose().pushPose();g.pose().translate(0,y,0);TexturedMeters.draw(g,texture,center,0,height,ratio,color,opacity);g.pose().popPose();}
    private CenteredHud(){}
}
