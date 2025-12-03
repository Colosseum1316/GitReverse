package colosseum.gitreverse;

import colosseum.utility.arcade.GameType;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@AllArgsConstructor
final class MapData {

    public final String name;
    public final String author;
    public final GameType gameType;

    public final Location[] corners;

    public final HashMap<String, ArrayList<Location>> dataLocations;
    public final HashMap<String, ArrayList<Location>> teamLocsLocations;
    public final HashMap<String, ArrayList<Location>> customLocsLocations;

    public List<Location> getIronLocations(String key) {
        return dataLocations.computeIfAbsent(key, k -> new ArrayList<>());
    }

    public List<Location> getGoldLocations(String key) {
        return teamLocsLocations.computeIfAbsent(key, k -> new ArrayList<>());
    }

    public List<Location> getSpongeLocations(String key) {
        return customLocsLocations.computeIfAbsent(key, k -> new ArrayList<>());
    }
}
