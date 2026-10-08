package dev.rpgprogression.client.gui;

import dev.rpgprogression.client.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.skill.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;

/** Advancement-style viewport, supplied frame and tabs, and server-authoritative skills. */
public final class SkillTreeScreen extends Screen {
    private static final int W=640,H=360,FRAME_X=112,FRAME_Y=27,FRAME_W=416,FRAME_H=329;
    private static final int VX=138,VY=74,VW=364,VH=216,TEXT=0xFFE8E2D4,GOLD=0xFFD3BA80,MUTED=0xFFB5AC98;
    private static final int PX=202,PY=72,PW=236,PH=252;
    private static final Item[] TAB_ICONS={Items.NETHER_STAR,Items.FEATHER,Items.IRON_SWORD,Items.ENCHANTED_BOOK};
    private final Screen parent;
    private final Map<Integer,Camera> cameras=new HashMap<>();
    private final List<SkillTabButton> tabs=new ArrayList<>();
    private SkillTreeLayout.Layout layout=SkillTreeLayout.arrange(List.of());
    private List<SkillTreeLayout.Entry> entries=List.of();
    private List<SkillNode> nodes=List.of();
    private int branch,revision=-1,refreshTicks;
    private float scale;
    private double left,top;
    private String selected="",pressed="";
    private boolean popup,panning,moved;
    private double dragDistance;
    private SkillTreeButton learnButton;
    private static final class Camera { double x,y; float zoom=1; boolean initialized; }

    public SkillTreeScreen(Screen parent,int branch){super(tr("title"));this.parent=parent;this.branch=Math.max(0,Math.min(3,branch));}
    private static Component tr(String key,Object...args){return Component.translatable("rpgprogression.skills."+key,args);}
    private static Component branchName(int branch){return tr("branch."+branch);}
    private Camera camera(){return cameras.computeIfAbsent(branch,key->new Camera());}
    private boolean available(int index){return switch(index){case 1->ClientSkillTree.parcool;case 2->ClientSkillTree.epic;case 3->ClientSkillTree.irons;default->true;};}
    private SkillNode selectedNode(){return nodes.stream().filter(node->node.id().equals(selected)).findFirst().orElse(null);}
    @Override protected void init(){
        scale=Math.min(1.5f,Math.min(width/(float)W,height/(float)H));left=(width-W*scale)/2;top=(height-H*scale)/2;
        revision=ClientSkillTree.revision;tabs.clear();learnButton=null;
        nodes=ClientSkillTree.nodes.stream().filter(node->node.branch()==branch).toList();
        var updated=nodes.stream().map(node->new SkillTreeLayout.Entry(node.id(),node.parents())).toList();
        if(!updated.equals(entries)){entries=updated;layout=SkillTreeLayout.arrange(entries);}
        if(!camera().initialized&&!nodes.isEmpty())centerFirst();
        for(int i=0;i<4;i++){
            final int tab=i;int x=i<2?136+i*28:452+(i-2)*28;
            var button=new SkillTabButton(x,8,branchName(i),new ItemStack(TAB_ICONS[i]),i==branch,b->switchBranch(tab));
            button.active=available(i);tabs.add(addRenderableWidget(button));
        }
        var back=Button.builder(Component.literal("‹"),b->onClose()).bounds(138,53,16,16).build();back.setTooltip(Tooltip.create(tr("back")));addRenderableWidget(back);
        var center=Button.builder(Component.literal("↺"),b->fitView()).bounds(486,53,16,16).build();center.setTooltip(Tooltip.create(tr("center")));addRenderableWidget(center);
        if(popup&&selectedNode()==null)popup=false;
        if(popup)createDetails();
        clamp();
    }
    private void rebuild(){clearWidgets();init();}
    private void switchBranch(int target){if(target==branch)return;branch=target;selected="";popup=false;entries=List.of();rebuild();}
    private void createDetails(){
        var node=selectedNode();var d=ClientData.get();
        addRenderableWidget(Button.builder(Component.literal("×"),b->closeDetails()).bounds(PX+PW-22,PY+5,16,16).build());
        learnButton=addRenderableWidget(new SkillTreeButton(PX+12,PY+111,tr("learn"),b->ModNetworking.CHANNEL.sendToServer(new ModNetworking.UnlockSkill(node.id()))));
        learnButton.active=SkillTreeManager.canUnlock(d,node);
        if(node.branch()==2&&d.unlockedSkills.contains(node.id())){
            int y=PY+162;
            for(var slot:ClientSkillTree.slots)if(slot.getString("category").equals(node.category())){
                boolean equipped=slot.getString("skill").equals(node.id());
                var label=Component.translatable("rpgprogression.skills.slot."+slot.getString("name"));
                var button=Button.builder(tr(equipped?"equipped":"equip",label),b->ModNetworking.CHANNEL.sendToServer(new ModNetworking.EquipSkill(node.id(),slot.getInt("slot")))).bounds(PX+12,y,PW-24,20).build();
                button.active=!equipped&&(slot.getInt("cooldown")==0||minecraft.player!=null&&minecraft.player.isCreative());
                addRenderableWidget(button);y+=22;
            }
        }
    }
    private void closeDetails(){popup=false;rebuild();}
    @Override public void added(){ModNetworking.CHANNEL.sendToServer(new ModNetworking.RequestTree());}
    @Override public void tick(){
        if(revision!=ClientSkillTree.revision)rebuild();
        if(++refreshTicks>=20){refreshTicks=0;if(ClientSkillTree.epic&&ClientSkillTree.slots.stream().anyMatch(slot->slot.getInt("cooldown")>0))ModNetworking.CHANNEL.sendToServer(new ModNetworking.RequestTree());}
        if(learnButton!=null)learnButton.active=SkillTreeManager.canUnlock(ClientData.get(),selectedNode());
    }
    private void centerFirst(){
        var point=layout.positions().get(nodes.get(0).id());var c=camera();c.zoom=1;c.x=point.x()-VW/2.0;c.y=-10;c.initialized=true;
    }
    private void fitView(){
        var c=camera();c.zoom=(float)Math.max(.2,Math.min(1,Math.min((VW-24)/layout.width(),(VH-24)/layout.height())));
        c.x=(layout.width()-VW/c.zoom)/2;c.y=(layout.height()-VH/c.zoom)/2;c.initialized=true;clamp();
    }
    private void clamp(){
        var c=camera();double halfX=VW/(2.0*c.zoom),halfY=VH/(2.0*c.zoom);
        c.x=Math.max(-halfX,Math.min(layout.width()-halfX,c.x));c.y=Math.max(-halfY,Math.min(layout.height()-halfY,c.y));
    }
    /** Focus a prerequisite or a keyboard-selected node without changing its learned state. */
    public void focusSkill(String id){
        var target=ClientSkillTree.nodes.stream().filter(node->node.id().equals(id)).findFirst().orElse(null);if(target==null)return;
        if(target.branch()!=branch)switchBranch(target.branch());
        var p=layout.positions().get(id);var c=camera();c.zoom=1;c.x=p.x()-VW/2.0;c.y=p.y()+20-VH/2.0;c.initialized=true;clamp();
    }
    public double[] skillPosition(String id){
        var p=layout.positions().get(id);if(p==null)return new double[]{-1,-1};var c=camera();
        return new double[]{VX+(p.x()-c.x)*c.zoom,VY+(p.y()+20-c.y)*c.zoom};
    }
    private String hit(double x,double y){
        if(x<VX||x>=VX+VW||y<VY||y>=VY+VH)return "";
        var c=camera();double wx=c.x+(x-VX)/c.zoom,wy=c.y+(y-VY)/c.zoom;
        for(var node:nodes){var p=layout.positions().get(node.id());if(Math.abs(wx-p.x())<=20&&wy>=p.y()&&wy<p.y()+40)return node.id();}return "";
    }
    private void text(GuiGraphics g,Component value,int x,int y,int maxWidth,int color){
        float fitted=Math.min(1,maxWidth/(float)Math.max(1,font.width(value)));g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(fitted,fitted,1);g.drawString(font,value,0,0,color,false);g.pose().popPose();
    }
    private void edge(GuiGraphics g,SkillTreeLayout.Position parent,SkillTreeLayout.Position child,int color){
        int x1=(int)parent.x(),x2=(int)child.x(),y1=(int)parent.y()+40,y2=(int)child.y(),middle=(y1+y2)/2;
        line(g,x1,y1,x1,middle,0xFF080705,3);line(g,x1,middle,x2,middle,0xFF080705,3);line(g,x2,middle,x2,y2,0xFF080705,3);
        line(g,x1,y1,x1,middle,color,1);line(g,x1,middle,x2,middle,color,1);line(g,x2,middle,x2,y2,color,1);
    }
    private static void line(GuiGraphics g,int x1,int y1,int x2,int y2,int color,int thickness){
        int offset=thickness/2;g.fill(Math.min(x1,x2)-offset,Math.min(y1,y2)-offset,Math.max(x1,x2)+offset+1,Math.max(y1,y2)+offset+1,color);
    }
    private void drawTree(GuiGraphics g,String hovered){
        var c=camera();var d=ClientData.get();
        g.enableScissor((int)Math.ceil(left+VX*scale),(int)Math.ceil(top+VY*scale),(int)Math.floor(left+(VX+VW)*scale),(int)Math.floor(top+(VY+VH)*scale));
        g.pose().pushPose();g.pose().translate(VX,VY,0);g.pose().scale(c.zoom,c.zoom,1);g.pose().translate(-c.x,-c.y,0);
        for(var node:nodes)for(var id:node.parents()){
            var p=layout.positions().get(id);if(p!=null)edge(g,p,layout.positions().get(node.id()),d.unlockedSkills.contains(node.id())?GOLD:d.unlockedSkills.contains(id)?0xFFAB8B50:0xFF6C5C44);
        }
        RenderSystem.enableBlend();
        for(var node:nodes){
            var p=layout.positions().get(node.id());int x=(int)p.x()-20,y=(int)p.y();boolean learned=d.unlockedSkills.contains(node.id()),over=node.id().equals(hovered);
            int state=!SkillTreeManager.canUnlock(d,node)?2:over?1:0;
            if(learned)g.renderOutline(x-2,y-2,44,44,GOLD);else if(over)g.renderOutline(x-1,y-1,42,42,0xFFB4A385);
            g.blit(GuiTextures.texture("skill_tree_buttons"),x,y,40,40,0,state*40,40,40,64,128);
            if(learned){g.fill(x+30,y+30,x+38,y+38,0xFF17120A);g.drawString(font,"+",x+31,y+30,GOLD,false);}
            if(c.zoom>=.55){
                var labels=font.split(node.name(),112);g.pose().pushPose();g.pose().translate(p.x(),y+43,0);g.pose().scale(.75f,.75f,1);
                for(int row=0;row<Math.min(2,labels.size());row++)g.drawCenteredString(font,labels.get(row),0,row*10,learned?GOLD:TEXT);g.pose().popPose();
            }
        }
        RenderSystem.disableBlend();g.pose().popPose();g.disableScissor();
    }
    private void details(GuiGraphics g){
        var node=selectedNode();var d=ClientData.get();g.fill(VX,VY,VX+VW,VY+VH,0xA0080705);
        g.fill(PX,PY,PX+PW,PY+PH,0xFF211C14);g.renderOutline(PX,PY,PW,PH,0xFF9D855A);
        int y=PY+12;for(var part:font.split(node.name(),PW-48)){g.drawString(font,part,PX+12,y,TEXT,false);y+=10;if(y>=PY+34)break;}
        text(g,tr(d.unlockedSkills.contains(node.id())?"learned":"cost",1),PX+12,PY+39,PW-24,GOLD);
        text(g,tr("requires"),PX+12,PY+55,PW-24,MUTED);
        int row=0;
        if(node.parents().isEmpty())text(g,tr("no_requires"),PX+12,PY+70,PW-24,TEXT);
        for(var id:node.parents()){
            var dependency=ClientSkillTree.nodes.stream().filter(n->n.id().equals(id)).findFirst().orElse(null);
            text(g,dependency==null?Component.literal(id):dependency.name(),PX+12,PY+70+row*11,PW-24,d.unlockedSkills.contains(id)?GOLD:MUTED);if(++row==3)break;
        }
        text(g,tr(d.unlockedSkills.contains(node.id())?"learned":SkillTreeManager.canUnlock(d,node)?"learn":"locked"),PX+62,PY+124,PW-74,learnButton.active?GOLD:MUTED);
        if(!d.unlockedSkills.contains(node.id())&&d.skillPoints==0)text(g,tr("no_points"),PX+62,PY+137,PW-74,MUTED);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);int mx=(int)((mouseX-left)/scale),my=(int)((mouseY-top)/scale);var d=ClientData.get();
        String hovered=popup?"":hit(mx,my);
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        for(var tab:tabs)tab.render(g,mx,my,partial);
        g.fill(FRAME_X+25,FRAME_Y+25,FRAME_X+391,FRAME_Y+302,0xF016130F);
        GuiTextures.draw(g,"skill_tree",FRAME_X,FRAME_Y,FRAME_W,FRAME_H,416,329);
        g.drawCenteredString(font,branchName(branch),320,56,GOLD);
        text(g,tr("points",d.skillPoints),VX,299,155,GOLD);
        text(g,tr(d.level>=RPGConfig.MAX_LEVEL.get()?"max_points":"next_point",(d.skillMilestones+1)*RPGConfig.SKILL_LEVEL_INTERVAL.get()),VX+162,299,VW-162,MUTED);
        text(g,tr("navigation"),VX,316,VW,MUTED);
        if(!ClientSkillTree.ready)g.drawCenteredString(font,tr("loading"),320,180,MUTED);
        else if(nodes.isEmpty())g.drawCenteredString(font,tr(available(branch)?"empty":"unavailable"),320,180,MUTED);
        else drawTree(g,hovered);
        // Font glyphs write depth just in front of their plane; a modal needs its own
        // foreground plane to cover previously rendered node names as well as sprites.
        if(popup){g.pose().translate(0,0,100);details(g);}
        // Draw each tab once, behind the frame's top rail.
        for(var child:renderables)if(!(child instanceof SkillTabButton))child.render(g,mx,my,partial);
        if(!popup){
            for(var tab:tabs)if(tab.isMouseOver(mx,my)){var tips=new ArrayList<Component>();tips.add(tab.getMessage());if(!tab.active)tips.add(tr("unavailable"));g.renderComponentTooltip(font,tips,mx,my);}
            if(!hovered.isEmpty()){
                var node=nodes.stream().filter(n->n.id().equals(hovered)).findFirst().orElseThrow();var tips=new ArrayList<Component>();tips.add(node.name());tips.add(tr(d.unlockedSkills.contains(node.id())?"learned":"cost",1));
                for(var id:node.parents())ClientSkillTree.nodes.stream().filter(n->n.id().equals(id)).findFirst().ifPresent(n->tips.add(tr("requires_skill",n.name())));
                tips.add(tr("details"));g.renderComponentTooltip(font,tips,mx,my);
            }
        }
        g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        double mx=(x-left)/scale,my=(y-top)/scale;
        if(popup){if(mx<PX||mx>=PX+PW||my<PY||my>=PY+PH){closeDetails();return true;}return super.mouseClicked(mx,my,button);}
        if(super.mouseClicked(mx,my,button))return true;
        if((button==0||button==1)&&mx>=VX&&mx<VX+VW&&my>=VY&&my<VY+VH){panning=true;moved=false;dragDistance=0;pressed=button==0?hit(mx,my):"";return true;}
        return false;
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(panning){panning=false;if(!moved&&!pressed.isEmpty()&&button==0){selected=pressed;popup=true;rebuild();}pressed="";return true;}
        return super.mouseReleased((x-left)/scale,(y-top)/scale,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(panning){dragDistance+=Math.abs(dx/scale)+Math.abs(dy/scale);moved=dragDistance>3;var c=camera();c.x-=dx/(scale*c.zoom);c.y-=dy/(scale*c.zoom);clamp();return true;}
        return super.mouseDragged((x-left)/scale,(y-top)/scale,button,dx/scale,dy/scale);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){
        double mx=(x-left)/scale,my=(y-top)/scale;if(popup)return true;
        if(mx<VX||mx>=VX+VW||my<VY||my>=VY+VH)return false;
        zoomAt(mx,my,delta);return true;
    }
    private void zoomAt(double mx,double my,double delta){
        var c=camera();double worldX=c.x+(mx-VX)/c.zoom,worldY=c.y+(my-VY)/c.zoom;
        c.zoom=(float)Math.max(.2,Math.min(1.75,c.zoom*Math.pow(1.15,delta)));c.x=worldX-(mx-VX)/c.zoom;c.y=worldY-(my-VY)/c.zoom;clamp();
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==256&&popup){closeDetails();return true;}
        if(ClientRegistration.CHARACTER.matches(key,scan)){onClose();return true;}
        if(!popup){
            var c=camera();if(key==268){fitView();return true;}
            if(key>=262&&key<=265){if(key==262)c.x+=24/c.zoom;if(key==263)c.x-=24/c.zoom;if(key==264)c.y+=24/c.zoom;if(key==265)c.y-=24/c.zoom;clamp();return true;}
            if(key==61||key==334){zoomAt(VX+VW/2.0,VY+VH/2.0,1);return true;}
            if(key==45||key==333){zoomAt(VX+VW/2.0,VY+VH/2.0,-1);return true;}
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}

