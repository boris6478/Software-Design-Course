package nl.vu.cs.softwaredesign;

public class Garage extends GridItem {

    private final Direction exitDirection;
    private final Position connectionPosition;

    public Garage(int x, int y, Direction exitDirection) {
        super(x, y);
        this.exitDirection = exitDirection;
        this.connectionPosition = exitDirection.getNextPosition(x, y);
    }

    public Direction exitDirection() {
        return exitDirection;
    }

    public Position connectionPosition() {
        return connectionPosition;
    }
}
