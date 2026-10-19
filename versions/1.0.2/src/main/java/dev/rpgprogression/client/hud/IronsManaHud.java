package dev.rpgprogression.client.hud;

import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.gui.GuiTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import io.redspace.ironsspellbooks.api.item.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.api.CuriosApi;

/** Loaded only after the optional Iron's adapter is enabled. */
public final class IronsManaHud {
    private static Object session;
    private static int lastUnits;
    private static ResourceLocation lastIcon;
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
        render(g,1);
    }
    public static void render(GuiGraphics g,float opacity){
        var mc=Minecraft.getInstance();var d=ClientData.get();
        if(opacity<=0)return;
        if(session!=mc.getConnection()){session=mc.getConnection();lastUnits=0;lastIcon=null;}
        if(d.manaActive&&visible(mc.player)){
            lastUnits=Math.max(0,Math.min(8,(int)Math.floor(d.mana+0.00001)));
            SpellData spell=selected(mc.player);lastIcon=spell!=null&&spell!=SpellData.EMPTY&&spell.getSpell()!=null?spell.getSpell().getSpellIconResource():null;
        }
        String state=lastUnits==0?"empty":lastUnits==8?"full":Integer.toString(lastUnits);
        if(lastIcon!=null)ManaIconMask.draw(g,lastIcon,opacity);
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,opacity);
        g.blit(GuiTextures.texture("mana/mana_"+state),0,0,0,0,72,72,72,72);
        RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();
    }
    private IronsManaHud(){}
}
