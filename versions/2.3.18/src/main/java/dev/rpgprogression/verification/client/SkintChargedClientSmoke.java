package dev.rpgprogression.verification.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import dev.rpgprogression.block.*;
import dev.rpgprogression.client.*;
import dev.rpgprogression.client.gui.*;
import dev.rpgprogression.client.hud.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.skint.*;
import dev.rpgprogression.verification.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class SkintChargedClientSmoke {
    private static final JsonArray checks=new JsonArray();
    private static final BlockPos RU=new BlockPos(200,200,70),EN=RU.offset(16,0,0),OCEAN=new BlockPos(240,185,100);
    private static int stage,age,boot,packets;private static boolean loading,done;private static String capture;private static CompletableFuture<?> pending;
    private static Path directory(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/charged-2.3.18");}
    private static boolean enabled(){return Boolean.getBoolean("rpgprogression.skintChargedSmoke")&&!done;}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void check(boolean ok,String text){SkintTravelChecks.check(checks,ok,text);}
    private static void advance(){stage++;age=0;com.mojang.logging.LogUtils.getLogger().info("SKINT_CHARGED_STAGE {}",stage);}
    private static void server(Minecraft mc,Runnable action){pending=CompletableFuture.runAsync(action,mc.getSingleplayerServer());}
    private static void language(Minecraft mc,String code){mc.getLanguageManager().setSelected(code);mc.options.languageCode=code;pending=mc.reloadResourcePacks();}
    private static void click(Minecraft mc,BlockPos pos){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false));}
    private static void prepare(Minecraft mc,BlockPos pos){var p=player(mc);SkintManager.close(p,true);SkintActivationChecks.inactive(p.serverLevel(),pos);p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.removeAllEffects();p.getInventory().clearContent();p.serverLevel().setDayTime(6000);p.connection.teleport(pos.getX()+.5,pos.getY(),pos.getZ()+2.5,180,10);}
    private static void finish(Minecraft mc,Throwable error)throws Exception{
        var report=new JsonObject();report.addProperty("version","2.3.18");report.add("checks",checks);report.addProperty("activationPackets",packets);if(error!=null)report.addProperty("error",error.toString());
        report.addProperty("result",error==null&&checks.asList().stream().allMatch(c->c.getAsJsonObject().get("passed").getAsBoolean())?"PASSED":"FAILED");
        Files.createDirectories(directory(mc));Files.writeString(directory(mc).resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));done=true;mc.stop();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!enabled())return;var mc=Minecraft.getInstance();
        try{
            if(mc.player==null||mc.getSingleplayerServer()==null){if(++boot>6000)throw new IllegalStateException("Startup timeout");if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var previous=mc.screen;mc.createWorldOpenFlows().openWorld(System.getProperty("rpgprogression.smokeWorld"),()->mc.setScreen(previous));}return;}
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}if(mc.getOverlay()!=null)return;if(++age>400)throw new IllegalStateException("Stage timeout "+stage);
            switch(stage){
                case 0->{if(age<30)return;mc.options.guiScale().set(2);mc.options.hideGui=false;mc.resizeDisplay();mc.setScreen(null);var previous=ModNetworking.clientSkintCharged;ModNetworking.clientSkintCharged=p->{packets++;previous.accept(p);};server(mc,()->prepare(mc,RU));advance();}
                case 1->{if(age<35)return;check(!SkintChargedHud.active()&&packets==0,"login and loaded active points do not trigger an activation banner");language(mc,"ru_ru");advance();}
                case 2->{if(age<10)return;click(mc,RU);advance();}
                case 3->{if(age<6)return;check(!mc.level.getBlockState(RU).getValue(TravelSkintBlock.ACTIVE)&&!SkintChargedHud.active()&&packets==0,"the first right-click starts the clip without an early completion notification");capture="01-activating.png";advance();}
                case 4->{if(SkintChargedHud.opacity(0)<1||packets!=1||!mc.level.getBlockState(RU).getValue(TravelSkintBlock.ACTIVE))return;check(!LevelUpHud.active()&&mc.screen==null,"real completion shows the Russian skint banner without a fake level gain or opening the hub");capture="02-charged-ru.png";advance();}
                case 5->{if(capture!=null||age<5)return;click(mc,RU);advance();}
                case 6->{if(!(mc.screen instanceof SkintScreen)||age<35)return;check(packets==1,"opening an already active skint never repeats its charge notification");var hub=mc.screen;hub.resize(mc,1000,360);check(hub.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.AbstractWidget).allMatch(w->((net.minecraft.client.gui.components.AbstractWidget)w).getX()==16),"all main-menu widgets stay aligned to the left in a wide viewport");hub.resize(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());capture="03-left-aligned-hub.png";advance();}
                case 7->{if(capture!=null||age<5)return;mc.screen.onClose();advance();}
                case 8->{if(age<90)return;check(!SkintChargedHud.active(),"the activation notification fades away on the same timeline as a level-up banner");language(mc,"en_us");advance();}
                case 9->{server(mc,()->prepare(mc,EN));advance();}
                case 10->{if(age<35)return;click(mc,EN);advance();}
                case 11->{if(SkintChargedHud.opacity(0)<1||packets!=2)return;capture="04-charged-en.png";advance();}
                case 12->{if(capture!=null||age<3)return;mc.options.hideGui=true;check(SkintChargedHud.active(),"F1 hides rendering without restarting or discarding the activation timeline");capture="05-f1-hidden.png";advance();}
                case 13->{if(capture!=null||age<5)return;mc.options.hideGui=false;advance();}
                case 14->{if(age<90)return;check(!SkintChargedHud.active()&&packets==2,"both real activations produce exactly one packet and eventually disappear");server(mc,()->{
                    var p=player(mc);var level=p.serverLevel();Skint2318Checks.wetPoint(level,OCEAN);var origin=OCEAN.offset(-3,-1,-3);
                    var template=level.getStructureManager().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("rpgprogression","skint_ocean")).orElseThrow();template.placeInWorld(level,origin,origin,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setIgnoreEntities(true),level.random,2);
                    level.setBlock(OCEAN,level.getBlockState(OCEAN).setValue(TravelSkintBlock.ACTIVE,true),3);SkintNetwork.get(level).ensure(level,OCEAN);p.connection.teleport(OCEAN.getX()+.5,OCEAN.getY(),OCEAN.getZ()+2.5,180,10);p.setAirSupply(p.getMaxAirSupply());ModNetworking.sync(p);
                });advance();}
                case 15->{if(age<35)return;click(mc,OCEAN);advance();}
                case 16->{if(age<10)return;check(!ClientSkintData.active()&&mc.screen==null&&!SkintChargedHud.active()&&packets==2,"an underwater hub rejects a player without breathing; snapshots and programmatic active states stay silent");server(mc,()->player(mc).addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,1200)));advance();}
                case 17->{if(age<5)return;if(!(mc.screen instanceof SkintScreen)){if(age%10==0)click(mc,OCEAN);return;}if(age<50)return;check(ClientSkintData.allowed()&&mc.player.hasEffect(MobEffects.WATER_BREATHING),"real water-breathing packets permit the ocean skint's seated main menu");capture="06-ocean-hub.png";advance();}
                case 18->{if(capture!=null||age<8)return;mc.screen.onClose();advance();}
                case 19->{if(age<45)return;server(mc,()->{var p=player(mc);p.removeEffect(MobEffects.WATER_BREATHING);p.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER,1200));});advance();}
                case 20->{if(age<5)return;if(!(mc.screen instanceof SkintScreen)){if(age%10==0)click(mc,OCEAN);return;}check(ClientSkintData.allowed()&&mc.player.hasEffect(MobEffects.CONDUIT_POWER),"real conduit power independently permits the underwater main menu");server(mc,()->player(mc).removeEffect(MobEffects.CONDUIT_POWER));advance();}
                case 21->{if(age<15)return;check(!ClientSkintData.active()&&mc.screen==null,"removing the final breathing effect closes the real underwater menu");server(mc,()->{var p=player(mc);p.connection.teleport(RU.getX()+.5,RU.getY(),RU.getZ()+2.5,180,10);var d=ProgressionManager.get(p);ProgressionManager.addXPAdministrative(p,d.requiredXP()-d.xp);});advance();}
                case 22->{if(age<10||LevelUpHud.opacity(0)<1)return;check(LevelUpHud.active()&&!SkintChargedHud.active()&&packets==2,"a real RPG level increase still uses its original banner after the shared-renderer change");capture="07-level-up-regression.png";advance();}
                case 23->{if(capture!=null||age<8)return;ClientData.reset();check(!SkintChargedHud.active()&&!LevelUpHud.active(),"disconnect cleanup clears both independent notification states");finish(mc,null);}
            }
        }catch(Throwable error){com.mojang.logging.LogUtils.getLogger().error("SKINT_CHARGED_CLIENT failed",error);try{finish(mc,error);}catch(Exception write){throw new RuntimeException(write);}}
    }
    private static void pixels(Minecraft mc,NativeImage image)throws Exception{
        boolean russian=mc.getLanguageManager().getSelected().startsWith("ru_");String name=capture.equals("07-level-up-regression.png")?"level_up": "skint_charged";int w=name.equals("level_up")?(russian?254:116):(russian?388:192);
        int guiW=mc.getWindow().getGuiScaledWidth(),guiH=mc.getWindow().getGuiScaledHeight();float scale=Math.min(1.25f,Math.max(1,guiW-32)/(float)w);int drawW=Math.round(w*scale),drawH=Math.round(21*scale),left=(guiW-drawW)/2,top=guiH/2-drawH/2,probes=0,matches=0;
        try(var input=mc.getResourceManager().open(GuiTextures.texture(name+(russian?"_ru":"_en")));var source=NativeImage.read(input)){
            for(int x=2;x<w-2;x+=7){int y=-1;for(int row=2;row<19;row++)if((source.getPixelRGBA(x,row)>>>24)==255){y=row;break;}if(y<0)continue;
                int px=(int)((left+(x+.5)*drawW/w)*image.getWidth()/guiW),py=(int)((top+(y+.5)*drawH/21)*image.getHeight()/guiH),color=source.getPixelRGBA(x,y);boolean match=false;
                for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++)if(image.getPixelRGBA(px+dx,py+dy)==color)match=true;probes++;if(match)matches++;
            }
        }
        check(probes>10&&(mc.options.hideGui?matches<probes/4:matches==probes),"native framebuffer "+(mc.options.hideGui?"hides":"matches")+" the supplied "+name+" artwork: "+matches+"/"+probes);
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
        if(!enabled()||capture==null)return;var mc=Minecraft.getInstance();try{Files.createDirectories(directory(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){if(capture.contains("charged-")||capture.contains("f1-")||capture.contains("level-up-"))pixels(mc,image);image.writeToFile(directory(mc).resolve(capture));}capture=null;}catch(Exception error){try{finish(mc,error);}catch(Exception write){throw new RuntimeException(write);}}
    }
    private SkintChargedClientSmoke(){}
}
