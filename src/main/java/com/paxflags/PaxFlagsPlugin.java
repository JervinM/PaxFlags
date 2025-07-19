package com.paxflags;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class PaxFlagsPlugin extends JavaPlugin {
    private TownManager townManager;
    private BorderManager borderManager;
    private DynmapHook dynmapHook;

    private FileConfiguration messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Create default messages.yml if missing & load it
        saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));

        // Initialize dynmapHook FIRST so TownManager can use it
        dynmapHook = new DynmapHook(this);
        townManager = new TownManager(this);
        borderManager = new BorderManager(this);

        // Register commands
        getCommand("town").setExecutor(new TownCommand(this));
        getCommand("placeflag").setExecutor(new PlaceFlagCommand(this));
        getCommand("flags").setExecutor(new FlagsCommand(borderManager));
        getCommand("togglelines").setExecutor(new ToggleLinesCommand(this));

        // Register events
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(townManager), this);
        Bukkit.getPluginManager().registerEvents(borderManager, this);

        getLogger().info("PaxFlags has been enabled!");
    }

    @Override
    public void onDisable() {
        // Add null check to prevent crash if plugin failed to enable
        if (borderManager != null) borderManager.stop();
        if (townManager != null) townManager.saveData();
        getLogger().info("PaxFlags has been disabled!");
    }

    public TownManager getTownManager() {
        return townManager;
    }

    public BorderManager getBorderManager() {
        return borderManager;
    }

    public DynmapHook getDynmapHook() {
        return dynmapHook;
    }

    public String getMessage(String path) {
        String message = messages.getString("messages." + path);
        if (message == null) {
            getLogger().warning("Missing message: " + path);
            return ChatColor.RED + "[" + getName() + "] Message not configured!";
        }
        // Replace %prefix% with actual prefix from config and translate colors
        String prefix = ChatColor.translateAlternateColorCodes('&', messages.getString("prefix", "&7[&cPaxFlags&7] "));
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
