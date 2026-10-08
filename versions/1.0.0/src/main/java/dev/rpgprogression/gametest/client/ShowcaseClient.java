package dev.rpgprogression.gametest.client;

import com.mojang.logging.LogUtils;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.*;
import dev.rpgprogression.client.gui.*;
import dev.rpgprogression.compat.IronsCompat;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Framebuffer captures in our isolated demonstration world. Excluded from the release JAR. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class ShowcaseClient {
    private static boolean loading,failed,hardcore;
    private static int ticks;
    private static Horse mount;
    private static String parkourId,epicId;
    private static final List<String> captures=new ArrayList<>();
    private static final com.google.gson.JsonObject states=new com.google.gson.JsonObject();
    private static List<? extends String> originalRequirements;
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void server(Minecraft mc,Runnable task){mc.getSingleplayerServer().execute(()->{try{task.run();}catch(Throwable e){failed=true;LogUtils.getLogger().error("HEART_SHOWCASE server failure",e);}});}
    private static void check(boolean ok,String message){if(!ok)failed=true;LogUtils.getLogger().info("HEART_SHOWCASE {} {}",ok?"verified":"FAILED",message);}
    private static void shot(String name){captures.add("screenshots/"+name+".png");}
    private static void frame(String clip){captures.add("frames/"+clip+"/"+String.format(Locale.ROOT,"%05d",ticks)+".png");}
    private static void sync(Minecraft mc){ModNetworking.sync(player(mc));}
    private static void clear(ServerPlayer p){p.removeAllEffects();p.setAbsorptionAmount(0);p.setTicksFrozen(0);}
    private static void reset(ServerPlayer p){
        clear(p);p.stopRiding();if(mount!=null){mount.discard();mount=null;}
        p.getInventory().clearContent();var d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();
        d.points=12;d.level=20;ProgressionManager.refresh(p,10,false);p.setHealth(12);d.stamina=d.maxStamina();d.regenCooldown=20000;
        if(IronsCompat.enabled())OptionalManaClientSmoke.foodHudFixture(p);
        d.spark=d.maxSpark()*.55;d.mana=d.manaCapacity;d.manaRegenCooldown=20000;ModNetworking.sync(p);
    }
    private static void horse(ServerPlayer p){
        mount=EntityType.HORSE.create(p.serverLevel());mount.moveTo(p.getX(),p.getY(),p.getZ(),p.getYRot(),0);
        mount.setTamed(true);mount.setNoAi(true);mount.setSilent(true);mount.setInvulnerable(true);
        mount.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);mount.setHealth(22);
        mount.equipSaddle(net.minecraft.sounds.SoundSource.NEUTRAL);p.serverLevel().addFreshEntity(mount);check(p.startRiding(mount,true),"ridden horse attached");
    }
    private static double scale(Screen s){return Math.min(1.5,Math.min(s.width/640.0,s.height/360.0));}
    private static double sx(Screen s,double x){return (s.width-640*scale(s))/2+x*scale(s);}
    private static double sy(Screen s,double y){return (s.height-360*scale(s))/2+y*scale(s);}
    private static void click(Minecraft mc,double x,double y){var s=mc.screen;check(s.mouseClicked(sx(s,x),sy(s,y),0),"menu click "+x+","+y);s.mouseReleased(sx(s,x),sy(s,y),0);}
    private static void cursor(Minecraft mc,double x,double y){
        try{for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,(name.equals("xpos")?x:y)*mc.getWindow().getGuiScale());}}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private static void tree(Minecraft mc,int branch){mc.setScreen(new SkillTreeScreen(new CharacterScreen(),branch));cursor(mc,mc.screen.width-5,mc.screen.height-5);}
    private static String details(Minecraft mc,int branch){
        var node=ClientSkillTree.nodes.stream().filter(n->n.branch()==branch&&n.parents().isEmpty()).findFirst().orElseThrow();
        var s=(SkillTreeScreen)mc.screen;s.focusSkill(node.id());var p=s.skillPosition(node.id());click(mc,p[0],p[1]);return node.id();
    }
    private static void hardcore(Minecraft mc,boolean value){
        try{var data=mc.level.getLevelData();var field=Arrays.stream(data.getClass().getDeclaredFields()).filter(f->f.getType()==boolean.class&&f.getName().toLowerCase(Locale.ROOT).contains("hardcore")).findFirst().orElseThrow();field.setAccessible(true);field.setBoolean(data,value);hardcore=value;}
        catch(Exception e){check(false,"hardcore visual fixture: "+e);}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("rpgprogression.showcase")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld","HeartShowcase100"));return;}
        if(loading&&mc.player==null){
            String label=mc.screen instanceof BackupConfirmScreen?net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;
        }
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        ticks++;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        if(Boolean.getBoolean("rpgprogression.showcaseQuick")){quick(mc);return;}
        mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(),false);
        if(ticks==20){mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.bobView().set(false);mc.options.screenEffectScale().set(1.0);
            server(mc,()->{var p=player(mc);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.serverLevel().setDayTime(6000);p.serverLevel().setWeatherParameters(100000,0,false,false);EpicTreeClientCheck.prepareFixture(p);reset(p);});}
        if(ticks==60){check(ClientData.get().initialized&&RPGConfig.BASE_XP.get()==500,"1.0.0 base and synchronized character");mc.setScreen(new CharacterScreen());cursor(mc,mc.screen.width-5,mc.screen.height-5);}
        if(ticks==75)shot("01-character-menu");
        if(ticks==85)server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);for(var a:RPGAttribute.values())d.set(a,a==RPGAttribute.SPARK_POTENTIAL?50:20);ProgressionManager.refresh(p,10,false);p.setHealth(p.getMaxHealth()*.65f);d.stamina=d.maxStamina()*.55;d.regenCooldown=20000;sync(mc);});
        if(ticks==105)shot("02-character-upgraded");
        if(ticks==110)server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);d.set(RPGAttribute.ENDURANCE,10);d.set(RPGAttribute.STRENGTH,10);ProgressionManager.refresh(p,20,false);p.getInventory().setItem(0,new ItemStack(Items.DIAMOND_SWORD));originalRequirements=List.copyOf(RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).get());RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(List.of("minecraft:diamond_sword=20"));ModNetworking.rules(p);});
        if(ticks==120){mc.setScreen(new InventoryScreen(mc.player));cursor(mc,mc.screen.width-5,mc.screen.height-5);}
        if(ticks==135)shot("03-inventory-locked");
        if(ticks==140)cursor(mc,(mc.screen.width-176)/2.0+16,(mc.screen.height-166)/2.0+150);
        if(ticks==150)shot("04-item-requirement-tooltip");
        if(ticks==155)server(mc,()->{var p=player(mc);RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(originalRequirements);ModNetworking.rules(p);var d=ProgressionManager.get(p);d.set(RPGAttribute.ENDURANCE,37);ProgressionManager.refresh(p,20,false);for(int i=9;i<36;i++)p.getInventory().setItem(i,new ItemStack(i%3==0?Items.COOKED_BEEF:i%3==1?Items.APPLE:Items.MUSHROOM_STEW));});
        if(ticks==175){cursor(mc,mc.screen.width-5,mc.screen.height-5);shot("05-inventory-unlocked");}
        if(ticks==180){mc.setScreen(null);server(mc,()->reset(player(mc)));}
        if(ticks==195)shot("06-health-normal");
        if(ticks==200)server(mc,()->player(mc).addEffect(new MobEffectInstance(MobEffects.POISON,100,0)));
        if(ticks==215)shot("07-health-poison");
        if(ticks==220)server(mc,()->{clear(player(mc));player(mc).addEffect(new MobEffectInstance(MobEffects.WITHER,100,0));});
        if(ticks==235)shot("08-health-wither");
        if(ticks==240)server(mc,()->clear(player(mc)));
        if(ticks>=245&&ticks<270){mc.player.setTicksFrozen(200);server(mc,()->player(mc).setTicksFrozen(200));}
        if(ticks==260)shot("09-health-frozen");
        if(ticks==261)hardcore(mc,true);
        if(ticks==265)shot("09b-health-hardcore-frozen");
        if(ticks==269)hardcore(mc,false);
        if(ticks==270)server(mc,()->{clear(player(mc));player(mc).addEffect(new MobEffectInstance(MobEffects.ABSORPTION,500,0));player(mc).setAbsorptionAmount(4);});
        if(ticks==285)shot("10-health-absorption");
        if(ticks==286)hardcore(mc,true);
        if(ticks==288)shot("10b-health-hardcore-absorption");
        if(ticks==290){hardcore(mc,true);server(mc,()->clear(player(mc)));}
        if(ticks==305)shot("11-health-hardcore");
        if(ticks==310)server(mc,()->player(mc).addEffect(new MobEffectInstance(MobEffects.POISON,100,0)));
        if(ticks==320)shot("12-health-hardcore-poison");
        if(ticks==325)server(mc,()->{clear(player(mc));player(mc).addEffect(new MobEffectInstance(MobEffects.WITHER,100,0));});
        if(ticks==335)shot("13-health-hardcore-wither");
        if(ticks==340){hardcore(mc,false);server(mc,()->clear(player(mc)));tree(mc,0);}
        if(ticks==355)shot("14-tree-own-placeholder");
        if(ticks==360)tree(mc,1);
        if(ticks==375){mc.screen.keyPressed(268,0,0);shot("15-tree-parcool-overview");}
        if(ticks==385){parkourId=details(mc,1);}
        if(ticks==395)shot("16-parcool-skill-description");
        if(ticks==400){for(var child:mc.screen.children())if(child instanceof SkillTreeButton b&&b.active){click(mc,b.getX()+20,b.getY()+20);break;}}
        if(ticks==420){check(ClientData.get().unlockedSkills.contains(parkourId),"ParCool learned through real menu packet");shot("17-parcool-skill-learned");}
        if(ticks==425)mc.screen.keyPressed(256,0,0);
        if(ticks==430){var s=mc.screen;s.mouseClicked(sx(s,440),sy(s,260),1);}
        if(ticks>430&&ticks<=450){var s=mc.screen;s.mouseDragged(sx(s,440+(ticks-430)*3),sy(s,260-(ticks-430)),1,3*scale(s),-scale(s));}
        if(ticks==451){var s=mc.screen;s.mouseReleased(sx(s,500),sy(s,240),1);}
        if(ticks>=460&&ticks<480&&ticks%2==0){var s=mc.screen;s.mouseScrolled(sx(s,320),sy(s,180),.2);}
        if(ticks>=430&&ticks<=490&&ticks%2==0)frame("tree-pan-zoom");
        if(ticks==495)tree(mc,2);
        if(ticks==510){mc.screen.keyPressed(268,0,0);shot("18-tree-epic-overview");}
        if(ticks==520)epicId=details(mc,2);
        if(ticks==530)shot("19-epic-skill-description");
        if(ticks==535){for(var child:mc.screen.children())if(child instanceof SkillTreeButton b&&b.active){click(mc,b.getX()+20,b.getY()+20);break;}}
        if(ticks==550){check(ClientData.get().unlockedSkills.contains(epicId),"Epic learned through real menu packet");shot("20-epic-skill-learned");}
        if(ticks==555){for(var child:mc.screen.children())if(child instanceof Button b&&!(b instanceof SkillTreeButton)&&b.active&&b.getX()==176&&b.getY()>=258){click(mc,b.getX()+20,b.getY()+10);break;}}
        if(ticks==570)shot("21-epic-skill-equipped");
        if(ticks==575)tree(mc,3);
        if(ticks==590)shot("22-tree-irons-placeholder");
        if(ticks==595){mc.setScreen(null);server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);d.stamina=d.maxStamina();d.regenCooldown=20000;ModNetworking.sync(p);});}
        if(ticks==600)server(mc,()->{var d=ProgressionManager.get(player(mc));d.stamina=d.maxStamina()*.4;d.regenCooldown=20000;sync(mc);});
        if(ticks>=610&&ticks<=660)mc.player.setAirSupply(120);
        if(ticks==625)server(mc,()->horse(player(mc)));
        if(ticks==635)server(mc,()->player(mc).giveExperiencePoints(31));
        if(ticks==648){check(dev.rpgprogression.client.hud.CenteredHud.oxygenVisible()&&dev.rpgprogression.client.hud.CenteredHud.riddenAnimal()!=null,"air and mount visible together");shot("23-hud-all-indicators");}
        if(ticks==675)server(mc,()->{player(mc).stopRiding();mount.discard();mount=null;});
        if(ticks==725)server(mc,()->{var d=ProgressionManager.get(player(mc));d.stamina=d.maxStamina();sync(mc);});
        if(ticks>=598&&ticks<=755&&ticks%2==0)frame("hud-appearance-layout");
        if(ticks==760)shot("24-hud-unused-indicators-hidden");
        if(ticks==800)server(mc,()->{var d=ProgressionManager.get(player(mc));d.stamina=0;d.regenCooldown=30;dev.rpgprogression.stamina.StaminaManager.updateExhaustion(player(mc),d);sync(mc);});
        if(ticks==810)shot("25-stamina-exhausted");
        if(ticks>=798&&ticks<=1100&&ticks%4==0)frame("stamina-recovery");
        if(ticks==1050)shot("26-stamina-recovering");
        if(ticks==1105)server(mc,()->{var d=ProgressionManager.get(player(mc));d.mana=0;d.manaRegenCooldown=0;sync(mc);});
        if(ticks>=1110&&ticks<=1990&&ticks%5==0)frame("mana-recovery");
        if(ticks>=1110&&ticks<=1910&&(ticks-1110)%100==0)shot("27-mana-state-"+((ticks-1110)/100));
        if(ticks==1500){io.redspace.ironsspellbooks.player.ClientMagicData.updateSpellSelectionManager();io.redspace.ironsspellbooks.player.ClientMagicData.getSpellSelectionManager().makeSelection(1);}
        if(ticks==1520)shot("28-mana-selected-heal");
        if(ticks==1600)io.redspace.ironsspellbooks.player.ClientMagicData.getSpellSelectionManager().makeSelection(0);
        if(ticks==1995)server(mc,()->dev.rpgprogression.effect.ModEffects.exhaust(player(mc),60));
        if(ticks>=1994&&ticks<=2080&&ticks%2==0)frame("spark-exhaustion");
        if(ticks==2015)shot("29-spark-exhaustion-world");
        if(ticks==2085)server(mc,()->dev.rpgprogression.resource.SparkManager.set(player(mc),ProgressionManager.get(player(mc)),0));
        if(ticks==2090)mc.setScreen(new InventoryScreen(mc.player));
        if(ticks==2105)shot("30-spark-exhaustion-icon");
        if(ticks==2110){mc.setScreen(new CharacterScreen());cursor(mc,mc.screen.width-5,mc.screen.height-5);}
        if(ticks==2125)shot("31-character-empty-spark");
        if(ticks==2140)server(mc,()->{clear(player(mc));var d=ProgressionManager.get(player(mc));dev.rpgprogression.food.FoodManager.consume(player(mc),new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));sync(mc);});
        if(ticks==2155)shot("32-character-food-protection");
        if(ticks==2160){mc.setScreen(null);server(mc,()->{clear(player(mc));if(originalRequirements!=null)RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(originalRequirements);mc.getSingleplayerServer().getPlayerList().saveAll();});}
        if(ticks==2190){check(ClientData.get().manaCapacity==8,"eight mana capacity retained");check(!hardcore,"hardcore visual flag restored");LogUtils.getLogger().info("HEART_SHOWCASE_RESULT {}",failed?"FAILED":"PASSED");mc.stop();}
    }
    private static void quick(Minecraft mc){
        if(ticks==20){
            mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.bobView().set(false);
            server(mc,()->{var p=player(mc);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=true;EpicTreeClientCheck.prepareFixture(p);reset(p);var d=ProgressionManager.get(p);d.level=1;d.points=0;d.skillPoints=0;d.skillMilestones=0;d.set(RPGAttribute.SPARK_POTENTIAL,10);d.spark=25;d.mana=0;ProgressionManager.refresh(p,10,false);p.setHealth(p.getMaxHealth());ModNetworking.sync(p);});
        }
        if(ticks==60){mc.setScreen(new CharacterScreen());cursor(mc,mc.screen.width-5,mc.screen.height-5);}
        if(ticks==75){check(ClientData.get().level==1&&ClientData.get().requiredXP()==500&&ClientData.get().manaCapacity==0,"starting character level1, threshold500 and no mana");shot("00-starting-character-ru");}
        if(ticks==80){mc.getLanguageManager().setSelected("en_us");mc.options.languageCode="en_us";mc.reloadResourcePacks();}
        if(ticks==135)shot("33-starting-character-en");
        if(ticks==145)tree(mc,0);
        if(ticks==160)shot("34-own-tree-en");
        if(ticks==165)tree(mc,1);
        if(ticks==180){mc.screen.keyPressed(268,0,0);shot("35-parcool-tree-en");}
        if(ticks==185)details(mc,1);
        if(ticks==200)shot("36-parcool-description-en");
        if(ticks==205)tree(mc,2);
        if(ticks==220){mc.screen.keyPressed(268,0,0);shot("37-epic-tree-en");}
        if(ticks==225)details(mc,2);
        if(ticks==240)shot("38-epic-description-en");
        if(ticks==245)tree(mc,3);
        if(ticks==260)shot("39-irons-tree-en");
        if(ticks==270){mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();}
        if(ticks==300){mc.setScreen(null);LogUtils.getLogger().info("HEART_SHOWCASE_QUICK_RESULT {}",failed?"FAILED":"PASSED");mc.stop();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
        if(captures.isEmpty()||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var pending=List.copyOf(captures);captures.clear();
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            for(var name:pending){var file=mc.gameDirectory.toPath().resolve("screenshots/heart-1.0.0").resolve(name);Files.createDirectories(file.getParent());pixels.writeToFile(file);
                var s=new com.google.gson.JsonObject();var d=ClientData.get();s.addProperty("tick",ticks);s.addProperty("health",mc.player.getHealth());s.addProperty("stamina",d.stamina);s.addProperty("maximumStamina",d.maxStamina());s.addProperty("mana",d.mana);s.addProperty("manaCapacity",d.manaCapacity);s.addProperty("air",mc.player.getAirSupply());s.addProperty("mounted",mc.player.isPassenger());s.addProperty("hardcore",hardcore);s.addProperty("screen",mc.screen==null?"HUD":mc.screen.getClass().getSimpleName());states.add(name,s);
                LogUtils.getLogger().info("HEART_SHOWCASE capture {}",name);
            }
            var index=mc.gameDirectory.toPath().resolve("screenshots/heart-1.0.0/states.json");Files.writeString(index,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(states));
        }catch(Exception error){check(false,"framebuffer capture: "+error);}
    }
    private ShowcaseClient(){}
}
