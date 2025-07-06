package com.paxflags;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_RED + "✦✦✦ " + ChatColor.GOLD + "" + ChatColor.BOLD + "Welcome to PaxMC" + ChatColor.DARK_RED + " ✦✦✦");
        player.sendMessage(ChatColor.GRAY + "An Earth SMP ruled by flags, conquest, and diplomacy.");
        player.sendMessage(ChatColor.YELLOW + "You are entering the world of " + ChatColor.RED + "" + ChatColor.BOLD + "PaxFlags");
        player.sendMessage(ChatColor.GRAY + "Will you forge a nation, or fall to another?");
        player.sendMessage("");
    }
}
