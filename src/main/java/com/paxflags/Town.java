package com.paxflags;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Town {
    private final String name;
    private UUID owner;
    private Set<UUID> members = new HashSet<>();
    private Set<UUID> allies = new HashSet<>(); // New: For relationship tracking
    private Set<String> claimedChunks = new HashSet<>();
    private String borderColor; // New: For custom border colors

    public Town(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void setMembers(Set<UUID> members) {
        this.members = members != null ? members : new HashSet<>();
    }

    public Set<UUID> getAllies() {
        return allies;
    }

    public boolean addAlly(UUID playerId) {
        return allies.add(playerId);
    }

    public boolean removeAlly(UUID playerId) {
        return allies.remove(playerId);
    }

    public Set<String> getClaimedChunks() {
        return claimedChunks;
    }

    public void setClaimedChunks(Set<String> claimedChunks) {
        this.claimedChunks = claimedChunks != null ? claimedChunks : new HashSet<>();
    }

    public void claimChunk(String chunkKey) {
        claimedChunks.add(chunkKey);
    }

    public boolean unclaimChunk(String chunkKey) {
        return claimedChunks.remove(chunkKey);
    }

    public boolean isChunkClaimed(String chunkKey) {
        return claimedChunks.contains(chunkKey);
    }

    // New methods for border system
    public String getBorderColor() {
        return borderColor != null ? borderColor : "#FF0000"; // Default red
    }

    public void setBorderColor(String hexColor) {
        // Basic validation
        if (hexColor != null && hexColor.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
            this.borderColor = hexColor;
        }
    }

    public boolean isEnemy(UUID playerId) {
        return !owner.equals(playerId) &&
               !members.contains(playerId) &&
               !allies.contains(playerId);
    }
}
