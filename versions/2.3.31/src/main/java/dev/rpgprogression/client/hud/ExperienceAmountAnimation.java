package dev.rpgprogression.client.hud;

/** A client-only 0.6-second count toward the synchronized amount, retaining its position on retarget. */
public final class ExperienceAmountAnimation {
    public static final int DURATION_TICKS=12;
    private Object session;
    private long from,target;
    private int age=DURATION_TICKS;
    public void reset(){session=null;from=target=0;age=DURATION_TICKS;}
    public void update(Object currentSession,long amount){
        if(currentSession==null){reset();return;}
        long next=Math.max(0,amount);
        if(session!=currentSession){session=currentSession;from=target=next;age=DURATION_TICKS;}
        else if(next!=target){from=value(1);target=next;age=0;}
        else if(age<DURATION_TICKS)age++;
    }
    public long value(float partial){
        if(age>=DURATION_TICKS||from==target)return target;
        double t=Math.min(1,(age+Math.max(0,Math.min(1,partial)))/DURATION_TICKS);
        double eased=1-Math.pow(1-t,3);
        long value=Math.round(from+(target-from)*eased);
        return Math.max(Math.min(from,target),Math.min(Math.max(from,target),value));
    }
}
