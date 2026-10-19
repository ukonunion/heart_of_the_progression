package dev.rpgprogression.gametest.client;

import com.mojang.logging.LogUtils;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.compat.EpicFightCompat;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.gametest.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.requirements.ItemCombatStats;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class AttributeBalanceCapture {
    private static boolean loading;
    private static volatile boolean failed;
    private static int ticks,slot;
    private static String profile,originalLanguage;
    private static com.electronwill.nightconfig.core.CommentedConfig originalConfig;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static final List<String> captures=new ArrayList<>();
    private static final com.google.gson.JsonObject states=new com.google.gson.JsonObject();
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable error){failed=true;LogUtils.getLogger().error("HEART_BALANCE server failure",error);}});}
    private static void check(boolean ok,String message){if(!ok)failed=true;LogUtils.getLogger().info("HEART_BALANCE {} {}",ok?"verified":"FAILED",message);}
    private static void language(Minecraft mc,String value){mc.getLanguageManager().setSelected(value);mc.options.languageCode=value;reload=mc.reloadResourcePacks();}
    private static void hover(Minecraft mc,int index){
        slot=index;mc.setScreen(new InventoryScreen(mc.player));double x=(mc.screen.width-176)/2.0+16+index*18,y=(mc.screen.height-166)/2.0+150;
        try{for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,(name.equals("xpos")?x:y)*mc.getWindow().getGuiScale());}}
        catch(Exception error){throw new IllegalStateException(error);}
    }
    private static void shot(String name){captures.add(name+".png");}
    private static boolean costLine(Component c){return c.getContents() instanceof TranslatableContents t&&t.getKey().startsWith("rpgprogression.item.stamina.");}
    private static void registry(Minecraft mc){
        int n=0;boolean valid=true;
        for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS){if(item==Items.AIR)continue;n++;var stack=new ItemStack(item);var tooltip=Screen.getTooltipFromItem(mc,stack);
            boolean noDetails=tooltip.stream().noneMatch(c->c.getContents() instanceof TranslatableContents t&&(t.getKey().startsWith("rpgprogression.item.scaling.")||t.getKey().startsWith("rpgprogression.requirements.")));
            long count=tooltip.stream().filter(AttributeBalanceCapture::costLine).count();boolean costs=count==(ItemCombatStats.weapon(stack)?stack.getItem() instanceof TridentItem?2:1:0);
            if(!noDetails||!costs){valid=false;LogUtils.getLogger().error("HEART_BALANCE invalid tooltip {}",net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item));}
        }
        check(valid,"every registered item keeps only its applicable stamina footer: "+n);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("rpgprogression.attributeBalanceCapture")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld"));return;}
        if(loading&&mc.player==null){String label=mc.screen instanceof BackupConfirmScreen?Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;}
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        if(reload!=null){if(!reload.isDone())return;reload.join();reload=null;}
        // The reload future finishes before the loading overlay has faded away.
        if(mc.getOverlay()!=null)return;
        ticks++;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        boolean epic=EpicFightCompat.present(),irons=dev.rpgprogression.compat.IronsCompat.present();
        if(ticks==20){
            profile=epic&&irons?"all":"standalone";check(System.getProperty("rpgprogression.smokeWorld").endsWith("-"+profile),"actual optional-mod profile "+profile);
            mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.advancedItemTooltips=false;mc.options.pauseOnLostFocus=false;originalLanguage=mc.getLanguageManager().getSelected();language(mc,"ru_ru");
            server(mc,()->{var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.stopRiding();p.removeAllEffects();p.getInventory().clearContent();
                var config=net.minecraftforge.fml.config.ConfigTracker.INSTANCE.fileMap().get("rpgprogression-server.toml");originalConfig=config.getConfigData();
                if(originalConfig instanceof com.electronwill.nightconfig.core.file.FileConfig file)com.electronwill.nightconfig.core.file.FileWatcher.defaultInstance().removeWatch(file.getNioPath());
                RPGConfig.SERVER.setConfig(new com.electronwill.nightconfig.toml.TomlParser().parse(new com.electronwill.nightconfig.toml.TomlWriter().writeToString(originalConfig)));
                var d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();d.set(RPGAttribute.STRENGTH,20);d.set(RPGAttribute.AGILITY,20);d.set(RPGAttribute.ENDURANCE,40);d.set(RPGAttribute.VITALITY,20);d.set(RPGAttribute.SPARK_POTENTIAL,20);ProgressionManager.refresh(p,10,false);d.regenCooldown=20000;
                Item[] items={Items.DIAMOND_SWORD,Items.BOW,Items.CROSSBOW,Items.TRIDENT,Items.DIRT,Items.COOKED_BEEF,Items.DIAMOND_CHESTPLATE};for(int i=0;i<items.length;i++)p.getInventory().setItem(i,new ItemStack(items[i]));
                p.getInventory().getItem(0).enchant(Enchantments.SHARPNESS,1);p.getInventory().getItem(2).enchant(Enchantments.MULTISHOT,1);
                if(epic){EpicTreeClientCheck.prepareFixture(p);RangedEpicSetup.mode(p,false);p.getInventory().setItem(7,new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("epicfight:iron_greatsword"))));}
                if(irons){var staff=net.minecraftforge.registries.ForgeRegistries.ITEMS.getEntries().stream().filter(e->e.getKey().location().getNamespace().equals("irons_spellbooks")&&e.getKey().location().getPath().endsWith("staff")&&ItemCombatStats.melee(new ItemStack(e.getValue()))).findFirst().orElseThrow();p.getInventory().setItem(8,new ItemStack(staff.getValue()));}
                p.setHealth(p.getMaxHealth());check(RPGConfig.SPARK_PER_LEVEL.get()==1&&RPGConfig.VITALITY_HP.get()==1&&RPGConfig.RULES_VERSION.get()==9,"old-world config migrates to +1/+1 and rules nine");check(Math.abs(p.getMaxHealth()-30)<.001&&d.maxSpark()==35,"native refreshed attributes have HP thirty and reservoir thirty-five");ModNetworking.sync(p);
            });
        }
        if(ticks==40)server(mc,()->{AttributeBalanceChecks.run(player(mc),AttributeBalanceCapture::check);RangedNativeChecks.run(player(mc),AttributeBalanceCapture::check);ModNetworking.sync(player(mc));});
        if(ticks==70){if(epic)ItemTooltipEpicCheck.clientMode(mc.player,false);registry(mc);hover(mc,0);}
        if(ticks==85)shot("01-sword-ru");if(ticks==90)hover(mc,1);if(ticks==105)shot("02-bow-ru");
        if(ticks==110)hover(mc,2);if(ticks==125)shot("03-crossbow-charge-ru");if(ticks==130)hover(mc,3);if(ticks==145)shot("04-trident-ru");
        if(ticks==150)hover(mc,4);if(ticks==165)shot("05-block-native-tooltip");if(ticks==170)hover(mc,6);if(ticks==185)shot("06-armour-native-tooltip");
        if(ticks==190&&epic){server(mc,()->RangedEpicSetup.mode(player(mc),true));ItemTooltipEpicCheck.clientMode(mc.player,true);hover(mc,7);}
        if(ticks==210&&epic)shot("07-epic-greatsword");if(ticks==215&&irons)hover(mc,8);if(ticks==230&&irons)shot("08-irons-staff");
        if(ticks==235){mc.options.guiScale().set(3);mc.resizeDisplay();hover(mc,2);}if(ticks==250)shot("09-crossbow-scale3");
        if(ticks==255){mc.options.guiScale().set(2);mc.resizeDisplay();language(mc,"en_us");}if(ticks==270)hover(mc,1);if(ticks==285)shot("10-bow-en");
        if(ticks==290)hover(mc,2);if(ticks==305)shot("11-crossbow-en");if(ticks==310)hover(mc,3);if(ticks==325)shot("12-trident-en");
        if(ticks==330){mc.setScreen(null);mc.player.getInventory().selected=2;server(mc,()->{var p=player(mc);p.getInventory().selected=2;p.getInventory().setItem(2,new ItemStack(Items.CROSSBOW));p.getInventory().setItem(1,new ItemStack(Items.ARROW,16));var d=ProgressionManager.get(p);d.stamina=d.maxStamina();d.exhausted=false;d.regenCooldown=20000;ModNetworking.sync(p);});}
        if(ticks==350)shot("13-hud-before-charge");
        if(ticks==355)server(mc,()->{var p=player(mc);var stack=p.getMainHandItem();stack.getItem().releaseUsing(stack,p.level(),p,stack.getUseDuration()-25);ProgressionManager.get(p).regenCooldown=20000;});
        if(ticks==375)shot("14-hud-charged");
        if(ticks==380)server(mc,()->{var p=player(mc);p.getMainHandItem().getItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);});
        if(ticks==400)shot("15-hud-fired-no-extra-cost");
        if(ticks==405){language(mc,originalLanguage);server(mc,()->{if(epic)RangedEpicSetup.mode(player(mc),false);RPGConfig.SERVER.setConfig(originalConfig);});}
        if(ticks==435){LogUtils.getLogger().info("HEART_BALANCE_RESULT {} {}",profile,failed?"FAILED":"PASSED");mc.stop();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
        if(captures.isEmpty()||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();var pending=List.copyOf(captures);captures.clear();
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            var dir=mc.gameDirectory.toPath().resolve("screenshots/balance-1.0.7").resolve(profile);Files.createDirectories(dir);
            for(var name:pending){pixels.writeToFile(dir.resolve(name));var s=new com.google.gson.JsonObject();s.addProperty("tick",ticks);s.addProperty("overlayClear",mc.getOverlay()==null);s.addProperty("guiScale",mc.options.guiScale().get());s.addProperty("language",mc.getLanguageManager().getSelected());s.addProperty("stamina",ClientData.get().stamina);s.addProperty("battle",EpicFightCompat.battle(mc.player));
                if(mc.screen instanceof InventoryScreen){var stack=mc.player.getInventory().getItem(slot);s.addProperty("item",net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());var list=new com.google.gson.JsonArray();for(var c:Screen.getTooltipFromItem(mc,stack))list.add(c.getString());s.add("tooltip",list);}
                states.add(name,s);LogUtils.getLogger().info("HEART_BALANCE capture {}/{}",profile,name);
            }
            var mods=new com.google.gson.JsonArray();for(var mod:ModList.get().getMods()){var m=new com.google.gson.JsonObject();m.addProperty("id",mod.getModId());m.addProperty("version",mod.getVersion().toString());mods.add(m);}
            var report=new com.google.gson.JsonObject();report.add("loadedMods",mods);report.add("screenshots",states);Files.writeString(dir.resolve("capture-states.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception error){check(false,"capture: "+error);}
    }
    private AttributeBalanceCapture(){}
}
