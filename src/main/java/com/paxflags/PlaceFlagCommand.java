package com.paxflags;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlaceFlagCommand implements CommandExecutor {

    private final TownManager townManager;

    public PlaceFlagCommand(TownManager manager) {
        this.townManager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can place flags.");
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !targetBlock.getType().toString().contains("BANNER")) {
            player.sendMessage("You must look at a banner block within 5 blocks to place a flag.");
            return true;
        }

        Location loc = targetBlock.getLocation();
        // TODO: add flag chaining logic

        player.sendMessage("Flag placed at " + loc.getBlockX() + ", " + loc.getBlockZ());
        return true;
    }
}
