package dev.rpgprogression.client;

import dev.rpgprogression.animation.SkintFadeTimeline;
import dev.rpgprogression.client.gui.SkintScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Drawn above every screen, including ReceivingLevelScreen and loading overlays. */
public final class SkintFade {
    private static final SkintFadeTimeline timeline=new SkintFadeTimeline();
    private static boolean committed,recovering,waiting;
    private static int blackFrames,readyFrames;
    private static double renderedOpacity;
    public static void begin(){timeline.clear();waiting=true;committed=recovering=false;blackFrames=readyFrames=0;}
    public static boolean active(){return waiting||timeline.active();}
    public static boolean committed(){return committed;}
    public static double renderedOpacity(){return renderedOpacity;}
    public static void reveal(){waiting=false;timeline.reveal(System.nanoTime());committed=recovering=false;blackFrames=readyFrames=0;}
    public static void recover(){if(waiting){clear();return;}if(!active())return;recovering=true;}
    public static void clear(){waiting=false;timeline.clear();committed=recovering=false;blackFrames=readyFrames=0;renderedOpacity=0;}
    private static boolean ready(Minecraft mc){
        if(ClientSkintData.busy()||ClientSkintData.arriving()||!ClientSkintData.allowed()||!(mc.screen instanceof SkintScreen)
                ||mc.getOverlay()!=null||!SkintCamera.settled())return false;
        var pos=ClientSkintData.origin().pos();
        // Wait for actual rendered terrain around the new seat, not just the block-state packet.
        for(int x:new int[]{-6,0,6})for(int z:new int[]{-6,0,6})
            if(!mc.levelRenderer.isSectionCompiled(pos.offset(x,-1,z)))return false;
        return true;
    }
    public static void render(GuiGraphics g){
        long now=System.nanoTime();var mc=Minecraft.getInstance();
        if(waiting){
            renderedOpacity=0;
            if(!(mc.player!=null&&mc.player.getVehicle() instanceof dev.rpgprogression.skint.SkintSeat seat&&seat.standing())
                    ||seat.animationSeconds(mc.getTimer().getGameTimeDeltaPartialTick(true))<1.4)return;
            waiting=false;timeline.darken(now);
        }
        timeline.advance(now);renderedOpacity=timeline.opacity(now);
        if(!active()){committed=false;return;}
        if(timeline.black(now)){
            timeline.hold();renderedOpacity=1;
            if(recovering){
                if(mc.player!=null&&mc.level!=null&&mc.getOverlay()==null&&!(mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen)
                        &&mc.levelRenderer.isSectionCompiled(mc.player.blockPosition().below()))timeline.reveal(now);
            }else if(!committed&&++blackFrames>=2){committed=true;ClientSkintData.action(4,null);}
            if(!recovering&&committed&&ready(mc)){if(++readyFrames>=3)timeline.reveal(now);}else readyFrames=0;
        }
        int alpha=(int)Math.round(renderedOpacity*255);
        if(alpha>0){g.flush();g.fill(0,0,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),1000,alpha<<24);g.flush();}
    }
    private SkintFade(){}
}
