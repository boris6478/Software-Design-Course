package nl.vu.cs.softwaredesign;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class VehiclePathfinder {

    private VehiclePathfinder() {}

    public static boolean computeRouteOrRemove(City city, Vehicle vehicle) {
        GridItem src = vehicle.sourceGridItem();
        GridItem tgt = vehicle.targetGridItem();
        if (!(src instanceof Garage sourceGarage) || !(tgt instanceof Garage targetGarage)) {
            System.err.println(
                    "ERROR: Vehicle needs garage source and target; removing vehicle from simulation.");
            city.removeVehicle(vehicle);
            return false;
        }
        List<GridItem> path = findPath(city, sourceGarage, targetGarage);
        if (path.isEmpty()) {
            System.err.println(
                    "ERROR: No route from garage ("
                            + sourceGarage.x()
                            + ", "
                            + sourceGarage.y()
                            + ") to garage ("
                            + targetGarage.x()
                            + ", "
                            + targetGarage.y()
                            + "); removing vehicle from simulation.");
            city.removeVehicle(vehicle);
            return false;
        }
        vehicle.setRoute(path);
        return true;
    }

    public static List<GridItem> findPath(City city, Garage source, Garage target) {
        if (source == target) {
            return List.of(source);
        }
        Map<Position, GridItem> cellOf = cellMap(city);
        Set<GridItem> visited = new HashSet<>();
        Map<GridItem, GridItem> parent = new HashMap<>();
        Queue<GridItem> queue = new ArrayDeque<>();

        visited.add(source);
        queue.add(source);

        while (!queue.isEmpty()) {
            GridItem u = queue.poll();
            if (u == target) {
                return reconstruct(parent, source, target);
            }
            for (GridItem v : neighbors(u, cellOf)) {
                if (visited.add(v)) {
                    parent.put(v, u);
                    queue.add(v);
                }
            }
        }
        return List.of();
    }

    private static List<GridItem> reconstruct(
            Map<GridItem, GridItem> parent, Garage source, GridItem target) {
        List<GridItem> path = new ArrayList<>();
        GridItem cur = target;
        while (cur != null) {
            path.add(cur);
            if (cur == source) {
                break;
            }
            cur = parent.get(cur);
        }
        Collections.reverse(path);
        if (path.isEmpty() || path.get(0) != source) {
            return List.of();
        }
        return List.copyOf(path);
    }

    static Map<Position, GridItem> cellMap(City city) {
        Map<Position, GridItem> map = new HashMap<>();
        for (GridItem g : city.gridItems()) {
            map.put(new Position(g.x(), g.y()), g);
        }
        return map;
    }

    static List<GridItem> neighbors(GridItem item, Map<Position, GridItem> cellOf) {
        List<GridItem> out = new ArrayList<>();
        if (item instanceof Garage g) {
            GridItem next = cellOf.get(g.connectionPosition());
            if (next instanceof Road r && connectsGarageAndRoad(g, r)) {
                out.add(r);
            } else if (next instanceof Intersection i
                    && g.connectionPosition().equals(new Position(i.x(), i.y()))) {
                out.add(i);
            }
        } else if (item instanceof Road r) {
            addRoadNeighbor(r, r.startPosition(), cellOf, out);
            addRoadNeighbor(r, r.endPosition(), cellOf, out);
        } else if (item instanceof Intersection intersection) {
            addIntersectionNeighbors(intersection, cellOf, out);
        }
        return out;
    }

    private static void addIntersectionNeighbors(
            Intersection intersection, Map<Position, GridItem> cellOf, List<GridItem> out) {
        Position ip = new Position(intersection.x(), intersection.y());
        for (Direction d : Direction.values()) {
            Position np = d.getNextPosition(intersection.x(), intersection.y());
            GridItem o = cellOf.get(np);
            if (o == null) {
                continue;
            }
            if (o instanceof Road r && connectsRoadAndIntersection(r, intersection)) {
                out.add(r);
            } else if (o instanceof Garage g && g.connectionPosition().equals(ip)) {
                out.add(g);
            } else if (o instanceof Intersection i2) {
                out.add(i2);
            }
        }
    }

    private static void addRoadNeighbor(
            Road r, Position p, Map<Position, GridItem> cellOf, List<GridItem> out) {
        GridItem o = cellOf.get(p);
        if (o == null) {
            return;
        }
        if (o instanceof Garage g) {
            if (connectsGarageAndRoad(g, r)) {
                out.add(g);
            }
        } else if (o instanceof Road r2) {
            if (connectsRoadAndRoad(r, r2)) {
                out.add(r2);
            }
        } else if (o instanceof Intersection i) {
            if (connectsRoadAndIntersection(r, i)) {
                out.add(i);
            }
        }
    }

    private static boolean connectsGarageAndRoad(Garage g, Road r) {
        return g.connectionPosition().equals(new Position(r.x(), r.y()));
    }

    private static boolean connectsRoadAndRoad(Road a, Road b) {
        Position ap = new Position(a.x(), a.y());
        Position bp = new Position(b.x(), b.y());
        return ap.equals(b.startPosition())
                || ap.equals(b.endPosition())
                || bp.equals(a.startPosition())
                || bp.equals(a.endPosition());
    }

    private static boolean connectsRoadAndIntersection(Road r, Intersection i) {
        Position ip = new Position(i.x(), i.y());
        return ip.equals(r.startPosition()) || ip.equals(r.endPosition());
    }
}
