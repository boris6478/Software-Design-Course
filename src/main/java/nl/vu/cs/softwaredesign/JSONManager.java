package nl.vu.cs.softwaredesign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JSONManager {

    public static final Path DEFAULT_CITY_FILE = Path.of("city.json");

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public void exportCity(City city) throws IOException {
        exportCity(city, DEFAULT_CITY_FILE);
    }

    public void exportCity(City city, Path path) throws IOException {
        List<GridItem> gridList = city.gridItems();
        Map<GridItem, Integer> indexOf = new IdentityHashMap<>();
        for (int i = 0; i < gridList.size(); i++) {
            indexOf.put(gridList.get(i), i);
        }

        ArrayNode gridItemsNode = mapper.createArrayNode();
        for (GridItem item : gridList) {
            if (item instanceof Garage garage) {
                ObjectNode n = mapper.createObjectNode();
                n.put("type", "garage");
                n.put("x", garage.x());
                n.put("y", garage.y());
                n.put("exitDirection", garage.exitDirection().name());
                gridItemsNode.add(n);
            } else if (item instanceof Road road) {
                ObjectNode n = mapper.createObjectNode();
                n.put("type", "road");
                n.put("x", road.x());
                n.put("y", road.y());
                n.put("orientation", road.orientation().name());
                n.put("lengthMeters", road.lengthMeters());
                gridItemsNode.add(n);
            } else if (item instanceof Intersection intersection) {
                ObjectNode n = mapper.createObjectNode();
                n.put("type", "intersection");
                n.put("x", intersection.x());
                n.put("y", intersection.y());
                n.put("currentTrafficDirection", intersection.currentTrafficDirection().name());
                n.put("ticksSinceLastChanged", intersection.ticksSinceLastChanged());
                gridItemsNode.add(n);
            } else {
                throw new IOException("Unsupported grid item type: " + item.getClass().getName());
            }
        }

        ArrayNode vehiclesNode = mapper.createArrayNode();
        for (Vehicle v : city.vehicles()) {
            ObjectNode n = mapper.createObjectNode();
            n.put("vehicleType", v.type().name());
            n.put("driverProfile", v.driverProfile().name());
            n.put("currentSpeed", v.currentSpeed());
            putIndexIfPresent(n, "sourceIndex", indexOf, v.sourceGridItem());
            putIndexIfPresent(n, "targetIndex", indexOf, v.targetGridItem());
            putIndexIfPresent(n, "locationIndex", indexOf, city.vehicleLocations().get(v));
            vehiclesNode.add(n);
        }

        ObjectNode root = mapper.createObjectNode();
        root.put("tick", city.tick());
        root.set("gridItems", gridItemsNode);
        root.set("vehicles", vehiclesNode);

        Files.writeString(path, mapper.writeValueAsString(root));
    }

    public City importCity() throws IOException {
        return importCity(DEFAULT_CITY_FILE);
    }

    public City importCity(Path path) throws IOException {
        JsonNode root = mapper.readTree(Files.readAllBytes(path));
        JsonNode gridArray = root.get("gridItems");
        if (gridArray == null || !gridArray.isArray()) {
            throw new IOException("Missing or invalid 'gridItems' array");
        }

        List<GridItem> items = new ArrayList<>();
        Set<Position> occupied = new HashSet<>();
        for (JsonNode node : gridArray) {
            String type = requireText(node, "type");
            int x = node.get("x").asInt();
            int y = node.get("y").asInt();
            Position cell = new Position(x, y);
            if (!occupied.add(cell)) {
                throw new IOException("Duplicate grid item at (" + x + ", " + y + ")");
            }
            switch (type) {
                case "garage" -> {
                    Direction exit = Direction.valueOf(requireText(node, "exitDirection"));
                    items.add(new Garage(x, y, exit));
                }
                case "road" -> {
                    RoadOrientation orientation = RoadOrientation.valueOf(requireText(node, "orientation"));
                    double lengthMeters = readLengthMeters(node);
                    items.add(new Road(x, y, orientation, lengthMeters));
                }
                case "intersection" -> {
                    RoadOrientation dir = readOptionalRoadOrientation(node, "currentTrafficDirection");
                    if (dir == null) {
                        dir = RoadOrientation.NORTH_SOUTH;
                    }
                    int ticks = 0;
                    JsonNode ts = node.get("ticksSinceLastChanged");
                    if (ts != null && ts.isNumber()) {
                        ticks = ts.asInt();
                    }
                    items.add(new Intersection(x, y, dir, ticks));
                }
                default -> throw new IOException("Unknown grid item type: " + type);
            }
        }

        City city = new City();
        JsonNode tickNode = root.get("tick");
        if (tickNode != null && tickNode.isNumber()) {
            city.setTick(tickNode.asLong());
        }
        city.gridItems().addAll(items);

        JsonNode vehiclesArray = root.get("vehicles");
        if (vehiclesArray == null || !vehiclesArray.isArray()) {
            throw new IOException("Missing or invalid 'vehicles' array");
        }

        GridItem[] grid = items.toArray(GridItem[]::new);

        for (JsonNode node : vehiclesArray) {
            VehicleType vehicleType = VehicleType.valueOf(requireText(node, "vehicleType"));
            DriverProfile driverProfile = DriverProfile.valueOf(requireText(node, "driverProfile"));
            int currentSpeed = node.get("currentSpeed").asInt();
            GridItem source = gridAt(grid, readNullableIndex(node, "sourceIndex"));
            GridItem target = gridAt(grid, readNullableIndex(node, "targetIndex"));
            Vehicle v = new Vehicle(vehicleType, driverProfile, currentSpeed, source, target);
            city.vehicles().add(v);
            Integer locationIndex = readNullableIndex(node, "locationIndex");
            if (locationIndex != null) {
                city.vehicleLocations().put(v, grid[locationIndex]);
            } else if (source != null) {
                city.vehicleLocations().put(v, source);
            }
            VehiclePathfinder.computeRouteOrRemove(city, v);
        }

        return city;
    }

    private static Integer indexOrNull(Map<GridItem, Integer> indexOf, GridItem item) {
        if (item == null) {
            return null;
        }
        Integer i = indexOf.get(item);
        if (i == null) {
            throw new IllegalStateException("Grid item not part of this city");
        }
        return i;
    }

    private static void putIndexIfPresent(
            ObjectNode n, String field, Map<GridItem, Integer> indexOf, GridItem item) {
        if (item == null) {
            return;
        }
        n.put(field, indexOrNull(indexOf, item));
    }

    private static double readLengthMeters(JsonNode node) throws IOException {
        JsonNode v = node.get("lengthMeters");
        if (v == null || v.isNull() || !v.isNumber()) {
            return 100.0;
        }
        double m = v.asDouble();
        if (m <= 0) {
            throw new IOException("Road lengthMeters must be positive, got: " + m);
        }
        return m;
    }

    private static RoadOrientation readOptionalRoadOrientation(JsonNode node, String field)
            throws IOException {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isTextual()) {
            return null;
        }
        return RoadOrientation.valueOf(v.asText());
    }

    private static String requireText(JsonNode node, String field) throws IOException {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isTextual()) {
            throw new IOException("Missing or invalid field: " + field);
        }
        return v.asText();
    }

    private static Integer readNullableIndex(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        return v.asInt();
    }

    private static GridItem gridAt(GridItem[] grid, Integer index) {
        if (index == null) {
            return null;
        }
        return grid[index];
    }
}
