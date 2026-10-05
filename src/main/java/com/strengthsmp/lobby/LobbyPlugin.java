package com.strengthsmp.lobby;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Standalone plugin, independent of the original StrengthSMP.jar. Talks to
 * the server only through standard Bukkit worlds/events/entities — never
 * touches that plugin's internal classes, which are not a public API.
 */
public final class LobbyPlugin extends JavaPlugin {

    private String lobbyWorldName;
    private Location spawnLocation;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lobbyWorldName = getConfig().getString("lobby-world", "world");

        double x = getConfig().getDouble("spawn.x", 0.5);
        double y = getConfig().getDouble("spawn.y", -1.0);
        double z = getConfig().getDouble("spawn.z", 0.5);
        float yaw = (float) getConfig().getDouble("spawn.yaw", 0.0);
        float pitch = (float) getConfig().getDouble("spawn.pitch", 0.0);

        World world = getServer().getWorld(lobbyWorldName);
        if (world == null) {
            getLogger().warning("Lobby world '" + lobbyWorldName + "' is not loaded yet. "
                    + "Spawn teleports will be skipped until it loads.");
        }
        spawnLocation = new Location(world, x, y, z, yaw, pitch);

        getServer().getPluginManager().registerEvents(
                new LobbySafetyListener(this, lobbyWorldName), this);

        getLogger().info("StrengthSMP-Lobby enabled. Lobby world='" + lobbyWorldName + "'");
    }

    @Override
    public void onDisable() {
        getLogger().info("StrengthSMP-Lobby disabled.");
    }

    public String lobbyWorldName() {
        return lobbyWorldName;
    }

    /** Resolves the spawn Location against the currently-loaded world each call, since the world may load after plugin enable. */
    public Location spawnLocation() {
        World world = getServer().getWorld(lobbyWorldName);
        if (world == null) {
            return null;
        }
        Location loc = spawnLocation.clone();
        loc.setWorld(world);
        return loc;
    }
}
