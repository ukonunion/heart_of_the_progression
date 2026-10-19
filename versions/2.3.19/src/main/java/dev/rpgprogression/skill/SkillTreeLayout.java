package dev.rpgprogression.skill;

import java.util.*;

/** A forest layout with shared prerequisite edges and no client or optional mod types. */
public final class SkillTreeLayout {
    public record Entry(String id, List<String> parents) {}
    public record Position(double x, double y, int depth) {}
    public record Layout(Map<String, Position> positions, double width, double height) {}
    private static final double COLUMN = 96, ROW = 92, GAP = 28;
    private static final Map<String,Position> DESIGNED=new LinkedHashMap<>();
    static {
        at("parcool:hide_in_block",60,80);at("parcool:crawl",140,80);
        at("parcool:slide",220,40);at("parcool:dive",220,120);
        at("parcool:fast_run",300,80);at("parcool:fast_swim",380,80);at("parcool:charge_jump",460,80);
        at("parcool:trick_jump",540,40);at("parcool:vault",540,120);
        at("parcool:horizontal_wall_run",300,160);at("parcool:wall_run",380,160);at("parcool:wall_jump",460,160);
        at("parcool:ride_zipline",300,240);at("parcool:hang_down",380,240);at("parcool:hang_on",460,240);at("parcool:pole_climb",540,240);
        at("parcool:slide_down",420,320);at("parcool:climb_up",500,320);
        at("parcool:skydive",60,200);at("parcool:breakfall",140,200);at(DodgeSkillGroup.ID,220,200);
        at("epicfight:emergency_escape",180,280);at("epicfight:technician",260,280);at("parcool:long_jump",100,280);
        at("epicfight:phantom_ascent",60,360);at("epicfight:demolition_leap",140,360);at("epicfight:meteor_slam",220,360);
        at("epicfight:impact_guard",340,360);at("epicfight:adaptive_skin",300,440);at("epicfight:vengeance",380,440);
        at("epicfight:revelation",460,440);at("epicfight:guard",540,400);at("epicfight:parrying",540,480);
        at("epicfight:endurance",140,440);at("epicfight:stamina_pillager",140,520);at("epicfight:hypervitality",60,520);
        at("epicfight:forbidden_strength",220,520);at("epicfight:adrenaline_fiend",60,600);at("epicfight:catharsis",140,600);at("epicfight:death_harvest",220,600);
        at("epicfight:swordmaster",300,560);at("epicfight:bonebreaker",380,520);at("epicfight:berserker",380,600);
    }
    private static void at(String id,double x,double y){DESIGNED.put(id,new Position(x,y,0));}
    /** Keep the artist's spatial grouping, including upward and sideways branches. */
    public static Layout arrangeDesigned(List<Entry> entries){
        if(entries.isEmpty())return arrange(entries);
        var depths=arrange(entries).positions();var points=new LinkedHashMap<String,Position>();
        var extras=arrangeCompact(entries.stream().filter(e->!DESIGNED.containsKey(e.id())).toList());
        for(var entry:entries){
            var p=DESIGNED.get(entry.id());
            if(p==null){var extra=extras.positions().get(entry.id());p=new Position(640+extra.x(),extra.y(),0);}
            points.put(entry.id(),new Position(p.x(),p.y(),depths.get(entry.id()).depth()));
        }
        double minX=points.values().stream().mapToDouble(Position::x).min().orElse(40)-40;
        double minY=points.values().stream().mapToDouble(Position::y).min().orElse(12)-12;
        points.replaceAll((id,p)->new Position(p.x()-minX,p.y()-minY,p.depth()));
        double width=points.values().stream().mapToDouble(p->p.x()+40).max().orElse(1);
        double height=points.values().stream().mapToDouble(p->p.y()+52).max().orElse(1);
        return new Layout(Collections.unmodifiableMap(points),width,height);
    }
    public static List<Position> connection(Layout layout,String parentId,String childId){
        var parent=layout.positions().get(parentId);var child=layout.positions().get(childId);
        double dx=child.x()-parent.x(),dy=child.y()-parent.y();
        if(Math.abs(dx)>=Math.abs(dy)&&dx!=0){
            double x1=parent.x()+Math.copySign(20,dx),x2=child.x()-Math.copySign(20,dx),middle=(x1+x2)/2;
            return List.of(point(x1,parent.y()+20),point(middle,parent.y()+20),point(middle,child.y()+20),point(x2,child.y()+20));
        }
        double y1=parent.y()+(dy>=0?40:0),y2=child.y()+(dy>=0?0:40),middle=(y1+y2)/2;
        return List.of(point(parent.x(),y1),point(parent.x(),middle),point(child.x(),middle),point(child.x(),y2));
    }
    private static Position point(double x,double y){return new Position(x,y,0);}
    /** Pack whole connected trees into rows while keeping every prerequisite edge together. */
    public static Layout arrangeCompact(List<Entry> entries) {
        var nodes = new LinkedHashMap<String, Entry>();
        entries.forEach(entry -> nodes.putIfAbsent(entry.id(), entry));
        if (nodes.isEmpty()) return arrange(List.of());
        var neighbors = new LinkedHashMap<String, Set<String>>();
        nodes.keySet().forEach(id -> neighbors.put(id, new LinkedHashSet<>()));
        for (var entry : nodes.values()) for (var parent : entry.parents()) if (nodes.containsKey(parent)) {
            neighbors.get(entry.id()).add(parent); neighbors.get(parent).add(entry.id());
        }
        var unplaced = new LinkedHashSet<>(nodes.keySet());
        var positions = new LinkedHashMap<String, Position>();
        double x = 0, y = 0, rowHeight = 0, width = 1;
        while (!unplaced.isEmpty()) {
            var component = new LinkedHashSet<String>(); var queue = new ArrayDeque<String>();
            queue.add(unplaced.iterator().next());
            while (!queue.isEmpty()) {
                var id = queue.removeFirst(); if (!component.add(id)) continue;
                for (var neighbor : neighbors.get(id)) if (!component.contains(neighbor)) queue.addLast(neighbor);
            }
            unplaced.removeAll(component);
            var tree = arrange(nodes.values().stream().filter(entry -> component.contains(entry.id())).toList());
            if (x > 0 && x + tree.width() > 960) { x = 0; y += rowHeight + ROW; rowHeight = 0; }
            for (var entry : tree.positions().entrySet()) {
                var position = entry.getValue();
                positions.put(entry.getKey(), new Position(x + position.x(), y + position.y(), position.depth()));
            }
            width = Math.max(width, x + tree.width()); rowHeight = Math.max(rowHeight, tree.height());
            x += tree.width() + GAP;
        }
        return new Layout(Collections.unmodifiableMap(positions), width, Math.max(1, y + rowHeight));
    }
    public static Layout arrange(List<Entry> entries) {
        var nodes = new LinkedHashMap<String, Entry>();
        entries.forEach(entry -> nodes.putIfAbsent(entry.id(), entry));
        if (nodes.isEmpty()) return new Layout(Map.of(), 1, 1);
        var depths = new HashMap<String, Integer>();
        var remaining = new LinkedHashSet<>(nodes.keySet());
        while (!remaining.isEmpty()) {
            boolean changed = false;
            for (var iterator = remaining.iterator(); iterator.hasNext();) {
                String id = iterator.next();
                var parents = nodes.get(id).parents().stream().filter(p -> nodes.containsKey(p) && !p.equals(id)).toList();
                if (!depths.keySet().containsAll(parents)) continue;
                depths.put(id, parents.stream().mapToInt(depths::get).max().orElse(-1) + 1);
                iterator.remove(); changed = true;
            }
            // Invalid addon cycles stay visible instead of freezing the screen.
            if (!changed) { remaining.forEach(id -> depths.put(id, 0)); remaining.clear(); }
        }
        var children = new LinkedHashMap<String, List<String>>();
        nodes.keySet().forEach(id -> children.put(id, new ArrayList<>()));
        var roots = new ArrayList<String>();
        for (var entry : nodes.values()) {
            String parent = entry.parents().stream().filter(p -> depths.containsKey(p) && depths.get(p) < depths.get(entry.id()))
                    .max(Comparator.comparingInt(depths::get)).orElse(null);
            if (parent == null) roots.add(entry.id()); else children.get(parent).add(entry.id());
        }
        var widths = new HashMap<String, Double>();
        for (var root : roots) measure(root, children, widths);
        var positions = new LinkedHashMap<String, Position>();
        double offset = 0;
        for (var root : roots) { place(root, offset, children, widths, depths, positions); offset += widths.get(root) + GAP; }
        double height = positions.values().stream().mapToDouble(p -> p.y() + 76).max().orElse(1);
        return new Layout(Collections.unmodifiableMap(positions), Math.max(1, offset - GAP), height);
    }
    private static double measure(String id, Map<String, List<String>> children, Map<String, Double> widths) {
        double width = Math.max(COLUMN, children.get(id).stream().mapToDouble(child -> measure(child, children, widths)).sum());
        widths.put(id, width); return width;
    }
    private static void place(String id, double left, Map<String, List<String>> children, Map<String, Double> widths, Map<String, Integer> depths, Map<String, Position> positions) {
        positions.put(id, new Position(left + widths.get(id) / 2, 12 + depths.get(id) * ROW, depths.get(id)));
        for (var child : children.get(id)) { place(child, left, children, widths, depths, positions); left += widths.get(child); }
    }
    private SkillTreeLayout() {}
}
