package nl.vu.cs.softwaredesign;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CityCli {

    private City city;

    public CityCli(City city) {
        this.city = city;
    }

    public void run() throws IOException {
        BufferedReader in =
                new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        JSONManager json = new JSONManager();
        for (;;) {
            System.out.print("> ");
            System.out.flush();
            String line = in.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();
            switch (cmd) {
                case "print" -> {
                    if (parts.length != 1) {
                        System.out.println("Usage: print");
                        break;
                    }
                    System.out.print(CityLayoutRenderer.render(this.city));
                }
                case "save" -> {
                    if (parts.length != 1) {
                        System.out.println("Usage: save");
                        break;
                    }
                    json.exportCity(this.city, JSONManager.DEFAULT_CITY_FILE);
                    System.out.println("Saved to " + JSONManager.DEFAULT_CITY_FILE);
                }
                case "load" -> {
                    if (parts.length != 1) {
                        System.out.println("Usage: load");
                        break;
                    }
                    try {
                        this.city = json.importCity(JSONManager.DEFAULT_CITY_FILE);
                        System.out.println("Loaded from " + JSONManager.DEFAULT_CITY_FILE);
                    } catch (IOException e) {
                        System.out.println("Load failed: " + e.getMessage());
                    }
                }
                case "setposition" -> {
                    if (parts.length != 3) {
                        System.out.println("Usage: setposition <x> <y>");
                        break;
                    }
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        runSetPositionInteractive(in, this.city, x, y);
                    } catch (NumberFormatException e) {
                        System.out.println("x and y must be integers.");
                    }
                }
                case "tick" -> {
                    if (parts.length != 1) {
                        System.out.println("Usage: tick");
                        break;
                    }
                    long t = this.city.advanceTick();
                    this.city.stepIntersections();
                    this.city.stepVehicles();
                    System.out.println("Tick is now " + t + ".");
                }
                case "spawnvehicle" -> {
                    if (parts.length < 5 || parts.length > 7) {
                        System.out.println(
                                "Usage: spawnvehicle <sx> <sy> <tx> <ty> [vehicleType] [driverProfile]");
                        break;
                    }
                    try {
                        int sx = Integer.parseInt(parts[1]);
                        int sy = Integer.parseInt(parts[2]);
                        int tx = Integer.parseInt(parts[3]);
                        int ty = Integer.parseInt(parts[4]);
                        VehicleType vt =
                                parts.length >= 6 ? VehicleType.valueOf(parts[5].toUpperCase()) : VehicleType.CAR;
                        DriverProfile dp = parts.length == 7
                                ? DriverProfile.valueOf(parts[6].toUpperCase())
                                : DriverProfile.REGULAR;
                        spawnVehicle(this.city, sx, sy, tx, ty, vt, dp);
                    } catch (NumberFormatException e) {
                        System.out.println("Coordinates must be integers.");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid vehicle type or driver profile.");
                    }
                }
                case "help", "?" -> {
                    if (parts.length != 1) {
                        System.out.println("Usage: help");
                        break;
                    }
                    printHelp();
                }
                default -> System.out.println("Unknown command: " + line + " (type help for commands)");
            }
        }
    }

    private static void printHelp() {
        System.out.println(
                """
                Commands:
                  help              Show this list
                  print             Draw the city grid (y=0 at bottom, x axis below; - and | for roads)
                  save              Write city to city.json
                  load              Replace city from city.json
                  setposition x y   Place a garage, road, or intersection at (x, y); you will be prompted
                  tick              Advance the global tick by 1 and move vehicles along their routes
                  spawnvehicle sx sy tx ty [vehicleType] [driverProfile]
                                    Spawn a vehicle from garage (sx,sy) to garage (tx,ty); path is computed
                """
                        .stripTrailing());
    }

    private static void spawnVehicle(
            City city, int sx, int sy, int tx, int ty, VehicleType type, DriverProfile profile) {
        GridItem start = gridItemAt(city, sx, sy);
        GridItem end = gridItemAt(city, tx, ty);
        if (!(start instanceof Garage sourceGarage)) {
            System.out.println("Start cell (" + sx + ", " + sy + ") is not a garage.");
            return;
        }
        if (!(end instanceof Garage targetGarage)) {
            System.out.println("Target cell (" + tx + ", " + ty + ") is not a garage.");
            return;
        }
        Vehicle v = new Vehicle(type, profile, 0, sourceGarage, targetGarage);
        city.registerVehicle(v, sourceGarage);
        if (!VehiclePathfinder.computeRouteOrRemove(city, v)) {
            return;
        }
        System.out.println(
                "Spawned vehicle with route length "
                        + v.route().size()
                        + " (tick "
                        + city.tick()
                        + ").");
    }

    private static GridItem gridItemAt(City city, int x, int y) {
        for (GridItem g : city.gridItems()) {
            if (g.x() == x && g.y() == y) {
                return g;
            }
        }
        return null;
    }

    private static void runSetPositionInteractive(BufferedReader in, City city, int x, int y)
            throws IOException {
        System.out.print("Cell type (1 = garage, 2 = road, 3 = intersection, 0 = cancel): ");
        System.out.flush();
        String t = in.readLine();
        if (t == null) {
            return;
        }
        t = t.trim();
        switch (t) {
            case "0" -> {
                return;
            }
            case "1" -> {
                System.out.print("Exit direction (1 = NORTH, 2 = WEST, 3 = SOUTH, 4 = EAST): ");
                System.out.flush();
                String d = in.readLine();
                if (d == null) {
                    return;
                }
                Direction dir = parseDirectionChoice(d.trim());
                if (dir == null) {
                    System.out.println("Invalid choice.");
                    return;
                }
                city.setGridCell(new Garage(x, y, dir));
                System.out.println("Set garage at (" + x + ", " + y + ").");
            }
            case "2" -> {
                System.out.print("Road orientation (1 = NORTH_SOUTH, 2 = WEST_EAST): ");
                System.out.flush();
                String o = in.readLine();
                if (o == null) {
                    return;
                }
                RoadOrientation ro = parseRoadOrientationChoice(o.trim());
                if (ro == null) {
                    System.out.println("Invalid choice.");
                    return;
                }
                System.out.print("Length in meters (default 100): ");
                System.out.flush();
                String lenLine = in.readLine();
                double lengthMeters = parseLengthMeters(lenLine);
                if (lengthMeters <= 0) {
                    System.out.println("Invalid length.");
                    return;
                }
                city.setGridCell(new Road(x, y, ro, lengthMeters));
                System.out.println("Set road at (" + x + ", " + y + ").");
            }
            case "3" -> {
                city.setGridCell(new Intersection(x, y));
                System.out.println("Set intersection at (" + x + ", " + y + ").");
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    private static Direction parseDirectionChoice(String s) {
        return switch (s) {
            case "1" -> Direction.NORTH;
            case "2" -> Direction.WEST;
            case "3" -> Direction.SOUTH;
            case "4" -> Direction.EAST;
            default -> null;
        };
    }

    private static RoadOrientation parseRoadOrientationChoice(String s) {
        return switch (s) {
            case "1" -> RoadOrientation.NORTH_SOUTH;
            case "2" -> RoadOrientation.WEST_EAST;
            default -> null;
        };
    }

    private static double parseLengthMeters(String line) {
        if (line == null) {
            return 100.0;
        }
        String t = line.trim();
        if (t.isEmpty()) {
            return 100.0;
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static City loadCity(String[] args) throws IOException {
        if (args.length >= 1) {
            return new JSONManager().importCity(Path.of(args[0]));
        }
        JSONManager jm = new JSONManager();
        if (Files.isRegularFile(JSONManager.DEFAULT_CITY_FILE)) {
            return jm.importCity(JSONManager.DEFAULT_CITY_FILE);
        }
        return new City();
    }
}
