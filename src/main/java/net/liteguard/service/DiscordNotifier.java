package net.liteguard.service;

import net.liteguard.LiteGuardPlugin;
import net.liteguard.model.Punishment;
import net.liteguard.model.PunishmentType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Small asynchronous webhook sender. It never logs the configured URL or response body. */
public final class DiscordNotifier implements AutoCloseable {
    private final LiteGuardPlugin plugin;
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(128), runnable -> {
                Thread thread = new Thread(runnable, "LiteGuard-Webhook"); thread.setDaemon(true); return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private final HttpClient client = HttpClient.newBuilder().executor(executor).connectTimeout(Duration.ofSeconds(5)).build();
    private final AtomicBoolean closed = new AtomicBoolean();

    public DiscordNotifier(LiteGuardPlugin plugin) { this.plugin = plugin; }

    public void punishment(Punishment punishment, boolean revoked) {
        String title = (revoked ? "Revoked " : "Issued ") + label(punishment.type());
        String body = "**Player:** " + punishment.playerName() + " (`" + punishment.playerId() + "`)\n"
                + "**Reason:** " + (punishment.reason().isBlank() ? "No reason specified" : punishment.reason()) + "\n"
                + "**Staff:** " + punishment.staffName() + "\n"
                + "**Duration:** " + (punishment.expiresAtMillis() == null ? "Permanent" : punishment.expiryText());
        send(title, body, color(punishment.type()));
    }

    public void revocation(PunishmentType type, java.util.UUID uuid, String playerName, String staffName) {
        send("Revoked " + label(type), "**Player:** " + playerName + " (`" + uuid + "`)\n**Staff:** " + staffName, 0x51C878);
    }

    public void serverMute(boolean enabled, String staffName) {
        send(enabled ? "Global chat lockdown enabled" : "Global chat lockdown disabled", "**Staff:** " + staffName, enabled ? 0xE6A23C : 0x51C878);
    }

    private void send(String title, String description, int color) {
        if (closed.get()) return;
        String configured = plugin.getConfig().getString("discord.webhook-url", "").strip();
        if (configured.isEmpty()) return;
        final URI uri;
        try {
            uri = URI.create(configured);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || !(host.equalsIgnoreCase("discord.com") || host.equalsIgnoreCase("discordapp.com"))
                    || uri.getUserInfo() != null || uri.getPort() != -1 && uri.getPort() != 443
                    || uri.getPath() == null || !uri.getPath().startsWith("/api/webhooks/")) {
                plugin.getLogger().warning("Discord webhook was skipped: configure an HTTPS Discord webhook endpoint.");
                return;
            }
        } catch (IllegalArgumentException invalid) {
            plugin.getLogger().warning("Discord webhook was skipped: the configured URL is invalid.");
            return;
        }
        int timeout = Math.clamp(plugin.getConfig().getInt("discord.request-timeout-seconds", 5), 2, 15);
        String json = "{\"allowed_mentions\":{\"parse\":[]},\"embeds\":[{\"title\":\"" + json(title) + "\",\"description\":\"" + json(description)
                + "\",\"color\":" + color + ",\"timestamp\":\"" + Instant.now() + "\"}]}";
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("User-Agent", "LiteGuard/1.0.0 (Paper moderation plugin)")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        try {
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).whenComplete((response, error) -> {
                if (error != null && !closed.get()) plugin.getLogger().warning("Discord webhook delivery failed: " + error.getClass().getSimpleName());
                else if (response != null && (response.statusCode() < 200 || response.statusCode() >= 300) && !closed.get()) plugin.getLogger().warning("Discord webhook returned HTTP " + response.statusCode() + ".");
            });
        } catch (java.util.concurrent.RejectedExecutionException saturated) {
            if (!closed.get()) plugin.getLogger().warning("Discord webhook queue is full; skipping this notification.");
        }
    }

    private static String json(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (c < 0x20) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
            }
        }
        return out.toString();
    }

    private static String label(PunishmentType type) { return switch (type) { case BAN -> "Ban"; case TEXT_MUTE -> "Text mute"; case VOICE_MUTE -> "Voice mute"; }; }
    private static int color(PunishmentType type) { return switch (type) { case BAN -> 0xD84A4A; case TEXT_MUTE -> 0xE6A23C; case VOICE_MUTE -> 0x8E62D9; }; }

    @Override public void close() { closed.set(true); executor.shutdown(); }
}
