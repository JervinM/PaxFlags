package com.paxflags;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class PaxFlagsPlugin extends JavaPlugin {
    private TownManager townManager;
    private BorderManager borderManager;
    private DynmapHook dynmapHook;
    private Economy economy; // Vault economy instance

    private FileConfiguration messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Load messages.yml
        saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));

        // Setup Vault economy (soft dependency)
        if (!setupEconomy()) {
            getLogger().info("Vault not found - economy features disabled.");
        }

        // Initialize core components
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
        if (borderManager != null) borderManager.stop();
        if (townManager != null) townManager.saveData();
        getLogger().info("PaxFlags has been disabled!");
    }

    /**
     * Sets up Vault economy integration
     * @return true if successful
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    // Getters for core components
    public TownManager getTownManager() {
        return townManager;
    }

    public BorderManager getBorderManager() {
        return borderManager;
    }

    public DynmapHook getDynmapHook() {
        return dynmapHook;
    }

    /**
     * @return Vault economy instance (null if Vault not installed)
     */
    public Economy getEconomy() {
        return economy;
    }

    public String getMessage(String path) {
        String message = messages.getString("messages." + path);
        if (message == null) {
            getLogger().warning("Missing message: " + path);
            return ChatColor.RED + "[" + getName() + "] Message not configured!";
        }
        String prefix = ChatColor.translateAlternateColorCodes('&', messages.getString("prefix", "&7[&cPaxFlags&7] "));
        return ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
    }
}