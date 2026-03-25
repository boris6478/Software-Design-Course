package nl.vu.cs.softwaredesign;

public abstract class GridItem {

    private final int x;
    private final int y;

    protected GridItem(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }
}
