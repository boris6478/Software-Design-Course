package nl.vu.cs.softwaredesign;

public enum DriverProfile {
    SLOW,
    REGULAR,
    AGGRESSIVE;

    public int ticksPerSegment() {
        return switch (this) {
            case SLOW -> 4;
            case REGULAR -> 2;
            case AGGRESSIVE -> 1;
        };
    }
}
