package dev.rpgprogression.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.rpgprogression.client.gui.GuiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.Locale;

/** Shown only by the server's RPG LevelUp packet, never by a progress snapshot. */
@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class LevelUpHud {
    private static final LevelUpAnimation ANIMATION=new LevelUpAnimation();
    public static void show(){if(Minecraft.getInstance().player!=null)ANIMATION.show();}
    public static void reset(){ANIMATION.reset();}
    public static boolean active(){return ANIMATION.active();}
    public static float opacity(float partial){return ANIMATION.opacity(partial);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.getConnection()==null){reset();return;}
        if(!mc.isPaused())ANIMATION.tick();
    }
    public static void render(GuiGraphics g,DeltaTracker tracker){
        renderBanner(g,ANIMATION.opacity(tracker.getGameTimeDeltaPartialTick(false)),"level_up",254,116);
    }
    /** Both milestone banners share the same layout, band, scaling and fade animation. */
    public static void renderBanner(GuiGraphics g,float opacity,String texture,int russianWidth,int englishWidth){
        int width=g.guiWidth(),height=g.guiHeight();
        var mc=Minecraft.getInstance();
        if(opacity<=0||mc.player==null||!mc.player.isAlive()||mc.player.isSpectator()||mc.options.hideGui)return;
        int center=height/2;
        // A dark full-width band, feathered vertically so its edges disappear into the world.
        for(int row=-34;row<34;row++){
            float edge=Math.max(0,Math.min(1,(34-Math.abs(row))/14f));
            int alpha=Math.round(200*opacity*edge*edge*(3-2*edge));
            if(alpha>0)g.fill(0,center+row,width,center+row+1,alpha<<24);
        }
        boolean russian=mc.getLanguageManager().getSelected().toLowerCase(Locale.ROOT).startsWith("ru_");
        int sourceWidth=russian?russianWidth:englishWidth;
        float scale=Math.min(1.25f,Math.max(1,width-32)/(float)sourceWidth);
        int titleWidth=Math.round(sourceWidth*scale),titleHeight=Math.round(21*scale);
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,opacity);
        g.blit(GuiTextures.texture(texture+(russian?"_ru":"_en")),(width-titleWidth)/2,center-titleHeight/2,
                titleWidth,titleHeight,0,0,sourceWidth,21,sourceWidth,21);
        g.flush();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();
    }
    private LevelUpHud(){}
}
