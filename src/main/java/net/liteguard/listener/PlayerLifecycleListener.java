package net.liteguard.listener;

import net.liteguard.LiteGuardPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerLifecycleListener implements Listener {
    private final LiteGuardPlugin plugin;
    public PlayerLifecycleListener(LiteGuardPlugin plugin) { this.plugin = plugin; }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        plugin.cache().setServerMuteBypass(player.getUniqueId(), player.hasPermission("liteguard.bypass.servermute"));
        plugin.database().rememberPlayer(player.getUniqueId(), player.getName()).exceptionally(error -> {
            plugin.getLogger().warning("Could not remember a joining player: " + error.getClass().getSimpleName());
            return null;
        });
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        plugin.cache().clearServerMuteBypass(event.getPlayer().getUniqueId());
        plugin.forgetActionBar(event.getPlayer().getUniqueId());
    }
}
