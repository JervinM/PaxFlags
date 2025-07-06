package com.paxflags;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TownManager {

    private Plugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    private Map<String, Town> towns = new HashMap<>();

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
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save towns.yml");
            e.printStackTrace();
        }
    }

    public boolean createTown(String name) {
        if (towns.containsKey(name.toLowerCase())) {
            return false;
        }
        towns.put(name.toLowerCase(), new Town(name));
        return true;
    }
}
