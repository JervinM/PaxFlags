package com.paxflags;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class FlagsCommand implements CommandExecutor {
    private final BorderManager borderManager;

    public FlagsCommand(BorderManager borderManager) {
        this.borderManager = borderManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        // Example: toggle particles (you'd store player prefs)
        player.sendMessage("Border particles toggled (not fully implemented).");
        return true;
    }
}
