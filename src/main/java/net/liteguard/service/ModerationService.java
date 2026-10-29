package net.liteguard.service;

import net.kyori.adventure.text.Component;
import net.liteguard.LiteGuardPlugin;
import net.liteguard.db.PlayerRecord;
import net.liteguard.model.Punishment;
import net.liteguard.model.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** Coordinates async target resolution, hierarchy checks, persistence and cache updates. */
public final class ModerationService {
    private final LiteGuardPlugin plugin;
    private final AtomicBoolean serverMuteChangePending = new AtomicBoolean();

    public ModerationService(LiteGuardPlugin plugin) { this.plugin = plugin; }

    public void issue(CommandSender sender, String targetName, PunishmentType type, String reason, Long expiresAt) {
        String staffName = sender.getName();
        UUID staffId = sender instanceof Player player ? player.getUniqueId() : null;
        String cleanReason = cleanReason(reason);
        resolve(targetName).thenCompose(target -> plugin.luckPerms().canPunish(staffId, target.uuid())
                .thenCompose(allowed -> {
                    if (!allowed) return CompletableFuture.failedFuture(new HierarchyDenied());
                    long now = System.currentTimeMillis();
                    var punishment = new Punishment(0, target.uuid(), target.name(), type, cleanReason, now, expiresAt, staffId, staffName);
                    return plugin.database().issue(punishment);
                })).whenComplete((punishment, error) -> {
                    if (error != null) { reportFailure(sender, error); return; }
                    plugin.cache().put(punishment);
                    plugin.discord().punishment(punishment, false);
                    runMain(() -> {
                        send(sender, label(type) + " applied to " + targetName + " · " + durationText(punishment));
                        if (type == PunishmentType.BAN) {
                            Player online = plugin.getServer().getPlayer(punishment.playerId());
                            if (online != null) online.kick(Component.text("You are banned from this server.\nReason: " + punishment.reason() + "\n" + durationText(punishment)));
                        }
                    });
                });
    }

    public void revoke(CommandSender sender, String targetName, PunishmentType type) {
        String staffName = sender.getName();
        UUID staffId = sender instanceof Player player ? player.getUniqueId() : null;
        resolve(targetName).thenCompose(target -> plugin.luckPerms().canPunish(staffId, target.uuid())
                .thenCompose(allowed -> {
                    if (!allowed) return CompletableFuture.failedFuture(new HierarchyDenied());
                    return plugin.database().revoke(target.uuid(), type, staffName).thenApply(changed -> new RevokeResult(target, changed));
                })).whenComplete((result, error) -> {
                    if (error != null) { reportFailure(sender, error); return; }
                    if (result.changed()) {
                        plugin.cache().remove(result.target().uuid(), type);
                        plugin.discord().revocation(type, result.target().uuid(), result.target().name(), staffName);
                    }
                    runMain(() -> send(sender, result.changed() ? label(type) + " removed for " + result.target().name() + "." : "No active " + label(type).toLowerCase(java.util.Locale.ROOT) + " found for " + result.target().name() + "."));
                });
    }

    public void toggleServerMute(CommandSender sender) {
        String staffName = sender.getName();
        if (!serverMuteChangePending.compareAndSet(false, true)) {
            send(sender, "A server mute change is already being saved.");
            return;
        }
        boolean desired = !plugin.cache().serverMuted();
        plugin.database().setServerMuted(desired).whenComplete((ignored, error) -> {
            serverMuteChangePending.set(false);
            if (error != null) { reportFailure(sender, error); return; }
            plugin.cache().setServerMuted(desired);
            plugin.discord().serverMute(desired, staffName);
            runMain(() -> send(sender, desired ? "Global text and voice chat lockdown enabled." : "Global text and voice chat lockdown disabled."));
        });
    }

    private CompletableFuture<PlayerRecord> resolve(String name) {
        // Online lookup is in-memory and the command executor invokes this from the server thread.
        Player online = plugin.getServer().getPlayerExact(name);
        if (online != null) {
            var record = new PlayerRecord(online.getUniqueId(), online.getName());
            return plugin.database().rememberPlayer(record.uuid(), record.name()).thenApply(ignored -> record);
        }
        var cachedOffline = plugin.getServer().getOfflinePlayerIfCached(name);
        if (cachedOffline != null) {
            String knownName = cachedOffline.getName() == null ? name : cachedOffline.getName();
            var record = new PlayerRecord(cachedOffline.getUniqueId(), knownName);
            return plugin.database().rememberPlayer(record.uuid(), record.name()).thenApply(ignored -> record);
        }
        return plugin.database().findPlayer(name).thenApply(optional -> optional.orElseThrow(() -> new UnknownPlayer(name)));
    }

    private String cleanReason(String input) {
        int max = Math.max(32, plugin.getConfig().getInt("limits.maximum-reason-length", 240));
        String value = input == null ? "No reason specified" : input.replaceAll("[\\p{Cntrl}]", " ").strip();
        if (value.isBlank()) value = "No reason specified";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private void reportFailure(CommandSender sender, Throwable error) {
        Throwable cause = root(error);
        if (cause instanceof UnknownPlayer unknown) {
            runMain(() -> send(sender, "Player not found in the LiteGuard player history: " + unknown.getMessage()));
        } else if (cause instanceof HierarchyDenied) {
            runMain(() -> send(sender, "You must have a strictly higher LuckPerms group weight than the target."));
        } else {
            plugin.getLogger().warning("Moderation action failed: " + cause.getClass().getSimpleName() + ": " + safeError(cause));
            runMain(() -> send(sender, "The moderation action could not be saved. Check the server console for the error type."));
        }
    }

    private static Throwable root(Throwable error) {
        Throwable current = error;
        while ((current instanceof java.util.concurrent.CompletionException || current instanceof java.util.concurrent.ExecutionException) && current.getCause() != null) current = current.getCause();
        return current;
    }

    private static String safeError(Throwable error) {
        String message = error.getMessage();
        if (message == null) return "no detail";
        return message.replaceAll("(?i)(jdbc:sqlite:)[^ ]+", "$1<path>").replaceAll("[\\r\\n]", " ");
    }

    private void runMain(Runnable action) {
        if (!plugin.isEnabled()) return;
        if (Bukkit.isPrimaryThread()) action.run();
        else try { plugin.getServer().getScheduler().runTask(plugin, action); }
        catch (org.bukkit.plugin.IllegalPluginAccessException ignored) { /* A queued command completed during shutdown. */ }
    }

    private static void send(CommandSender sender, String text) { sender.sendMessage(Component.text(text)); }
    private static String label(PunishmentType type) { return switch (type) { case BAN -> "Ban"; case TEXT_MUTE -> "Text mute"; case VOICE_MUTE -> "Voice mute"; }; }
    private static String durationText(Punishment value) { return value.expiresAtMillis() == null ? "Permanent" : "Expires " + value.expiryText(); }

    private record RevokeResult(PlayerRecord target, boolean changed) { }
    private static final class HierarchyDenied extends RuntimeException { private static final long serialVersionUID = 1L; }
    private static final class UnknownPlayer extends RuntimeException { private static final long serialVersionUID = 1L; private UnknownPlayer(String name) { super(name); } }
}
