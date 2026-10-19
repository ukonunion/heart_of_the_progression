package dev.rpgprogression.client.hud;

import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.gui.GuiTextures;
import dev.rpgprogression.config.RPGConfig;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;

/** Supplied gold-star frame with only the current RPG experience amount. */
public final class ProgressionExperienceHud {
    public static final int WIDTH=70,HEIGHT=19;
    public static int x(int width){return Math.max(0,width-WIDTH);}
    public static int y(int height){return Math.max(0,Math.min(height-HEIGHT,height*3/4-HEIGHT/2));}
    public static boolean visible(){
        var mc=Minecraft.getInstance();return mc.player!=null&&mc.level!=null&&!mc.options.hideGui&&!mc.player.isSpectator()&&RPGConfig.SHOW_HUD.get()&&ClientData.get().initialized;
    }
    public static String amount(){return ProgressionExperienceFormat.format(ClientData.get().xp,Minecraft.getInstance().options.languageCode.startsWith("ru"));}
    public static void render(GuiGraphics g,DeltaTracker tracker){
        if(!visible())return;
        var font=Minecraft.getInstance().font;int x=x(g.guiWidth()),y=y(g.guiHeight());
        GuiTextures.draw(g,"amount_info",x,y,WIDTH,HEIGHT,WIDTH,HEIGHT);
        String amount=amount();float scale=Math.min(1,50f/Math.max(1,font.width(amount)));
        g.pose().pushPose();g.pose().translate(x+16,y+(HEIGHT-font.lineHeight*scale)/2f,0);g.pose().scale(scale,scale,1);
        g.drawString(font,amount,0,0,0xFFFFFFFF,false);g.pose().popPose();
    }
    private ProgressionExperienceHud(){}
}
