package dev.rpgprogression.verification.client;

import com.google.gson.*;
import dev.rpgprogression.block.*;
import dev.rpgprogression.verification.TravelSkintChecks;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.model.data.ModelData;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class SkintClientSmoke {
    private static int boot,ticks;
    private static boolean loading,creating,finished,failed;
    private static BlockPos center;
    private static CompletableFuture<?> pending,reload;
    private static final JsonArray checks=new JsonArray();
    private static final JsonObject captures=new JsonObject();
    private static final List<String> shots=new ArrayList<>();
    private static boolean enabled(){return Boolean.getBoolean("rpgprogression.skintSmoke");}
    private static Path directory(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/skint-2.2.4/all");}
    private static Object field(Object obj,String name)throws Exception{var f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
    private static void invoke(Object obj,String name,Class<?>[] types,Object...args)throws Exception{var method=obj.getClass().getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(obj,args);}
    private static void check(boolean passed,String name){var row=new JsonObject();row.addProperty("check",name);row.addProperty("passed",passed);checks.add(row);if(!passed)failed=true;com.mojang.logging.LogUtils.getLogger().info("SKINT_CLIENT {} {}",passed?"PASSED":"FAILED",name);}
    private static void server(Minecraft mc,Runnable action){pending=CompletableFuture.runAsync(action,mc.getSingleplayerServer());}
    private static void finish(Minecraft mc){
        if(finished)return;finished=true;
        try{
            var report=new JsonObject();report.addProperty("version","2.2.4");report.addProperty("result",failed?"FAILED":"PASSED");report.addProperty("world",System.getProperty("rpgprogression.smokeWorld"));report.add("checks",checks);report.add("screenshots",captures);
            var mods=new JsonArray();for(var mod:ModList.get().getMods()){var row=new JsonObject();row.addProperty("id",mod.getModId());row.addProperty("version",mod.getVersion().toString());mods.add(row);}report.add("loadedMods",mods);
            Files.createDirectories(directory(mc));Files.writeString(directory(mc).resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception error){failed=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_CLIENT report failed",error);}
        com.mojang.logging.LogUtils.getLogger().info("SKINT_CLIENT_RESULT {}",failed?"FAILED":"PASSED");mc.stop();
    }
    private static void flow(Minecraft mc)throws Exception{
        if(++boot>6000)throw new IllegalStateException("World startup timed out");
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;creating=true;mc.options.pauseOnLostFocus=false;CreateWorldScreen.openFresh(mc,mc.screen);return;}
        if(creating&&mc.screen instanceof CreateWorldScreen){var state=field(mc.screen,"uiState");invoke(state,"setName",new Class[]{String.class},System.getProperty("rpgprogression.smokeWorld","HeartSkint224-A"));creating=false;invoke(mc.screen,"onCreate",new Class[]{});return;}
        if(loading&&mc.screen instanceof ConfirmScreen)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString())){b.onPress();break;}
    }
    private static void camera(Minecraft mc,boolean back){
        mc.player.setYRot(back?-35:145);mc.player.yRotO=mc.player.getYRot();mc.player.setXRot(15);mc.player.xRotO=15;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!enabled()||finished)return;var mc=Minecraft.getInstance();
        try{
            if(mc.player==null||mc.getSingleplayerServer()==null){flow(mc);return;}
            if(reload!=null){if(!reload.isDone())return;reload.join();reload=null;}
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(mc.getOverlay()!=null)return;if(mc.isPaused()){mc.setScreen(null);return;}
            ticks++;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
            if(ticks==1){mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.languageCode="ru_ru";mc.getLanguageManager().setSelected("ru_ru");reload=mc.reloadResourcePacks();}
            if(ticks==20)server(mc,()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var level=p.serverLevel();center=new BlockPos(p.getBlockX(),Math.max(90,p.getBlockY()+10),p.getBlockZ());
                var nativeChecks=TravelSkintChecks.run(level,p,center.offset(32,0,0));synchronized(checks){for(var row:nativeChecks){checks.add(row);if(!row.getAsJsonObject().get("passed").getAsBoolean())failed=true;}}
                p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.getAbilities().invulnerable=true;p.getInventory().clearContent();p.removeAllEffects();
                for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=-1;y<=5;y++)level.setBlock(center.offset(x,y,z),y==-1?Blocks.SMOOTH_SANDSTONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
                level.setBlock(center,ModBlocks.TRAVELSKINT.get().defaultBlockState().setValue(dev.rpgprogression.block.TravelSkintBlock.ACTIVE,true),3);level.setDayTime(6000);level.setWeatherParameters(10000,0,false,false);
                p.connection.teleport(center.getX()+3,center.getY()+.6,center.getZ()+4,145,15);
            });
            if(ticks==50){
                for(var direction:Direction.Plane.HORIZONTAL){
                    var state=ModBlocks.TRAVELSKINT.get().defaultBlockState().setValue(dev.rpgprogression.block.TravelSkintBlock.ACTIVE,true).setValue(TravelSkintBlock.FACING,direction);
                    var model=mc.getBlockRenderer().getBlockModel(state);var quads=model.getQuads(state,null,RandomSource.create(0),ModelData.EMPTY,null);
                    check(quads.size()==44,"native baked model preserves 44 non-degenerate faces: "+direction);
                    check(quads.stream().noneMatch(q->q.getSprite().contents().name().getPath().equals("missingno")),"all model materials resolve to real textures: "+direction);
                }
                var itemModel=mc.getItemRenderer().getModel(new ItemStack(ModBlocks.TRAVELSKINT_ITEM.get()),mc.level,mc.player,0);
                check(!itemModel.getQuads(null,null,RandomSource.create(0)).isEmpty(),"creative item has a real baked model");
                check(new ItemStack(ModBlocks.TRAVELSKINT_ITEM.get()).getHoverName().getString().equals("Путеводный скинт"),"the Russian block name matches the requested name");
                mc.options.hideGui=true;camera(mc,false);
            }
            if(ticks==80)shots.add("01-travelskint-front.png");
            if(ticks==90)server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.connection.teleport(center.getX()-2,center.getY()+.6,center.getZ()-3,-35,15);});
            if(ticks==105)camera(mc,true);
            if(ticks==125)shots.add("02-travelskint-back.png");
            if(ticks==140){
                mc.options.hideGui=false;var selected=CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");selected.setAccessible(true);selected.set(null,ModBlocks.MAIN_TAB.get());
                mc.setScreen(new CreativeModeInventoryScreen(mc.player,mc.level.enabledFeatures(),true));
                check(ModBlocks.MAIN_TAB.get().getDisplayItems().stream().anyMatch(s->s.is(ModBlocks.TRAVELSKINT_ITEM.get())),"native creative screen contains the skint on the mod tab");
                var screen=mc.screen;var slot=((CreativeModeInventoryScreen)screen).getMenu().slots.getFirst();
                double x=(screen.width-195)/2.0+slot.x+8,y=(screen.height-136)/2.0+slot.y+8;
                for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,(name.equals("xpos")?x:y)*mc.getWindow().getGuiScale());}
            }
            if(ticks==165)shots.add("03-creative-tab.png");
            if(ticks==180){mc.screen.onClose();mc.options.hideGui=true;server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.connection.teleport(center.getX()+3,center.getY()+.6,center.getZ()+4,145,15);});}
            if(ticks==195)camera(mc,false);
            if(ticks==210)shots.add("04-before-support-loss.png");
            if(ticks==225)server(mc,()->mc.getSingleplayerServer().overworld().setBlock(center.below(),Blocks.AIR.defaultBlockState(),3));
            if(ticks==245){check(mc.level.getBlockState(center).isAir(),"support loss is synchronized to the native client");shots.add("05-after-support-loss.png");}
            if(ticks>=260&&shots.isEmpty()){check(captures.size()==5,"five native framebuffer captures completed");mc.options.hideGui=false;finish(mc);}
        }catch(Throwable error){failed=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_CLIENT failure at tick "+ticks,error);finish(mc);}
    }
    @SubscribeEvent public static void render(RenderFrameEvent.Post event){
        if(!enabled()||finished||shots.isEmpty())return;var mc=Minecraft.getInstance();var batch=List.copyOf(shots);shots.clear();
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            Files.createDirectories(directory(mc));for(String name:batch){pixels.writeToFile(directory(mc).resolve(name));var state=new JsonObject();state.addProperty("tick",ticks);state.addProperty("screen",mc.screen==null?"World":mc.screen.getClass().getSimpleName());state.addProperty("language",mc.getLanguageManager().getSelected());state.addProperty("blockPresent",center!=null&&mc.level.getBlockState(center).is(ModBlocks.TRAVELSKINT.get()));captures.add(name,state);com.mojang.logging.LogUtils.getLogger().info("SKINT_CLIENT captured {}",name);}
        }catch(Exception error){failed=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_CLIENT capture failed",error);}
    }
    private SkintClientSmoke(){}
}
