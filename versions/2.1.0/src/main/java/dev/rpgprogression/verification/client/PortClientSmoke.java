package dev.rpgprogression.verification.client;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.*;
import dev.rpgprogression.client.gui.*;
import dev.rpgprogression.client.hud.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;
import java.util.*;

/** Opt-in native 1.21.1 client verification. The verification package is excluded from release JARs. */
@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class PortClientSmoke {
    private record PreviewPose(float yaw,float pitch,float head,float body) {}
    private static boolean loading,creating,createdFresh,finished;
    private static volatile boolean failed;
    private static int bootTicks,ticks,reloginCatalogReceivedTick;
    private static String profile,learnedId;
    private static double cursorX,cursorY;
    private static PreviewPose preview;
    private static Horse horse;
    private static BlockPos arena;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static final List<String> captures=new ArrayList<>();
    private static final JsonObject states=new JsonObject();
    private static final JsonArray checks=new JsonArray();
    private static final Map<String,PreviewPose> previews=new LinkedHashMap<>();
    private static final Map<String,Boolean> canceledLayers=new LinkedHashMap<>();
    private static final long CAPTURE_ORIGIN=System.nanoTime();
    private static boolean enabled(){return Boolean.getBoolean("rpgprogression.portSmoke")||Boolean.getBoolean("rpgprogression.portRelogin");}
    private static boolean relogin(){return Boolean.getBoolean("rpgprogression.portRelogin");}
    private static String world(){return System.getProperty("rpgprogression.smokeWorld","HeartNeo210-standalone");}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void check(boolean value,String message){
        if(!value)failed=true;var state=new JsonObject();state.addProperty("passed",value);state.addProperty("check",message);checks.add(state);
        LogUtils.getLogger().info("HEART_PORT_CLIENT {} {}",value?"verified":"FAILED",message);
    }
    private static Object field(Object instance,String name){
        try{var field=instance.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(instance);}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static void invoke(Object instance,String name,Class<?>[] types,Object...arguments){
        try{var method=instance.getClass().getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(instance,arguments);}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static SkillInfoTooltip info(Minecraft mc){return mc.screen instanceof SkillTreeScreen?(SkillInfoTooltip)field(mc.screen,"info"):null;}
    private static String pinned(Minecraft mc){return mc.screen instanceof SkillTreeScreen?(String)field(mc.screen,"pinnedId"):"";}
    private static String described(Minecraft mc){return mc.screen instanceof SkillTreeScreen?(String)field(mc.screen,"hoverId"):"";}
    private static double scale(Screen screen){return Math.min(1.5,Math.min(screen.width/640.0,screen.height/360.0));}
    private static double sx(Screen screen,double x){return(screen.width-640*scale(screen))/2+x*scale(screen);}
    private static double sy(Screen screen,double y){return(screen.height-360*scale(screen))/2+y*scale(screen);}
    private static void cursor(Minecraft mc,double x,double y){
        cursorX=x;cursorY=y;var screen=mc.screen;
        try{for(String name:new String[]{"xpos","ypos"}){var field=MouseHandler.class.getDeclaredField(name);field.setAccessible(true);
            field.setDouble(mc.mouseHandler,(name.equals("xpos")?sx(screen,x):sy(screen,y))*mc.getWindow().getGuiScale());}}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static void away(Minecraft mc){cursor(mc,638,358);}
    private static void cursorGui(Minecraft mc,double x,double y){
        try{for(String name:new String[]{"xpos","ypos"}){var field=MouseHandler.class.getDeclaredField(name);field.setAccessible(true);field.setDouble(mc.mouseHandler,(name.equals("xpos")?x:y)*mc.getWindow().getGuiScale());}}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static void hover(Minecraft mc,String id){var p=((SkillTreeScreen)mc.screen).skillPosition(id);cursor(mc,p[0],p[1]);}
    private static void click(Minecraft mc,double x,double y){var screen=mc.screen;screen.mouseClicked(sx(screen,x),sy(screen,y),0);screen.mouseReleased(sx(screen,x),sy(screen,y),0);}
    private static void clickNode(Minecraft mc,String id){var p=((SkillTreeScreen)mc.screen).skillPosition(id);click(mc,p[0],p[1]);}
    private static void tree(Minecraft mc,String id){mc.setScreen(new SkillTreeScreen(new CharacterScreen()));if(id!=null)((SkillTreeScreen)mc.screen).focusSkill(id);away(mc);}
    private static void emptyClick(Minecraft mc){
        try{var method=SkillTreeScreen.class.getDeclaredMethod("hit",double.class,double.class);method.setAccessible(true);
            for(int y=76;y<313;y+=8)for(int x=140;x<501;x+=8)if(((String)method.invoke(mc.screen,(double)x,(double)y)).isEmpty()&&(info(mc)==null||!info(mc).contains(x,y))){click(mc,x,y);away(mc);return;}}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}throw new IllegalStateException("No empty viewport location");
    }
    private static void server(Minecraft mc,Runnable action){
        mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable error){failed=true;LogUtils.getLogger().error("HEART_PORT_CLIENT server failure",error);}});
    }
    private static void shot(String name){captures.add(name+".png");}
    private static void frame(String clip){
        String prefix="frames/"+clip+"/";
        // If several native ticks occur before a render, save that framebuffer
        // once rather than writing multiple copies as imaginary extra frames.
        captures.removeIf(name->name.startsWith(prefix));shot(prefix+String.format(Locale.ROOT,"%04d",ticks));
    }
    private static void interpolateCursor(Minecraft mc,int start,int end,double x1,double y1,double x2,double y2){
        if(ticks>=start&&ticks<=end){double t=(ticks-start)/(double)(end-start);cursor(mc,x1+(x2-x1)*t,y1+(y2-y1)*t);}
    }
    private static Path directory(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/port-2.1.0").resolve(profile==null?"boot":profile);}
    private static void language(Minecraft mc,String code){mc.getLanguageManager().setSelected(code);mc.options.languageCode=code;reload=mc.reloadResourcePacks();}
    private static void report(Minecraft mc)throws Exception{
        Files.createDirectories(directory(mc));var report=new JsonObject();report.addProperty("result",failed?"FAILED":"PASSED");report.addProperty("profile",profile);
        report.addProperty("world",world());report.addProperty("createdFreshMinecraft1211World",createdFresh);report.addProperty("relogin",relogin());
        report.addProperty("completed",finished);
        if(relogin()){report.addProperty("catalogReady",ClientSkillTree.ready);report.addProperty("catalogReceivedTick",reloginCatalogReceivedTick);}
        report.addProperty("renderModsRequested",Boolean.getBoolean("rpgprogression.portRenderMods"));
        var mods=new JsonArray();for(var mod:ModList.get().getMods()){var entry=new JsonObject();entry.addProperty("id",mod.getModId());entry.addProperty("version",mod.getVersion().toString());mods.add(entry);}
        report.add("loadedMods",mods);report.add("checks",checks);report.add("screenshots",states);
        Files.writeString(directory(mc).resolve(relogin()?"relogin-result.json":"capture-states.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
    private static void finish(Minecraft mc){
        if(finished)return;finished=true;try{report(mc);}catch(Exception error){failed=true;LogUtils.getLogger().error("HEART_PORT_CLIENT report failure",error);}
        LogUtils.getLogger().info("HEART_PORT_CLIENT_RESULT {} {}",failed?"FAILED":"PASSED",profile);mc.stop();
    }
    private static void prepare(Minecraft mc){
        var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.stopRiding();p.removeAllEffects();p.getInventory().clearContent();p.setNoGravity(true);
        arena=new BlockPos(p.getBlockX(),Math.max(80,p.getBlockY()+8),p.getBlockZ());
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=-1;y<=8;y++)p.serverLevel().setBlock(arena.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        p.teleportTo(arena.getX()+.5,arena.getY(),arena.getZ()+.5);p.setYRot(180);p.setXRot(0);p.serverLevel().setDayTime(6000);
        var data=ProgressionManager.get(p);data.load(new PlayerProgression().save());data.initialize();data.level=25;data.xp=0;data.points=3;data.skillMilestones=5;data.skillPoints=4;
        data.regenCooldown=10000;data.stamina=data.maxStamina();ProgressionManager.refresh(p,10,false);p.setHealth(p.getMaxHealth());SkillTreeManager.sync(p);
    }
    private static void tooltip(Minecraft mc,Item item,String...expected){
        var lines=new ItemStack(item).getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.NORMAL);
        for(var key:expected)check(lines.stream().anyMatch(line->line.getContents() instanceof TranslatableContents text&&text.getKey().equals(key)),"native "+item+" tooltip includes "+key);
    }
    private static void manaScroll(Minecraft mc){
        try{
            var p=player(mc);var spell=Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry").getField("FIREBOLT_SPELL").get(null);
            var value=((java.util.function.Supplier<?>)spell).get();var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks","scroll")));
            Class.forName("io.redspace.ironsspellbooks.api.spells.ISpellContainer").getMethod("createScrollContainer",Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell"),int.class,ItemStack.class).invoke(null,value,1,stack);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);var data=ProgressionManager.get(p);
            data.set(RPGAttribute.SPARK_POTENTIAL,dev.rpgprogression.config.RPGConfig.SPARK_START.get()+
                    dev.rpgprogression.resource.ResourceMath.POTENTIAL_PER_MANA*dev.rpgprogression.resource.ResourceMath.MAX_MANA);
            ProgressionManager.refresh(p,10,false);data.mana=4;data.manaRegenCooldown=10000;ModNetworking.resources(p);
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static void worldFlow(Minecraft mc){
        if(++bootTicks>6000)throw new IllegalStateException("Native world creation/loading timed out");
        if(!loading&&mc.screen instanceof TitleScreen){
            loading=true;mc.options.pauseOnLostFocus=false;
            if(Files.isDirectory(mc.gameDirectory.toPath().resolve("saves").resolve(world()))){var back=mc.screen;mc.createWorldOpenFlows().openWorld(world(),()->mc.setScreen(back));}
            else{if(relogin())throw new IllegalStateException("Relogin requires an existing native 1.21.1 test world");creating=true;CreateWorldScreen.openFresh(mc,mc.screen);}
            return;
        }
        if(creating&&mc.screen instanceof CreateWorldScreen){
            var state=field(mc.screen,"uiState");invoke(state,"setName",new Class<?>[]{String.class},world());createdFresh=true;creating=false;
            invoke(mc.screen,"onCreate",new Class<?>[]{});return;
        }
        if(loading&&mc.player==null&&mc.screen!=null){
            String label=mc.screen instanceof BackupConfirmScreen?Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button button&&button.getMessage().getString().equals(label)){button.onPress();break;}
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!enabled()||finished)return;var mc=Minecraft.getInstance();
        try{
            if(mc.player==null||mc.getSingleplayerServer()==null){worldFlow(mc);return;}
            if(reload!=null){if(!reload.isDone())return;reload.join();reload=null;}if(mc.getOverlay()!=null)return;
            if(mc.isPaused()){mc.setScreen(null);return;}
            ticks++;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
            boolean parcool=ModList.get().isLoaded("parcool"),epic=ModList.get().isLoaded("epicfight"),irons=ModList.get().isLoaded("irons_spellbooks");
            if(ticks==1)profile=parcool&&epic&&irons?"all":!parcool&&!epic&&!irons?"standalone":"partial";
            if(relogin()){
                if(ticks==40){var d=ClientData.get();check(d.level==42&&d.xp==123&&d.points==2&&d.get(RPGAttribute.STRENGTH)==16,"progression persists after a full native client/server restart");
                    check(!CenteredHud.experienceVisible()&&!LevelUpHud.active(),"login snapshots do not trigger XP or level-up notifications");ModNetworking.CHANNEL.sendToServer(new ModNetworking.RequestTree());
                    server(mc,()->{var p=player(mc);LogUtils.getLogger().info("HEART_PORT_CLIENT relogin server player alive={} removed={} health={} tick={} lastTreeRequest={}",p.isAlive(),p.isRemoved(),p.getHealth(),p.tickCount,ProgressionManager.get(p).lastTreeRequestTick);});}
                // Opening the real menu issues its normal request as well. A
                // loaded integrated server can still be scheduling startup
                // work, so await the response instead of imposing a one-second
                // deadline in client ticks.
                if(ticks==60){tree(mc,null);LogUtils.getLogger().info("HEART_PORT_CLIENT relogin opened actual skill tree; awaiting native catalog");}
                if(ticks>=60&&ClientSkillTree.ready&&mc.screen instanceof SkillTreeScreen&&((List<?>)field(mc.screen,"nodes")).size()==ClientSkillTree.nodes.size()){
                    reloginCatalogReceivedTick=ticks;check(true,"skill catalog returns after relogin");
                    check(!parcool||ClientSkillTree.nodes.stream().anyMatch(node->node.branch()==1),"relogin catalog contains native ParCool skills");
                    check(!epic||ClientSkillTree.nodes.stream().anyMatch(node->node.branch()==2),"relogin catalog contains native Epic Fight skills");
                    if(parcool)check(ClientData.get().unlockedSkills.contains("parcool:crawl"),"learned ParCool skill persists after relogin");
                    LogUtils.getLogger().info("HEART_PORT_CLIENT relogin catalog received at tick {} with {} nodes",ticks,ClientSkillTree.nodes.size());finish(mc);
                }else if(ticks>=300){check(false,"skill catalog returns after relogin within the bounded native startup wait");finish(mc);}return;
            }
            if(ticks==20){
                check(profile.equals(world().matches(".*-all(?:-.*)?")?"all":"standalone"),"actual optional-mod profile matches the isolated test world");
                if(profile.equals("all")){
                    check(ModList.get().isLoaded("epicparcool"),"official Epic x ParCool bridge is actually loaded");
                    check(dev.rpgprogression.compat.ParCoolCompat.present()&&dev.rpgprogression.compat.EpicFightCompat.present(),"exact supported ParCool3 and Epic Fight versions are loaded");
                }
                if(Boolean.getBoolean("rpgprogression.portRenderMods"))check(ModList.get().isLoaded("sodium")&&ModList.get().isLoaded("iris"),"Sodium and Iris are loaded in the requested rendering profile");
                mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.bobView().set(false);language(mc,"ru_ru");server(mc,()->prepare(mc));
            }
            if(ticks==50){check(ClientData.get().initialized&&ClientData.get().level==25&&ClientData.get().skillPoints==4,"server progression attachment synchronizes to the client");
                check(!CenteredHud.experienceVisible()&&!LevelUpHud.active(),"initial snapshots keep transient XP and level-up overlays hidden");mc.setScreen(new CharacterScreen());cursor(mc,35,143);}
            if(ticks==70){check(preview!=null,"native player rendering exposes the preview pose");shot("01-character-left");}
            if(ticks==75)cursor(mc,620,143);
            interpolateCursor(mc,75,89,35,143,620,143);
            if(ticks==90)shot("02-character-right");
            if(ticks==95)cursor(mc,114,36);
            interpolateCursor(mc,95,109,620,143,114,36);
            if(ticks==110)shot("03-character-up");
            if(ticks==115)cursor(mc,114,333);
            interpolateCursor(mc,115,129,114,36,114,333);
            if(ticks==130)shot("04-character-down");
            if(ticks==135)click(mc,574,89);
            if(ticks==140){mc.options.guiScale().set(3);mc.resizeDisplay();cursor(mc,620,143);}
            if(ticks==150)shot("04b-character-scale3");
            if(ticks==155){check(ClientData.get().get(RPGAttribute.STRENGTH)==11&&ClientData.get().points==2,"artwork attribute button spends exactly one synchronized point");ModNetworking.CHANNEL.sendToServer(new ModNetworking.RequestTree());}
            if(ticks==160){mc.options.guiScale().set(2);mc.resizeDisplay();}
            if(ticks==170){tree(mc,null);}
            if(ticks==195){
                check(ClientSkillTree.ready,"server catalog received");
                check(mc.screen.children().stream().noneMatch(widget->widget instanceof SkillTabButton),"unified skill screen has no provider tabs");
                var shown=(List<?>)field(mc.screen,"nodes");check(shown.size()==ClientSkillTree.nodes.size(),"one viewport contains the complete installed skill catalog");
                check(!parcool||ClientSkillTree.nodes.stream().anyMatch(n->n.branch()==1),"native ParCool skills remain available");
                check(ClientSkillTree.nodes.stream().noneMatch(n->SkillDescriptions.name(n).getString().startsWith("rpgprogression.skills.name.")),"skill names resolve to localized text");
                check(!epic||ClientSkillTree.nodes.stream().anyMatch(n->n.branch()==2),"native Epic Fight skills remain available");
                check(mc.getResourceManager().getResource(GuiTextures.texture("icons/placeholder")).isPresent(),"supplied placeholder texture loads in NeoForge");
                mc.screen.keyPressed(268,0,0);away(mc);shot("05-unified-tree-overview");
                learnedId=ClientSkillTree.nodes.stream().filter(node->SkillTreeManager.canUnlock(ClientData.get(),node)).map(SkillNode::id).findFirst().orElse(null);
            }
            if(ticks==205&&learnedId!=null){((SkillTreeScreen)mc.screen).focusSkill(learnedId);hover(mc,learnedId);}
            if(ticks==220&&learnedId!=null){check(info(mc)!=null&&pinned(mc).isEmpty(),"hover shows an unpinned description");shot("06-hover-description");}
            if(ticks==225&&learnedId!=null)away(mc);
            if(ticks==240&&learnedId!=null){check(info(mc)==null&&pinned(mc).isEmpty(),"leaving an unpinned icon hides its description");shot("07-hover-left");}
            if(ticks==245&&learnedId!=null){hover(mc,learnedId);clickNode(mc,learnedId);}
            if(ticks==265&&learnedId!=null)away(mc);
            if(ticks==280&&learnedId!=null){check(ClientData.get().unlockedSkills.contains(learnedId)&&ClientData.get().skillPoints==3&&pinned(mc).equals(learnedId)&&info(mc)!=null,"one click learns for one point and pins after leaving");shot("08-click-pinned");server(mc,()->SkillTreeManager.sync(player(mc)));}
            if(ticks==305&&learnedId!=null){check(pinned(mc).equals(learnedId)&&info(mc)!=null,"server refresh retains the pin");var panel=info(mc);var before=((SkillTreeScreen)mc.screen).skillPosition(learnedId);
                check(mc.screen.mouseScrolled(sx(mc.screen,panel.x()+40),sy(mc.screen,panel.y()+90),0,-1)&&Arrays.equals(before,((SkillTreeScreen)mc.screen).skillPosition(learnedId)),"pinned panel owns wheel scrolling without zooming the tree");}
            if(ticks==310&&learnedId!=null){mc.options.guiScale().set(3);mc.resizeDisplay();away(mc);}
            if(ticks==325&&learnedId!=null){check(pinned(mc).equals(learnedId)&&info(mc)!=null,"GUI scale changes preserve the pin");shot("09-scale3-pinned");}
            if(ticks==330){mc.options.guiScale().set(2);mc.resizeDisplay();if(learnedId!=null)emptyClick(mc);}
            if(ticks==345&&learnedId!=null)check(info(mc)==null&&pinned(mc).isEmpty(),"empty click dismisses the pin");
            if(ticks==350&&epic){var id=ClientSkillTree.nodes.stream().filter(n->n.branch()==2).map(SkillNode::id).findFirst().orElseThrow();((SkillTreeScreen)mc.screen).focusSkill(id);hover(mc,id);clickNode(mc,id);away(mc);}
            if(ticks==370&&epic){check(info(mc)!=null&&pinned(mc).startsWith("epicfight:"),"Epic descriptions are accessible in the same screen without switching tabs");shot("10-epic-same-tree");}
            if(ticks==375){mc.screen.onClose();check(mc.screen instanceof CharacterScreen,"Escape returns to the character menu");cursor(mc,114,143);}
            if(ticks==395){shot("11-character-centered");server(mc,()->player(mc).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD)));}
            if(ticks==400){mc.setScreen(new InventoryScreen(mc.player));}
            if(ticks==405){var inventory=(InventoryScreen)mc.screen;cursorGui(mc,inventory.getGuiLeft()+8+8,inventory.getGuiTop()+142+8);}
            if(ticks==420)shot("12-inventory-locked");
            if(ticks==425){mc.setScreen(null);server(mc,()->{var p=player(mc);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));});}
            if(ticks==445){check(!CenteredHud.experienceVisible(),"native XP remains hidden while experience is unchanged");
                tooltip(mc,Items.IRON_SWORD,"rpgprogression.item.stamina.hit");tooltip(mc,Items.BOW,"rpgprogression.item.stamina.shot");
                tooltip(mc,Items.CROSSBOW,"rpgprogression.item.stamina.charge");tooltip(mc,Items.TRIDENT,"rpgprogression.item.stamina.hit","rpgprogression.item.stamina.throw");shot("13-hud-idle");}
            if(ticks==450)server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);d.stamina=d.maxStamina()*.55;d.regenCooldown=10000;ModNetworking.stamina(p);p.giveExperiencePoints(5);});
            if(ticks==470){check(CenteredHud.staminaVisible()&&CenteredHud.experienceVisible(),"spent stamina and changed vanilla XP appear");check(Boolean.FALSE.equals(canceledLayers.get("minecraft:experience_bar"))&&Boolean.FALSE.equals(canceledLayers.get("minecraft:experience_level")),"native XP bar and level layers are both visible during the change window");shot("14-hud-xp-stamina");}
            if(ticks==475)server(mc,()->{var p=player(mc);horse=EntityType.HORSE.create(p.serverLevel());if(horse==null)throw new IllegalStateException("Native horse creation failed");horse.setPos(arena.getX()+.5,arena.getY(),arena.getZ()+.5);horse.setNoAi(true);horse.setNoGravity(true);horse.setInvulnerable(true);horse.setTamed(true);horse.setOwnerUUID(p.getUUID());p.serverLevel().addFreshEntity(horse);horse.equipSaddle(new ItemStack(Items.SADDLE),net.minecraft.sounds.SoundSource.PLAYERS);horse.setHealth(horse.getMaxHealth()*.65f);p.startRiding(horse,true);p.giveExperiencePoints(5);});
            if(ticks==500){check(CenteredHud.riddenAnimal()!=null&&mc.player.jumpableVehicle()!=null,"saddled animal health uses the centered supplied meter");check(Boolean.TRUE.equals(canceledLayers.get("minecraft:jump_meter"))&&Boolean.TRUE.equals(canceledLayers.get("minecraft:vehicle_health")),"native horse jump and vehicle-heart indicators stay suppressed");check(CenteredHud.experienceVisible()&&mc.player.experienceLevel>0&&Boolean.FALSE.equals(canceledLayers.get("minecraft:experience_bar")),"native XP is available while riding a jumpable horse");shot("15-hud-mounted-xp");}
            if(ticks==505)server(mc,()->{var p=player(mc);p.stopRiding();if(horse!=null)horse.discard();p.teleportTo(arena.getX()+.5,arena.getY()+1,arena.getZ()+.5);
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=5;y++)p.serverLevel().setBlock(arena.offset(x,y,z),Blocks.WATER.defaultBlockState(),3);p.setAirSupply(120);});
            if(ticks==530){check(CenteredHud.oxygenVisible(),"supplied oxygen meter appears underwater");check(Boolean.TRUE.equals(canceledLayers.get("minecraft:air_level"))&&Boolean.TRUE.equals(canceledLayers.get("minecraft:food_level"))&&Boolean.TRUE.equals(canceledLayers.get("minecraft:armor_level")),"vanilla air, hunger and armor indicators remain suppressed");shot("16-hud-oxygen");}
            if(ticks==535)server(mc,()->{var p=player(mc);p.teleportTo(arena.getX()+4.5,arena.getY(),arena.getZ()+4.5);p.setAirSupply(p.getMaxAirSupply());var d=ProgressionManager.get(p);ProgressionManager.addXPAdministrative(p,d.requiredXP());ModNetworking.sync(p);});
            if(ticks==545){check(LevelUpHud.active(),"actual RPG level increase triggers the localized toast");shot("17-level-up-fade-in");}
            if(ticks==565)shot("18-level-up-visible");
            if(ticks==575){mc.setScreen(new CharacterScreen());cursor(mc,114,143);language(mc,"en_us");}
            if(ticks==600){shot("19-character-en");server(mc,()->{var p=player(mc);var data=ProgressionManager.get(p);ModNetworking.levelUp(p,data.level,data.points);});}
            if(ticks==605)mc.setScreen(null);
            if(ticks==630)shot("20-level-up-en");
            if(ticks==635&&irons)server(mc,()->manaScroll(mc));
            if(ticks==650&&irons){check(ClientData.get().manaActive&&IronsManaHud.visible(mc.player)&&IronsManaHud.selected(mc.player)!=null,"native Iron's scroll selects the textured mana meter and live spell icon");
                check(Math.abs(ClientData.get().mana-4)<.0001&&Math.abs(ClientData.get().manaCapacity-8)<.0001,"upgraded Spark Potential synchronizes four mana units in the eight-unit meter");shot("21-irons-mana");}
            if(ticks==655){check(!CenteredHud.experienceVisible(),"native XP hides again after the change window");server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);d.level=42;d.xp=123;d.points=2;d.set(RPGAttribute.STRENGTH,16);ProgressionManager.refresh(p,10,false);ModNetworking.sync(p);});}
            if(ticks%2==0){
                if(ticks>=68&&ticks<=132)frame("mouse-tracking");
                if(learnedId!=null&&ticks>=202&&ticks<=284)frame("hover-click-pin");
                if(ticks>=448&&ticks<=534)frame("hud-appearance");
                if(ticks>=534&&ticks<=574)frame("level-up-fade");
            }
            if(ticks==680){check(ClientData.get().level==42&&ClientData.get().xp==123&&ClientData.get().get(RPGAttribute.STRENGTH)==16,"final persistence fixture synchronizes before clean shutdown");
                if(profile.equals("all"))server(mc,()->BridgeClientChecks.prepare(player(mc)));else finish(mc);}
            if(ticks==700&&profile.equals("all"))BridgeClientChecks.begin(mc);
            if(ticks==702&&profile.equals("all")){var animation=BridgeClientChecks.animation(mc);check(animation.startsWith("epicparcool:"),"official bridge plays a real Epic x ParCool Dodge animation: "+animation);}
            if(ticks==720&&profile.equals("all")){server(mc,()->{check(BridgeClientChecks.serverPrice(player(mc)),"real bridge client/server Dodge spends Epic stamina exactly once");BridgeClientChecks.reset(player(mc));});finish(mc);}
        }catch(Throwable error){failed=true;LogUtils.getLogger().error("HEART_PORT_CLIENT failure at native tick "+ticks,error);finish(mc);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void preview(RenderLivingEvent.Pre<?,?> event){
        if(enabled()&&Minecraft.getInstance().screen instanceof CharacterScreen&&event.getEntity()==Minecraft.getInstance().player){var entity=event.getEntity();preview=new PreviewPose(entity.getYRot(),entity.getXRot(),entity.yHeadRot,entity.yBodyRot);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void playerPreview(RenderPlayerEvent.Pre event){
        if(enabled()&&Minecraft.getInstance().screen instanceof CharacterScreen&&event.getEntity()==Minecraft.getInstance().player){var entity=event.getEntity();preview=new PreviewPose(entity.getYRot(),entity.getXRot(),entity.yHeadRot,entity.yBodyRot);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void layers(RenderGuiLayerEvent.Pre event){
        if(enabled())canceledLayers.put(event.getName().toString(),event.isCanceled());
    }
    @SubscribeEvent public static void render(RenderFrameEvent.Post event){
        if(!enabled()||captures.isEmpty()||finished)return;var mc=Minecraft.getInstance();var pending=List.copyOf(captures);captures.clear();
        long capturedAtMs=(System.nanoTime()-CAPTURE_ORIGIN)/1000000;
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            Files.createDirectories(directory(mc));for(var name:pending){var path=directory(mc).resolve(name);Files.createDirectories(path.getParent());pixels.writeToFile(path);var state=new JsonObject();state.addProperty("tick",ticks);state.addProperty("capturedAtMs",capturedAtMs);state.addProperty("guiScale",mc.options.guiScale().get());state.addProperty("language",mc.getLanguageManager().getSelected());state.addProperty("screen",mc.screen==null?"HUD":mc.screen.getClass().getSimpleName());
                state.addProperty("level",ClientData.get().level);state.addProperty("skillPoints",ClientData.get().skillPoints);state.addProperty("nodes",ClientSkillTree.nodes.size());state.addProperty("pinned",pinned(mc));state.addProperty("description",described(mc));state.addProperty("cursorArtworkX",cursorX);state.addProperty("cursorArtworkY",cursorY);
                state.addProperty("staminaVisible",CenteredHud.staminaVisible());state.addProperty("oxygenVisible",CenteredHud.oxygenVisible());state.addProperty("mounted",CenteredHud.riddenAnimal()!=null);state.addProperty("experienceVisible",CenteredHud.experienceVisible());state.addProperty("experienceOpacity",CenteredHud.experienceOpacity());state.addProperty("levelUpActive",LevelUpHud.active());
                state.addProperty("mana",ClientData.get().mana);state.addProperty("manaCapacity",ClientData.get().manaCapacity);
                if(mc.screen instanceof CharacterScreen&&preview!=null){var pose=new JsonObject();pose.addProperty("yaw",preview.yaw());pose.addProperty("pitch",preview.pitch());pose.addProperty("head",preview.head());pose.addProperty("body",preview.body());state.add("nativePreviewPose",pose);}
                switch(name){
                    case "01-character-left.png"->previews.put("left",preview);
                    case "02-character-right.png"->previews.put("right",preview);
                    case "03-character-up.png"->previews.put("up",preview);
                    case "04-character-down.png"->previews.put("down",preview);
                    case "04b-character-scale3.png"->{
                        var left=previews.get("left");var right=previews.get("right");var up=previews.get("up");var down=previews.get("down");
                        check(left!=null&&right!=null&&left.yaw()>right.yaw()+10,"rendered character turns toward left and right cursor positions");
                        check(up!=null&&down!=null&&up.pitch()<down.pitch()-10,"rendered character looks toward upper and lower cursor positions");
                        check(preview!=null&&right!=null&&Math.abs(preview.yaw()-right.yaw())<1,"mouse tracking angles remain consistent after changing GUI scale");
                    }
                    default->{}
                }
                if(info(mc)!=null){state.addProperty("title",((Component)field(info(mc),"title")).getString());state.addProperty("scroll",info(mc).scroll());state.addProperty("maximumScroll",info(mc).maximumScroll());}
                var layers=new JsonObject();canceledLayers.forEach(layers::addProperty);state.add("canceledNativeLayers",layers);states.add(name,state);LogUtils.getLogger().info("HEART_PORT_CLIENT capture {}/{}",profile,name);
            }report(mc);
        }catch(Throwable error){failed=true;LogUtils.getLogger().error("HEART_PORT_CLIENT native framebuffer capture failed",error);}
    }
    private PortClientSmoke(){}
}
