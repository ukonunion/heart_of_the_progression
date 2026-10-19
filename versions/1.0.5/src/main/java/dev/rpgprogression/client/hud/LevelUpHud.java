package dev.rpgprogression.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.rpgprogression.client.gui.GuiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Locale;

/** Shown only by the server's RPG LevelUp packet, never by a progress snapshot. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class LevelUpHud {
    private static final LevelUpAnimation ANIMATION=new LevelUpAnimation();
    public static void show(){if(Minecraft.getInstance().player!=null)ANIMATION.show();}
    public static void reset(){ANIMATION.reset();}
    public static boolean active(){return ANIMATION.active();}
    public static float opacity(float partial){return ANIMATION.opacity(partial);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.getConnection()==null){reset();return;}
        if(!mc.isPaused())ANIMATION.tick();
    }
    public static void render(ForgeGui gui,GuiGraphics g,float partial,int width,int height){
        var mc=Minecraft.getInstance();
        float opacity=ANIMATION.opacity(partial);
        if(opacity<=0||mc.player==null||!mc.player.isAlive()||mc.player.isSpectator()||mc.options.hideGui)return;
        gui.setupOverlayRenderState(true,false);
        int center=height/2;
        // A dark full-width band, feathered vertically so its edges disappear into the world.
        for(int row=-34;row<34;row++){
            float edge=Math.max(0,Math.min(1,(34-Math.abs(row))/14f));
            int alpha=Math.round(200*opacity*edge*edge*(3-2*edge));
            if(alpha>0)g.fill(0,center+row,width,center+row+1,alpha<<24);
        }
        boolean russian=mc.getLanguageManager().getSelected().toLowerCase(Locale.ROOT).startsWith("ru_");
        int sourceWidth=russian?254:116;
        float scale=Math.min(1.25f,Math.max(1,width-32)/(float)sourceWidth);
        int titleWidth=Math.round(sourceWidth*scale),titleHeight=Math.round(21*scale);
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,opacity);
        g.blit(GuiTextures.texture(russian?"level_up_ru":"level_up_en"),(width-titleWidth)/2,center-titleHeight/2,
                titleWidth,titleHeight,0,0,sourceWidth,21,sourceWidth,21);
        RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();
    }
    private LevelUpHud(){}
}
