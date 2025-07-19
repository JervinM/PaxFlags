package com.paxflags;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;

public class ProtectionListener implements Listener {
    private final TownManager townManager;

    public ProtectionListener(TownManager townManager) {
        this.townManager = townManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!canEdit(event.getPlayer().getUniqueId(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis land is claimed by a town. You cannot break blocks here.");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!canEdit(event.getPlayer().getUniqueId(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis land is claimed by a town. You cannot place blocks here.");
        }
    }

    private boolean canEdit(UUID playerId, Chunk chunk) {
        String key = chunk.getX() + "," + chunk.getZ();
        for (Town town : townManager.getAllTowns()) {
            if (town.isChunkClaimed(key)) {
                return playerId.equals(town.getOwner()) || town.getMembers().contains(playerId);
            }
        }
        return true; // Not claimed - allow editing
    }
}
