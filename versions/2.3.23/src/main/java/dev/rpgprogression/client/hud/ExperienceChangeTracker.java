package dev.rpgprogression.client.hud;

/** A session's first XP snapshot is a baseline. Later gains and spending refresh a five-second window. */
public final class ExperienceChangeTracker {
    public static final int DISPLAY_TICKS=100;
    private Object session;
    private int total,level,progressBits,remaining;
    public void reset(){session=null;remaining=0;}
    public void update(Object currentSession,int currentTotal,int currentLevel,float progress){
        if(currentSession==null){reset();return;}
        int bits=Float.floatToIntBits(progress);
        if(session!=currentSession){session=currentSession;remaining=0;}
        else if(total!=currentTotal||level!=currentLevel||progressBits!=bits)remaining=DISPLAY_TICKS;
        else if(remaining>0)remaining--;
        total=currentTotal;level=currentLevel;progressBits=bits;
    }
    public boolean visible(){return remaining>0;}
}
