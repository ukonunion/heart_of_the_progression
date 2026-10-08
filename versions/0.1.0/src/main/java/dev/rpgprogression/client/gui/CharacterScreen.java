package dev.rpgprogression.client.gui;

import dev.rpgprogression.attribute.*;
import dev.rpgprogression.client.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.defense.*;
import dev.rpgprogression.inventory.InventorySlotRules;
import dev.rpgprogression.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.Locale;

/** Resolution-independent panel: both rendering and mouse input use the same logical coordinate system. */
public final class CharacterScreen extends Screen {
    private static final int W=560,H=326,GOLD=0xFFC8AC70,TEXT=0xFFE8E2D4,MUTED=0xFFAAA397;
    private static final Item[] ICONS={Items.IRON_SWORD,Items.IRON_CHESTPLATE,Items.FEATHER,Items.APPLE,Items.BOOK};
    private final Button[] plus=new Button[5];
    private float scale; private int left,top;
    public CharacterScreen(){super(Component.translatable("rpgprogression.gui.character"));}
    @Override protected void init() {
        scale=Math.min(1.5f,Math.min((width-12f)/W,(height-12f)/H));
        left=(int)((width-W*scale)/2);top=(int)((height-H*scale)/2);
        for(RPGAttribute a:RPGAttribute.values()) {
            plus[a.ordinal()]=addRenderableWidget(Button.builder(Component.literal("+"),b->ModNetworking.CHANNEL.sendToServer(new ModNetworking.SpendPoint(a.ordinal())))
                    .bounds(520,66+a.ordinal()*29,20,20).build());
        }
    }
    private Component tr(String key,Object...args){return Component.translatable("rpgprogression."+key,args);}
    private void label(GuiGraphics g,Component text,int x,int y,int color){g.drawString(font,text,x,y,color,false);}
    private void panel(GuiGraphics g,int x,int y,int w,int h) {
        g.fill(x,y,x+w,y+h,0xFF11151D);g.renderOutline(x,y,w,h,0xFF4A4030);
        g.fill(x,y,x+9,y+2,GOLD);g.fill(x,y,x+2,y+9,GOLD);
        g.fill(x+w-9,y+h-2,x+w,y+h,GOLD);g.fill(x+w-2,y+h-9,x+w,y+h,GOLD);
    }
    private void bar(GuiGraphics g,int x,int y,int w,double ratio,int color) {
        g.fill(x,y,x+w,y+7,0xFF090C11);g.fill(x+1,y+1,x+1+(int)((w-2)*Math.min(1,Math.max(0,ratio))),y+6,color);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        renderBackground(g);var d=ClientData.get();
        int mx=(int)((mouseX-left)/scale),my=(int)((mouseY-top)/scale);
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        g.fillGradient(0,0,W,H,0xFF20212A,0xFF0B0E14);g.renderOutline(0,0,W,H,GOLD);g.renderOutline(3,3,W-6,H-6,0xFF51432E);
        label(g,tr("gui.character"),20,16,GOLD);
        g.fill(20,33,W-20,34,0xFF68563A);label(g,Component.literal("RPG PROGRESSION"),W-116,18,MUTED);
        panel(g,16,46,142,254); panel(g,170,46,166,254);panel(g,348,46,196,164);panel(g,348,218,196,82);
        if(minecraft!=null&&minecraft.player!=null) {
            g.fillGradient(24,56,150,240,0xFF252932,0xFF12151C);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g,87,231,76,87-mx,140-my,minecraft.player);
            g.drawCenteredString(font,minecraft.player.getName(),87,253,TEXT);
            g.drawCenteredString(font,tr("gui.level",d.level),87,274,GOLD);
        }
        label(g,tr("gui.progression"),182,59,GOLD);
        label(g,tr("gui.level",d.level),182,83,TEXT);
        String xp=d.level>=RPGConfig.MAX_LEVEL.get()?tr("gui.max_level").getString():compact(d.xp)+" / "+compact(d.requiredXP())+" XP";
        label(g,Component.literal(xp),182,105,MUTED);bar(g,182,122,142,d.level>=RPGConfig.MAX_LEVEL.get()?1:(double)d.xp/d.requiredXP(),0xFFB79D61);
        label(g,tr("gui.stats"),182,148,GOLD);
        double health=minecraft.player==null?0:minecraft.player.getHealth(),maxHealth=minecraft.player==null?20:minecraft.player.getMaxHealth();
        label(g,tr("stat.health"),182,172,TEXT);label(g,Component.literal(fmt(health)+" / "+fmt(maxHealth)),182,186,MUTED);
        bar(g,182,201,142,health/maxHealth,0xFFAD4E50);
        label(g,tr("stat.stamina"),182,221,TEXT);label(g,Component.literal(fmt(d.stamina)+" / "+fmt(d.maxStamina())),182,235,MUTED);
        bar(g,182,250,142,d.stamina/d.maxStamina(),d.exhausted?0xFFAD4E50:0xFF8DAE86);
        label(g,tr("gui.slots",minecraft.player==null?0:InventorySlotRules.unlocked(minecraft.player)),182,278,MUTED);
        label(g,tr("gui.attributes"),360,53,GOLD);
        for(RPGAttribute a:RPGAttribute.values()) {
            int y=70+a.ordinal()*29;
            g.renderItem(new ItemStack(ICONS[a.ordinal()]),359,y-3);
            label(g,Component.translatable(a.key()),380,y,TEXT);
            label(g,Component.literal(Integer.toString(d.get(a))),495,y,GOLD);
            plus[a.ordinal()].active=d.points>0&&d.get(a)<RPGConfig.MAX_ATTRIBUTE.get();
            if(a.ordinal()<4)g.fill(380,y+16,511,y+17,0xFF302F2D);
        }
        label(g,tr("gui.defense"),360,225,GOLD);
        int row=0;for(DefenseType type:DefenseType.values()) {
            int y=239+row++*11;label(g,Component.translatable(type.key()),360,y,MUTED);
            String value=String.format(Locale.ROOT,"%.1f%%",DefenseManager.percent(d,type));
            label(g,Component.literal(value),534-font.width(value),y,TEXT);
        }
        label(g,tr("gui.points",d.points),20,310,GOLD);label(g,tr("gui.close"),385,310,MUTED);
        super.render(g,mx,my,partial);
        for(RPGAttribute a:RPGAttribute.values())if(mx>=359&&mx<517&&my>=66+a.ordinal()*29&&my<91+a.ordinal()*29)
            g.renderTooltip(font,font.split(Component.translatable(a.key()+".tooltip"),210),mx,my);
        if(mx>=182&&mx<324&&my>=103&&my<=130)g.renderTooltip(font,Component.literal(d.xp+" / "+d.requiredXP()+" XP"),mx,my);
        g.pose().popPose();
    }
    private String fmt(double n){return String.format(Locale.ROOT,"%.0f",n);}
    private String compact(long n){
        if(n>=1_000_000_000_000L)return String.format(Locale.ROOT,"%.1fT",n/1e12);
        if(n>=1_000_000_000L)return String.format(Locale.ROOT,"%.1fB",n/1e9);
        if(n>=1_000_000L)return String.format(Locale.ROOT,"%.1fM",n/1e6);
        if(n>=100_000)return String.format(Locale.ROOT,"%.1fK",n/1e3);
        return Long.toString(n);
    }
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked((x-left)/scale,(y-top)/scale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased((x-left)/scale,(y-top)/scale,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged((x-left)/scale,(y-top)/scale,b,dx/scale,dy/scale);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(ClientRegistration.CHARACTER.matches(key,scan)){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public boolean isPauseScreen(){return false;}
}
