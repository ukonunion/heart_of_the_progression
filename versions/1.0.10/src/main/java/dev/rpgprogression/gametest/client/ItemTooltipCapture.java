package dev.rpgprogression.gametest.client;

import com.mojang.logging.LogUtils;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.RequirementTooltip;
import dev.rpgprogression.compat.EpicFightCompat;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.requirements.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Native inventory captures in our own copied worlds. Never included in the release JAR. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class ItemTooltipCapture {
    private static boolean loading;
    private static volatile boolean failed;
    private static int ticks,slot;
    private static String profile,language;
    private static List<? extends String> strengthRules,agilityRules;
    private static com.electronwill.nightconfig.core.CommentedConfig originalConfig;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static final List<String> captures=new ArrayList<>();
    private static final com.google.gson.JsonObject states=new com.google.gson.JsonObject();
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable error){failed=true;LogUtils.getLogger().error("HEART_ITEM_TOOLTIP server failure",error);}});}
    public static void check(boolean ok,String message){if(!ok)failed=true;LogUtils.getLogger().info("HEART_ITEM_TOOLTIP {} {}",ok?"verified":"FAILED",message);}
    private static void language(Minecraft mc,String value){mc.getLanguageManager().setSelected(value);mc.options.languageCode=value;reload=mc.reloadResourcePacks();}
    private static void hover(Minecraft mc,int index){
        slot=index;mc.setScreen(new InventoryScreen(mc.player));
        double x=(mc.screen.width-176)/2.0+16+index*18,y=(mc.screen.height-166)/2.0+150;
        try{for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,(name.equals("xpos")?x:y)*mc.getWindow().getGuiScale());}}
        catch(Exception error){throw new IllegalStateException(error);}
    }
    private static void shot(String name){captures.add(name+".png");}
    private static ItemStack registered(String id){var key=new ResourceLocation(id);check(net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(key),"registered optional item "+id);return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(key));}
    private static void registryCheck(Minecraft mc){
        int count=0;boolean ok=true;
        for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS){if(item==Items.AIR)continue;count++;
            var stack=new ItemStack(item);var tooltip=Screen.getTooltipFromItem(mc,stack);
            String requirements=Component.translatable("rpgprogression.requirements.title").getString(),scaling=Component.translatable("rpgprogression.item.scaling.title").getString();
            boolean valid=tooltip.stream().filter(c->c.getString().equals(requirements)).count()==1&&tooltip.stream().filter(c->c.getString().equals(scaling)).count()==1;
            if(ItemRequirements.required(stack).isEmpty())valid&=tooltip.stream().anyMatch(c->c.getString().equals(Component.translatable("rpgprogression.requirements.none").getString()));
            if(!valid){ok=false;LogUtils.getLogger().error("HEART_ITEM_TOOLTIP invalid sections {}",net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item));}
        }
        check(ok,"both sections appear exactly once on every registered item: "+count);
        var noEntity=new ArrayList<Component>(List.of(Component.literal("Item")));
        RequirementTooltip.tooltip(new ItemTooltipEvent(new ItemStack(Items.DIRT),null,noEntity,TooltipFlag.Default.NORMAL));
        check(noEntity.stream().anyMatch(c->c.getString().equals(Component.translatable("rpgprogression.item.scaling.none").getString())),"null-entity tooltip safely falls back to the local player");
        check(!ItemCombatStats.weapon(new ItemStack(Items.DIAMOND_CHESTPLATE))&&Screen.getTooltipFromItem(mc,new ItemStack(Items.DIAMOND_CHESTPLATE)).stream().anyMatch(c->c.getString().equals(Component.translatable("rpgprogression.item.scaling.none").getString())),"armour is not classified as a weapon by optional Epic capabilities");
        check(ItemCombatStats.weapon(new ItemStack(Items.DIAMOND_HOE)),"tools with a zero base damage modifier still receive weapon information");
    }
    private static void basicCosts(ServerPlayer p){
        var d=ProgressionManager.get(p);var original=p.getMainHandItem().copy();double originalCost=RPGConfig.ATTACK_COST.get();
        try{
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
            for(double cost:new double[]{12,13.5,0}){
                RPGConfig.ATTACK_COST.set(cost);d.stamina=d.maxStamina();d.exhausted=false;d.attackCooldown=0;
                double displayed=ItemCombatStats.attackCost(p,p.getMainHandItem()),before=d.stamina;
                var cow=net.minecraft.world.entity.EntityType.COW.create(p.serverLevel());
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.AttackEntityEvent(p,cow));
                check(Math.abs(displayed-cost)<.001&&Math.abs(before-d.stamina-displayed)<.001,"normal attack preview equals real debit at configured cost "+cost+"; shown="+displayed+", debit="+(before-d.stamina));
            }
            check(ItemCombatStats.rangedCost(p,new ItemStack(Items.BOW))==0,"normal mode ranged shots have no stamina charge");
        }finally{RPGConfig.ATTACK_COST.set(originalCost);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,original);d.stamina=d.maxStamina();d.regenCooldown=20000;ModNetworking.sync(p);}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("rpgprogression.itemTooltipCapture")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld"));return;}
        if(loading&&mc.player==null){
            String label=mc.screen instanceof BackupConfirmScreen?Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;
        }
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        if(reload!=null){if(!reload.isDone())return;reload.join();reload=null;}
        ticks++;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        boolean epic=EpicFightCompat.present(),irons=dev.rpgprogression.compat.IronsCompat.present();
        if(ticks==20){
            profile=epic&&irons?"all":"standalone";check(System.getProperty("rpgprogression.smokeWorld").endsWith("-"+profile),"actual optional-mod profile "+profile);
            mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.advancedItemTooltips=false;mc.options.pauseOnLostFocus=false;
            language=mc.getLanguageManager().getSelected();language(mc,"ru_ru");
            server(mc,()->{var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.stopRiding();p.removeAllEffects();p.getInventory().clearContent();
                // Avoid async file autosave/reload races while trying multiple prices in one tick.
                var config=net.minecraftforge.fml.config.ConfigTracker.INSTANCE.fileMap().get("rpgprogression-server.toml");originalConfig=config.getConfigData();
                if(originalConfig instanceof com.electronwill.nightconfig.core.file.FileConfig file)com.electronwill.nightconfig.core.file.FileWatcher.defaultInstance().removeWatch(file.getNioPath());
                RPGConfig.SERVER.setConfig(new com.electronwill.nightconfig.toml.TomlParser().parse(new com.electronwill.nightconfig.toml.TomlWriter().writeToString(originalConfig)));
                if(epic){EpicTreeClientCheck.prepareFixture(p);ItemTooltipEpicCheck.mode(p,false);}
                var d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();d.set(RPGAttribute.STRENGTH,20);d.set(RPGAttribute.AGILITY,10);
                ProgressionManager.refresh(p,10,false);d.regenCooldown=20000;
                Item[] items={Items.DIAMOND_SWORD,Items.DIRT,Items.COOKED_BEEF,Items.DIAMOND_CHESTPLATE,Items.BOW,Items.CROSSBOW,Items.TRIDENT};
                for(int i=0;i<items.length;i++)p.getInventory().setItem(i,new ItemStack(items[i]));
                var sword=p.getInventory().getItem(0);sword.enchant(Enchantments.SHARPNESS,2);
                var lore=new net.minecraft.nbt.ListTag();lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("Сохранённое описание"))));sword.getOrCreateTagElement("display").put("Lore",lore);
                if(epic)p.getInventory().setItem(7,registered("epicfight:iron_greatsword"));
                if(irons){var staff=net.minecraftforge.registries.ForgeRegistries.ITEMS.getEntries().stream().filter(e->e.getKey().location().getNamespace().equals("irons_spellbooks")&&e.getKey().location().getPath().endsWith("staff")&&ItemCombatStats.melee(new ItemStack(e.getValue()))).findFirst().orElseThrow();p.getInventory().setItem(8,new ItemStack(staff.getValue()));}
                strengthRules=List.copyOf(RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).get());agilityRules=List.copyOf(RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.AGILITY).get());
                RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(List.of("minecraft:diamond_sword=15"));RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.AGILITY).set(List.of("minecraft:diamond_sword=12"));
                ModNetworking.rules(p);ModNetworking.sync(p);
            });
        }
        if(ticks==45)server(mc,()->basicCosts(player(mc)));
        if(ticks==65){check(ClientData.get().initialized&&ClientData.get().get(RPGAttribute.STRENGTH)==20,"server-synchronized attributes");if(epic)ItemTooltipEpicCheck.clientMode(mc.player,false);registryCheck(mc);hover(mc,0);}
        if(ticks==80){
            var t=Screen.getTooltipFromItem(mc,mc.player.getInventory().getItem(0));
            check(t.stream().anyMatch(c->c.getString().contains("Сохранённое описание"))&&t.stream().anyMatch(c->c.getString().contains("Острота")),"native lore and enchantments preserved");
            for(var entry:Map.of("Сила: 15",net.minecraft.ChatFormatting.GREEN,"Ловкость: 12",net.minecraft.ChatFormatting.RED).entrySet())check(t.stream().filter(c->c.getString().equals(entry.getKey())).anyMatch(c->c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text&&text.getArgs()[1] instanceof Component value&&value.getStyle().getColor()!=null&&value.getStyle().getColor().getValue()==entry.getValue().getColor()),"requirement colour "+entry.getKey());
            shot("01-sword-requirements-ru");
        }
        if(ticks==85)hover(mc,1);if(ticks==100)shot("02-block-no-requirements-no-scaling");
        if(ticks==105)hover(mc,2);if(ticks==120)shot("03-food-no-requirements-no-scaling");
        if(ticks==125)hover(mc,3);if(ticks==140)shot("04-armour-native-stats");
        if(ticks==145){server(mc,()->{var p=player(mc);ProgressionManager.get(p).set(RPGAttribute.AGILITY,20);ProgressionManager.refresh(p,20,false);ModNetworking.sync(p);});hover(mc,4);}
        if(ticks==165)shot("05-bow-agility-normal-mode");
        if(ticks==170)hover(mc,5);if(ticks==185)shot("06-crossbow-agility-normal-mode");
        if(ticks==190)hover(mc,6);if(ticks==205)shot("07-trident-hit-and-throw");
        if(ticks==210&&epic){server(mc,()->ItemTooltipEpicCheck.costs(player(mc)));ItemTooltipEpicCheck.clientMode(mc.player,true);hover(mc,7);}
        if(ticks==240&&epic)shot("08-epic-greatsword-battle-mode");
        if(ticks==245&&epic)hover(mc,0);if(ticks==260&&epic)shot("09-epic-sword-battle-mode");
        if(ticks==265&&epic)hover(mc,4);if(ticks==280&&epic)shot("10-epic-bow-shot-cost");
        if(ticks==285&&irons)hover(mc,8);if(ticks==300&&irons)shot("11-irons-staff-native-stats");
        if(ticks==305){mc.options.guiScale().set(3);mc.resizeDisplay();hover(mc,0);}
        if(ticks==320){var tooltip=Screen.getTooltipFromItem(mc,mc.player.getInventory().getItem(0));check(tooltip.size()*(mc.font.lineHeight+1)+2<=mc.screen.height-14,"complete weapon tooltip fits GUI Scale 3 including native Epic statistics");shot("12-sword-ru-scale3");}
        if(ticks==325){mc.options.guiScale().set(2);mc.resizeDisplay();language(mc,"en_us");}
        if(ticks==340)hover(mc,0);if(ticks==355)shot("13-sword-requirements-en");
        if(ticks==360)hover(mc,1);if(ticks==375)shot("14-block-en");
        if(ticks==380){mc.options.advancedItemTooltips=true;hover(mc,6);}if(ticks==395)shot("15-trident-en-advanced");
        if(ticks==400){
            server(mc,()->{var p=player(mc);RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(strengthRules);RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.AGILITY).set(agilityRules);ModNetworking.rules(p);if(epic)ItemTooltipEpicCheck.mode(p,false);});
            mc.options.advancedItemTooltips=false;mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(null);language(mc,language);
        }
        if(ticks==410)server(mc,()->{var p=player(mc);p.setGameMode(GameType.CREATIVE);check(ItemCombatStats.attackCost(p,p.getMainHandItem())==0,"creative exemption reports zero");p.setGameMode(GameType.SURVIVAL);});
        if(ticks==420)server(mc,()->RPGConfig.SERVER.setConfig(originalConfig));
        if(ticks==430){check(ItemRequirements.required(mc.player.getInventory().getItem(0)).isEmpty()&&RPGConfig.ATTACK_COST.get()==12,"temporary requirement fixtures restored");LogUtils.getLogger().info("HEART_ITEM_TOOLTIP_RESULT {} {}",profile,failed?"FAILED":"PASSED");mc.stop();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
        if(captures.isEmpty()||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var pending=List.copyOf(captures);captures.clear();
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            var dir=mc.gameDirectory.toPath().resolve("screenshots/items-1.0.5").resolve(profile);Files.createDirectories(dir);
            for(var name:pending){pixels.writeToFile(dir.resolve(name));var state=new com.google.gson.JsonObject();
                state.addProperty("tick",ticks);state.addProperty("guiScale",mc.options.guiScale().get());state.addProperty("language",mc.getLanguageManager().getSelected());
                var stack=mc.player.getInventory().getItem(slot);state.addProperty("item",net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());state.addProperty("battle",EpicFightCompat.battle(mc.player));state.addProperty("creative",mc.player.isCreative());state.addProperty("spectator",mc.player.isSpectator());
                state.addProperty("hitCost",ItemCombatStats.attackCost(mc.player,stack));state.addProperty("rangedCost",ItemCombatStats.rangedCost(mc.player,stack));
                var tooltip=new com.google.gson.JsonArray();for(var c:Screen.getTooltipFromItem(mc,stack))tooltip.add(c.getString());state.add("tooltip",tooltip);states.add(name,state);
                LogUtils.getLogger().info("HEART_ITEM_TOOLTIP capture {}/{}",profile,name);
            }
            var mods=new com.google.gson.JsonArray();for(var mod:ModList.get().getMods()){var entry=new com.google.gson.JsonObject();entry.addProperty("id",mod.getModId());entry.addProperty("version",mod.getVersion().toString());mods.add(entry);}
            var report=new com.google.gson.JsonObject();report.add("loadedMods",mods);report.add("screenshots",states);
            Files.writeString(dir.resolve("capture-states.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception error){check(false,"framebuffer capture: "+error);}
    }
    private ItemTooltipCapture(){}
}
