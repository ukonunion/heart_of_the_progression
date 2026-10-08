package dev.rpgprogression.client.hud;

/** Pixel pairs expand symmetrically; odd tracks start with their central pixel. */
public final class CenteredFill {
    public static int halfWidth(int width,double ratio){
        if(!Double.isFinite(ratio))return 0;
        int half=Math.max(0,width/2);
        return (int)Math.round(half*Math.max(0,Math.min(1,ratio)));
    }
    public static int pixels(int width,double ratio){
        if(!Double.isFinite(ratio)||ratio<=0)return 0;
        int bounded=Math.max(0,width);
        return halfWidth(bounded,ratio)*2+(bounded&1);
    }
    private CenteredFill(){}
}
