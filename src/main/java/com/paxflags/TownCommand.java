package com.paxflags;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class TownCommand implements CommandExecutor {
    private final PaxFlagsPlugin plugin;
    private final TownManager townManager;

    public TownCommand(PaxFlagsPlugin plugin) {
        this.plugin = plugin;
        this.townManager = plugin.getTownManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("general.only_players"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return handleCreate(player, args);
            case "delete":
                return handleDelete(player, args);
            case "info":
                return handleInfo(player, args);
            case "unclaim":
                return handleUnclaim(player);
            default:
                sendUsage(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (!player.hasPermission("paxflags.town.create")) {
            player.sendMessage(plugin.getMessage("general.no_permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("town.create.usage"));
            return true;
        }

        String townName = args[1];
        boolean success = townManager.createTown(townName, player.getUniqueId());
        if (success) {
            player.sendMessage(plugin.getMessage("town.create.success").replace("%town%", townName));
        } else {
            player.sendMessage(plugin.getMessage("town.create.exists").replace("%town%", townName));
        }
        return true;
    }

    private boolean handleDelete(Player player, String[] args) {
        if (!player.hasPermission("paxflags.town.delete")) {
            player.sendMessage(plugin.getMessage("general.no_permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("town.delete.usage"));
            return true;
        }

        String townName = args[1];
        Town town = townManager.getTown(townName);
        if (town == null || !town.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("town.not_owner"));
            return true;
        }

        boolean success = townManager.deleteTown(townName);
        if (success) {
            plugin.getDynmapHook().removeTownMarker(townName);
            player.sendMessage(plugin.getMessage("town.delete.success").replace("%town%", townName));
        } else {
            player.sendMessage(plugin.getMessage("town.delete.error"));
        }
        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        Town town = args.length >= 2
                ? townManager.getTown(args[1])
                : townManager.getTownOwnedByPlayer(player.getUniqueId());

        if (town == null) {
            player.sendMessage(plugin.getMessage("town.not_found"));
            return true;
        }

        player.sendMessage(plugin.getMessage("town.info.header").replace("%town%", town.getName()));
        player.sendMessage(plugin.getMessage("town.info.owner")
                .replace("%owner%", Bukkit.getOfflinePlayer(town.getOwner()).getName()));
        player.sendMessage(plugin.getMessage("town.info.claims")
                .replace("%count%", String.valueOf(town.getClaimedChunks().size())));
        return true;
    }

    private boolean handleUnclaim(Player player) {
        if (!player.hasPermission("paxflags.town.unclaim")) {
            player.sendMessage(plugin.getMessage("general.no_permission"));
            return true;
        }

        Town town = townManager.getTownOwnedByPlayer(player.getUniqueId());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.not_owner"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = chunk.getX() + "," + chunk.getZ();

        boolean success = town.unclaimChunk(chunkKey);
        if (success) {
            townManager.saveData();
            player.sendMessage(plugin.getMessage("town.unclaim.success"));
        } else {
            player.sendMessage(plugin.getMessage("town.unclaim.error"));
        }
        return true;
    }

    private void sendUsage(Player player) {
        Arrays.asList(
                plugin.getMessage("town.usage.header"),
                plugin.getMessage("town.usage.create"),
                plugin.getMessage("town.usage.delete"),
                plugin.getMessage("town.usage.info"),
                plugin.getMessage("town.usage.unclaim")
        ).forEach(player::sendMessage);
    }
}
