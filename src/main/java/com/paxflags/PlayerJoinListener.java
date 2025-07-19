package com.paxflags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final PaxFlagsPlugin plugin;

    public PlayerJoinListener(PaxFlagsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // TODO: Load player settings if needed
        // You can access the player with event.getPlayer()
    }
}
