package nl.vu.cs.softwaredesign;

import java.util.List;

public class Vehicle {

    private final VehicleType type;
    private final DriverProfile driverProfile;
    private int currentSpeed;
    private GridItem sourceGridItem;
    private GridItem targetGridItem;
    private List<GridItem> route = List.of();
    private int ticksOnCurrentSegment;

    public Vehicle(
            VehicleType type,
            DriverProfile driverProfile,
            int currentSpeed,
            GridItem sourceGridItem,
            GridItem targetGridItem) {
        this.type = type;
        this.driverProfile = driverProfile;
        this.currentSpeed = currentSpeed;
        this.sourceGridItem = sourceGridItem;
        this.targetGridItem = targetGridItem;
    }

    public VehicleType type() {
        return type;
    }

    public DriverProfile driverProfile() {
        return driverProfile;
    }

    public int currentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(int currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public GridItem sourceGridItem() {
        return sourceGridItem;
    }

    public void setSourceGridItem(GridItem sourceGridItem) {
        this.sourceGridItem = sourceGridItem;
    }

    public GridItem targetGridItem() {
        return targetGridItem;
    }

    public void setTargetGridItem(GridItem targetGridItem) {
        this.targetGridItem = targetGridItem;
    }

    public List<GridItem> route() {
        return route;
    }

    public void setRoute(List<GridItem> route) {
        this.route = route == null ? List.of() : List.copyOf(route);
        this.ticksOnCurrentSegment = 0;
    }

    public int ticksOnCurrentSegment() {
        return ticksOnCurrentSegment;
    }

    public void tick(City city) {
        List<GridItem> route = this.route;
        if (route.size() < 2) {
            return;
        }
        GridItem here = city.vehicleLocation(this);
        if (here == null) {
            return;
        }
        int idx = indexOnRoute(here, route);
        if (idx < 0) {
            return;
        }
        if (idx >= route.size() - 1) {
            city.removeVehicle(this);
            return;
        }
        GridItem next = route.get(idx + 1);
        int need = driverProfile.ticksPerSegment();

        if (here instanceof Intersection) {
            city.updateVehicleLocation(this, next);
            ticksOnCurrentSegment = 0;
            if (idx + 1 >= route.size() - 1) {
                city.removeVehicle(this);
            }
            return;
        }

        if (next instanceof Intersection inter) {
            if (ticksOnCurrentSegment >= need && !inter.allowsEntryFrom(here)) {
                ticksOnCurrentSegment = need;
                return;
            }
        }

        if (ticksOnCurrentSegment < need) {
            ticksOnCurrentSegment++;
            if (ticksOnCurrentSegment < need) {
                return;
            }
        }

        if (next instanceof Intersection inter && !inter.allowsEntryFrom(here)) {
            ticksOnCurrentSegment = need;
            return;
        }

        city.updateVehicleLocation(this, next);
        ticksOnCurrentSegment = 0;
        if (idx + 1 >= route.size() - 1) {
            city.removeVehicle(this);
        }
    }

    private static int indexOnRoute(GridItem here, List<GridItem> route) {
        int idx = route.indexOf(here);
        if (idx >= 0) {
            return idx;
        }
        for (int i = 0; i < route.size(); i++) {
            GridItem step = route.get(i);
            if (step != null && step.x() == here.x() && step.y() == here.y()) {
                return i;
            }
        }
        return -1;
    }
}
