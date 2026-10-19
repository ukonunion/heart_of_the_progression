package dev.rpgprogression.client.hud;

/** A reversible 0.4-second transition, interpolated between ticks with smooth endpoints. */
public final class HudVisibilityAnimation {
    private static final float STEP=1f/8;
    private float previous,current;
    public void reset(){previous=current=0;}
    public void tick(boolean visible){previous=current;current=Math.max(0,Math.min(1,current+(visible?STEP:-STEP)));}
    public float value(float partial){float t=previous+(current-previous)*Math.max(0,Math.min(1,partial));return t*t*(3-2*t);}
    public boolean active(){return previous>0||current>0;}
}
