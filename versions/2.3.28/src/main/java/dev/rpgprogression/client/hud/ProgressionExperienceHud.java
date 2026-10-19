package dev.rpgprogression.client.hud;

import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.gui.GuiTextures;
import dev.rpgprogression.config.RPGConfig;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Supplied gold-star frame with only the current RPG experience amount. */
@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class ProgressionExperienceHud {
    public static final int WIDTH=70,HEIGHT=19;
    private static final ExperienceAmountChangeTracker CHANGES=new ExperienceAmountChangeTracker();
    private static final HudVisibilityAnimation VISIBILITY=new HudVisibilityAnimation();
    public static void reset(){CHANGES.reset();VISIBILITY.reset();}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.getConnection()==null||!ClientData.get().initialized||!RPGConfig.SHOW_EXPERIENCE_HUD.get()){reset();return;}
        if(mc.isPaused())return;
        CHANGES.update(mc.player,ClientData.get().xp);
        VISIBILITY.tick(CHANGES.visible()&&!mc.player.isSpectator());
    }
    public static int x(int width){return Math.max(0,width-WIDTH);}
    public static int y(int height){return Math.max(0,Math.min(height-HEIGHT,height*3/4-HEIGHT/2));}
    public static boolean visible(){
        var mc=Minecraft.getInstance();return mc.player!=null&&mc.level!=null&&!mc.options.hideGui&&!mc.player.isSpectator()&&RPGConfig.SHOW_EXPERIENCE_HUD.get()&&ClientData.get().initialized&&VISIBILITY.active();
    }
    public static float opacity(float partial){return VISIBILITY.value(partial);}
    public static String amount(){return Long.toString(Math.max(0,ClientData.get().xp));}
    public static void render(GuiGraphics g,DeltaTracker tracker){
        if(!visible())return;
        float opacity=opacity(tracker.getGameTimeDeltaPartialTick(false));if(opacity<=0)return;
        var font=Minecraft.getInstance().font;int x=x(g.guiWidth()),y=y(g.guiHeight());
        GuiTextures.drawAlpha(g,"amount_info",x,y,WIDTH,HEIGHT,WIDTH,HEIGHT,opacity);
        String amount=amount();float scale=Math.min(1,50f/Math.max(1,font.width(amount)));
        g.pose().pushPose();g.pose().translate(x+16,y+(HEIGHT-font.lineHeight*scale)/2f,0);g.pose().scale(scale,scale,1);
        int alpha=Math.round(255*opacity);if(alpha>3)g.drawString(font,amount,0,0,alpha<<24|0xFFFFFF,false);g.pose().popPose();
    }
    private ProgressionExperienceHud(){}
}
