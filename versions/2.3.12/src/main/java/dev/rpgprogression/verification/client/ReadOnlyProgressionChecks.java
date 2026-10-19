package dev.rpgprogression.verification.client;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.*;
import dev.rpgprogression.client.gui.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.verification.SkintTravelChecks;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/** Real hotkey/mouse/Esc input plus deliberately forged mutation payloads without a Skint session. */
public final class ReadOnlyProgressionChecks {
    private static int stage,age;private static CompoundTag saved;private static CompletableFuture<?> pending;private static String skill;
    private static void check(JsonArray checks,boolean ok,String text){SkintTravelChecks.check(checks,ok,text);if(!ok)throw new IllegalStateException(text);}
    private static void server(Minecraft mc,Runnable action){pending=CompletableFuture.runAsync(action,mc.getSingleplayerServer());}
    private static net.minecraft.server.level.ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void shot(Minecraft mc,String file)throws Exception{var folder=mc.gameDirectory.toPath().resolve("screenshots/requirements-2.3.11");Files.createDirectories(folder);try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder.resolve(file));}}
    private static double scale(Minecraft mc){return Math.min(1.5,Math.min(mc.screen.width/640.0,mc.screen.height/360.0));}
    private static void click(Minecraft mc,double x,double y){double s=scale(mc),px=(mc.screen.width-640*s)/2+x*s,py=(mc.screen.height-360*s)/2+y*s;mc.screen.mouseClicked(px,py,0);mc.screen.mouseReleased(px,py,0);}
    public static boolean tick(Minecraft mc,JsonArray checks)throws Exception{
        if(stage==8)return true;
        if(pending!=null){if(!pending.isDone())return false;pending.join();pending=null;}
        age++;
        switch(stage){
            case 0->{server(mc,()->{var p=player(mc);dev.rpgprogression.skint.SkintManager.close(p,false);var d=ProgressionManager.get(p);saved=d.save();d.points=3;d.skillPoints=3;ModNetworking.sync(p);});KeyMapping.click(ClientRegistration.CHARACTER.getKey());stage++;age=0;}
            case 1->{if(age<12)return false;check(checks,mc.screen instanceof CharacterScreen c&&c.readOnly()&&!ClientSkintData.managed(c),"the character hotkey opens the actual read-only screen without Skint authorization (screen="+mc.screen+")");
                check(checks,mc.screen.children().stream().filter(c->c.getClass().getSimpleName().equals("AttributeButton")).allMatch(c->!((Button)c).active),"all six attribute buttons are inactive even with spare points");
                shot(mc,"12-read-only-character.png");click(mc,574,89);ModNetworking.CHANNEL.sendToServer(new ModNetworking.SpendPoint(RPGAttribute.STRENGTH.ordinal()));
                var button=(Button)mc.screen.children().stream().filter(c->c.getClass().getSimpleName().equals("SkillTreeButton")).findFirst().orElseThrow();click(mc,button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0);stage++;age=0;}
            case 2->{if(age<15||!ClientSkillTree.ready)return false;check(checks,mc.screen instanceof SkillTreeScreen t&&t.readOnly()&&!ClientSkillTree.nodes.isEmpty(),"the server supplies the unified skill catalogue for viewing without Skint access");
                var tree=(SkillTreeScreen)mc.screen;skill=ClientSkillTree.nodes.get(0).id();tree.focusSkill(skill);var xy=tree.skillPosition(skill);click(mc,xy[0],xy[1]);
                check(checks,tree.children().stream().filter(c->c instanceof Button).allMatch(c->!((Button)c).active),"read-only skill details never enable skill equipment changes");
                ModNetworking.CHANNEL.sendToServer(new ModNetworking.UnlockSkill(skill));ModNetworking.CHANNEL.sendToServer(new ModNetworking.EquipSkill(skill,0));stage++;age=0;}
            case 3->{if(age<15)return false;shot(mc,"13-read-only-tree.png");server(mc,()->{var d=ProgressionManager.get(player(mc));check(checks,d.points==3&&d.skillPoints==3&&d.get(RPGAttribute.STRENGTH)==saved.getInt("strength")&&d.unlockedSkills.equals(new java.util.HashSet<>(saved.getList("unlockedSkills",8).stream().map(net.minecraft.nbt.Tag::getAsString).toList())),"forged spending, unlocking and equipping payloads cannot change progression outside Skint authorization");});check(checks,mc.screen.keyPressed(256,0,0)&&mc.screen instanceof CharacterScreen,"Esc from the viewing tree returns to the viewing character screen");stage++;age=0;}
            case 4->{if(age<5)return false;check(checks,mc.screen.keyPressed(ClientRegistration.CHARACTER.getKey().getValue(),0,0)&&mc.screen==null,"the hotkey closes the viewing menu back to the world");check(checks,!ClientSkintData.active()&&!SkintCamera.active()&&!(mc.player.getVehicle() instanceof dev.rpgprogression.skint.SkintSeat),"viewing progression never creates a seat, camera cutscene or Skint session");server(mc,()->{var p=player(mc);ProgressionManager.get(p).load(saved);ModNetworking.sync(p);});stage++;age=0;}
            case 5->{if(age<5)return false;stage=8;return true;}
        }
        return false;
    }
    private ReadOnlyProgressionChecks(){}
}
