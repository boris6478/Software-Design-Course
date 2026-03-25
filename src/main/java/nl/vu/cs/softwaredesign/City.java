package nl.vu.cs.softwaredesign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class City {

    private final List<GridItem> gridItems = new ArrayList<>();
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final Map<Vehicle, GridItem> vehicleLocations = new HashMap<>();
    private long tick;

    public long tick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public long advanceTick() {
        return ++tick;
    }

    public void stepIntersections() {
        for (GridItem g : gridItems) {
            if (g instanceof Intersection intersection) {
                intersection.advanceLight();
            }
        }
    }

    public void stepVehicles() {
        for (Vehicle v : new ArrayList<>(vehicles)) {
            v.tick(this);
        }
    }

    public GridItem vehicleLocation(Vehicle v) {
        return vehicleLocations.get(v);
    }

    public void updateVehicleLocation(Vehicle v, GridItem newLocation) {
        if (!vehicles.contains(v)) {
            return;
        }
        vehicleLocations.put(v, newLocation);
    }

    public void registerVehicle(Vehicle v, GridItem initialLocation) {
        vehicles.add(v);
        vehicleLocations.put(v, initialLocation);
    }

    public void removeVehicle(Vehicle v) {
        vehicles.remove(v);
        vehicleLocations.remove(v);
    }

    public List<GridItem> gridItems() {
        return gridItems;
    }

    public List<Vehicle> vehicles() {
        return vehicles;
    }

    public Map<Vehicle, GridItem> vehicleLocations() {
        return vehicleLocations;
    }

    public void setGridCell(GridItem item) {
        int x = item.x();
        int y = item.y();
        List<GridItem> snapshot = new ArrayList<>(gridItems);
        for (GridItem g : snapshot) {
            if (g.x() == x && g.y() == y) {
                gridItems.remove(g);
                cleanupVehicleRefs(g);
            }
        }
        gridItems.add(item);
    }

    public void removeGridItemAt(int x, int y) {
        GridItem removed = null;
        Iterator<GridItem> it = gridItems.iterator();
        while (it.hasNext()) {
            GridItem g = it.next();
            if (g.x() == x && g.y() == y) {
                removed = g;
                it.remove();
                break;
            }
        }
        if (removed == null) {
            return;
        }
        cleanupVehicleRefs(removed);
    }

    private void cleanupVehicleRefs(GridItem old) {
        for (Vehicle v : vehicles) {
            if (v.sourceGridItem() == old) {
                v.setSourceGridItem(null);
            }
            if (v.targetGridItem() == old) {
                v.setTargetGridItem(null);
            }
        }
        vehicleLocations.entrySet().removeIf(e -> e.getValue() == old);
    }
}
