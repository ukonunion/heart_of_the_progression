package dev.rpgprogression.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Screen-local confirmation of an actual saved life, with no optional Clavis class linkage. */
@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class ClavisAgilityFeedback {
    private static final int DURATION=38;
    private static Screen owner;
    private static AbstractWidget widget;
    private static int age;

    public static void show(Screen screen) {
        if(Minecraft.getInstance().screen!=screen)return;
        var game=screen.children().stream().filter(c->c instanceof AbstractWidget&&isMinigame(c.getClass()))
                .map(c->(AbstractWidget)c).findFirst().orElse(null);
        if(game==null)return;
        owner=screen;widget=game;age=0;
    }
    private static boolean isMinigame(Class<?> type) {
        for(;type!=null;type=type.getSuperclass())
            if(type.getName().equals("it.hurts.shatterbyte.clavis.common.client.screen.widget.AbstractMinigameWidget"))return true;
        return false;
    }
    public static boolean active(Screen screen){return owner==screen&&owner!=null&&age<DURATION;}
    public static void clear(Screen screen){if(owner==screen)reset();}
    public static void reset(){owner=null;widget=null;age=0;}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(owner==null)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.screen!=owner||++age>=DURATION)reset();
    }
    @SubscribeEvent public static void close(ScreenEvent.Closing event){clear(event.getScreen());}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){reset();}
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if(!active(event.getScreen())||widget==null)return;
        var g=event.getGuiGraphics();var mc=Minecraft.getInstance();float t=age+event.getPartialTick();
        float alpha=Mth.clamp((t+1)/3,0,1)*Mth.clamp((DURATION-t)/12,0,1);
        float flash=Mth.clamp(1-t/10,0,1);
        int cx=widget.getX()+widget.getWidth()/2,heartTop=widget.getY()+widget.getHeight()+8;
        g.pose().pushPose();g.pose().translate(0,0,400);
        if(flash>0){
            int grow=Math.round((1-flash)*5),edge=color(0xF7D989,flash*.8f);
            border(g,cx-83-grow,heartTop-3-grow,cx+83+grow,heartTop+35+grow,edge);
            for(int i=0;i<8;i++){
                double angle=i*Math.PI/4;int x=cx+(int)Math.round(Math.cos(angle)*(76+(1-flash)*17));
                int y=heartTop+16+(int)Math.round(Math.sin(angle)*(22+(1-flash)*10));
                int tint=color(i%2==0?0xF7D989:0xA1F4C1,flash);
                g.fill(x-2,y,x+3,y+1,tint);g.fill(x,y-2,x+1,y+3,tint);
            }
        }
        var title=Component.translatable("rpgprogression.compat.clavis.protected.title");
        var detail=Component.translatable("rpgprogression.compat.clavis.protected.detail");
        int textWidth=Math.max(mc.font.width(title),mc.font.width(detail));
        var placement=placement(owner,widget,textWidth);
        int x=placement.x(),y=placement.y(),width=placement.width(),height=placement.height();
        float textScale=placement.textScale();
        g.fill(x+1,y+1,x+width-1,y+height-1,color(0x10251F,alpha*.92f));
        border(g,x+1,y,x+width-1,y+height,color(0xCBAE68,alpha));
        g.fill(x,y+2,x+1,y+height-2,color(0xCBAE68,alpha));
        g.fill(x+width-1,y+2,x+width,y+height-2,color(0xCBAE68,alpha));
        // A small pixel shield and check mark keep the confirmation readable without relying on colour.
        int sx=x+6,sy=y+8;
        g.fill(sx,sy,sx+11,sy+9,color(0x2F6854,alpha));
        g.fill(sx+2,sy+9,sx+9,sy+11,color(0x2F6854,alpha));
        g.fill(sx+4,sy+11,sx+7,sy+13,color(0x2F6854,alpha));
        g.fill(sx+2,sy+4,sx+4,sy+6,color(0xB7FFD0,alpha));
        g.fill(sx+4,sy+6,sx+6,sy+8,color(0xB7FFD0,alpha));
        g.fill(sx+6,sy+4,sx+8,sy+6,color(0xB7FFD0,alpha));
        g.fill(sx+8,sy+2,sx+10,sy+4,color(0xB7FFD0,alpha));
        g.pose().pushPose();g.pose().translate(x+23,y+5,1);g.pose().scale(textScale,textScale,1);
        if(alpha>.03f){
            g.drawString(mc.font,title,0,0,color(0xF8DB85,alpha),false);
            g.drawString(mc.font,detail,0,Math.round(11/textScale),color(0xB7FFD0,alpha),false);
        }
        g.pose().popPose();g.pose().popPose();
    }
    private static int color(int rgb,float alpha){return Math.round(255*Mth.clamp(alpha,0,1))<<24|rgb;}
    private record Placement(int x,int y,int width,int height,float textScale){}
    private static Placement placement(Screen screen,AbstractWidget game,int textWidth){
        int width=Math.min(Math.max(48,screen.width-12),textWidth+32),height=30;
        int x=Mth.clamp(game.getX()+game.getWidth()/2-width/2,6,Math.max(6,screen.width-width-6));
        int y=game.getY()+game.getHeight()+47;
        if(y+height>screen.height-6){
            int side=game.getX()+game.getWidth()+8,space=screen.width-side-6;
            if(space>=92){width=Math.min(width,space);x=side;y=Math.max(6,screen.height-height-6);}
            else y=6;
        }
        return new Placement(x,y,width,height,Math.min(1,(width-28f)/Math.max(1,textWidth)));
    }
    private static void border(GuiGraphics g,int left,int top,int right,int bottom,int color){
        g.fill(left+2,top,right-2,top+1,color);g.fill(left+2,bottom-1,right-2,bottom,color);
        g.fill(left,top+2,left+1,bottom-2,color);g.fill(right-1,top+2,right,bottom-2,color);
    }
    private ClavisAgilityFeedback(){}
}
