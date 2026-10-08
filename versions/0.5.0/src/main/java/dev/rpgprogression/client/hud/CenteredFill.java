package dev.rpgprogression.client.hud;

/** Integer pixels are added in pairs so neither side gets a head start. */
public final class CenteredFill {
    public static int halfWidth(int width,double ratio){
        if(!Double.isFinite(ratio))return 0;
        int half=Math.max(0,width/2);
        return (int)Math.round(half*Math.max(0,Math.min(1,ratio)));
    }
    private CenteredFill(){}
}
