package nl.vu.cs.softwaredesign;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CityLayoutRenderer {

    private CityLayoutRenderer() {}

    public static String render(City city) {
        var items = city.gridItems();
        if (items.isEmpty()) {
            return "(empty city — no grid items)\n";
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (GridItem g : items) {
            minX = Math.min(minX, g.x());
            maxX = Math.max(maxX, g.x());
            minY = Math.min(minY, g.y());
            maxY = Math.max(maxY, g.y());
        }

        Map<Position, GridItem> at = new HashMap<>();
        for (GridItem g : items) {
            at.put(new Position(g.x(), g.y()), g);
        }

        StringBuilder sb = new StringBuilder();
        for (int y = maxY; y >= minY; y--) {
            sb.append(String.format("%3d  ", y));
            for (int x = minX; x <= maxX; x++) {
                GridItem cell = at.get(new Position(x, y));
                char c;
                if (cell == null) {
                    c = '.';
                } else if (cell instanceof Garage) {
                    c = 'G';
                } else if (cell instanceof Road road) {
                    c = road.orientation() == RoadOrientation.WEST_EAST ? '-' : '|';
                } else if (cell instanceof Intersection) {
                    c = '+';
                } else {
                    c = '?';
                }
                sb.append(' ').append(c).append(' ');
            }
            sb.append('\n');
        }

        sb.append("     ");
        for (int x = minX; x <= maxX; x++) {
            sb.append(String.format("%2d ", x));
        }
        sb.append('\n');

        sb.append("\nLegend:  G  garage   -  road (west-east)   |  road (north-south)   +  intersection   .  empty\n");

        appendTrafficLights(city, sb);
        appendVehicleList(city, sb);
        return sb.toString();
    }

    private static void appendTrafficLights(City city, StringBuilder sb) {
        List<Intersection> lights = new ArrayList<>();
        for (GridItem g : city.gridItems()) {
            if (g instanceof Intersection i) {
                lights.add(i);
            }
        }
        sb.append("\nTraffic lights:\n");
        if (lights.isEmpty()) {
            sb.append("  (none)\n");
            return;
        }
        lights.sort(Comparator.comparingInt(Intersection::y).thenComparingInt(Intersection::x));
        for (Intersection i : lights) {
            sb.append(String.format(
                    "  (%d, %d)  %s  %d/%d ticks since last change%n",
                    i.x(),
                    i.y(),
                    i.currentTrafficDirection().name(),
                    i.ticksSinceLastChanged(),
                    Intersection.TOGGLE_DELAY));
        }
    }

    private static void appendVehicleList(City city, StringBuilder sb) {
        var vehicles = city.vehicles();
        if (vehicles.isEmpty()) {
            sb.append("\nVehicles: (none)\n");
            return;
        }
        sb.append("\nVehicles:\n");
        int n = 1;
        for (Vehicle v : vehicles) {
            GridItem current = city.vehicleLocation(v);
            sb.append(String.format(
                    "  %d. %s — start %s, current %s, destination %s%s%n",
                    n++,
                    v.type(),
                    formatCell(v.sourceGridItem()),
                    formatCell(current),
                    formatCell(v.targetGridItem()),
                    formatSegmentProgress(city, v)));
        }
    }

    private static String formatSegmentProgress(City city, Vehicle v) {
        List<GridItem> route = v.route();
        GridItem here = city.vehicleLocation(v);
        if (route.isEmpty() || here == null) {
            return "";
        }
        int idx = route.indexOf(here);
        if (idx < 0) {
            return "";
        }
        if (idx >= route.size() - 1) {
            return " (arrived)";
        }
        int need = v.driverProfile().ticksPerSegment();
        int done = v.ticksOnCurrentSegment();
        int pct = need == 0 ? 0 : (int) Math.round(100.0 * done / need);
        return String.format(" segment %d%%", Math.min(100, pct));
    }

    private static String formatCell(GridItem item) {
        if (item == null) {
            return "—";
        }
        String kind =
                item instanceof Garage
                        ? "garage"
                        : item instanceof Road ? "road"
                        : item instanceof Intersection ? "intersection" : "cell";
        return String.format("(%d, %d) %s", item.x(), item.y(), kind);
    }
}
