package com.paxflags;

import org.dynmap.DynmapAPI;
import org.dynmap.markers.*;
import org.bukkit.*;
import java.util.*;

public class DynmapHook {
    private final PaxFlagsPlugin plugin;
    private DynmapAPI dynmapAPI;
    private MarkerAPI markerAPI;
    private MarkerSet markerSet;
    private final Map<String, AreaMarker> townMarkers = new HashMap<>();

    public DynmapHook(PaxFlagsPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (!plugin.getConfig().getBoolean("dynmap_integration", true)) return;

        try {
            dynmapAPI = (DynmapAPI) Bukkit.getPluginManager().getPlugin("dynmap");
            if (dynmapAPI == null) return;
            
            markerAPI = dynmapAPI.getMarkerAPI();
            if (markerAPI == null) return;
            
            markerSet = markerAPI.getMarkerSet("paxflags.towns");
            if (markerSet == null) {
                markerSet = markerAPI.createMarkerSet(
                    "paxflags.towns", 
                    "PaxFlags Towns", 
                    null, 
                    false
                );
            }
            markerSet.setLayerPriority(10);
        } catch (Exception e) {
            plugin.getLogger().warning("Dynmap setup failed: " + e.getMessage());
        }
    }

    public void updateTownMarker(String townName, int minX, int minZ, 
                                 int maxX, int maxZ, String worldName) {
        if (markerSet == null) return;

        String markerId = "town_" + townName.toLowerCase();
        AreaMarker marker = townMarkers.get(markerId);

        double[] xCoords = new double[]{minX * 16, (maxX + 1) * 16};
        double[] zCoords = new double[]{minZ * 16, (maxZ + 1) * 16};

        if (marker == null) {
            // Create new marker if none exists yet
            marker = markerSet.createAreaMarker(
                markerId, townName, false, worldName,
                xCoords,
                zCoords,
                false
            );
            townMarkers.put(markerId, marker);
        } else {
            // Fix: update existing marker position
            marker.setCornerLocations(xCoords, zCoords);
        }

        // Color parsing with fallback to red
        String colorHex = plugin.getConfig().getString("dynmap_default_color", "#FF0000");
        int color = 0xFF0000;
        if (colorHex != null && colorHex.startsWith("#") && colorHex.length() == 7) {
            try {
                color = Integer.parseInt(colorHex.substring(1), 16);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid dynmap_default_color format, using red.");
            }
        }

        marker.setFillStyle(0.3, color);
        marker.setLineStyle(2, 1.0, color);
        marker.setLabel(townName);
    }

    public void removeTownMarker(String townName) {
        AreaMarker marker = townMarkers.remove("town_" + townName.toLowerCase());
        if (marker != null) marker.deleteMarker();
    }

    public boolean isEnabled() {
        return markerSet != null;
    }
}
