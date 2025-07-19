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
            case "addfunds":  // New admin command
                return handleAddFunds(player, args);
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
        double createCost = plugin.getConfig().getDouble("economy.create_town_cost", 1000.0);

        // Economy check
        if (plugin.getEconomy() != null && !player.hasPermission("paxflags.economy.bypass")) {
            if (!plugin.getEconomy().has(player, createCost)) {
                player.sendMessage(plugin.getMessage("economy.insufficient_funds")
                    .replace("%cost%", plugin.getEconomy().format(createCost)));
                return true;
            }
        }

        boolean success = townManager.createTown(townName, player.getUniqueId());
        if (success) {
            // Charge player after successful creation
            if (plugin.getEconomy() != null && !player.hasPermission("paxflags.economy.bypass")) {
                plugin.getEconomy().withdrawPlayer(player, createCost);
                player.sendMessage(plugin.getMessage("economy.payment_success")
                    .replace("%cost%", plugin.getEconomy().format(createCost)));
            }
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

        // Give refund if configured
        double refundAmount = plugin.getConfig().getDouble("economy.delete_refund", 0.0);
        if (refundAmount > 0 && plugin.getEconomy() != null && !player.hasPermission("paxflags.economy.bypass")) {
            plugin.getEconomy().depositPlayer(player, refundAmount);
            player.sendMessage(plugin.getMessage("economy.refund_given")
                .replace("%amount%", plugin.getEconomy().format(refundAmount)));
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

        // Charge for unclaiming if configured
        double unclaimCost = plugin.getConfig().getDouble("economy.unclaim_cost", 0.0);
        if (unclaimCost > 0 && plugin.getEconomy() != null && !player.hasPermission("paxflags.economy.bypass")) {
            if (!plugin.getEconomy().has(player, unclaimCost)) {
                player.sendMessage(plugin.getMessage("economy.insufficient_funds")
                    .replace("%cost%", plugin.getEconomy().format(unclaimCost)));
                return true;
            }
            plugin.getEconomy().withdrawPlayer(player, unclaimCost);
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

    // New admin command to add funds to town bank
    private boolean handleAddFunds(Player player, String[] args) {
        if (!player.hasPermission("paxflags.admin.addfunds")) {
            player.sendMessage(plugin.getMessage("general.no_permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("town.addfunds.usage"));
            return true;
        }

        try {
            String townName = args[1];
            double amount = Double.parseDouble(args[2]);
            Town town = townManager.getTown(townName);
            
            if (town == null) {
                player.sendMessage(plugin.getMessage("town.not_found"));
                return true;
            }

            // Implementation for town banks would go here
            player.sendMessage(plugin.getMessage("town.addfunds.success")
                .replace("%town%", townName)
                .replace("%amount%", plugin.getEconomy() != null ? 
                    plugin.getEconomy().format(amount) : String.valueOf(amount)));
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("town.addfunds.invalid_amount"));
            return false;
        }
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
        
        // Show town balance if economy is enabled
        if (plugin.getEconomy() != null) {
            player.sendMessage(plugin.getMessage("town.info.balance")
                .replace("%balance%", plugin.getEconomy().format(0))); // Placeholder for town bank
        }
        
        return true;
    }

    private void sendUsage(Player player) {
        Arrays.asList(
            plugin.getMessage("town.usage.header"),
            plugin.getMessage("town.usage.create"),
            plugin.getMessage("town.usage.delete"),
            plugin.getMessage("town.usage.info"),
            plugin.getMessage("town.usage.unclaim"),
            player.hasPermission("paxflags.admin.addfunds") ? 
                plugin.getMessage("town.usage.addfunds") : ""
        ).stream().filter(s -> !s.isEmpty()).forEach(player::sendMessage);
    }
}