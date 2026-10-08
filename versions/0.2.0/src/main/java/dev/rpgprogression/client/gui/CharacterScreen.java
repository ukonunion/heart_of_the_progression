package dev.rpgprogression.client.gui;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.ClientRegistration;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.defense.*;
import dev.rpgprogression.inventory.InventorySlotRules;
import dev.rpgprogression.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** The supplied 320x180 artwork uses a 640x360 logical canvas for readable text. */
public final class CharacterScreen extends Screen {
    private static final int W=640,H=360,GOLD=0xFFD3BA80,TEXT=0xFFE8E2D4,MUTED=0xFFB5AC98;
    private final Button[] plus=new Button[5];
    private float scale; private double left,top;
    public CharacterScreen(){super(Component.translatable("rpgprogression.gui.character"));}
    @Override protected void init() {
        scale=Math.min(1.5f,Math.min((width-8f)/W,(height-8f)/H));
        left=(width-W*scale)/2;top=(height-H*scale)/2;
        for(RPGAttribute a:RPGAttribute.values()) {
            plus[a.ordinal()]=addRenderableWidget(new AttributeButton(a,578,117+a.ordinal()*14));
        }
    }
    private Component tr(String key,Object...args){return Component.translatable("rpgprogression."+key,args);}
    private void label(GuiGraphics g,Component text,int x,int y,int color){g.drawString(font,text,x,y,color,false);}
    private void right(GuiGraphics g,String text,int x,int y,int color){label(g,Component.literal(text),x-font.width(text),y,color);}
    private void heading(GuiGraphics g,String icon,String key,int x,int y) {
        GuiTextures.icon(g,icon,x,y-7,24);label(g,tr(key),x+33,y,TEXT);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        renderBackground(g);var d=ClientData.get();
        int mx=(int)((mouseX-left)/scale),my=(int)((mouseY-top)/scale);
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        GuiTextures.draw(g,"character",0,0,W,H,320,180);
        if(minecraft!=null&&minecraft.player!=null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g,114,273,96,114-mx,150-my,minecraft.player);
        }
        g.drawCenteredString(font,tr("gui.level",d.level),114,325,TEXT);
        heading(g,"level","gui.progression",218,89);
        heading(g,"stats","gui.stats",218,189);
        heading(g,"attributes","gui.attributes",408,89);
        heading(g,"defense","gui.defense",408,189);
        GuiTextures.icon(g,"level",226,136,32);
        String xp=d.level>=RPGConfig.MAX_LEVEL.get()?tr("gui.max_level").getString():compact(d.xp)+" / "+compact(d.requiredXP());
        float xpScale=Math.min(1.4f,120f/Math.max(1,font.width(xp)));
        g.pose().pushPose();g.pose().translate(322,143,0);g.pose().scale(xpScale,xpScale,1);
        g.drawCenteredString(font,Component.literal(xp),0,0,TEXT);g.pose().popPose();
        g.drawCenteredString(font,tr("gui.next_level"),322,166,MUTED);
        GuiTextures.bar(g,"xp",271,181,101,2,d.level>=RPGConfig.MAX_LEVEL.get()?1:(double)d.xp/d.requiredXP());
        double health=minecraft.player==null?0:minecraft.player.getHealth(),maxHealth=minecraft.player==null?20:minecraft.player.getMaxHealth();
        GuiTextures.icon(g,"stats/health",218,224,14);
        label(g,tr("stat.health"),239,227,TEXT);right(g,fmt(health)+" / "+fmt(maxHealth),382,227,TEXT);
        GuiTextures.bar(g,"health",239,240,143,2,health/Math.max(1,maxHealth));
        GuiTextures.icon(g,"stats/stamina",218,248,14);
        label(g,tr("stat.stamina"),239,251,TEXT);right(g,fmt(d.stamina)+" / "+fmt(d.maxStamina()),382,251,TEXT);
        GuiTextures.bar(g,d.exhausted?"exhausted":"stamina",239,264,143,2,d.stamina/d.maxStamina());
        GuiTextures.icon(g,"stats/inventory",218,278,14);
        label(g,tr("gui.slots",minecraft.player==null?0:InventorySlotRules.unlocked(minecraft.player)),239,281,MUTED);
        for(RPGAttribute a:RPGAttribute.values()) {
            int y=119+a.ordinal()*14;
            GuiTextures.icon(g,"attributes/"+a.id(),411,y-2,12);
            label(g,Component.translatable(a.key()),430,y,TEXT);
            right(g,Integer.toString(d.get(a)),572,y,TEXT);
            plus[a.ordinal()].active=d.points>0&&d.get(a)<RPGConfig.MAX_ATTRIBUTE.get();
        }
        int row=0;for(DefenseType type:DefenseType.values()) {
            int y=223+row++*15;
            GuiTextures.icon(g,"defense/"+type.name().toLowerCase(Locale.ROOT),411,y-2,12);
            label(g,Component.translatable(type.key()),430,y,TEXT);
            right(g,String.format(Locale.ROOT,"%.1f%%",DefenseManager.percent(d,type)),583,y,TEXT);
        }
        label(g,tr("gui.points",d.points),216,329,GOLD);right(g,tr("gui.close").getString(),590,329,MUTED);
        super.render(g,mx,my,partial);
        for(RPGAttribute a:RPGAttribute.values())if(mx>=411&&mx<576&&my>=117+a.ordinal()*14&&my<130+a.ordinal()*14)
            g.renderTooltip(font,font.split(Component.translatable(a.key()+".tooltip"),210),mx,my);
        if(mx>=260&&mx<390&&my>=137&&my<=183)g.renderTooltip(font,Component.literal(d.xp+" / "+d.requiredXP()+" XP"),mx,my);
        g.pose().popPose();
    }
    private final class AttributeButton extends Button {
        AttributeButton(RPGAttribute attribute,int x,int y) {
            super(x,y,12,12,tr("gui.increase",Component.translatable(attribute.key())),
                    b->ModNetworking.CHANNEL.sendToServer(new ModNetworking.SpendPoint(attribute.ordinal())),DEFAULT_NARRATION);
        }
        @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial) {
            GuiTextures.draw(g,"buttons/plus_"+(!active?"disabled":isHoveredOrFocused()?"hover":"normal"),getX(),getY(),width,height,12,12);
        }
    }
    private String fmt(double n){return String.format(Locale.ROOT,"%.0f",n);}
    private String compact(long n){
        if(n>=1_000_000_000_000L)return String.format(Locale.ROOT,"%.1fT",n/1e12);
        if(n>=1_000_000_000L)return String.format(Locale.ROOT,"%.1fB",n/1e9);
        if(n>=1_000_000L)return String.format(Locale.ROOT,"%.1fM",n/1e6);
        if(n>=10_000)return String.format(Locale.ROOT,"%.1fK",n/1e3);
        return Long.toString(n);
    }
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked((x-left)/scale,(y-top)/scale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased((x-left)/scale,(y-top)/scale,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged((x-left)/scale,(y-top)/scale,b,dx/scale,dy/scale);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(ClientRegistration.CHARACTER.matches(key,scan)){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public boolean isPauseScreen(){return false;}
}
