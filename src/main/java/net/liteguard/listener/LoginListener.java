package net.liteguard.listener;

import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import net.kyori.adventure.text.Component;
import net.liteguard.LiteGuardPlugin;
import net.liteguard.model.PunishmentType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class LoginListener implements Listener {
    private final LiteGuardPlugin plugin;
    public LoginListener(LiteGuardPlugin plugin) { this.plugin = plugin; }

    @EventHandler public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.awaitReady()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text(plugin.getConfig().getString("prelogin.database-error-message", "Moderation data is temporarily unavailable. Please try again shortly.")));
            return;
        }
        var ban = plugin.cache().active(event.getUniqueId(), PunishmentType.BAN);
        if (ban != null) {
            String expiry = ban.expiresAtMillis() == null ? "Permanent" : "Expires " + ban.expiryText();
            String reason = ban.reason().isBlank() ? "No reason specified" : ban.reason();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    Component.text("You are banned from this server.\nReason: " + reason + "\n" + expiry));
            return;
        }
        plugin.database().rememberPlayer(event.getUniqueId(), event.getName()).exceptionally(error -> {
            plugin.getLogger().warning("Could not update the player registry during pre-login: " + error.getClass().getSimpleName());
            return null;
        });
    }
}
