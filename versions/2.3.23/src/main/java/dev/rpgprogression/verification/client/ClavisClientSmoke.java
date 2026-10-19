package dev.rpgprogression.verification.client;

import com.google.gson.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.gui.CharacterScreen;
import dev.rpgprogression.compat.ClavisCompat;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.verification.ClavisChecks;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class ClavisClientSmoke {
    private static final JsonArray checks=new JsonArray();
    private static boolean loading,done;private static int boot,stage,age;private static CompletableFuture<?> pending;
    private static final String ROOT="it.hurts.shatterbyte.clavis.common.";
    private static boolean present(){return Boolean.getBoolean("rpgprogression.expectClavis");}
    private static Path folder(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/clavis-2.3.23-"+(present()?"on":"off"));}
    private static void check(boolean ok,String text){ClavisChecks.check(checks,ok,text);}
    private static Field member(Class<?> type,String name)throws Exception {
        for(;type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static Object field(Object o,String name)throws Exception{return member(o.getClass(),name).get(o);}
    private static void set(Object o,String name,Object value)throws Exception{member(o.getClass(),name).set(o,value);}
    private static Object call(Object o,String name)throws Exception{return o.getClass().getMethod(name).invoke(o);}
    private static void advance(){stage++;age=0;com.mojang.logging.LogUtils.getLogger().info("CLAVIS_CLIENT stage {}",stage);}
    @SuppressWarnings("unchecked") private static List<Component> tooltip(CharacterScreen screen,RPGAttribute a)throws Exception {
        var method=CharacterScreen.class.getDeclaredMethod("attributeTooltip",RPGAttribute.class);method.setAccessible(true);return (List<Component>)method.invoke(screen,a);
    }
    private static Screen game(Minecraft mc,String type,String widget)throws Exception {
        var pos=mc.player.blockPosition();
        var boxClass=Class.forName(ROOT+"data.Box");var box=boxClass.getConstructor(int.class,int.class,int.class,int.class,int.class,int.class).newInstance(pos.getX(),pos.getY(),pos.getZ(),pos.getX(),pos.getY(),pos.getZ());
        var lockClass=Class.forName(ROOT+"data.Lock");var lock=lockClass.getConstructor(UUID.class,boxClass,float.class,long.class,boolean.class,List.class,ResourceLocation.class)
                .newInstance(UUID.randomUUID(),box,.5f,2323L,false,List.of(),ResourceLocation.parse("clavis:"+type));
        Supplier<Object> factory=()->{try{return Class.forName(ROOT+"client.screen.widget."+widget).getConstructor().newInstance();}catch(Exception e){throw new IllegalStateException(e);}};
        var screen=(Screen)Class.forName(ROOT+"client.screen.LockpickingScreen").getConstructor(BlockPos.class,lockClass,Supplier.class).newInstance(pos,lock,factory);
        mc.setScreen(screen);return screen;
    }
    @SuppressWarnings("unchecked") private static List<Object> particles(Screen screen)throws Exception {
        var map=(Map<Screen,List<Object>>)member(Class.forName("it.hurts.octostudios.octolib.client.particle.ParticleSystem"),"SCREEN_PARTICLES").get(null);
        return map.computeIfAbsent(screen,s->new ArrayList<>());
    }
    private static void gameplay(Minecraft mc,String type,String widget)throws Exception {
        var screen=game(mc,type,widget);var game=call(screen,"getGame");var nativeWidget=field(screen,"minigameWidget");var hurt=game.getClass().getMethod("hurt");
        var data=ClientData.get();var saved=data.save();double old=RPGConfig.CLAVIS_LIFE_SAVE.get();var random=new Random(2323);int retained=0,lost=0;
        try {
            for(int agility:new int[]{10,14,15,19,20,24,25,99,100}) {
                data.set(RPGAttribute.AGILITY,agility);RPGConfig.CLAVIS_LIFE_SAVE.set(.025);double chance=ClavisCompat.lifeSaveChance(mc.player);
                boolean valid=true;
                for(int trial=0;trial<64;trial++){
                    long seed=random.nextLong();mc.player.getRandom().setSeed(seed);boolean expected=mc.player.getRandom().nextDouble()<chance;mc.player.getRandom().setSeed(seed);
                    set(game,"health",5);set(game,"lootQuality",1f);particles(screen).clear();var tween=field(nativeWidget,"mainTween");
                    hurt.invoke(game);
                    int health=((Number)call(game,"getHealth")).intValue();float quality=((Number)call(game,"getLootQuality")).floatValue();
                    Object config=Class.forName(ROOT+"Clavis").getField("CONFIG").get(null);float penalty=((Number)call(config,"getQualityPenaltyPerHit")).floatValue();
                    valid&=health==(expected?5:4)&&Math.abs(quality-(1-penalty))<1e-5&&tween!=field(nativeWidget,"mainTween")&&(particles(screen).isEmpty()==expected);
                    if(expected)retained++;else lost++;
                }
                check(valid,type+": 64 seeded native mistakes at Agility "+agility+" preserve life exactly at the requested chance, keep loot penalties and recovery, and only animate lost hearts");
            }
            check(retained>0&&lost>0,type+": native rolls exercise both saved and lost lives ("+retained+" / "+lost+")");
            data.set(RPGAttribute.AGILITY,100);RPGConfig.CLAVIS_LIFE_SAVE.set(0.0);set(game,"health",2);hurt.invoke(game);
            check(((Number)call(game,"getHealth")).intValue()==1,type+": disabling the bonus immediately restores ordinary life loss");
            RPGConfig.CLAVIS_LIFE_SAVE.set(1.0);data.set(RPGAttribute.AGILITY,15);set(game,"health",1);particles(screen).clear();hurt.invoke(game);
            check(((Number)call(game,"getHealth")).intValue()==1&&mc.screen==screen&&particles(screen).isEmpty(),type+": a saved last life does not lose the game or break a heart");
        }finally{data.load(saved);RPGConfig.CLAVIS_LIFE_SAVE.set(old);particles(screen).clear();mc.setScreen(null);}
    }
    private static void hover(Minecraft mc)throws Exception {
        double scale=Math.min(1.5,Math.min(mc.screen.width/640.0,mc.screen.height/360.0));
        double x=(mc.screen.width-640*scale)/2+500*scale,y=(mc.screen.height-360*scale)/2+116*scale;
        set(mc.mouseHandler,"xpos",x*mc.getWindow().getScreenWidth()/mc.screen.width);set(mc.mouseHandler,"ypos",y*mc.getWindow().getScreenHeight()/mc.screen.height);
    }
    private static void capture(Minecraft mc,String name)throws Exception {
        if(name.equals("02-gear.png")||name.equals("03-mirror.png"))check(mc.screen!=null&&mc.screen.getClass().getName().equals(ROOT+"client.screen.LockpickingScreen"),"the native "+name+" screenshot retains the open Clavis minigame within interaction distance");
        Files.createDirectories(folder(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder(mc).resolve(name));}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(done||!Boolean.getBoolean("rpgprogression.clavisClientSmoke"))return;var mc=Minecraft.getInstance();
        try {
            if(mc.player==null||mc.getSingleplayerServer()==null){if(++boot>6000)throw new IllegalStateException("startup timeout");if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var prev=mc.screen;mc.createWorldOpenFlows().openWorld(System.getProperty("rpgprogression.smokeWorld"),()->mc.setScreen(prev));}return;}
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(mc.getOverlay()!=null)return;if(++age>600)throw new IllegalStateException("stage timeout "+stage);
            switch(stage){
                case 0->{if(age<30)return;check(net.neoforged.fml.ModList.get().isLoaded("clavis")==present(),"client Clavis presence matches the test runtime");
                    mc.options.guiScale().set(2);mc.options.hideGui=false;mc.resizeDisplay();
                    pending=CompletableFuture.runAsync(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());ClavisChecks.common(p,checks);var d=ProgressionManager.get(p);d.set(RPGAttribute.AGILITY,25);ModNetworking.sync(p);}catch(Exception e){throw new IllegalStateException(e);}},mc.getSingleplayerServer());advance();}
                case 1->{if(ClientData.get().get(RPGAttribute.AGILITY)!=25)return;check(Math.abs(ClavisCompat.lifeSaveChance(mc.player)-.075)<1e-12,"the client receives server Agility and calculates 7.5% at level 25");
                    if(present()){gameplay(mc,"gear","gear.GearMechanismWidget");gameplay(mc,"mirror","mirror.MirrorWidget");}
                    mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";pending=mc.reloadResourcePacks();advance();}
                case 2->{var screen=new CharacterScreen();mc.setScreen(screen);var lines=tooltip(screen,RPGAttribute.AGILITY);
                    check(lines.size()==(present()?2:1),"Agility adds a Clavis tooltip only when Clavis is installed");
                    if(present())check(lines.get(1).getString().contains("7.5%")&&lines.get(1).getString().contains("выше 10"),"Russian tooltip shows the synced chance and level-10 baseline");
                    check(tooltip(screen,RPGAttribute.STRENGTH).size()==1,"other attribute tooltips remain unchanged");hover(mc);advance();}
                case 3->{hover(mc);if(age<12)return;capture(mc,"01-agility-ru.png");if(present())game(mc,"gear","gear.GearMechanismWidget");advance();}
                case 4->{if(age<12)return;if(present()){capture(mc,"02-gear.png");game(mc,"mirror","mirror.MirrorWidget");}advance();}
                case 5->{if(age<12)return;if(present())capture(mc,"03-mirror.png");advance();}
                case 6->{mc.getLanguageManager().setSelected("en_us");mc.options.languageCode="en_us";pending=mc.reloadResourcePacks();advance();}
                case 7->{var screen=new CharacterScreen();mc.setScreen(screen);var lines=tooltip(screen,RPGAttribute.AGILITY);if(present())check(lines.get(1).getString().contains("7.5%")&&lines.get(1).getString().contains("above 10"),"English tooltip shows the same synced chance and level-10 baseline");hover(mc);advance();}
                case 8->{hover(mc);if(age<12)return;capture(mc,"04-agility-en.png");done=true;ClavisChecks.report(folder(mc).resolve("result.json"),checks,null);mc.stop();}
            }
        } catch(Throwable error) {
            done=true;com.mojang.logging.LogUtils.getLogger().error("CLAVIS_CLIENT failed",error);
            try{ClavisChecks.report(folder(mc).resolve("result.json"),checks,error);}catch(Exception e){throw new IllegalStateException(e);}mc.stop();
        }
    }
    private ClavisClientSmoke(){}
}
