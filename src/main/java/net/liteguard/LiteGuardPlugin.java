package net.liteguard;

import net.liteguard.command.ModerationCommand;
import net.liteguard.db.DatabaseService;
import net.liteguard.listener.ChatListener;
import net.liteguard.listener.LoginListener;
import net.liteguard.listener.PlayerLifecycleListener;
import net.liteguard.service.DiscordNotifier;
import net.liteguard.service.ActionBarThrottle;
import net.liteguard.service.LuckPermsHook;
import net.liteguard.service.ModerationCache;
import net.liteguard.service.ModerationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class LiteGuardPlugin extends JavaPlugin {
    private static volatile LiteGuardPlugin instance;
    private DatabaseService database;
    private final ModerationCache cache = new ModerationCache();
    private CompletableFuture<Void> ready = new CompletableFuture<>();
    private LuckPermsHook luckPerms;
    private DiscordNotifier discord;
    private ModerationService moderation;
    private final ActionBarThrottle actionBarThrottle = new ActionBarThrottle();
    private long actionBarThrottleNanos = TimeUnit.MILLISECONDS.toNanos(1200);

    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        luckPerms = new LuckPermsHook(this);
        discord = new DiscordNotifier(this);
        moderation = new ModerationService(this);
        actionBarThrottleNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(250, getConfig().getLong("voice.actionbar-throttle-ms", 1200)));

        Path file = getDataFolder().toPath().resolve(getConfig().getString("database.file", "liteguard.db"));
        database = new DatabaseService(file,
                getConfig().getInt("database.pool-size", 2),
                getConfig().getInt("database.connection-timeout-ms", 5000),
                getConfig().getInt("database.busy-timeout-ms", 5000));
        ready = database.initialize().thenCompose(ignored -> database.loadSnapshot())
                .thenAccept(cache::replace)
                .whenComplete((ignored, error) -> {
                    if (error != null) getLogger().severe("Database initialization failed; logins are temporarily denied to prevent ban-check bypasses: " + rootMessage(error));
                    else getLogger().info("Moderation database ready; loaded " + cache.size(net.liteguard.model.PunishmentType.BAN) + " active bans, " + cache.size(net.liteguard.model.PunishmentType.TEXT_MUTE) + " text mutes and " + cache.size(net.liteguard.model.PunishmentType.VOICE_MUTE) + " voice mutes.");
                });

        getServer().getPluginManager().registerEvents(new LoginListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this), this);
        var executor = new ModerationCommand(this);
        for (String name : java.util.List.of("ban", "tempban", "unban", "mute", "unmute", "vcmute", "vcunmute", "servermute")) {
            var command = getCommand(name);
            if (command == null) { getLogger().severe("Command /" + name + " is missing from plugin.yml"); continue; }
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        long refreshTicks = Math.max(20L, getConfig().getLong("cache.refresh-seconds", 30) * 20L);
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::refreshCaches, refreshTicks, refreshTicks);
        getServer().getScheduler().runTaskTimer(this, this::refreshBypassPermissions, 20L, 20L);
        if (!getServer().getPluginManager().isPluginEnabled("voicechat"))
            getLogger().info("Simple Voice Chat is not installed; voice punishment records will be saved but microphone packets cannot be intercepted.");
        getLogger().info("LiteGuard enabled. SQLite work runs asynchronously; voice chat integration is optional.");
    }

    private void refreshCaches() {
        if (!cache.ready() || database == null) return;
        long generation = cache.generation();
        database.loadSnapshot().thenAccept(snapshot -> cache.replace(snapshot, generation)).exceptionally(error -> { getLogger().warning("Moderation cache refresh failed: " + rootMessage(error)); return null; });
        database.pruneExpired().exceptionally(error -> { getLogger().warning("Expired-punishment cleanup failed: " + rootMessage(error)); return 0; });
    }

    private void refreshBypassPermissions() {
        for (Player player : getServer().getOnlinePlayers()) cache.setServerMuteBypass(player.getUniqueId(), player.hasPermission("liteguard.bypass.servermute"));
    }

    @Override public void onDisable() {
        instance = null;
        if (database != null) database.close();
        if (discord != null) discord.close();
    }

    public static LiteGuardPlugin instance() { return instance; }
    public DatabaseService database() { return database; }
    public ModerationCache cache() { return cache; }
    public LuckPermsHook luckPerms() { return luckPerms; }
    public DiscordNotifier discord() { return discord; }
    public ModerationService moderation() { return moderation; }

    public boolean awaitReady() {
        try { ready.get(Math.max(1000, getConfig().getLong("prelogin.cache-ready-timeout-ms", 8000)), TimeUnit.MILLISECONDS); return cache.ready(); }
        catch (Exception error) { return false; }
    }

    public void throttledActionBar(UUID uuid, String text) {
        if (!isEnabled()) return;
        long now = System.nanoTime();
        if (!actionBarThrottle.tryAcquire(uuid, now, actionBarThrottleNanos)) return;
        try {
            getServer().getScheduler().runTask(this, () -> {
                Player player = getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) player.sendActionBar(net.kyori.adventure.text.Component.text(text));
            });
        } catch (IllegalPluginAccessException ignored) { /* Shutdown raced with voice packet feedback. */ }
    }

    public void forgetActionBar(UUID uuid) { actionBarThrottle.clear(uuid); }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
