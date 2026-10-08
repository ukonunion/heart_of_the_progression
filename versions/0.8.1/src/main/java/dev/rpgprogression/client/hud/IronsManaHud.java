package dev.rpgprogression.client.hud;

import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.gui.GuiTextures;
import io.redspace.ironsspellbooks.api.item.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Loaded only after the optional Iron's adapter is enabled. */
public final class IronsManaHud {
    public static boolean visible(Player p){
        return p!=null&&(scroll(p.getMainHandItem())||scroll(p.getOffhandItem())||
                CuriosApi.getCuriosInventory(p).map(h->h.getStacksHandler("spellbook")
                .map(s->{var items=s.getStacks();for(int i=0;i<items.getSlots();i++)
                    if(items.getStackInSlot(i).getItem() instanceof ISpellbook)return true;return false;}).orElse(false)).orElse(false));
    }
    private static boolean scroll(ItemStack stack){return !stack.isEmpty()&&stack.getItem() instanceof IScroll;}
    public static SpellData selected(Player p){
        for(ItemStack stack:new ItemStack[]{p.getMainHandItem(),p.getOffhandItem()})
            if(scroll(stack))return ISpellContainer.get(stack).getSpellAtIndex(0);
        return ClientMagicData.getSpellSelectionManager().getSelectedSpellData();
    }
    public static void render(GuiGraphics g){
        var mc=Minecraft.getInstance();var d=ClientData.get();
        if(!d.manaActive||!visible(mc.player))return;
        int units=Math.max(0,Math.min(8,(int)Math.floor(d.mana+0.00001)));
        String state=units==0?"empty":units==8?"full":Integer.toString(units);
        SpellData spell=selected(mc.player);
        if(spell!=null&&spell!=SpellData.EMPTY&&spell.getSpell()!=null){
            ManaIconMask.draw(g,spell.getSpell().getSpellIconResource());
        }
        GuiTextures.draw(g,"mana/mana_"+state,0,0,72,72,72,72);
    }
    private IronsManaHud(){}
}
