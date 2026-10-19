package dev.rpgprogression.skill;

import java.util.*;

/** Texture placement in tree coordinates. A fork is drawn once for two siblings. */
public final class SkillTreePaths {
    public record Path(String parent,List<String> children,boolean fork,double x,double y,double length,double breadth,int rotation){
        public String texture(Set<String> learned){return "paths/"+(fork?"double_line_":"line_")+(learned.contains(parent)?"on":"off");}
    }
    private record Child(String id,double x,double y){}
    private record Segment(double x1,double y1,double x2,double y2){}
    public static List<Path> arrange(SkillTreeLayout.Layout layout,List<SkillTreeLayout.Entry> entries){
        var children=new LinkedHashMap<String,List<Child>>();
        for(var entry:entries){var child=layout.positions().get(entry.id());if(child==null)continue;
            for(var parent:entry.parents())if(layout.positions().containsKey(parent))
                children.computeIfAbsent(parent,k->new ArrayList<>()).add(new Child(entry.id(),child.x(),child.y()+20));
        }
        var result=new ArrayList<Path>();
        for(var entry:children.entrySet()){
            String source=entry.getKey();var p=layout.positions().get(source);double px=p.x(),py=p.y()+20;
            var nodes=entry.getValue();var used=new HashSet<String>();
            for(int i=0;i<nodes.size();i++)for(int j=i+1;j<nodes.size();j++){
                var a=nodes.get(i);var b=nodes.get(j);if(used.contains(a.id)||used.contains(b.id))continue;
                Path fork=null;
                if(a.x==b.x&&a.y!=b.y&&Math.abs((a.y+b.y)/2-py)<1e-6&&Math.abs(a.x-px)>40){
                    boolean right=a.x>px;fork=new Path(source,List.of(a.id,b.id),true,px+(right?20:-20),py,Math.abs(a.x-px)-40,Math.abs(a.y-b.y)*1.5,right?0:180);
                }else if(a.y==b.y&&a.x!=b.x&&Math.abs((a.x+b.x)/2-px)<1e-6&&Math.abs(a.y-py)>40){
                    boolean down=a.y>py;fork=new Path(source,List.of(a.id,b.id),true,px,py+(down?20:-20),Math.abs(a.y-py)-40,Math.abs(a.x-b.x)*1.5,down?90:270);
                }
                if(fork!=null){result.add(fork);used.add(a.id);used.add(b.id);}
            }
            var drawn=new HashSet<Segment>();
            for(var child:nodes)if(!used.contains(child.id)){
                var route=simplify(SkillTreeLayout.connection(layout,source,child.id));
                for(int i=1;i<route.size();i++){
                    var a=route.get(i-1);var b=route.get(i);double dx=b.x()-a.x(),dy=b.y()-a.y();if(dx==0&&dy==0)continue;
                    var segment=new Segment(Math.min(a.x(),b.x()),Math.min(a.y(),b.y()),Math.max(a.x(),b.x()),Math.max(a.y(),b.y()));if(!drawn.add(segment))continue;
                    int angle=dx>0?0:dx<0?180:dy>0?90:270;
                    result.add(new Path(source,List.of(child.id),false,a.x(),a.y(),Math.abs(dx)+Math.abs(dy),40,angle));
                }
            }
        }return List.copyOf(result);
    }
    private static List<SkillTreeLayout.Position> simplify(List<SkillTreeLayout.Position> route){
        var result=new ArrayList<SkillTreeLayout.Position>();
        for(var p:route){
            if(!result.isEmpty()&&result.getLast().x()==p.x()&&result.getLast().y()==p.y())continue;
            if(result.size()>1){var a=result.get(result.size()-2);var b=result.getLast();
                if(a.x()==b.x()&&b.x()==p.x()||a.y()==b.y()&&b.y()==p.y())result.removeLast();
            }result.add(p);
        }return result;
    }
    private SkillTreePaths(){}
}
