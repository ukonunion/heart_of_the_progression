package dev.rpgprogression.skill;

import java.util.*;

/** Render-clock transition; skills already known on opening the tree do not replay. */
public final class SkillPathGlow {
    public static final long DURATION=600_000_000L;
    private final Map<String,Boolean> previous=new HashMap<>();
    private final Map<String,Long> started=new HashMap<>();
    public void observe(Collection<String> sources,Set<String> learned,long now){
        for(String id:sources){
            boolean on=learned.contains(id);Boolean old=previous.put(id,on);
            if(old!=null&&!old&&on)started.put(id,now);
            if(!on)started.remove(id);
        }
    }
    public float alpha(String id,Set<String> learned,long now){
        if(!learned.contains(id))return 0;
        Long start=started.get(id);if(start==null)return 1;
        double t=Math.clamp((now-start)/(double)DURATION,0,1);
        if(t>=1)started.remove(id);
        return (float)(t*t*(3-2*t));
    }
}
