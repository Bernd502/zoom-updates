package net.liteguard.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.liteguard.LiteGuardPlugin;
import net.liteguard.model.Punishment;
import net.liteguard.model.PunishmentType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ChatListener implements Listener {
    private final LiteGuardPlugin plugin;
    public ChatListener(LiteGuardPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        var player = event.getPlayer();
        if (!plugin.cache().ready()) {
            event.setCancelled(true);
            plugin.throttledActionBar(player.getUniqueId(), "Moderation checks are starting; try again shortly.");
            return;
        }
        Punishment mute = plugin.cache().active(player.getUniqueId(), PunishmentType.TEXT_MUTE);
        if (mute != null) {
            event.setCancelled(true);
            plugin.throttledActionBar(player.getUniqueId(), "You are muted: " + mute.reason());
            return;
        }
        if (plugin.cache().serverMuted() && !plugin.cache().hasServerMuteBypass(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.throttledActionBar(player.getUniqueId(), "Text chat is temporarily locked down.");
        }
    }
}
