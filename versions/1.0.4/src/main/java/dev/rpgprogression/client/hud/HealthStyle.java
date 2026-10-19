package dev.rpgprogression.client.hud;

/** One health skin per frame, even when several effects coexist. */
public enum HealthStyle {
    NORMAL("empty","main_hc",0xFFD32F30),
    ABSORPTION("absorption","absorption_hc",0xFFF4BB28),
    FROZEN("frozen","frozen_hc",0xFF24DDE6),
    POISON("poison","poison_hc",0xFF309F3B),
    WITHER("wither","wither_hc",0xFF9E5872);
    private final String survival,hardcore;
    public final int color;
    HealthStyle(String survival,String hardcore,int color){this.survival=survival;this.hardcore=hardcore;this.color=color;}
    public String texture(boolean hc){return "hp/"+(hc?"hardcore/"+hardcore:"survival/"+survival);}
    public static HealthStyle select(boolean wither,boolean poison,boolean frozen,boolean absorption){
        if(wither)return WITHER;
        if(poison)return POISON;
        if(frozen)return FROZEN;
        if(absorption)return ABSORPTION;
        return NORMAL;
    }
}
