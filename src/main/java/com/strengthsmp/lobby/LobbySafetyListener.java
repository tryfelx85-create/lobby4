package com.strengthsmp.lobby;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Lobby safe-zone rules, per the design doc section 2.2:
 * - every join (first-time or returning) lands in the lobby, always
 * - force Adventure mode while in the lobby world
 * - cancel ALL damage sources while in the lobby (full safe zone, PvE included)
 * - falling into the void teleports back to lobby spawn instead of just
 *   cancelling (a bottomless fall looks broken even if harmless)
 *
 * This listener only reads world names and standard Bukkit events — it has
 * no dependency on, and no awareness of, the original StrengthSMP.jar.
 */
public final class LobbySafetyListener implements Listener {

    private final LobbyPlugin plugin;
    private final String lobbyWorldName;

    public LobbySafetyListener(LobbyPlugin plugin, String lobbyWorldName) {
        this.plugin = plugin;
        this.lobbyWorldName = lobbyWorldName;
    }

    private boolean isLobbyWorld(World world) {
        return world != null && world.getName().equals(lobbyWorldName);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        teleportToLobby(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // If their respawn world would be the lobby anyway, let vanilla handle it;
        // otherwise this is not our concern (SMP respawn rules live in Core/SMP module).
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        applyAdventureModeIfInLobby(event.getPlayer());
    }

    private void teleportToLobby(Player player) {
        Location spawn = plugin.spawnLocation();
        if (spawn == null) {
            plugin.getLogger().warning("Cannot teleport " + player.getName()
                    + " to lobby spawn: lobby world not loaded.");
            return;
        }
        player.teleport(spawn);
        applyAdventureModeIfInLobby(player);
    }

    private void applyAdventureModeIfInLobby(Player player) {
        if (isLobbyWorld(player.getWorld())) {
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    /**
     * Full safe zone: every damage source is cancelled while the victim is in
     * the lobby world, PvE included. VOID damage additionally teleports the
     * player back to spawn instead of just no-op cancelling, since endlessly
     * falling looks broken even though it's harmless.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isLobbyWorld(player.getWorld())) {
            return;
        }

        event.setCancelled(true);

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            Location spawn = plugin.spawnLocation();
            if (spawn != null) {
                player.teleport(spawn);
            }
            player.setFireTicks(0);
            player.setFallDistance(0f);
        }
    }
}
