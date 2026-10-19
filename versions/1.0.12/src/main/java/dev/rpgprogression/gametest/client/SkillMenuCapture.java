package dev.rpgprogression.gametest.client;

import com.mojang.logging.LogUtils;
import dev.rpgprogression.client.*;
import dev.rpgprogression.client.gui.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Native menu interaction and screenshots in a copied world; never shipped in the release JAR. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class SkillMenuCapture {
    private static final List<String> ICONS=List.of("climb_up","hang_on","slide_down","pole_climb","hang_down");
    private static final List<String> EPIC=List.of("bonebreaker","vengeance","catharsis");
    private static boolean loading;
    private static volatile boolean failed;
    private static int ticks;
    private static String originalLanguage,longestId;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static final List<String> captures=new ArrayList<>();
    private static final com.google.gson.JsonObject states=new com.google.gson.JsonObject();
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void check(boolean ok,String message){if(!ok)failed=true;LogUtils.getLogger().info("HEART_SKILL_MENU {} {}",ok?"verified":"FAILED",message);}
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable error){failed=true;LogUtils.getLogger().error("HEART_SKILL_MENU server failure",error);}});}
    private static Object field(Object object,String name){try{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception error){throw new IllegalStateException(error);}}
    private static SkillInfoTooltip info(Minecraft mc){return (SkillInfoTooltip)field(mc.screen,"info");}
    private static String pinned(Minecraft mc){return (String)field(mc.screen,"pinnedId");}
    private static String described(Minecraft mc){return (String)field(mc.screen,"hoverId");}
    private static double scale(Screen s){return Math.min(1.5,Math.min(s.width/640.0,s.height/360.0));}
    private static double sx(Screen s,double x){return (s.width-640*scale(s))/2+x*scale(s);}
    private static double sy(Screen s,double y){return (s.height-360*scale(s))/2+y*scale(s);}
    private static void cursor(Minecraft mc,double x,double y){
        try{var s=mc.screen;for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,(name.equals("xpos")?sx(s,x):sy(s,y))*mc.getWindow().getGuiScale());}}
        catch(Exception error){throw new IllegalStateException(error);}
    }
    private static void away(Minecraft mc){cursor(mc,638,358);}
    private static void hover(Minecraft mc,String id){var p=((SkillTreeScreen)mc.screen).skillPosition(id);cursor(mc,p[0],p[1]);}
    private static void click(Minecraft mc,double x,double y){var s=mc.screen;s.mouseClicked(sx(s,x),sy(s,y),0);s.mouseReleased(sx(s,x),sy(s,y),0);}
    private static void clickNode(Minecraft mc,String id){var p=((SkillTreeScreen)mc.screen).skillPosition(id);click(mc,p[0],p[1]);}
    private static void emptyClick(Minecraft mc){
        try{var s=mc.screen;var method=SkillTreeScreen.class.getDeclaredMethod("hit",double.class,double.class);method.setAccessible(true);
            for(int y=76;y<313;y+=8)for(int x=140;x<501;x+=8)if(((String)method.invoke(s,(double)x,(double)y)).isEmpty()&&(info(mc)==null||!info(mc).contains(x,y))){click(mc,x,y);away(mc);return;}
        }catch(Exception error){throw new IllegalStateException(error);}throw new IllegalStateException("No empty viewport position");
    }
    private static void tree(Minecraft mc,String id){mc.setScreen(new SkillTreeScreen(new CharacterScreen(),id.startsWith("parcool:")?1:2));((SkillTreeScreen)mc.screen).focusSkill(id);away(mc);}
    private static void language(Minecraft mc,String value){mc.getLanguageManager().setSelected(value);mc.options.languageCode=value;reload=mc.reloadResourcePacks();}
    private static void shot(String name){captures.add(name+".png");}
    private static void title(Minecraft mc,String expected,boolean hitBold){
        var component=(Component)field(info(mc),"title");check(component.getString().equals(expected),"localized title: "+expected);
        var glyphs=new StringBuilder();var bold=new ArrayList<Boolean>();component.getVisualOrderText().accept((index,style,cp)->{glyphs.appendCodePoint(cp);bold.add(style.isBold());return true;});
        check(glyphs.toString().equals(expected)&&java.util.stream.IntStream.range(0,bold.size()).allMatch(i->bold.get(i)==(hitBold&&i<3)),"only the three letters of Bey are bold when requested");
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("rpgprogression.skillMenuCapture")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld"));return;}
        if(loading&&mc.player==null){String label=mc.screen instanceof BackupConfirmScreen?Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;}
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        if(reload!=null){if(!reload.isDone())return;reload.join();reload=null;}if(mc.getOverlay()!=null)return;
        ticks++;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        if(ticks==20){
            check(SkillTreeManager.parcool()&&SkillTreeManager.epic(),"native ParCool and Epic Fight are loaded");mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.pauseOnLostFocus=false;
            originalLanguage=mc.getLanguageManager().getSelected();language(mc,"ru_ru");
            server(mc,()->{var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.stopRiding();p.removeAllEffects();p.getInventory().clearContent();EpicTreeClientCheck.prepareFixture(p);
                var d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();d.level=25;d.skillMilestones=5;d.skillPoints=4;ProgressionManager.refresh(p,10,false);SkillTreeManager.refresh(p,d);ModNetworking.sync(p);});
        }
        if(ticks==40)ModNetworking.CHANNEL.sendToServer(new ModNetworking.RequestTree());
        if(ticks==60){
            check(ClientSkillTree.ready&&ICONS.stream().allMatch(id->ClientSkillTree.nodes.stream().anyMatch(n->n.id().equals("parcool:"+id))),"all five new nodes received from server");
            for(var id:ICONS)check(mc.getResourceManager().getResource(GuiTextures.texture("icons/"+id)).isPresent(),"supplied icon loaded: "+id);
            check(mc.getResourceManager().getResource(GuiTextures.texture("icons/placeholder")).isPresent(),"supplied placeholder loaded");tree(mc,"parcool:crawl");
        }
        if(ticks==80)hover(mc,"parcool:crawl");
        if(ticks==85){check(info(mc)!=null&&pinned(mc).isEmpty(),"hover shows an unpinned description");shot("01-hover");}
        if(ticks==90)away(mc);
        if(ticks==100){check(info(mc)==null&&pinned(mc).isEmpty(),"moving off the icon hides an unpinned description");shot("02-hover-left");}
        if(ticks==105){hover(mc,"parcool:crawl");clickNode(mc,"parcool:crawl");}
        if(ticks==130)away(mc);
        if(ticks==140){check(ClientData.get().unlockedSkills.contains("parcool:crawl")&&ClientData.get().skillPoints==3&&pinned(mc).equals("parcool:crawl")&&info(mc)!=null,"one click learns for one point and pins after leaving");shot("03-click-pinned");}
        if(ticks==145)server(mc,()->SkillTreeManager.sync(player(mc)));
        if(ticks==165)check(pinned(mc).equals("parcool:crawl")&&info(mc)!=null,"server refresh preserves the pinned description");
        if(ticks==170)hover(mc,"parcool:slide");
        if(ticks==185)check(described(mc).equals("parcool:slide")&&pinned(mc).equals("parcool:crawl"),"hover previews another skill without replacing the pin");
        if(ticks==190)away(mc);
        if(ticks==200)check(described(mc).equals("parcool:crawl")&&info(mc)!=null,"leaving another preview restores the pinned skill");
        if(ticks==205)emptyClick(mc);
        if(ticks==220)check(pinned(mc).isEmpty()&&info(mc)==null,"empty click dismisses the pin");
        if(ticks==225){tree(mc,"parcool:slide");var s=(SkillTreeScreen)mc.screen;var p=s.skillPosition("parcool:slide");s.mouseClicked(sx(s,p[0]),sy(s,p[1]),0);s.mouseDragged(sx(s,p[0]+24),sy(s,p[1]),0,24*scale(s),0);s.mouseReleased(sx(s,p[0]+24),sy(s,p[1]),0);away(mc);}
        if(ticks==245)check(pinned(mc).isEmpty()&&!ClientData.get().unlockedSkills.contains("parcool:slide")&&ClientData.get().skillPoints==3,"dragging an available icon neither pins nor spends a point");
        if(ticks==250)server(mc,()->{var p=player(mc);ProgressionManager.get(p).skillPoints=0;SkillTreeManager.sync(p);});
        for(int i=0;i<ICONS.size();i++){
            String id="parcool:"+ICONS.get(i);int start=270+i*50;
            if(ticks==start)tree(mc,id);
            if(ticks==start+5)hover(mc,id);
            if(ticks==start+10)clickNode(mc,id);
            if(ticks==start+25)away(mc);
            if(ticks==start+35){check(pinned(mc).equals(id)&&info(mc)!=null&&ClientData.get().skillPoints==0&&!ClientData.get().unlockedSkills.contains(id),"locked skill pins without spending: "+id);shot(String.format(Locale.ROOT,"%02d-%s",i+4,ICONS.get(i)));}
        }
        if(ticks==515){tree(mc,"parcool:crawl");mc.screen.keyPressed(268,0,0);}
        if(ticks==540)shot("09-parcool-overview");
        for(int i=0;i<EPIC.size();i++){
            String id="epicfight:"+EPIC.get(i);int start=550+i*50;
            if(ticks==start)tree(mc,id);
            if(ticks==start+5)hover(mc,id);
            if(ticks==start+10)title(mc,List.of("Костолом","Бей или беги","Катарсис").get(i),i==1);
            if(ticks==start+15)clickNode(mc,id);
            if(ticks==start+25)away(mc);
            if(ticks==start+35){check(pinned(mc).equals(id)&&info(mc)!=null,"Epic placeholder skill remains pinned: "+id);shot(String.format(Locale.ROOT,"%02d-%s",i+10,EPIC.get(i)));}
        }
        if(ticks==700)server(mc,()->{var p=player(mc);ProgressionManager.get(p).skillPoints=4;check(SkillTreeManager.unlock(p,"epicfight:roll")&&SkillTreeManager.unlock(p,"epicfight:step"),"native dodge fixture learned");});
        if(ticks==720){tree(mc,"epicfight:step");clickNode(mc,"epicfight:step");away(mc);}
        if(ticks==770){var action=info(mc).buttons().stream().filter(b->b.active).findFirst().orElseThrow();cursor(mc,action.getX()+50,action.getY()+10);}
        if(ticks==775){check(pinned(mc).equals("epicfight:step")&&info(mc)!=null,"moving onto a pinned Epic action preserves the panel");var action=info(mc).buttons().stream().filter(b->b.active).findFirst().orElseThrow();click(mc,action.getX()+50,action.getY()+10);}
        if(ticks==790){check(pinned(mc).equals("epicfight:step")&&info(mc)!=null,"equipping and its server refresh preserve the pin");server(mc,()->EpicTreeClientCheck.equipped(player(mc),"epicfight:step",SkillMenuCapture::check));away(mc);shot("13-epic-equipped-pinned");}
        if(ticks==795){
            longestId=ClientSkillTree.nodes.stream().filter(n->n.branch()==2).max(Comparator.comparingInt(n->SkillDescriptions.describe(n).stream().mapToInt(c->c.getString().length()).sum()+n.parents().size()*60)).orElseThrow().id();
            // A learned passive has native slot actions and can need scrolling even
            // when its unlearned description fits. Restore a valid learned chain.
            server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);d.skillPoints=0;
                var catalog=SkillTreeManager.catalog(p).stream().collect(java.util.stream.Collectors.toMap(SkillNode::id,n->n));var pending=new ArrayDeque<String>();pending.add(longestId);
                while(!pending.isEmpty()){var id=pending.remove();if(d.unlockedSkills.add(id)&&catalog.containsKey(id))pending.addAll(catalog.get(id).parents());}
                SkillTreeManager.refresh(p,d);SkillTreeManager.sync(p);});
        }
        if(ticks==805){tree(mc,longestId);hover(mc,longestId);clickNode(mc,longestId);}
        if(ticks==830){
            var t=info(mc);var before=((SkillTreeScreen)mc.screen).skillPosition(longestId);boolean handled=true;cursor(mc,t.x()+40,t.y()+90);
            for(int i=0;i<100;i++)handled&=mc.screen.mouseScrolled(sx(mc.screen,t.x()+40),sy(mc.screen,t.y()+90),-1);
            check(handled&&t.scroll()==t.maximumScroll()&&pinned(mc).equals(longestId)&&Arrays.equals(before,((SkillTreeScreen)mc.screen).skillPosition(longestId)),"wheel events belong to the pinned panel and never zoom or dismiss the tree");
            LogUtils.getLogger().info("HEART_SKILL_MENU description scroll range {}",t.maximumScroll());shot("14-long-description-bottom");
        }
        if(ticks==835){mc.options.guiScale().set(3);mc.resizeDisplay();away(mc);}
        if(ticks==850){check(pinned(mc).equals(longestId)&&info(mc)!=null,"GUI resize preserves the pin");shot("15-scale3-pinned");}
        if(ticks==860){mc.options.guiScale().set(2);mc.resizeDisplay();language(mc,"en_us");}
        if(ticks==890){tree(mc,"epicfight:vengeance");hover(mc,"epicfight:vengeance");}
        if(ticks==900){title(mc,"Vengeance",false);shot("16-english");}
        if(ticks==905){mc.setScreen(null);language(mc,originalLanguage);}
        if(ticks==925){LogUtils.getLogger().info("HEART_SKILL_MENU_RESULT {}",failed?"FAILED":"PASSED");mc.stop();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
        if(captures.isEmpty()||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();var pending=List.copyOf(captures);captures.clear();
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            var dir=mc.gameDirectory.toPath().resolve("screenshots/menu-1.0.12");Files.createDirectories(dir);
            for(var name:pending){pixels.writeToFile(dir.resolve(name));var state=new com.google.gson.JsonObject();state.addProperty("tick",ticks);state.addProperty("skillPoints",ClientData.get().skillPoints);state.addProperty("pinned",pinned(mc));state.addProperty("description",described(mc));state.addProperty("guiScale",mc.options.guiScale().get());state.addProperty("language",mc.getLanguageManager().getSelected());
                if(info(mc)!=null){var t=info(mc);state.addProperty("scroll",t.scroll());state.addProperty("maximumScroll",t.maximumScroll());state.addProperty("title",((Component)field(t,"title")).getString());}
                states.add(name,state);LogUtils.getLogger().info("HEART_SKILL_MENU capture {}",name);
            }
            var mods=new com.google.gson.JsonArray();for(var mod:ModList.get().getMods()){var entry=new com.google.gson.JsonObject();entry.addProperty("id",mod.getModId());entry.addProperty("version",mod.getVersion().toString());mods.add(entry);}
            var report=new com.google.gson.JsonObject();report.add("loadedMods",mods);report.add("screenshots",states);Files.writeString(dir.resolve("capture-states.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception error){check(false,"framebuffer capture: "+error);}
    }
    private SkillMenuCapture(){}
}
