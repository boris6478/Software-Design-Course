package nl.vu.cs.softwaredesign;

public class Road extends GridItem {

    private final RoadOrientation orientation;
    private final double lengthMeters;
    private final Position startPosition;
    private final Position endPosition;

    public Road(int x, int y, RoadOrientation orientation, double lengthMeters) {
        super(x, y);
        if (lengthMeters <= 0) {
            throw new IllegalArgumentException("Road length must be positive, got: " + lengthMeters);
        }
        this.orientation = orientation;
        this.lengthMeters = lengthMeters;
        this.startPosition = switch (orientation) {
            case WEST_EAST -> Direction.WEST.getNextPosition(x, y);
            case NORTH_SOUTH -> Direction.NORTH.getNextPosition(x, y);
        };
        this.endPosition = switch (orientation) {
            case WEST_EAST -> Direction.EAST.getNextPosition(x, y);
            case NORTH_SOUTH -> Direction.SOUTH.getNextPosition(x, y);
        };
    }

    public RoadOrientation orientation() {
        return orientation;
    }

    public double lengthMeters() {
        return lengthMeters;
    }

    public Position startPosition() {
        return startPosition;
    }

    public Position endPosition() {
        return endPosition;
    }
}
