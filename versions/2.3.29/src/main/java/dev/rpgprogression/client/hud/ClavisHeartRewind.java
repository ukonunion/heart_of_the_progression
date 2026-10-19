package dev.rpgprogression.client.hud;

import dev.rpgprogression.mixin.clavis.ClavisParticleAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Vector2f;
import java.lang.reflect.Method;
import java.util.*;

/** Replays the actual native heart particles backwards, including their original random trajectories. */
@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class ClavisHeartRewind {
    private static final int FORWARD=30;
    private static Session current;
    private record Sample(float x,float y,float sx,float sy,float roll,int time){}
    private static final class Access {
        final Method tick,render,time,setTime,lifetime,transform,position,size,roll,setPosition,setSize,setRoll,old;
        Access(Class<?> type)throws ReflectiveOperationException {
            tick=type.getMethod("tick");render=type.getMethod("render",GuiGraphics.class,float.class);time=type.getMethod("getTime");setTime=type.getMethod("setTime",int.class);
            lifetime=type.getMethod("getLifetime");transform=type.getMethod("getTransform");var t=transform.getReturnType();
            position=t.getMethod("getPosition");size=t.getMethod("getSize");roll=t.getMethod("getRoll");setPosition=t.getMethod("setPosition",Vector2f.class);setSize=t.getMethod("setSize",Vector2f.class);setRoll=t.getMethod("setRoll",float.class);old=t.getMethod("updateOldValues");
        }
    }
    private static final ClassValue<Access> ACCESS=new ClassValue<>(){
        @Override protected Access computeValue(Class<?> type){try{return new Access(type);}catch(ReflectiveOperationException e){throw new IllegalStateException("Clavis heart particle API",e);}}
    };
    private static final class Track {
        final Object particle,transform;final Access api;final int lifetime;final List<Sample> frames=new ArrayList<>();
        Track(Object particle)throws ReflectiveOperationException {this.particle=particle;api=ACCESS.get(particle.getClass());transform=api.transform.invoke(particle);lifetime=((Number)api.lifetime.invoke(particle)).intValue();frames.add(sample());}
        Sample sample()throws ReflectiveOperationException {
            var p=(Vector2f)api.position.invoke(transform);var size=(Vector2f)api.size.invoke(transform);
            return new Sample(p.x,p.y,size.x,size.y,((Number)api.roll.invoke(transform)).floatValue(),((Number)api.time.invoke(particle)).intValue());
        }
        void forward()throws ReflectiveOperationException {if(((Number)api.time.invoke(particle)).intValue()<lifetime)api.tick.invoke(particle);frames.add(sample());}
        void set(Sample sample)throws ReflectiveOperationException {api.setPosition.invoke(transform,new Vector2f(sample.x,sample.y));api.setSize.invoke(transform,new Vector2f(sample.sx,sample.sy));api.setRoll.invoke(transform,sample.roll);api.setTime.invoke(particle,sample.time);}
        void backward(int index)throws ReflectiveOperationException {set(frames.get(Math.min(index+1,frames.size()-1)));api.old.invoke(transform);set(frames.get(index));}
        void render(GuiGraphics g,float partial)throws ReflectiveOperationException {if(((Number)api.time.invoke(particle)).intValue()<lifetime)api.render.invoke(particle,g,partial);}
    }
    private static final class Session {
        final Screen screen;final Object game;final int width,height;final List<Track> tracks=new ArrayList<>();int age;
        Session(Screen screen,Object game){this.screen=screen;this.game=game;width=screen.width;height=screen.height;}
    }
    public static void begin(Screen screen,Object game){current=new Session(screen,game);}
    public static List<Object> particles(Screen screen){return ClavisParticleAccess.rpgprogression$particles().computeIfAbsent(screen,s->new ArrayList<>());}
    public static void capture(Screen screen,Set<Object> before){
        if(current==null||current.screen!=screen)return;
        var list=particles(screen);var created=list.stream().filter(p->!before.contains(p)&&p.getClass().getName().startsWith("it.hurts.shatterbyte.clavis.common.client.particle.")).toList();
        try{for(var particle:created)current.tracks.add(new Track(particle));list.removeAll(created);if(current.tracks.isEmpty())reset();}
        catch(ReflectiveOperationException|RuntimeException e){reset();com.mojang.logging.LogUtils.getLogger().warn("Cannot rewind Clavis hearts",e);}
    }
    public static int displayedHealth(Object game,int actual){return current!=null&&current.game==game?Math.max(0,actual-1):actual;}
    public static boolean active(Screen screen){return current!=null&&current.screen==screen;}
    public static void clear(Screen screen){if(active(screen))reset();}
    public static void reset(){current=null;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(ClientTickEvent.Post event){
        var s=current;if(s==null)return;var mc=Minecraft.getInstance();
        if(mc.player==null||mc.screen!=s.screen||s.width!=s.screen.width||s.height!=s.screen.height||s.age>=FORWARD*2){reset();return;}
        try{if(s.age<FORWARD)for(var track:s.tracks)track.forward();else for(var track:s.tracks)track.backward(FORWARD*2-s.age-1);s.age++;}
        catch(ReflectiveOperationException e){reset();com.mojang.logging.LogUtils.getLogger().warn("Cannot animate Clavis hearts",e);}
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event){
        var s=current;if(s==null||s.screen!=event.getScreen())return;var g=event.getGuiGraphics();g.pose().pushPose();g.pose().translate(0,0,300);
        try{for(var track:s.tracks)track.render(g,event.getPartialTick());}catch(ReflectiveOperationException e){reset();}finally{g.pose().popPose();}
    }
    @SubscribeEvent public static void close(ScreenEvent.Closing event){clear(event.getScreen());}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){reset();}
    private ClavisHeartRewind(){}
}
