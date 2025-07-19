package com.paxflags;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle.DustOptions;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BorderManager implements Listener {
    private final PaxFlagsPlugin plugin;
    private final TownManager townManager;
    
    // Player state tracking
    private final Set<UUID> playersWithBordersOn = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<Chunk>> lastVisibleChunks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSoundPlayed = new ConcurrentHashMap<>();
    
    // Configuration cache
    private int particleSpacing;
    private int renderRadius;
    private double particleSize;
    private boolean enableSounds;
    private String defaultColor;
    
    // Animation
    private float pulsePhase = 0;
    private final BukkitRunnable animationTask;

    public BorderManager(PaxFlagsPlugin plugin) {
        this.plugin = plugin;
        this.townManager = plugin.getTownManager();
        reloadConfig();
        
        // Animation pulse effect
        this.animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                pulsePhase = (pulsePhase + 0.05f) % (2 * Math.PI);
            }
        };
        animationTask.runTaskTimer(plugin, 0, 1);
    }

    public void reloadConfig() {
        this.particleSpacing = plugin.getConfig().getInt("borders.particle_spacing", 3);
        this.renderRadius = plugin.getConfig().getInt("borders.render_radius", 5);
        this.particleSize = plugin.getConfig().getDouble("borders.particle_size", 0.8);
        this.enableSounds = plugin.getConfig().getBoolean("borders.sounds", true);
        this.defaultColor = plugin.getConfig().getString("borders.default_color", "#FF0000");
    }

    public boolean toggleBorders(UUID playerUUID) {
        boolean nowEnabled = !playersWithBordersOn.contains(playerUUID);
        
        if (nowEnabled) {
            playersWithBordersOn.add(playerUUID);
            if (enableSounds) {
                playToggleSound(Bukkit.getPlayer(playerUUID));
            }
        } else {
            playersWithBordersOn.remove(playerUUID);
            lastVisibleChunks.remove(playerUUID); // Clear cache
        }
        
        return nowEnabled;
    }

    private void playToggleSound(Player player) {
        long now = System.currentTimeMillis();
        if (now - lastSoundPlayed.getOrDefault(player.getUniqueId(), 0L) > 1000) {
            player.playSound(
                player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_PLING,
                0.5f,
                1.5f
            );
            lastSoundPlayed.put(player.getUniqueId(), now);
        }
    }

    // Called every tick (async-friendly)
    public void updatePlayerBorders() {
        for (UUID uuid : playersWithBordersOn) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            Set<Chunk> visibleChunks = calculateVisibleChunks(player);
            Set<Chunk> previouslyVisible = lastVisibleChunks.getOrDefault(uuid, Collections.emptySet());

            // Only render changed chunks
            Set<Chunk> toRender = new HashSet<>(visibleChunks);
            toRender.removeAll(previouslyVisible);

            renderChunkBorders(player, toRender);
            lastVisibleChunks.put(uuid, visibleChunks);
        }
    }

    private Set<Chunk> calculateVisibleChunks(Player player) {
        Set<Chunk> visible = new HashSet<>();
        Chunk center = player.getLocation().getChunk();
        World world = player.getWorld();

        for (int dx = -renderRadius; dx <= renderRadius; dx++) {
            for (int dz = -renderRadius; dz <= renderRadius; dz++) {
                Chunk chunk = world.getChunkAt(center.getX() + dx, center.getZ() + dz);
                if (townManager.isChunkClaimed(chunk)) {
                    visible.add(chunk);
                }
            }
        }
        return visible;
    }

    private void renderChunkBorders(Player player, Set<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            Town owner = townManager.getTownByChunk(chunk);
            DustOptions dust = getDustOptions(player, owner);

            // Animated pulse effect
            double size = particleSize * (0.9 + 0.2 * Math.sin(pulsePhase));

            renderChunkEdges(player, chunk, dust.withSize((float) size));
        }
    }

    private DustOptions getDustOptions(Player player, Town town) {
        // Color logic: Ally/Enemy/Neutral
        String colorHex = defaultColor;
        
        if (town != null) {
            if (town.getMembers().contains(player.getUniqueId())) {
                colorHex = "#00FF00"; // Friendly
            } else if (town.isEnemy(player.getUniqueId())) {
                colorHex = "#FF0000"; // Enemy
            } else {
                colorHex = town.getColor() != null ? town.getColor() : defaultColor;
            }
        }
        
        return parseColor(colorHex);
    }

    private DustOptions parseColor(String hex) {
        try {
            java.awt.Color awtColor = java.awt.Color.decode(hex);
            return new DustOptions(
                Color.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue()),
                (float) particleSize
            );
        } catch (Exception e) {
            return new DustOptions(Color.RED, (float) particleSize);
        }
    }

    private void renderChunkEdges(Player player, Chunk chunk, DustOptions dust) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;

        // Render vertical edges
        for (int z = 0; z < 16; z += particleSpacing) {
            int y1 = world.getHighestBlockYAt(baseX, baseZ + z) + 1;
            int y2 = world.getHighestBlockYAt(baseX + 15, baseZ + z) + 1;
            
            player.spawnParticle(Particle.REDSTONE, baseX, y1, baseZ + z, 1, dust);
            player.spawnParticle(Particle.REDSTONE, baseX + 15, y2, baseZ + z, 1, dust);
        }

        // Render horizontal edges
        for (int x = 0; x < 16; x += particleSpacing) {
            int y1 = world.getHighestBlockYAt(baseX + x, baseZ) + 1;
            int y2 = world.getHighestBlockYAt(baseX + x, baseZ + 15) + 1;
            
            player.spawnParticle(Particle.REDSTONE, baseX + x, y1, baseZ, 1, dust);
            player.spawnParticle(Particle.REDSTONE, baseX + x, y2, baseZ + 15, 1, dust);
        }
    }

    public void stop() {
        animationTask.cancel();
        playersWithBordersOn.clear();
        lastVisibleChunks.clear();
    }
}