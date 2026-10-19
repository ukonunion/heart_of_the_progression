package dev.rpgprogression.animation;

/** Monotonic, real-time opacity independent of server ticks and world changes. */
public final class SkintFadeTimeline {
    public static final long DURATION_NANOS=450_000_000L;
    private enum Phase { CLEAR, OUT, BLACK, IN }
    private Phase phase=Phase.CLEAR;
    private long began;
    private double from;
    public void darken(long now){phase=Phase.OUT;began=now;from=0;}
    public double opacity(long now){
        double t=Math.clamp((double)(now-began)/DURATION_NANOS,0,1),smooth=t*t*t*(t*(t*6-15)+10);
        return switch(phase){case CLEAR->0;case BLACK->1;case OUT->smooth;case IN->from*(1-smooth);};
    }
    public boolean black(long now){return phase==Phase.BLACK||phase==Phase.OUT&&now-began>=DURATION_NANOS;}
    public void hold(){phase=Phase.BLACK;}
    public void reveal(long now){from=opacity(now);began=now;phase=from>0?Phase.IN:Phase.CLEAR;}
    public void advance(long now){if(phase==Phase.IN&&now-began>=DURATION_NANOS)clear();}
    public boolean active(){return phase!=Phase.CLEAR;}
    public void clear(){phase=Phase.CLEAR;from=0;}
}
