package com.paxflags;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TownCommand implements CommandExecutor {

    private final TownManager townManager;

    public TownCommand(TownManager townManager) {
        this.townManager = townManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /town create <name>");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /town create <name>");
                return true;
            }
            String townName = args[1];
            if (townManager.createTown(townName)) {
                sender.sendMessage("Town '" + townName + "' created!");
            } else {
                sender.sendMessage("Town name already exists!");
            }
            return true;
        }

        sender.sendMessage("Unknown subcommand.");
        return true;
    }
}
