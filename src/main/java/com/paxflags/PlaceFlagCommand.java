package com.paxflags;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class PlaceFlagCommand implements CommandExecutor {
    private final PaxFlagsPlugin plugin;
    private final TownManager townManager;

    public PlaceFlagCommand(PaxFlagsPlugin plugin) {
        this.plugin = plugin;
        this.townManager = plugin.getTownManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("general.only_players"));
            return true;
        }

        if (!player.hasPermission("paxflags.placeflag")) {
            player.sendMessage(plugin.getMessage("general.no_permission"));
            return true;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || !target.getType().toString().contains("BANNER")) {
            player.sendMessage(plugin.getMessage("flag.invalid_target"));
            return true;
        }

        Town town = (args.length > 0)
            ? townManager.getTown(args[0])
            : townManager.getTownOwnedByPlayer(player.getUniqueId());

        if (town == null) {
            player.sendMessage(plugin.getMessage("town.not_found"));
            return true;
        }

        if (!town.getOwner().equals(player.getUniqueId()) && 
            !town.getMembers().contains(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("town.not_member"));
            return true;
        }

        Chunk chunk = target.getChunk();
        String chunkKey = chunk.getX() + "," + chunk.getZ();

        if (townManager.getAllClaimedChunks().contains(chunkKey)) {
            player.sendMessage(plugin.getMessage("claim.already_claimed"));
            return true;
        }

        town.claimChunk(chunkKey);
        townManager.saveData();

        if (plugin.getDynmapHook() != null && plugin.getDynmapHook().isEnabled()) {
            plugin.getDynmapHook().updateTownMarker(
                town.getName(),
                chunk.getX(), chunk.getZ(),
                chunk.getX(), chunk.getZ(),
                chunk.getWorld().getName()
            );
        }

        player.sendMessage(plugin.getMessage("claim.success")
            .replace("%town%", town.getName())
            .replace("%x%", String.valueOf(chunk.getX()))
            .replace("%z%", String.valueOf(chunk.getZ())));

        return true;
    }
}
