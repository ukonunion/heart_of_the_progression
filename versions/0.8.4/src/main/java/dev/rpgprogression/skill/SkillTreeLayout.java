package dev.rpgprogression.skill;

import java.util.*;

/** A forest layout with shared prerequisite edges and no client or optional mod types. */
public final class SkillTreeLayout {
    public record Entry(String id, List<String> parents) {}
    public record Position(double x, double y, int depth) {}
    public record Layout(Map<String, Position> positions, double width, double height) {}
    private static final double COLUMN = 96, ROW = 92, GAP = 28;
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
