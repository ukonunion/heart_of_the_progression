package dev.rpgprogression.gametest.client;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.hud.IronsManaHud;
import dev.rpgprogression.compat.IronsCompat;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.function.*;
public final class OptionalManaClientSmoke {
    private static double afterMain,afterOff;
    private static ItemStack book(){var stack=new ItemStack(ItemRegistry.COPPER_SPELL_BOOK.get());var spells=ISpellContainer.create(2,true,false).mutableCopy();spells.addSpell(SpellRegistry.FIREBOLT_SPELL.get(),1,false);spells.addSpell(SpellRegistry.HEAL_SPELL.get(),1,false);ISpellContainer.set(stack,spells.toImmutable());return stack;}
    private static ItemStack scroll(){var stack=new ItemStack(ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(SpellRegistry.FIREBOLT_SPELL.get(),1,stack);return stack;}
    private static void equip(net.minecraft.server.level.ServerPlayer p,ItemStack stack){CuriosApi.getCuriosInventory(p).orElseThrow(()->new IllegalStateException("no curios")).getStacksHandler("spellbook").orElseThrow(()->new IllegalStateException("no spellbook slot")).getStacks().setStackInSlot(0,stack);}
    private static void resource(Minecraft mc,int potential,double mana){var p=ManaClientSmoke.player(mc);var d=ProgressionManager.get(p);d.set(RPGAttribute.SPARK_POTENTIAL,potential);IronsCompat.refresh(p,d);d.mana=mana;d.manaRegenCooldown=20000;ModNetworking.sync(p);}
    public static void tick(int t,Minecraft mc,BiConsumer<Boolean,String> check,Consumer<String> capture){
        if(t==40)ManaClientSmoke.server(mc,()->equip(ManaClientSmoke.player(mc),ItemStack.EMPTY));
        if(t==65){check.accept(!IronsManaHud.visible(mc.player)&&ClientData.get().mana==0,"no book or scroll hides HUD at zero starting mana");capture.accept("no-equipment.png");}
        if(t==70)ManaClientSmoke.server(mc,()->ManaClientSmoke.player(mc).setItemInHand(InteractionHand.MAIN_HAND,book()));
        if(t==95){check.accept(!IronsManaHud.visible(mc.player),"book held in ordinary hand does not enable HUD");capture.accept("book-in-hand-hidden.png");}
        if(t==100)ManaClientSmoke.server(mc,()->{var p=ManaClientSmoke.player(mc);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);equip(p,book());});
        if(t==125){ClientMagicData.updateSpellSelectionManager();ClientMagicData.getSpellSelectionManager().makeSelection(0);}
        if(t==140){check.accept(IronsManaHud.visible(mc.player)&&ClientData.get().manaCapacity==0,"Curios spellbook enables empty0/0 HUD");check.accept(IronsManaHud.selected(mc.player).getSpell()==SpellRegistry.FIREBOLT_SPELL.get(),"selected firebolt icon");capture.accept("book-zero.png");}
        if(t==145)ManaClientSmoke.server(mc,()->resource(mc,15,0));
        if(t==170){check.accept(ClientData.get().manaCapacity==1&&ClientData.get().mana==0,"first mana capacity milestone synchronized");capture.accept("book-first-capacity.png");}
        if(t==180)ManaClientSmoke.server(mc,()->resource(mc,50,8));
        if(t==210){check.accept(ClientData.get().manaCapacity==8&&ClientData.get().mana==8,"full8 HUD");capture.accept("book-full.png");}
        if(t==220)ClientMagicData.getSpellSelectionManager().makeSelection(1);
        if(t==250){check.accept(IronsManaHud.selected(mc.player).getSpell()==SpellRegistry.HEAL_SPELL.get(),"native selection changes circle icon");capture.accept("book-selected-heal.png");}
        if(t==260)ManaClientSmoke.server(mc,()->{var p=ManaClientSmoke.player(mc);equip(p,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,scroll());resource(mc,50,3);});
        if(t==285){check.accept(IronsManaHud.visible(mc.player)&&IronsManaHud.selected(mc.player).getSpell()==SpellRegistry.FIREBOLT_SPELL.get(),"mainhand scroll shows its icon without a book");capture.accept("scroll-mainhand-three.png");}
        if(t==300)ManaClientSmoke.server(mc,()->{var p=ManaClientSmoke.player(mc);afterMain=3-SpellRegistry.FIREBOLT_SPELL.get().getManaCost(1);p.getMainHandItem().getItem().use(p.level(),p,InteractionHand.MAIN_HAND);});
        if(t==310)ManaClientSmoke.server(mc,()->{var d=ProgressionManager.get(ManaClientSmoke.player(mc));check.accept(Math.abs(d.mana-afterMain)<.002,"mainhand debit before recovery");d.manaRegenCooldown=20000;});
        if(t==345)ManaClientSmoke.server(mc,()->check.accept(Math.abs(ProgressionManager.get(ManaClientSmoke.player(mc)).mana-afterMain)<.05,"actual mainhand scroll use pays converted cost once"));
        if(t==350)ManaClientSmoke.server(mc,()->{var p=ManaClientSmoke.player(mc);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,scroll());resource(mc,50,2);});
        if(t==380){check.accept(IronsManaHud.visible(mc.player),"offhand scroll shows HUD");capture.accept("scroll-offhand-two.png");}
        if(t==390)ManaClientSmoke.server(mc,()->{var p=ManaClientSmoke.player(mc);afterOff=2-SpellRegistry.FIREBOLT_SPELL.get().getManaCost(1);var result=p.getOffhandItem().getItem().use(p.level(),p,InteractionHand.OFF_HAND);com.mojang.logging.LogUtils.getLogger().info("RPG_MANA_CLIENT offhand begin result={} mana={} level={} duration={} cost={}",result.getResult(),ProgressionManager.get(p).mana,MagicData.getPlayerMagicData(p).getCastingSpellLevel(),MagicData.getPlayerMagicData(p).getCastDurationRemaining(),SpellRegistry.FIREBOLT_SPELL.get().getManaCost(1));});
        if(t==400)ManaClientSmoke.server(mc,()->{var d=ProgressionManager.get(ManaClientSmoke.player(mc));check.accept(Math.abs(d.mana-afterOff)<.002,"offhand debit before recovery");d.manaRegenCooldown=20000;});
        if(t==435)ManaClientSmoke.server(mc,()->check.accept(Math.abs(ProgressionManager.get(ManaClientSmoke.player(mc)).mana-afterOff)<.05,"actual offhand scroll use pays converted cost once: actual="+ProgressionManager.get(ManaClientSmoke.player(mc)).mana+", expected="+afterOff+", casting="+MagicData.getPlayerMagicData(ManaClientSmoke.player(mc)).isCasting()));
        if(t==440)ManaClientSmoke.server(mc,()->{var p=ManaClientSmoke.player(mc);p.setItemInHand(InteractionHand.OFF_HAND,scroll());resource(mc,50,0);p.getOffhandItem().getItem().use(p.level(),p,InteractionHand.OFF_HAND);check.accept(!MagicData.getPlayerMagicData(p).isCasting(),"zero mana rejects real scroll use");});
        if(t==470){check.accept(mc.player.getOffhandItem().getCount()==1,"failed cast does not consume scroll");capture.accept("scroll-empty.png");}
        // Render each supplied state in a real framebuffer for pixel verification.
        if(t>=480&&t<=600&&(t-480)%15==0){int units=(t-480)/15;ManaClientSmoke.server(mc,()->{var p=ManaClientSmoke.player(mc);p.setItemInHand(InteractionHand.OFF_HAND,scroll());resource(mc,50,units);});}
        if(t>=490&&t<=610&&(t-490)%15==0)capture.accept("mana-state-"+((t-490)/15)+".png");
        if(t==620)ManaClientSmoke.server(mc,()->{var p=ManaClientSmoke.player(mc);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.getInventory().setItem(1,scroll());});
        if(t==645){check.accept(!IronsManaHud.visible(mc.player),"scroll stored in inventory does not show HUD");capture.accept("inventory-scroll-hidden.png");}
        if(t==650)ManaClientSmoke.server(mc,()->{var p=ManaClientSmoke.player(mc);p.getInventory().clearContent();equip(p,book());resource(mc,50,6);mc.getSingleplayerServer().getPlayerList().saveAll();});
        if(t==655)ManaClientSmoke.server(mc,()->ManaClientSmoke.player(mc).setGameMode(net.minecraft.world.level.GameType.CREATIVE));
        if(t==670){check.accept(IronsManaHud.visible(mc.player),"equipped book retains mana HUD in creative mode");capture.accept("book-creative.png");}
        if(t==675)ManaClientSmoke.server(mc,()->ManaClientSmoke.player(mc).setGameMode(net.minecraft.world.level.GameType.SURVIVAL));
        if(t==680){mc.options.guiScale().set(3);mc.resizeDisplay();capture.accept("book-scale3.png");}
        if(t==765)ManaClientSmoke.server(mc,()->{resource(mc,50,6);mc.getSingleplayerServer().getPlayerList().saveAll();});
    }
}
