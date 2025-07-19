package com.paxflags;

import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TownManager {
    private final Plugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<String, Town> towns = new HashMap<>();
    private final Map<UUID, String> playerTowns = new HashMap<>();

    public TownManager(Plugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "towns.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("towns.yml", false);
        }
        loadData();
    }

    public void loadData() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        towns.clear();
        playerTowns.clear();

        if (dataConfig.contains("towns")) {
            for (String townKey : dataConfig.getConfigurationSection("towns").getKeys(false)) {
                String path = "towns." + townKey;
                Town town = new Town(dataConfig.getString(path + ".name", townKey));

                // Load owner
                UUID owner = UUID.fromString(dataConfig.getString(path + ".owner"));
                town.setOwner(owner);
                playerTowns.put(owner, townKey.toLowerCase());

                // Load members
                List<String> memberStrings = dataConfig.getStringList(path + ".members");
                Set<UUID> members = memberStrings.stream().map(UUID::fromString).collect(Collectors.toSet());
                town.setMembers(members);

                // Load allies
                List<String> allyStrings = dataConfig.getStringList(path + ".allies");
                Set<UUID> allies = allyStrings.stream().map(UUID::fromString).collect(Collectors.toSet());
                allies.forEach(town::addAlly);

                // Load border color
                if (dataConfig.contains(path + ".color")) {
                    town.setBorderColor(dataConfig.getString(path + ".color"));
                }

                // Load claims
                town.setClaimedChunks(new HashSet<>(dataConfig.getStringList(path + ".claims")));
                towns.put(townKey.toLowerCase(), town);

                // Update dynmap marker with color
                if (plugin instanceof PaxFlagsPlugin paxPlugin && paxPlugin.getDynmapHook().isEnabled()) {
                    paxPlugin.getDynmapHook().updateTownMarker(
                        town.getName(),
                        getMinChunkX(town), getMinChunkZ(town),
                        getMaxChunkX(town), getMaxChunkZ(town),
                        plugin.getServer().getWorlds().get(0).getName(),
                        town.getBorderColor()
                    );
                }
            }
        }
    }

    public void saveData() {
        dataConfig.set("towns", null); // Clear existing data

        for (Map.Entry<String, Town> entry : towns.entrySet()) {
            String townKey = "towns." + entry.getKey();
            Town town = entry.getValue();

            dataConfig.set(townKey + ".name", town.getName());
            dataConfig.set(townKey + ".owner", town.getOwner().toString());
            dataConfig.set(townKey + ".members", town.getMembers().stream().map(UUID::toString).collect(Collectors.toList()));
            dataConfig.set(townKey + ".claims", new ArrayList<>(town.getClaimedChunks()));

            // Save allies
            dataConfig.set(townKey + ".allies", town.getAllies().stream().map(UUID::toString).collect(Collectors.toList()));

            // Save color
            if (town.getBorderColor() != null) {
                dataConfig.set(townKey + ".color", town.getBorderColor());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save town data: " + e.getMessage());
        }
    }

    public boolean createTown(String name, UUID owner) {
        if (towns.containsKey(name.toLowerCase())) return false;

        Town town = new Town(name);
        town.setOwner(owner);
        towns.put(name.toLowerCase(), town);
        playerTowns.put(owner, name.toLowerCase());

        saveData();

        // Update dynmap marker for new town
        if (plugin instanceof PaxFlagsPlugin paxPlugin && paxPlugin.getDynmapHook().isEnabled()) {
            paxPlugin.getDynmapHook().updateTownMarker(
                name, 0, 0, 0, 0,
                plugin.getServer().getWorlds().get(0).getName(),
                town.getBorderColor()
            );
        }

        return true;
    }

    public boolean deleteTown(String name) {
        if (!towns.containsKey(name.toLowerCase())) return false;

        // Remove player associations with this town
        playerTowns.values().removeIf(t -> t.equalsIgnoreCase(name));
        towns.remove(name.toLowerCase());
        saveData();

        // Remove dynmap marker
        if (plugin instanceof PaxFlagsPlugin paxPlugin && paxPlugin.getDynmapHook().isEnabled()) {
            paxPlugin.getDynmapHook().removeTownMarker(name);
        }
        return true;
    }

    public Town getTown(String name) {
        return towns.get(name.toLowerCase());
    }

    public Town getTownOwnedByPlayer(UUID playerUUID) {
        String townName = playerTowns.get(playerUUID);
        return townName != null ? towns.get(townName) : null;
    }

    public Set<String> getAllClaimedChunks() {
        return towns.values().stream()
                .flatMap(t -> t.getClaimedChunks().stream())
                .collect(Collectors.toSet());
    }

    public Collection<Town> getAllTowns() {
        return towns.values();
    }

    // NEW: get town by claimed chunk
    public Town getTownByChunk(Chunk chunk) {
        String key = chunk.getX() + "," + chunk.getZ();
        for (Town town : towns.values()) {
            if (town.isChunkClaimed(key)) {
                return town;
            }
        }
        return null;
    }

    // NEW: check if chunk is claimed
    public boolean isChunkClaimed(Chunk chunk) {
        String key = chunk.getX() + "," + chunk.getZ();
        return getAllClaimedChunks().contains(key);
    }

    // NEW: check if player is in own town
    public boolean isPlayerInOwnTown(UUID playerId, Chunk chunk) {
        Town town = getTownByChunk(chunk);
        return town != null && town.getOwner().equals(playerId);
    }

    // NEW: adjacency check for claiming chunks
    public boolean canClaimChunk(Town town, String chunkKey) {
        if (town.getClaimedChunks().isEmpty()) {
            // Allow first claim anywhere
            return true;
        }

        String[] parts = chunkKey.split(",");
        int x = Integer.parseInt(parts[0]);
        int z = Integer.parseInt(parts[1]);

        String[] neighbors = new String[] {
            (x + 1) + "," + z,
            (x - 1) + "," + z,
            x + "," + (z + 1),
            x + "," + (z - 1)
        };

        for (String neighbor : neighbors) {
            if (town.isChunkClaimed(neighbor)) {
                return true;
            }
        }
        return false;
    }

    // Helper methods for rectangular boundaries (for dynmap area markers)
    private int getMinChunkX(Town town) {
        return town.getClaimedChunks().stream()
                .map(s -> Integer.parseInt(s.split(",")[0]))
                .min(Integer::compareTo)
                .orElse(0);
    }

    private int getMaxChunkX(Town town) {
        return town.getClaimedChunks().stream()
                .map(s -> Integer.parseInt(s.split(",")[0]))
                .max(Integer::compareTo)
                .orElse(0);
    }

    private int getMinChunkZ(Town town) {
        return town.getClaimedChunks().stream()
                .map(s -> Integer.parseInt(s.split(",")[1]))
                .min(Integer::compareTo)
                .orElse(0);
    }

    private int getMaxChunkZ(Town town) {
        return town.getClaimedChunks().stream()
                .map(s -> Integer.parseInt(s.split(",")[1]))
                .max(Integer::compareTo)
                .orElse(0);
    }
}
