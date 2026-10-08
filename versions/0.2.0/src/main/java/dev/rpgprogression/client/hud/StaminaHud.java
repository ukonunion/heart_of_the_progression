package dev.rpgprogression.client.hud;

import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.config.RPGConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public final class StaminaHud {
    public static void render(ForgeGui gui,GuiGraphics g,float partialTick,int width,int height) {
        var mc=Minecraft.getInstance();
        if(!RPGConfig.SHOW_HUD.get()||mc.options.hideGui||mc.player==null||mc.player.isSpectator())return;
        var d=ClientData.get(); if(!d.initialized)return;
        int w=64,x=width/2+RPGConfig.HUD_X.get(),y=height-RPGConfig.HUD_Y.get();
        // At narrow GUI scales use the free area above the status stack, away from hotbar and XP.
        if(x+w>width-3||x<3){x=width/2-32;y=height-70;}
        y=Math.max(12,Math.min(height-8,y));
        dev.rpgprogression.client.gui.GuiTextures.bar(g,d.exhausted?"exhausted":"stamina",x,y,w,5,d.stamina/d.maxStamina());
        String amount=Integer.toString((int)Math.ceil(d.stamina));
        g.drawString(mc.font,amount,x+(w-mc.font.width(amount))/2,y-10,0xFFE8DCBD,true);
    }
    private StaminaHud() {}
}
