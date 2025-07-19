package com.paxflags;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleLinesCommand implements CommandExecutor {

    private final PaxFlagsPlugin plugin;
    private final BorderManager borderManager;

    public ToggleLinesCommand(PaxFlagsPlugin plugin) {
        this.plugin = plugin;
        this.borderManager = plugin.getBorderManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can toggle lines
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("only_players"));
            return true;
        }

        // Check permissions
        if (!player.hasPermission("paxflags.flags.togglelines")) {
            player.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }

        // Toggle border display for player
        boolean enabled = borderManager.toggleBorders(player.getUniqueId());
        player.sendMessage(enabled
                ? plugin.getMessage("borders_enabled")
                : plugin.getMessage("borders_disabled"));

        return true;
    }
}
