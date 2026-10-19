package dev.rpgprogression.verification.client;

import com.google.gson.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.gui.CharacterScreen;
import dev.rpgprogression.client.hud.ClavisHeartRewind;
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
    private static long beforePickupXP;private static int beforePickupLevel;
    private static final String ROOT="it.hurts.shatterbyte.clavis.common.";
    private static boolean present(){return Boolean.getBoolean("rpgprogression.expectClavis");}
    private static Path folder(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/clavis-2.3.26-"+(present()?"on":"off"));}
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
                    int health=((Number)field(game,"health")).intValue();float quality=((Number)call(game,"getLootQuality")).floatValue();
                    Object config=Class.forName(ROOT+"Clavis").getField("CONFIG").get(null);float penalty=((Number)call(config,"getQualityPenaltyPerHit")).floatValue();
                    valid&=health==(expected?5:4)&&Math.abs(quality-(expected?startingQuality:startingQuality-penalty))<1e-5&&tween!=field(nativeWidget,"mainTween")&&(particles(screen).isEmpty()==expected)&&ClavisHeartRewind.active(screen)==expected;
                    if(expected)retained++;else lost++;
                }
                check(valid,type+": 64 seeded native mistakes at Agility "+agility+" protect life and loot quality on the same roll; native reversal starts only on protection, with original penalties and recovery otherwise");
            }
            check(retained>0&&lost>0,type+": native rolls exercise both saved and lost lives ("+retained+" / "+lost+")");
            Object config=Class.forName(ROOT+"Clavis").getField("CONFIG").get(null);float penalty=((Number)call(config,"getQualityPenaltyPerHit")).floatValue();
            data.set(RPGAttribute.AGILITY,100);RPGConfig.CLAVIS_LIFE_SAVE.set(0.0);set(game,"health",2);set(game,"lootQuality",1f);hurt.invoke(game);
            check(((Number)field(game,"health")).intValue()==1&&Math.abs(((Number)call(game,"getLootQuality")).floatValue()-(1-penalty))<1e-5,type+": disabling the bonus immediately restores ordinary life and quality loss");
            RPGConfig.CLAVIS_LIFE_SAVE.set(1.0);data.set(RPGAttribute.AGILITY,15);set(game,"health",1);set(game,"lootQuality",.12f);particles(screen).clear();hurt.invoke(game);
            check(((Number)field(game,"health")).intValue()==1&&((Number)call(game,"getLootQuality")).floatValue()==.12f&&mc.screen==screen&&particles(screen).isEmpty(),type+": a saved last life keeps its exact loot quality and does not break a heart");
            set(game,"health",5);set(game,"lootQuality",1f);for(int i=0;i<10;i++)hurt.invoke(game);
            check(((Number)field(game,"health")).intValue()==5&&((Number)call(game,"getLootQuality")).floatValue()==1f,type+": ten successive protected mistakes neither lower nor increase loot quality");
            RPGConfig.CLAVIS_LIFE_SAVE.set(0.0);hurt.invoke(game);
            check(((Number)field(game,"health")).intValue()==4&&Math.abs(((Number)call(game,"getLootQuality")).floatValue()-(1-penalty))<1e-5,type+": the next unprotected mistake applies the ordinary penalty after a run of protected mistakes");
        }finally{data.load(saved);RPGConfig.CLAVIS_LIFE_SAVE.set(old);particles(screen).clear();mc.setScreen(null);}
    }
    private static void hover(Minecraft mc)throws Exception {
        double scale=Math.min(1.5,Math.min(mc.screen.width/640.0,mc.screen.height/360.0));
        double x=(mc.screen.width-640*scale)/2+500*scale,y=(mc.screen.height-360*scale)/2+116*scale;
        set(mc.mouseHandler,"xpos",x*mc.getWindow().getScreenWidth()/mc.screen.width);set(mc.mouseHandler,"ypos",y*mc.getWindow().getScreenHeight()/mc.screen.height);
    }
    private static void capture(Minecraft mc,String name)throws Exception {
        Files.createDirectories(folder(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder(mc).resolve(name));}
    }
    private static void proc(Minecraft mc,boolean success,int health,float quality)throws Exception {
        var screen=mc.screen;var game=call(screen,"getGame");set(game,"health",health);set(game,"lootQuality",quality);
        double chance=ClavisCompat.lifeSaveChance(mc.player);var seeds=new Random(2326);long seed;
        do{seed=seeds.nextLong();mc.player.getRandom().setSeed(seed);}while((mc.player.getRandom().nextDouble()<chance)!=success);
        mc.player.getRandom().setSeed(seed);game.getClass().getMethod("hurt").invoke(game);
        check(ClavisHeartRewind.active(screen)==success,"a real protection roll starts the native forward/backward heart sequence only on success");
        check(((Number)field(game,"health")).intValue()==(success?health:health-1)&&(!success||((Number)call(game,"getLootQuality")).floatValue()==quality),"the visual loss does not spend protected life or loot quality");
        check(((Number)call(game,"getHealth")).intValue()==health-1,"the heart row initially displays the ordinary loss even for a protected mistake");
    }
    private static void reversedPath()throws Exception {
        var session=member(ClavisHeartRewind.class,"current").get(null);int age=((Number)field(session,"age")).intValue();boolean exact=age>30&&age<60;
        for(var track:(List<?>)field(session,"tracks")){
            var frames=(List<?>)field(track,"frames");var expected=frames.get(60-age);var position=(org.joml.Vector2f)call(call(field(track,"particle"),"getTransform"),"getPosition");
            exact&=Math.abs(position.x-((Number)field(expected,"x")).floatValue())<.0001&&Math.abs(position.y-((Number)field(expected,"y")).floatValue())<.0001;
        }
        check(exact,"all five real Clavis heart fragments exactly retrace their recorded original positions in reverse");
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(done||!Boolean.getBoolean("rpgprogression.clavisClientSmoke"))return;var mc=Minecraft.getInstance();
        try {
            if(mc.player==null||mc.getSingleplayerServer()==null){if(++boot>6000)throw new IllegalStateException("startup timeout");if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var prev=mc.screen;mc.createWorldOpenFlows().openWorld(System.getProperty("rpgprogression.smokeWorld"),()->mc.setScreen(prev));}return;}
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(mc.getOverlay()!=null)return;if(++age>600)throw new IllegalStateException("stage timeout "+stage);
            switch(stage){
                case 0->{if(age<30)return;check(net.neoforged.fml.ModList.get().isLoaded("clavis")==present(),"client Clavis presence matches the test runtime");
                    mc.options.guiScale().set(2);mc.options.hideGui=false;mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);mc.resizeDisplay();
                    pending=CompletableFuture.runAsync(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());ClavisChecks.common(p,checks);dev.rpgprogression.verification.ProgressionOrbChecks.run(p,checks);p.setInvulnerable(true);p.setHealth(p.getMaxHealth());p.setAirSupply(p.getMaxAirSupply());var d=ProgressionManager.get(p);d.set(RPGAttribute.AGILITY,25);ModNetworking.sync(p);}catch(Exception e){throw new IllegalStateException(e);}},mc.getSingleplayerServer());advance();}
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
                case 8->{hover(mc);if(age<12)return;capture(mc,"04-agility-en.png");if(!present()){advance();stage=14;return;}game(mc,"gear","gear.GearMechanismWidget");proc(mc,true,5,2f);advance();}
                case 9->{
                    if(age==8)capture(mc,"05-heart-loss.png");if(age==28)capture(mc,"06-heart-separated.png");
                    if(age==38){reversedPath();capture(mc,"07-heart-rewind.png");}if(age==52)capture(mc,"08-heart-returning.png");
                    if(age<65)return;var game=call(mc.screen,"getGame");check(!ClavisHeartRewind.active(mc.screen)&&((Number)call(game,"getHealth")).intValue()==5&&((Number)call(game,"getLootQuality")).floatValue()==2f,"the normal heart is restored after the entire native animation plays backwards");capture(mc,"09-heart-restored.png");proc(mc,false,5,2f);advance();}
                case 10->{if(age==8)capture(mc,"10-unprotected-loss.png");if(age<65)return;check(!ClavisHeartRewind.active(mc.screen)&&((Number)call(call(mc.screen,"getGame"),"getHealth")).intValue()==4,"an unprotected loss stays lost and never rewinds");mc.setScreen(new CharacterScreen());mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";pending=mc.reloadResourcePacks();advance();}
                case 11->{game(mc,"mirror","mirror.MirrorWidget");proc(mc,true,1,.12f);advance();}
                case 12->{if(age==8)capture(mc,"11-last-heart-loss.png");if(age==38){reversedPath();capture(mc,"12-last-heart-rewind.png");}if(age<65)return;check(((Number)call(call(mc.screen,"getGame"),"getHealth")).intValue()==1&&!ClavisHeartRewind.active(mc.screen),"the protected last life completes the same loss/rewind without losing the minigame");capture(mc,"13-last-heart-restored.png");advance();}
                case 13->{proc(mc,true,5,2f);var previous=mc.screen;mc.setScreen(new CharacterScreen());check(!ClavisHeartRewind.active(previous),"leaving the minigame drops temporary particle history and restores the heart display");mc.setScreen(previous);check(!ClavisHeartRewind.active(previous),"cached screen reopening does not replay a previous heart rewind");advance();}
                case 14->{mc.setScreen(null);pending=CompletableFuture.runAsync(()->dev.rpgprogression.verification.ProgressionOrbChecks.scene(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())),mc.getSingleplayerServer());advance();}
                case 15->{if(age<12||mc.player.getX()<140||mc.player.getX()>150)return;long count=java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(),false).filter(e->e instanceof dev.rpgprogression.xp.ProgressionOrb).count();if(count==0)return;check(count>0,"real synchronized RPG star entities are visible to the native client");capture(mc,"14-progression-stars.png");advance();}
                case 16->{if(age<6)return;capture(mc,"15-stars-approaching.png");beforePickupXP=ClientData.get().xp;beforePickupLevel=ClientData.get().level;pending=CompletableFuture.runAsync(()->mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).connection.teleport(144.5,221,144.5,180,13),mc.getSingleplayerServer());advance();}
                case 17->{if(age<35)return;check(ClientData.get().xp>beforePickupXP||ClientData.get().level>beforePickupLevel,"normal world ticks pick up the visible RPG stars and synchronize progression through the native absorption packet");capture(mc,"16-stars-collected.png");done=true;ClavisChecks.report(folder(mc).resolve("result.json"),checks,null);mc.stop();}

            }
        } catch(Throwable error) {
            done=true;com.mojang.logging.LogUtils.getLogger().error("CLAVIS_CLIENT failed",error);
            try{ClavisChecks.report(folder(mc).resolve("result.json"),checks,error);}catch(Exception e){throw new IllegalStateException(e);}mc.stop();
        }
    }
    private ClavisClientSmoke(){}
}
