package dev.rpgprogression.client.gui;

import dev.rpgprogression.client.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.skill.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Temporary layout kept independent from the functional, server-authoritative skill system. */
public final class SkillTreeScreen extends Screen {
    private static final int W=640,H=360,TEXT=0xFFE8E2D4,GOLD=0xFFD3BA80,MUTED=0xFFB5AC98,PAGE_SIZE=8;
    private final Screen parent;private int branch,page=0,revision=-1,refreshTicks=0;private String selected="";
    private float scale;private double left,top;
    private final Map<SkillNode,SkillTreeButton> buttons=new LinkedHashMap<>();
    private List<SkillNode> visible=List.of();
    public SkillTreeScreen(Screen parent,int branch){super(tr("title"));this.parent=parent;this.branch=Math.max(0,Math.min(3,branch));}
    private static Component tr(String key,Object...args){return Component.translatable("rpgprogression.skills."+key,args);}
    private static Component branchName(int branch){return tr("branch."+branch);}
    private List<SkillNode> branchNodes(){return ClientSkillTree.nodes.stream().filter(n->n.branch()==branch).toList();}
    private boolean branchAvailable(int index){return switch(index){case 1->ClientSkillTree.parcool;case 2->ClientSkillTree.epic;case 3->ClientSkillTree.irons;default->true;};}
    @Override protected void init(){
        scale=Math.min(1.5f,Math.min(width/(float)W,height/(float)H));left=(width-W*scale)/2;top=(height-H*scale)/2;
        revision=ClientSkillTree.revision;buttons.clear();
        for(int i=0;i<4;i++){final int tab=i;var button=Button.builder(branchName(i),b->{branch=tab;page=0;selected="";rebuild();}).bounds(26+i*149,36,142,20).build();button.active=i!=branch&&branchAvailable(i);addRenderableWidget(button);}
        var nodes=branchNodes();page=Math.min(page,Math.max(0,(nodes.size()-1)/PAGE_SIZE));visible=nodes.subList(Math.min(page*PAGE_SIZE,nodes.size()),Math.min((page+1)*PAGE_SIZE,nodes.size()));
        if(nodes.stream().noneMatch(n->n.id().equals(selected)))selected=visible.isEmpty()?"":visible.get(0).id();
        for(int i=0;i<visible.size();i++){
            var node=visible.get(i);int x=28+(i%2)*196,y=83+(i/2)*52;
            var button=new SkillTreeButton(x+145,y+2,node.name(),b->{selected=node.id();ModNetworking.CHANNEL.sendToServer(new ModNetworking.UnlockSkill(node.id()));});
            button.active=SkillTreeManager.canUnlock(ClientData.get(),node);buttons.put(node,addRenderableWidget(button));
        }
        var backPage=Button.builder(Component.literal("<"),b->{page--;rebuild();}).bounds(28,301,20,20).build();backPage.active=page>0;addRenderableWidget(backPage);
        var nextPage=Button.builder(Component.literal(">"),b->{page++;rebuild();}).bounds(385,301,20,20).build();nextPage.active=(page+1)*PAGE_SIZE<nodes.size();addRenderableWidget(nextPage);
        addRenderableWidget(Button.builder(tr("back"),b->onClose()).bounds(432,327,182,20).build());
        var node=nodes.stream().filter(n->n.id().equals(selected)).findFirst().orElse(null);
        if(node!=null&&node.branch()==2&&ClientData.get().unlockedSkills.contains(node.id())){
            int y=220;for(var slot:ClientSkillTree.slots)if(slot.getString("category").equals(node.category())){
                boolean equipped=slot.getString("skill").equals(node.id());
                var label=Component.translatable("rpgprogression.skills.slot."+slot.getString("name"));
                var button=Button.builder(tr(equipped?"equipped":"equip",label),b->ModNetworking.CHANNEL.sendToServer(new ModNetworking.EquipSkill(node.id(),slot.getInt("slot")))).bounds(437,y,170,20).build();
                button.active=!equipped&&slot.getInt("cooldown")==0;addRenderableWidget(button);y+=23;
            }
        }
    }
    private void rebuild(){clearWidgets();init();}
    @Override public void added(){ModNetworking.CHANNEL.sendToServer(new ModNetworking.RequestTree());}
    @Override public void tick(){
        if(revision!=ClientSkillTree.revision)rebuild();
        // A server snapshot includes the native replacement cooldown. Refresh while it
        // runs so an equip button becomes available without closing and reopening the tree.
        if(++refreshTicks>=20){
            refreshTicks=0;
            if(ClientSkillTree.epic&&ClientSkillTree.slots.stream().anyMatch(slot->slot.getInt("cooldown")>0))
                ModNetworking.CHANNEL.sendToServer(new ModNetworking.RequestTree());
        }
    }
    private void drawText(GuiGraphics g,Component value,int x,int y,int maxWidth,int color){
        float size=Math.min(1,maxWidth/(float)Math.max(1,font.width(value)));g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size,size,1);g.drawString(font,value,0,0,color,false);g.pose().popPose();
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);int mx=(int)((mouseX-left)/scale),my=(int)((mouseY-top)/scale);var d=ClientData.get();
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        // This placeholder can be replaced by a resource pack without touching skill code.
        var texture=GuiTextures.texture("skill_tree");
        if(minecraft.getResourceManager().getResource(texture).isPresent())GuiTextures.draw(g,"skill_tree",16,5,608,350,608,350);
        else {g.fill(16,5,624,355,0xEF191610);g.renderOutline(16,5,608,350,0xFF8C7650);g.renderOutline(20,9,600,342,0xFF53452F);}
        g.drawCenteredString(font,title,320,16,GOLD);
        drawText(g,tr("points",d.skillPoints),28,64,225,GOLD);
        drawText(g,tr(d.level>=RPGConfig.MAX_LEVEL.get()?"max_points":"next_point",(d.skillMilestones+1)*RPGConfig.SKILL_LEVEL_INTERVAL.get()),281,64,320,MUTED);
        g.fill(420,82,421,322,0xFF645137);
        if(!ClientSkillTree.ready)g.drawCenteredString(font,tr("loading"),224,171,MUTED);
        else if(visible.isEmpty())g.drawCenteredString(font,tr(branchAvailable(branch)?"empty":"unavailable"),224,171,MUTED);
        for(int i=0;i<visible.size();i++){
            var node=visible.get(i);int x=28+(i%2)*196,y=83+(i/2)*52;boolean learned=d.unlockedSkills.contains(node.id());
            g.fill(x,y,x+189,y+46,node.id().equals(selected)?0xFF30291D:0xFF211D16);
            g.renderOutline(x,y,189,46,node.id().equals(selected)?0xFFB29A66:0xFF4D402D);
            var lines=font.split(node.name(),132);for(int line=0;line<Math.min(2,lines.size());line++)g.drawString(font,lines.get(line),x+5,y+5+line*10,TEXT,false);
            drawText(g,tr(learned?"learned":SkillTreeManager.canUnlock(d,node)?"cost":"locked",1),x+5,y+31,134,learned?GOLD:MUTED);
            buttons.get(node).active=SkillTreeManager.canUnlock(d,node);
        }
        int pages=Math.max(1,(branchNodes().size()+PAGE_SIZE-1)/PAGE_SIZE);
        g.drawCenteredString(font,Component.literal((page+1)+" / "+pages),222,307,MUTED);
        var node=branchNodes().stream().filter(n->n.id().equals(selected)).findFirst().orElse(null);
        if(node!=null){
            int y=87;for(var line:font.split(node.name(),171)){g.drawString(font,line,436,y,TEXT,false);y+=11;}
            drawText(g,tr("cost",1),436,y+6,170,GOLD);y+=28;
            drawText(g,tr("requires"),436,y,170,MUTED);y+=13;
            if(node.parents().isEmpty())drawText(g,tr("no_requires"),436,y,170,TEXT);
            else for(var id:node.parents()){
                var dependency=ClientSkillTree.nodes.stream().filter(n->n.id().equals(id)).findFirst().orElse(null);
                drawText(g,dependency==null?Component.literal(id):dependency.name(),436,y,170,d.unlockedSkills.contains(id)?GOLD:MUTED);y+=13;
            }
            drawText(g,tr(d.unlockedSkills.contains(node.id())?"learned":"locked"),436,194,170,GOLD);
        }
        super.render(g,mx,my,partial);
        for(var entry:buttons.entrySet())if(entry.getValue().isMouseOver(mx,my)){
            var n=entry.getKey();var tip=new ArrayList<Component>();tip.add(n.name());tip.add(tr(d.unlockedSkills.contains(n.id())?"learned":"cost",1));
            for(var id:n.parents())ClientSkillTree.nodes.stream().filter(a->a.id().equals(id)).findFirst().ifPresent(a->tip.add(tr("requires_skill",a.name())));
            g.renderComponentTooltip(font,tip,mx,my);
        }
        g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        double mx=(x-left)/scale,my=(y-top)/scale;
        for(int i=0;i<visible.size();i++){int rx=28+(i%2)*196,ry=83+(i/2)*52;if(mx>=rx&&mx<rx+142&&my>=ry&&my<ry+46){selected=visible.get(i).id();rebuild();return true;}}
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased((x-left)/scale,(y-top)/scale,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged((x-left)/scale,(y-top)/scale,b,dx/scale,dy/scale);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(ClientRegistration.CHARACTER.matches(key,scan)){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
