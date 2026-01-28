package colosseum.gitreverse;

import colosseum.utility.TeamName;
import colosseum.utility.MapConstants;
import nl.rutgerkok.hammer.ChunkAccess;
import nl.rutgerkok.hammer.anvil.AnvilChunk;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GitReverse {

    public static void main(String[] args) throws Exception {
        unparseMap(new File(args[0]));
    }

    public static void unparseMap(final File file) throws Exception {
        if (!file.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + file.getAbsolutePath());
        }
        Path directory = file.toPath();
        final MapData mapData = getMapData(directory);
        writeMapDat(mapData, directory);
        unparseMap(mapData, directory);
        FileUtils.deleteQuietly(directory.resolve(MapConstants.WORLDCONFIG_DAT).toFile());
    }

    private static MapData getMapData(final Path directory) throws Exception {
        String name = "null";
        String author = "null";

        final HashMap<String, ArrayList<Location>> dataLocations = new HashMap<>();
        final HashMap<String, ArrayList<Location>> teamLocsLocations = new HashMap<>();
        final HashMap<String, ArrayList<Location>> customLocsLocations = new HashMap<>();

        final List<String> lines = Files.readAllLines(directory.resolve(MapConstants.WORLDCONFIG_DAT));
        List<Location> current = null;
        int minX = -256, minY = 0, minZ = -256, maxX = 256, maxY = 256, maxZ = 256;

        for (String line : lines) {
            String[] tokens = line.split(":");

            if (tokens.length < 2 || tokens[0].isEmpty()) {
                continue;
            }

            String key = tokens[0], value = tokens[1];

            // Name & Author
            if (key.equalsIgnoreCase("MAP_NAME")) {
                name = value;
            } else if (key.equalsIgnoreCase("MAP_AUTHOR")) {
                author = value;
            } else if (key.equalsIgnoreCase("TEAM_NAME")) {
                // Spawn Locations
                current = teamLocsLocations.computeIfAbsent(value, k -> new ArrayList<>());
            } else if (key.equalsIgnoreCase("TEAM_SPAWNS")) {
                for (int i = 1; i < tokens.length; i++) {
                    Location location = fromString(tokens[i]);
                    if (location == null) {
                        continue;
                    }
                    current.add(location);
                }
            } else if (key.equalsIgnoreCase("DATA_NAME")) {
                // Data Locations
                current = dataLocations.computeIfAbsent(value, k -> new ArrayList<>());
            } else if (key.equalsIgnoreCase("DATA_LOCS")) {
                for (int i = 1; i < tokens.length; i++) {
                    Location location = fromString(tokens[i]);
                    if (location == null) {
                        continue;
                    }
                    current.add(location);
                }
            } else if (key.equalsIgnoreCase("CUSTOM_NAME")) {
                // Custom Locations
                current = customLocsLocations.computeIfAbsent(value, k -> new ArrayList<>());
            } else if (key.equalsIgnoreCase("CUSTOM_LOCS")) {
                for (int i = 1; i < tokens.length; i++) {
                    Location location = fromString(tokens[i]);
                    if (location == null) {
                        continue;
                    }
                    current.add(location);
                }
            }
            // Map Bounds
            else if (key.equalsIgnoreCase("MIN_X")) minX = Integer.parseInt(value);
            else if (key.equalsIgnoreCase("MAX_X")) maxX = Integer.parseInt(value);
            else if (key.equalsIgnoreCase("MIN_Z")) minZ = Integer.parseInt(value);
            else if (key.equalsIgnoreCase("MAX_Z")) maxZ = Integer.parseInt(value);
            else if (key.equalsIgnoreCase("MIN_Y")) minY = Integer.parseInt(value);
            else if (key.equalsIgnoreCase("MAX_Y")) maxY = Integer.parseInt(value);
        }

        Location[] corners = new Location[2];
        if (minY < 0) {
            minY = 0;
        }
        if (maxY > 254) {
            maxY = 254;
        }
        corners[0] = new Location(minX, minY, minZ);
        corners[1] = new Location(maxX, maxY, maxZ);

        return new MapData(name, author, corners, dataLocations, teamLocsLocations, customLocsLocations);
    }

    private static void writeMapDat(final MapData mapData, final Path directory) throws Exception {
        try (final BufferedWriter out = Files.newBufferedWriter(directory.resolve(MapConstants.MAP_DAT))) {
            out.write("MAP_NAME:" + mapData.name);
            out.write("\n");
            out.write("MAP_AUTHOR:" + mapData.author);
            out.write("\n");
            out.write("ADMIN_LIST:");
            out.write("\n");
            out.write("currentlyLive:" + false);
            out.write("\n");
            out.write("warps:");
            out.write("\n");
            out.write("LOCKED:" + false);
        }
    }

    private static void unparseMap(final MapData mapData, final Path directory) throws Exception {
        World world = new World(directory);

        // Min/Max Border
        final Block blockCornerA = world.getBlock(mapData.corners[0]);
        final Block blockCornerB = world.getBlock(mapData.corners[1]);
        world.setWool(blockCornerA);
        world.addGoldPlate(blockCornerA);
        world.setWool(blockCornerB);
        world.addGoldPlate(blockCornerB);

        // Data/Iron
        for (String woolCol : mapData.dataLocations.keySet()) {
            for (Location loc : mapData.getIronLocations(woolCol)) {
                world.setWool(world.getBlock(loc), TeamName.getByDyeColor(woolCol.toUpperCase()).getWoolData());
                world.addIronPlate(world.getBlock(loc));
            }
        }

        // Spawns/Gold
        for (String woolCol : mapData.teamLocsLocations.keySet()) {
            for (Location loc : mapData.getGoldLocations(woolCol)) {
                world.setWool(world.getBlock(loc), Arrays.stream(TeamName.values())
                    .filter(entry -> entry.name().equalsIgnoreCase(woolCol))
                    .map(TeamName::getWoolData)
                    .findFirst()
                    .orElseGet(() -> TeamName.getByDyeColor(woolCol.toUpperCase()).getWoolData()));
                world.addGoldPlate(world.getBlock(loc));
            }
        }

        // Custom/Sponge - Inc Data
        for (String name : mapData.customLocsLocations.keySet()) {
            for (Location loc : mapData.getSpongeLocations(name)) {
                try {
                    final int id = Integer.parseInt(name);
                    if (id > 197 || id < 0) {
                        throw new IllegalStateException("Invalid material id " + id + " at " + name);
                    }
                    world.setBlock(world.getBlock(loc), (short) id, (byte) 0);
                } catch (NumberFormatException e) {
                    world.setBlock(world.getBlock(loc), Util.MATERIAL_SPONGE, (byte) 0);

                    String[] lines = new String[4];
                    for (String data : name.split(" ")) {
                        int size = data.length();
                        for (int i = 0; i < 4; i++) {
                            if (lines[i] == null) {
                                lines[i] = "";
                            }
                            if (lines[i].length() + size + (lines[i].isEmpty() ? 0 : 1) <= 15) {
                                if (!lines[i].isEmpty()) {
                                    lines[i] += " ";
                                }
                                lines[i] += data;
                                break;
                            }
                        }
                    }
                    world.addAndSetSign(loc, lines);
                }
            }
        }

        try (final ChunkAccess<AnvilChunk> chunkAccess = world.getChunkAccess()) {
            for (Map.Entry<Pair<Integer, Integer>, AnvilChunk> c : world.getVisitedChunks().entrySet()) {
                chunkAccess.saveChunk(c.getValue());
            }
        }
    }

    private static Location fromString(final String location) {
        String[] cords = location.split(",");
        try {
            return new Location(
                Integer.parseInt(cords[0]) + 0.5,
                Integer.parseInt(cords[1]),
                Integer.parseInt(cords[2]) + 0.5);
        } catch (Exception e) {
            // no op
        }
        return null;
    }
}
