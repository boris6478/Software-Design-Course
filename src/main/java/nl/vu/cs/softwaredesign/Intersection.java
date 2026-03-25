package nl.vu.cs.softwaredesign;

public class Intersection extends GridItem {

    public static final int TOGGLE_DELAY = 4;

    private RoadOrientation currentTrafficDirection;
    private int ticksSinceLastChanged;

    public Intersection(int x, int y) {
        this(x, y, RoadOrientation.NORTH_SOUTH, 0);
    }

    public Intersection(int x, int y, RoadOrientation currentTrafficDirection, int ticksSinceLastChanged) {
        super(x, y);
        this.currentTrafficDirection = currentTrafficDirection;
        this.ticksSinceLastChanged = ticksSinceLastChanged;
    }

    public RoadOrientation currentTrafficDirection() {
        return currentTrafficDirection;
    }

    public int ticksSinceLastChanged() {
        return ticksSinceLastChanged;
    }

    public void advanceLight() {
        ticksSinceLastChanged++;
        if (ticksSinceLastChanged >= TOGGLE_DELAY) {
            currentTrafficDirection =
                    currentTrafficDirection == RoadOrientation.NORTH_SOUTH
                            ? RoadOrientation.WEST_EAST
                            : RoadOrientation.NORTH_SOUTH;
            ticksSinceLastChanged = 0;
        }
    }

    public boolean allowsEntryFrom(GridItem fromCell) {
        int ix = x();
        int iy = y();
        int fx = fromCell.x();
        int fy = fromCell.y();
        return switch (currentTrafficDirection) {
            case NORTH_SOUTH -> fx == ix && fy != iy;
            case WEST_EAST -> fy == iy && fx != ix;
        };
    }
}
