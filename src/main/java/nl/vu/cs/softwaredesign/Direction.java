package nl.vu.cs.softwaredesign;

public enum Direction {
    NORTH,
    WEST,
    SOUTH,
    EAST;

    public Position getNextPosition(int x, int y) {
        return switch (this) {
            case NORTH -> new Position(x, y - 1);
            case SOUTH -> new Position(x, y + 1);
            case EAST -> new Position(x + 1, y);
            case WEST -> new Position(x - 1, y);
        };
    }
}
