package dev.rpgprogression.verification.client;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.requirements.ItemRequirements;
import dev.rpgprogression.verification.LockpickRequirementChecks;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class LockpickRequirementClientSmoke {
    private static final JsonArray checks=new JsonArray();private static int age,stage,boot;private static boolean loading,done;private static CompletableFuture<?> pending;
    private static Path folder(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/lockpick-2.3.31");}
    private static void next(){stage++;age=0;}
    private static void check(boolean ok,String text){LockpickRequirementChecks.check(checks,ok,text);}
    private static void agility(Minecraft mc,int value){pending=CompletableFuture.runAsync(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.setGameMode(GameType.SURVIVAL);ProgressionManager.get(p).set(RPGAttribute.AGILITY,value);ModNetworking.sync(p);},mc.getSingleplayerServer());}
    private static boolean heading(Component line){return line.getContents() instanceof TranslatableContents t&&t.getKey().equals("rpgprogression.requirements.title");}
    private static void verify(Minecraft mc,boolean missing){var lines=LockpickRequirementChecks.pick().getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.Default.NORMAL);check(missing?lines.size()>2&&heading(lines.get(1))&&lines.get(2).getString().contains(Component.translatable(RPGAttribute.AGILITY.key()).getString())&&lines.get(2).getString().contains("20"):lines.stream().noneMatch(LockpickRequirementClientSmoke::heading),missing?"native localized lockpick tooltip shows missing Agility 20 under the item name":"native tooltip removes the requirement exactly at Agility 20");}
    private static void capture(Minecraft mc,String name)throws Exception{Files.createDirectories(folder(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder(mc).resolve(name));}}
    private static final class Preview extends Screen {
        Preview(){super(Component.literal("Clavis — "+ClientData.get().get(RPGAttribute.AGILITY)+" / 20"));}
        @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){g.fill(0,0,width,height,0xFF211D19);g.drawCenteredString(font,title,width/2,height/2-65,0xFFE9C477);g.renderItem(LockpickRequirementChecks.pick(),width/2-95,height/2-7);g.renderTooltip(font,LockpickRequirementChecks.pick(),width/2-65,height/2-15);}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(done||!Boolean.getBoolean("rpgprogression.lockpickClientSmoke"))return;var mc=Minecraft.getInstance();
        try{
            if(mc.player==null||mc.getSingleplayerServer()==null){if(++boot>6000)throw new IllegalStateException("startup timeout");if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var previous=mc.screen;mc.createWorldOpenFlows().openWorld(System.getProperty("rpgprogression.smokeWorld"),()->mc.setScreen(previous));}return;}
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}if(mc.getOverlay()!=null)return;if(++age>600)throw new IllegalStateException("stage timeout "+stage);
            switch(stage){
                case 0->{if(age<30)return;mc.options.guiScale().set(2);mc.resizeDisplay();pending=CompletableFuture.runAsync(()->{try{LockpickRequirementChecks.run(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()),checks);}catch(Exception e){throw new IllegalStateException(e);}},mc.getSingleplayerServer());next();}
                case 1->{mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";pending=mc.reloadResourcePacks();next();}
                case 2->{agility(mc,19);next();}
                case 3->{if(age<12||ClientData.get().get(RPGAttribute.AGILITY)!=19)return;verify(mc,true);check(!ItemRequirements.allowed(mc.player,LockpickRequirementChecks.pick()),"the synced client also refuses the lockpick at Agility 19");mc.setScreen(new Preview());next();}
                case 4->{if(age<10)return;capture(mc,"01-agility-19-ru.png");mc.setScreen(null);agility(mc,20);next();}
                case 5->{if(age<12||ClientData.get().get(RPGAttribute.AGILITY)!=20)return;verify(mc,false);check(ItemRequirements.allowed(mc.player,LockpickRequirementChecks.pick()),"the synced client permits the lockpick at exact Agility 20");mc.setScreen(new Preview());next();}
                case 6->{if(age<10)return;capture(mc,"02-agility-20-ru.png");mc.setScreen(null);mc.getLanguageManager().setSelected("en_us");mc.options.languageCode="en_us";pending=mc.reloadResourcePacks();next();}
                case 7->{agility(mc,19);next();}
                case 8->{if(age<12||ClientData.get().get(RPGAttribute.AGILITY)!=19)return;verify(mc,true);mc.setScreen(new Preview());next();}
                case 9->{if(age<10)return;capture(mc,"03-agility-19-en.png");mc.setScreen(null);done=true;LockpickRequirementChecks.report(folder(mc).resolve("result.json"),checks,null);mc.stop();}
            }
        }catch(Throwable e){done=true;com.mojang.logging.LogUtils.getLogger().error("LOCKPICK_CLIENT stage {}",stage,e);try{LockpickRequirementChecks.report(folder(mc).resolve("result.json"),checks,e);}catch(Exception ex){throw new IllegalStateException(ex);}mc.stop();}
    }
}
