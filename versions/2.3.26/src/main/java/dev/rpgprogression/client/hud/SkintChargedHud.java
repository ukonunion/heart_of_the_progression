package dev.rpgprogression.client.hud;

import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** A one-shot activation event; loading an already active point never shows this banner. */
@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class SkintChargedHud {
    private static final LevelUpAnimation ANIMATION=new LevelUpAnimation();
    public static void show(){if(Minecraft.getInstance().player!=null)ANIMATION.show();}
    public static void reset(){ANIMATION.reset();}
    public static boolean active(){return ANIMATION.active();}
    public static float opacity(float partial){return ANIMATION.opacity(partial);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getConnection()==null){reset();return;}
        if(!mc.isPaused())ANIMATION.tick();
    }
    public static void render(GuiGraphics graphics,DeltaTracker tracker){
        LevelUpHud.renderBanner(graphics,ANIMATION.opacity(tracker.getGameTimeDeltaPartialTick(false)),"skint_charged",388,192);
    }
    private SkintChargedHud(){}
}
