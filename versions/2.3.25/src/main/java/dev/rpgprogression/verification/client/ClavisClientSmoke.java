package dev.rpgprogression.verification.client;

import com.google.gson.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.gui.CharacterScreen;
import dev.rpgprogression.client.hud.ClavisAgilityFeedback;
import dev.rpgprogression.compat.ClavisCompat;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.verification.ClavisChecks;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.components.AbstractWidget;
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
    private static Path folder(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/clavis-2.3.25-"+(present()?"on":"off"));}
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
                    float startingQuality=new float[]{0f,.12f,1f,2f}[trial%4];
                    set(game,"health",5);set(game,"lootQuality",startingQuality);particles(screen).clear();var tween=field(nativeWidget,"mainTween");
                    hurt.invoke(game);
                    int health=((Number)call(game,"getHealth")).intValue();float quality=((Number)call(game,"getLootQuality")).floatValue();
                    Object config=Class.forName(ROOT+"Clavis").getField("CONFIG").get(null);float penalty=((Number)call(config,"getQualityPenaltyPerHit")).floatValue();
                    valid&=health==(expected?5:4)&&Math.abs(quality-(expected?startingQuality:startingQuality-penalty))<1e-5&&tween!=field(nativeWidget,"mainTween")&&(particles(screen).isEmpty()==expected)&&ClavisAgilityFeedback.active(screen)==expected;
                    if(expected)retained++;else lost++;
                }
                check(valid,type+": 64 seeded native mistakes at Agility "+agility+" protect life and loot quality on the same roll; feedback appears only on protection, with original penalties and recovery otherwise");
            }
            check(retained>0&&lost>0,type+": native rolls exercise both saved and lost lives ("+retained+" / "+lost+")");
            Object config=Class.forName(ROOT+"Clavis").getField("CONFIG").get(null);float penalty=((Number)call(config,"getQualityPenaltyPerHit")).floatValue();
            data.set(RPGAttribute.AGILITY,100);RPGConfig.CLAVIS_LIFE_SAVE.set(0.0);set(game,"health",2);set(game,"lootQuality",1f);hurt.invoke(game);
            check(((Number)call(game,"getHealth")).intValue()==1&&Math.abs(((Number)call(game,"getLootQuality")).floatValue()-(1-penalty))<1e-5,type+": disabling the bonus immediately restores ordinary life and quality loss");
            RPGConfig.CLAVIS_LIFE_SAVE.set(1.0);data.set(RPGAttribute.AGILITY,15);set(game,"health",1);set(game,"lootQuality",.12f);particles(screen).clear();hurt.invoke(game);
            check(((Number)call(game,"getHealth")).intValue()==1&&((Number)call(game,"getLootQuality")).floatValue()==.12f&&mc.screen==screen&&particles(screen).isEmpty(),type+": a saved last life keeps its exact loot quality and does not break a heart");
            set(game,"health",5);set(game,"lootQuality",1f);for(int i=0;i<10;i++)hurt.invoke(game);
            check(((Number)call(game,"getHealth")).intValue()==5&&((Number)call(game,"getLootQuality")).floatValue()==1f,type+": ten successive protected mistakes neither lower nor increase loot quality");
            RPGConfig.CLAVIS_LIFE_SAVE.set(0.0);hurt.invoke(game);
            check(((Number)call(game,"getHealth")).intValue()==4&&Math.abs(((Number)call(game,"getLootQuality")).floatValue()-(1-penalty))<1e-5,type+": the next unprotected mistake applies the ordinary penalty after a run of protected mistakes");
        }finally{data.load(saved);RPGConfig.CLAVIS_LIFE_SAVE.set(old);particles(screen).clear();mc.setScreen(null);}
    }
    private static void hover(Minecraft mc)throws Exception {
        double scale=Math.min(1.5,Math.min(mc.screen.width/640.0,mc.screen.height/360.0));
        double x=(mc.screen.width-640*scale)/2+500*scale,y=(mc.screen.height-360*scale)/2+116*scale;
        set(mc.mouseHandler,"xpos",x*mc.getWindow().getScreenWidth()/mc.screen.width);set(mc.mouseHandler,"ypos",y*mc.getWindow().getScreenHeight()/mc.screen.height);
    }
    private static void capture(Minecraft mc,String name)throws Exception {
        if(name.equals("02-gear.png")||name.equals("03-mirror.png"))check(mc.screen!=null&&mc.screen.getClass().getName().equals(ROOT+"client.screen.LockpickingScreen"),"the native "+name+" screenshot retains the open Clavis minigame within interaction distance");
        Files.createDirectories(folder(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            if(name.startsWith("05-")||name.startsWith("09-")||name.startsWith("10-")){
                var game=(AbstractWidget)field(mc.screen,"minigameWidget");
                int textWidth=Math.max(mc.font.width(Component.translatable("rpgprogression.compat.clavis.protected.title")),mc.font.width(Component.translatable("rpgprogression.compat.clavis.protected.detail")));
                var method=ClavisAgilityFeedback.class.getDeclaredMethod("placement",Screen.class,AbstractWidget.class,int.class);method.setAccessible(true);var placement=method.invoke(null,mc.screen,game,textWidth);
                int left=((Number)field(placement,"x")).intValue(),top=((Number)field(placement,"y")).intValue(),width=((Number)field(placement,"width")).intValue(),height=((Number)field(placement,"height")).intValue();
                check(left>=0&&top>=0&&left+width<=mc.screen.width&&top+height<=mc.screen.height,"the "+name+" confirmation fits entirely inside the viewport");
                check(left>=game.getRight()||left+width<=game.getX()||top>=game.getBottom()||top+height<=game.getY(),"the "+name+" confirmation does not cover the minigame controls");
                int gold=0,green=0;
                for(int x=left*image.getWidth()/mc.screen.width;x<(left+width)*image.getWidth()/mc.screen.width;x++)for(int y=top*image.getHeight()/mc.screen.height;y<(top+height)*image.getHeight()/mc.screen.height;y++){
                    int abgr=image.getPixelRGBA(x,y),r=abgr&255,g=abgr>>>8&255,b=abgr>>>16&255;
                    // Font vertex shading slightly darkens full-opacity text; accept that native rounding.
                    if(Math.abs(r-248)<=4&&Math.abs(g-219)<=4&&Math.abs(b-133)<=4)gold++;
                    if(Math.abs(r-183)<=4&&Math.abs(g-255)<=4&&Math.abs(b-208)<=4)green++;
                }
                check(gold>100&&green>150,"the actual "+name+" framebuffer contains the gold success title and green preservation text");
            }
            image.writeToFile(folder(mc).resolve(name));
        }
    }
    private static void proc(Minecraft mc,boolean success)throws Exception {
        var screen=mc.screen;var game=call(screen,"getGame");set(game,"health",5);set(game,"lootQuality",2f);
        double chance=ClavisCompat.lifeSaveChance(mc.player);if(chance<=0||chance>=1)throw new IllegalStateException("Expected a genuine fractional roll");
        var seeds=new Random(2325);long seed;do{seed=seeds.nextLong();mc.player.getRandom().setSeed(seed);}while((mc.player.getRandom().nextDouble()<chance)!=success);
        mc.player.getRandom().setSeed(seed);game.getClass().getMethod("hurt").invoke(game);
        check(ClavisAgilityFeedback.active(screen)==success,"a real "+(success?"successful":"unsuccessful")+" Agility roll "+(success?"shows":"clears")+" the confirmation");
        check(((Number)call(game,"getHealth")).intValue()==(success?5:4)&&(!success||((Number)call(game,"getLootQuality")).floatValue()==2f),"visual feedback preserves the actual native life and loot result");
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
                    pending=CompletableFuture.runAsync(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());ClavisChecks.common(p,checks);p.setInvulnerable(true);p.setHealth(p.getMaxHealth());p.setAirSupply(p.getMaxAirSupply());var d=ProgressionManager.get(p);d.set(RPGAttribute.AGILITY,25);ModNetworking.sync(p);}catch(Exception e){throw new IllegalStateException(e);}},mc.getSingleplayerServer());advance();}
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
                case 8->{hover(mc);if(age<12)return;capture(mc,"04-agility-en.png");if(!present()){done=true;ClavisChecks.report(folder(mc).resolve("result.json"),checks,null);mc.stop();return;}game(mc,"gear","gear.GearMechanismWidget");proc(mc,true);advance();}
                case 9->{
                    if(age==4)capture(mc,"05-proc-gear-en.png");
                    if(age==31){check(ClavisAgilityFeedback.active(mc.screen),"confirmation remains active through its fading phase");capture(mc,"06-fading.png");}
                    if(age<43)return;check(!ClavisAgilityFeedback.active(mc.screen),"the confirmation expires naturally after 38 ticks");capture(mc,"07-expired.png");
                    int children=mc.screen.children().size();proc(mc,true);proc(mc,true);
                    check(((Number)member(ClavisAgilityFeedback.class,"age").get(null)).intValue()==0&&mc.screen.children().size()==children,"repeated protection restarts one effect without adding interactive widgets");
                    proc(mc,false);var previous=mc.screen;mc.setScreen(new CharacterScreen());check(!ClavisAgilityFeedback.active(previous),"closing the minigame immediately clears its effect");
                    mc.setScreen(previous);check(!ClavisAgilityFeedback.active(previous),"reopening the same cached screen does not replay old feedback");advance();}
                case 10->{if(age<4)return;capture(mc,"08-unprotected-miss.png");mc.setScreen(new CharacterScreen());mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";pending=mc.reloadResourcePacks();advance();}
                case 11->{game(mc,"mirror","mirror.MirrorWidget");proc(mc,true);advance();}
                case 12->{if(age<4)return;capture(mc,"09-proc-mirror-ru.png");mc.options.guiScale().set(3);mc.resizeDisplay();proc(mc,true);advance();}
                case 13->{if(age<4)return;capture(mc,"10-proc-compact.png");var previous=mc.screen;mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new CharacterScreen());check(!ClavisAgilityFeedback.active(previous),"resizing and leaving the minigame leaves no stale success panel");done=true;ClavisChecks.report(folder(mc).resolve("result.json"),checks,null);mc.stop();}
            }
        } catch(Throwable error) {
            done=true;com.mojang.logging.LogUtils.getLogger().error("CLAVIS_CLIENT failed",error);
            try{ClavisChecks.report(folder(mc).resolve("result.json"),checks,error);}catch(Exception e){throw new IllegalStateException(e);}mc.stop();
        }
    }
    private ClavisClientSmoke(){}
}
