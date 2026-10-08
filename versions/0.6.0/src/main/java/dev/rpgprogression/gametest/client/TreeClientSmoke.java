package dev.rpgprogression.gametest.client;

import com.mojang.logging.LogUtils;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.*;
import dev.rpgprogression.client.gui.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;

/** Real menu clicks, C2S/S2C unlocks and optional native skill containers; excluded from release. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class TreeClientSmoke {
    private static boolean loading;private static volatile boolean failed;private static int ticks;private static String capture,parkourId,epicId;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("rpgprogression.treeSmoke")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld","RPGTreeV150"));return;}
        if(loading&&mc.player==null){
            String label=mc.screen instanceof BackupConfirmScreen?net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;
        }
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        ticks++;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        boolean parcool=dev.rpgprogression.skill.SkillTreeManager.parcool(),epic=dev.rpgprogression.skill.SkillTreeManager.epic();
        if(Boolean.getBoolean("rpgprogression.treeRelogin")){
            if(ticks==35){
                try{
                    var expected=net.minecraft.nbt.TagParser.parseTag(Files.readString(stateFile(mc)));
                    var actual=ClientData.get().save();
                    check(ClientData.get().level==20&&actual.getInt("skillPoints")==expected.getInt("skillPoints")&&actual.getInt("skillMilestones")==4,"skill point balance persists on a second client launch");
                    check(actual.getList("unlockedSkills",8).equals(expected.getList("unlockedSkills",8)),"all learned tree skills persist after restart");
                    server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);SkillTreeManager.refresh(p,d);if(epic)EpicTreeClientCheck.learned(p,d.unlockedSkills,TreeClientSmoke::check);});
                }catch(Exception error){check(false,"relogin: "+error);}
            }
            if(ticks==65)finish(mc,"RELOGIN");return;
        }
        if(ticks==30)server(mc,()->{
            var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.removeAllEffects();p.setAbsorptionAmount(0);p.getAbilities().invulnerable=true;
            if(epic)EpicTreeClientCheck.prepareFixture(p);
            var d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();d.level=20;ProgressionManager.refresh(p,10,false);p.setHealth(p.getMaxHealth());
        });
        if(ticks==65){
            check(ClientData.get().skillPoints==4,"separate skill points synchronized at level20");
            for(var a:RPGAttribute.values())check(ClientData.get().get(a)==10,"baseline10: "+a);
            mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new CharacterScreen());cursor(mc,140,148);
        }
        if(ticks==85)capture="menu-normal-scale2.png";
        if(ticks==90)cursor(mc,404,327);
        if(ticks==95)capture="menu-hover-scale2.png";
        if(ticks==100){click(mc,404,327);check(mc.screen instanceof SkillTreeScreen,"main atlas button opens skill tree");cursor(mc,620,350);}
        if(ticks==125){check(ClientSkillTree.ready,"server catalog received");capture="tree-empty.png";}
        if(ticks==135&&parcool){click(mc,246,46);cursor(mc,620,350);}
        if(ticks==155&&parcool)capture="tree-parcool.png";
        if(ticks==165&&parcool){parkourId=choose(mc,1);cursorOnSkill(mc,parkourId);}
        if(ticks==170&&parcool)capture="tree-parcool-hover.png";
        if(ticks==175&&parcool){clickSkill(mc,parkourId);cursor(mc,620,350);}
        if(ticks==200&&parcool){check(ClientData.get().unlockedSkills.contains(parkourId)&&ClientData.get().skillPoints==3,"real ParCool unlock packet charges one RPG point");capture="tree-parcool-learned.png";}
        if(ticks==205&&parcool)server(mc,()->{var p=player(mc);ProgressionManager.get(p).skillPoints=0;SkillTreeManager.sync(p);});
        if(ticks==230&&parcool)capture="tree-parcool-disabled.png";
        if(ticks==235&&parcool)server(mc,()->{var p=player(mc);ProgressionManager.get(p).skillPoints=3;SkillTreeManager.sync(p);});
        if(ticks==250&&epic){click(mc,395,46);cursor(mc,620,350);}
        if(ticks==270&&epic)capture="tree-epic.png";
        if(ticks==280&&epic){epicId=choose(mc,2);server(mc,()->EpicTreeClientCheck.blockEquipForFixture(player(mc),epicId));clickSkill(mc,epicId);cursor(mc,620,350);}
        if(ticks==310&&epic){
            check(ClientData.get().unlockedSkills.contains(epicId),"real Epic unlock packet learns native skill");
            check(mc.screen.children().stream().filter(child->child instanceof Button b&&!(b instanceof SkillTreeButton)&&b.getX()==437&&b.getY()>=220).anyMatch(child->!((Button)child).active),"native replacement cooldown disables equip button");
        }
        if(ticks==335&&epic){
            check(mc.screen.children().stream().anyMatch(child->child instanceof Button b&&!(b instanceof SkillTreeButton)&&b.getX()==437&&b.getY()>=220&&b.active),"equip button refreshes after cooldown without reopening tree");
            for(var child:mc.screen.children())if(child instanceof Button b&&!(b instanceof SkillTreeButton)&&b.getX()==437&&b.getY()>=220&&b.active){click(mc,b.getX()+80,b.getY()+10);break;}
        }
        if(ticks==345&&epic){server(mc,()->EpicTreeClientCheck.equipped(player(mc),epicId,TreeClientSmoke::check));capture="tree-epic-equipped.png";}
        if(ticks==360&&parcool){ParCoolTreeClientCheck.open(mc);check(mc.screen instanceof SkillTreeScreen,"native ParCool menu replaced by ours");cursor(mc,620,350);}
        if(ticks==380&&parcool)capture="tree-parcool-replaced.png";
        if(ticks==390&&ClientSkillTree.irons){click(mc,544,46);cursor(mc,620,350);}
        if(ticks==410&&ClientSkillTree.irons)capture="tree-irons-empty.png";
        if(ticks==420)server(mc,()->{
            var p=player(mc);var d=ProgressionManager.get(p);d.set(RPGAttribute.INTELLIGENCE,20);ProgressionManager.refresh(p,10,false);
            p.getAbilities().invulnerable=false;p.setHealth(p.getMaxHealth());p.invulnerableTime=0;
            float before=p.getHealth();boolean accepted=p.hurt(p.damageSources().magic(),10);
            check(accepted&&Math.abs(p.getHealth()-(before-9.6))<.01,"real player magic damage: before="+before+", after="+p.getHealth()+", expected="+(before-9.6));
            p.getAbilities().invulnerable=true;p.setHealth(p.getMaxHealth());d.set(RPGAttribute.INTELLIGENCE,10);ProgressionManager.refresh(p,20,false);
        });
        if(ticks==435){mc.options.guiScale().set(3);mc.resizeDisplay();mc.setScreen(new CharacterScreen());cursor(mc,140,148);}
        if(ticks==450)capture="menu-normal-scale3.png";
        if(ticks==460){click(mc,404,327);if(parcool)click(mc,246,46);cursor(mc,620,350);}
        if(ticks==480)capture="tree-scale3.png";
        if(ticks==485)server(mc,()->dev.rpgprogression.effect.ModEffects.exhaust(player(mc),200));
        if(ticks==495)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if(ticks==510){check(mc.getMobEffectTextures().get(dev.rpgprogression.effect.ModEffects.SPARK_EXHAUSTION.get()).contents().width()==16,"supplied16x16 spark effect icon loaded");capture="spark-icon.png";}
        if(ticks==520)server(mc,()->{
            try{var p=player(mc);var d=ProgressionManager.get(p);Files.writeString(stateFile(mc),d.save().toString());p.removeAllEffects();mc.getSingleplayerServer().getPlayerList().saveAll();}catch(Exception error){check(false,"persist fixture: "+error);}
        });
        if(ticks==545)finish(mc,"INITIAL");
    }
    private static Path stateFile(Minecraft mc){return mc.gameDirectory.toPath().resolve("tree-state-v150-"+(SkillTreeManager.parcool()?"combined":"standalone")+".snbt");}
    private static String choose(Minecraft mc,int branch){
        var nodes=ClientSkillTree.nodes.stream().filter(n->n.branch()==branch).toList();var chosen=nodes.stream().filter(n->SkillTreeManager.canUnlock(ClientData.get(),n)).findFirst().orElseThrow();
        int page=nodes.indexOf(chosen)/8;for(int i=0;i<page;i++)click(mc,395,311);return chosen.id();
    }
    private static int skillIndex(String id){var node=ClientSkillTree.nodes.stream().filter(n->n.id().equals(id)).findFirst().orElseThrow();return ClientSkillTree.nodes.stream().filter(n->n.branch()==node.branch()).toList().indexOf(node)%8;}
    private static void cursorOnSkill(Minecraft mc,String id){int i=skillIndex(id);cursor(mc,193+(i%2)*196,105+(i/2)*52);}
    private static void clickSkill(Minecraft mc,String id){int i=skillIndex(id);click(mc,193+(i%2)*196,105+(i/2)*52);}
    private static double scale(Screen screen){return Math.min(1.5,Math.min(screen.width/640.0,screen.height/360.0));}
    private static void click(Minecraft mc,double x,double y){var screen=mc.screen;double s=scale(screen);double sx=(screen.width-640*s)/2+x*s,sy=(screen.height-360*s)/2+y*s;check(screen.mouseClicked(sx,sy,0),"click "+x+","+y);screen.mouseReleased(sx,sy,0);}
    private static void cursor(Minecraft mc,double x,double y){
        var screen=mc.screen;double s=scale(screen);double sx=((screen.width-640*s)/2+x*s)*mc.getWindow().getGuiScale(),sy=((screen.height-360*s)/2+y*s)*mc.getWindow().getGuiScale();
        try{for(String name:new String[]{"xpos","ypos"}){var field=MouseHandler.class.getDeclaredField(name);field.setAccessible(true);field.setDouble(mc.mouseHandler,name.equals("xpos")?sx:sy);}}
        catch(Exception error){check(false,"cursor: "+error);}
    }
    private static net.minecraft.server.level.ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void server(Minecraft mc,Runnable task){mc.getSingleplayerServer().execute(()->{try{task.run();}catch(Throwable error){failed=true;LogUtils.getLogger().error("RPG_TREE_CLIENT exception",error);}});}
    private static void check(boolean result,String message){if(!result){failed=true;LogUtils.getLogger().error("RPG_TREE_CLIENT FAILED {}",message);}else LogUtils.getLogger().info("RPG_TREE_CLIENT verified {}",message);}
    private static void finish(Minecraft mc,String phase){LogUtils.getLogger().info("RPG_TREE_CLIENT_{} {}",phase,failed?"FAILED":"PASSED");mc.stop();}
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
        if(capture==null||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();var name=capture;capture=null;
        var file=mc.gameDirectory.toPath().resolve("screenshots/tree-v150-"+(SkillTreeManager.parcool()?"combined":"standalone")).resolve(name);
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(file.getParent());pixels.writeToFile(file);LogUtils.getLogger().info("RPG_TREE_CLIENT screenshot {}",file);}catch(Exception error){check(false,"screenshot: "+error);}
    }
    private TreeClientSmoke(){}
}
