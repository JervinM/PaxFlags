package com.paxflags;

import org.bukkit.plugin.java.JavaPlugin;

public class PaxFlagsPlugin extends JavaPlugin {

    private TownManager townManager;

    @Override
    public void onEnable() {
        this.townManager = new TownManager(this);
        getLogger().info("PaxFlags enabled!");

        getCommand("town").setExecutor(new TownCommand(townManager));
        getCommand("placeflag").setExecutor(new PlaceFlagCommand(townManager));

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("PaxFlags disabled!");
        townManager.saveData();
    }
}
