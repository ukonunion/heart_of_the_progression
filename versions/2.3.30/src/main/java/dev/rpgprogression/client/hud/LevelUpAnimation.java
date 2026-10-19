package dev.rpgprogression.client.hud;

/** Tick-based banner lifetime. Repeated gains extend it without restarting the fade. */
public final class LevelUpAnimation {
    public static final int DISPLAY_TICKS=64;
    private final HudVisibilityAnimation visibility=new HudVisibilityAnimation();
    private int remaining;
    public void show(){remaining=DISPLAY_TICKS;}
    public void tick(){visibility.tick(remaining>0);if(remaining>0)remaining--;}
    public float opacity(float partial){return visibility.value(partial);}
    public boolean active(){return remaining>0||visibility.active();}
    public void reset(){remaining=0;visibility.reset();}
}
