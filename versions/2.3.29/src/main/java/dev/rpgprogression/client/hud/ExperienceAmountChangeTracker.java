package dev.rpgprogression.client.hud;

/** The first synchronized amount is a baseline; later changes refresh a five-second window. */
public final class ExperienceAmountChangeTracker {
    public static final int DISPLAY_TICKS=100;
    private Object session;
    private long amount;
    private int remaining;
    public void reset(){session=null;remaining=0;}
    public void update(Object currentSession,long currentAmount){
        if(currentSession==null){reset();return;}
        if(session!=currentSession){session=currentSession;remaining=0;}
        else if(amount!=currentAmount)remaining=DISPLAY_TICKS;
        else if(remaining>0)remaining--;
        amount=currentAmount;
    }
    public boolean visible(){return remaining>0;}
}
