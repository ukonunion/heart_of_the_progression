package dev.rpgprogression.client.gui;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.ClientRegistration;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.defense.*;
import dev.rpgprogression.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** Exact 640x360 supplied artwork; all coordinates follow the user's reference. */
public final class CharacterScreen extends Screen {
    private static final int W=640,H=360,GOLD=0xFFD3BA80,TEXT=0xFFE8E2D4,MUTED=0xFFB5AC98;
    private final Button[] plus=new Button[RPGAttribute.values().length];
    private float scale;private double left,top;
    public CharacterScreen(){super(Component.translatable("rpgprogression.gui.character"));}
    @Override protected void init(){
        scale=Math.min(1.5f,Math.min(width/(float)W,height/(float)H));
        left=(width-W*scale)/2;top=(height-H*scale)/2;
        for(RPGAttribute a:RPGAttribute.values())plus[a.ordinal()]=addRenderableWidget(new AttributeButton(a,570,85+a.ordinal()*13));
        addRenderableWidget(new SkillTreeButton(384,307,tr("skills.title"),b->minecraft.setScreen(new SkillTreeScreen(this,0))));
    }
    private Component tr(String key,Object...args){return Component.translatable("rpgprogression."+key,args);}
    private void text(GuiGraphics g,Component value,int x,int y,int color,float size){
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size,size,1);g.drawString(font,value,0,0,color,false);g.pose().popPose();
    }
    private void centered(GuiGraphics g,Component value,int x,int y,float size,int maxWidth,int color){
        float fitted=Math.min(size,maxWidth/(float)Math.max(1,font.width(value)));
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(fitted,fitted,1);g.drawCenteredString(font,value,0,0,color);g.pose().popPose();
    }
    private void row(GuiGraphics g,Component name,String value,int x,int right,int y){
        float fitted=Math.min(1,(right-x-7f)/Math.max(1,font.width(name)+font.width(value)));
        text(g,name,x,y,TEXT,fitted);text(g,Component.literal(value),right-Math.round(font.width(value)*fitted),y,TEXT,fitted);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);var d=ClientData.get();int mx=(int)((mouseX-left)/scale),my=(int)((mouseY-top)/scale);
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        GuiTextures.draw(g,"menu_main",0,0,W,H,640,360);
        if(minecraft.player!=null){
            g.enableScissor((int)Math.ceil(left+41*scale),(int)Math.ceil(top+52*scale),(int)Math.floor(left+187*scale),(int)Math.floor(top+234*scale));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g,114,230,68,0,0,minecraft.player);
            g.disableScissor();
            g.pose().translate(0,0,300);
            centered(g,minecraft.player.getName(),114,235,1.5f,130,TEXT);
        }
        centered(g,tr("gui.level",d.level),114,307,1.6f,132,TEXT);
        text(g,tr("gui.progression"),265,53,TEXT,1.5f);
        text(g,tr("gui.stats"),265,196,TEXT,1.5f);
        text(g,tr("gui.attributes"),454,53,TEXT,1.5f);
        text(g,tr("gui.defense"),454,196,TEXT,1.5f);
        boolean maximum=d.level>=RPGConfig.MAX_LEVEL.get();
        String xp=maximum?tr("gui.max_level").getString():compact(d.xp)+" / "+compact(d.requiredXP());
        centered(g,Component.literal(xp),317,130,1.6f,145,TEXT);
        if(!maximum)centered(g,tr("gui.next_level"),317,155,1,145,MUTED);
        double health=minecraft.player==null?0:minecraft.player.getHealth(),maxHealth=minecraft.player==null?20:minecraft.player.getMaxHealth();
        row(g,tr("stat.health"),fmt(health)+"/"+fmt(maxHealth),244,382,236);
        row(g,tr("stat.stamina"),fmt(d.stamina)+"/"+fmt(d.maxStamina()),244,382,254);
        row(g,tr("stat.spark"),fmt(d.spark)+"/"+fmt(d.maxSpark()),244,382,272);
        if(d.manaActive){
            row(g,tr("stat.mana"),fmt(d.mana)+"/"+fmt(d.manaCapacity),244,382,290);
        }
        for(RPGAttribute a:RPGAttribute.values()){
            int y=87+a.ordinal()*13;
            row(g,Component.translatable(a.key()),Integer.toString(d.get(a)),454,563,y);
            plus[a.ordinal()].active=d.points>0&&d.get(a)<RPGConfig.MAX_ATTRIBUTE.get();
        }
        int index=0;for(DefenseType type:DefenseType.values())row(g,Component.translatable(type.key()),String.format(Locale.ROOT,"%.1f%%",DefenseManager.percent(d,type)),454,579,229+index++*13);
        text(g,tr("gui.points",d.points),216,345,GOLD,1);
        String close=tr("gui.close").getString();text(g,Component.literal(close),600-font.width(close),345,MUTED,1);
        super.render(g,mx,my,partial);
        for(RPGAttribute a:RPGAttribute.values())if(mx>=452&&mx<580&&my>=85+a.ordinal()*13&&my<98+a.ordinal()*13)
            GuiTooltips.show(g,font,java.util.List.of(Component.translatable(a.key()+".tooltip")),mx,my,0,0,W,H,false);
        if(!maximum&&mx>=240&&mx<393&&my>=124&&my<=170)GuiTooltips.show(g,font,java.util.List.of(Component.literal(d.xp+" / "+d.requiredXP()+" XP")),mx,my,0,0,W,H,false);
        g.pose().popPose();
    }
    private final class AttributeButton extends Button{
        AttributeButton(RPGAttribute attribute,int x,int y){super(x,y,9,9,tr("gui.increase",Component.translatable(attribute.key())),b->ModNetworking.CHANNEL.sendToServer(new ModNetworking.SpendPoint(attribute.ordinal())),DEFAULT_NARRATION);}
        @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial){GuiTextures.draw(g,active?"button_on":"button_off",getX(),getY(),width,height,9,9);}
    }
    private String fmt(double value){return Math.abs(value-Math.rint(value))<0.05?Long.toString(Math.round(value)):String.format(Locale.ROOT,"%.1f",value);}
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
