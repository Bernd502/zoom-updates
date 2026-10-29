package net.liteguard.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.liteguard.model.Punishment;
import net.liteguard.model.PunishmentType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** All filesystem and JDBC work runs on this bounded executor, never on Bukkit's main thread. */
public final class DatabaseService implements AutoCloseable {
    @FunctionalInterface private interface SqlTask<T> { T run(HikariDataSource source) throws Exception; }

    public record Snapshot(List<Punishment> active, boolean serverMuted) { }

    private final Path file;
    private final int poolSize;
    private final int connectionTimeoutMs;
    private final int busyTimeoutMs;
    private final ThreadPoolExecutor io;
    private volatile HikariDataSource source;
    private volatile boolean closed;

    public DatabaseService(Path file, int poolSize, int connectionTimeoutMs, int busyTimeoutMs) {
        this.file = file.toAbsolutePath().normalize();
        this.poolSize = Math.clamp(poolSize, 1, 4);
        this.connectionTimeoutMs = Math.max(1000, connectionTimeoutMs);
        this.busyTimeoutMs = Math.max(1000, busyTimeoutMs);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "LiteGuard-Database");
            thread.setDaemon(true);
            return thread;
        };
        io = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(2048), factory, new ThreadPoolExecutor.AbortPolicy());
    }

    public CompletableFuture<Void> initialize() {
        return enqueue(ignored -> {
            Files.createDirectories(file.getParent());
            HikariConfig config = new HikariConfig();
            config.setPoolName("LiteGuard-SQLite");
            config.setJdbcUrl("jdbc:sqlite:" + file + "?busy_timeout=" + busyTimeoutMs);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(connectionTimeoutMs);
            config.setValidationTimeout(Math.min(connectionTimeoutMs, 2000));
            config.setInitializationFailTimeout(connectionTimeoutMs);
            config.setConnectionInitSql("PRAGMA foreign_keys=ON");
            HikariDataSource initialized = new HikariDataSource(config);
            try (Connection connection = initialized.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA synchronous=NORMAL");
                statement.execute("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, name TEXT NOT NULL, name_key TEXT NOT NULL, last_seen INTEGER NOT NULL)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_players_name_key ON players(name_key, last_seen)");
                statement.execute("CREATE TABLE IF NOT EXISTS punishments (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT NOT NULL, player_name TEXT NOT NULL, type TEXT NOT NULL, reason TEXT NOT NULL, created_at INTEGER NOT NULL, expires_at INTEGER, staff_uuid TEXT, staff_name TEXT NOT NULL, revoked_at INTEGER, revoked_by TEXT)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_punishments_active_uuid_type ON punishments(uuid, type, revoked_at, expires_at)");
                statement.execute("CREATE TABLE IF NOT EXISTS server_settings (setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL)");
            } catch (Exception exception) {
                initialized.close();
                throw exception;
            }
            source = initialized;
            return null;
        });
    }

    public CompletableFuture<Snapshot> loadSnapshot() {
        return query(db -> {
            List<Punishment> values = new ArrayList<>();
            long now = System.currentTimeMillis();
            try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, uuid, player_name, type, reason, created_at, expires_at, staff_uuid, staff_name FROM punishments WHERE revoked_at IS NULL AND (expires_at IS NULL OR expires_at > ?) ORDER BY id DESC")) {
                statement.setLong(1, now);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) values.add(readPunishment(rows));
                }
            }
            boolean serverMuted = false;
            try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT setting_value FROM server_settings WHERE setting_key='server_muted'")) {
                try (ResultSet rows = statement.executeQuery()) { if (rows.next()) serverMuted = Boolean.parseBoolean(rows.getString(1)); }
            }
            return new Snapshot(List.copyOf(values), serverMuted);
        });
    }

    public CompletableFuture<Optional<PlayerRecord>> findPlayer(String suppliedName) {
        String key = suppliedName.toLowerCase(java.util.Locale.ROOT);
        return query(db -> {
            try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT uuid,name FROM players WHERE name_key=? ORDER BY last_seen DESC LIMIT 1")) {
                statement.setString(1, key);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(new PlayerRecord(UUID.fromString(rows.getString(1)), rows.getString(2))) : Optional.empty();
                }
            }
        });
    }

    public CompletableFuture<Void> rememberPlayer(UUID uuid, String name) {
        long now = System.currentTimeMillis();
        return query(db -> {
            try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO players(uuid,name,name_key,last_seen) VALUES(?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name,name_key=excluded.name_key,last_seen=excluded.last_seen")) {
                statement.setString(1, uuid.toString()); statement.setString(2, name);
                statement.setString(3, name.toLowerCase(java.util.Locale.ROOT)); statement.setLong(4, now);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Punishment> issue(Punishment value) {
        return query(db -> {
            String sql = "INSERT INTO punishments(uuid,player_name,type,reason,created_at,expires_at,staff_uuid,staff_name) VALUES(?,?,?,?,?,?,?,?)";
            try (Connection connection = db.getConnection()) {
                boolean previousAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    try (PreparedStatement revoke = connection.prepareStatement("UPDATE punishments SET revoked_at=?,revoked_by=? WHERE uuid=? AND type=? AND revoked_at IS NULL AND (expires_at IS NULL OR expires_at>?)")) {
                        revoke.setLong(1, value.createdAtMillis()); revoke.setString(2, value.staffName());
                        revoke.setString(3, value.playerId().toString()); revoke.setString(4, value.type().name()); revoke.setLong(5, value.createdAtMillis());
                        revoke.executeUpdate();
                    }
                    try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        statement.setString(1, value.playerId().toString()); statement.setString(2, value.playerName());
                        statement.setString(3, value.type().name()); statement.setString(4, value.reason());
                        statement.setLong(5, value.createdAtMillis());
                        if (value.expiresAtMillis() == null) statement.setNull(6, java.sql.Types.BIGINT); else statement.setLong(6, value.expiresAtMillis());
                        if (value.staffId() == null) statement.setNull(7, java.sql.Types.VARCHAR); else statement.setString(7, value.staffId().toString());
                        statement.setString(8, value.staffName()); statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("SQLite did not return the punishment ID.");
                            var saved = new Punishment(keys.getLong(1), value.playerId(), value.playerName(), value.type(), value.reason(), value.createdAtMillis(), value.expiresAtMillis(), value.staffId(), value.staffName());
                            connection.commit();
                            return saved;
                        }
                    }
                } catch (Exception error) {
                    connection.rollback();
                    throw error;
                } finally {
                    connection.setAutoCommit(previousAutoCommit);
                }
            }
        });
    }

    public CompletableFuture<Boolean> revoke(UUID uuid, PunishmentType type, String staffName) {
        return query(db -> {
            try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "UPDATE punishments SET revoked_at=?,revoked_by=? WHERE uuid=? AND type=? AND revoked_at IS NULL AND (expires_at IS NULL OR expires_at>?)")) {
                statement.setLong(1, System.currentTimeMillis()); statement.setString(2, staffName);
                statement.setString(3, uuid.toString()); statement.setString(4, type.name()); statement.setLong(5, System.currentTimeMillis());
                return statement.executeUpdate() > 0;
            }
        });
    }

    public CompletableFuture<Void> setServerMuted(boolean muted) {
        return query(db -> {
            try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO server_settings(setting_key,setting_value) VALUES('server_muted',?) ON CONFLICT(setting_key) DO UPDATE SET setting_value=excluded.setting_value")) {
                statement.setString(1, Boolean.toString(muted)); statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Integer> pruneExpired() {
        return query(db -> {
            try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM punishments WHERE expires_at IS NOT NULL AND expires_at<=?")) {
                statement.setLong(1, System.currentTimeMillis()); return statement.executeUpdate();
            }
        });
    }

    private static Punishment readPunishment(ResultSet rows) throws SQLException {
        String staff = rows.getString("staff_uuid");
        long expiration = rows.getLong("expires_at");
        Long expiry = rows.wasNull() ? null : expiration;
        return new Punishment(rows.getLong("id"), UUID.fromString(rows.getString("uuid")), rows.getString("player_name"),
                PunishmentType.valueOf(rows.getString("type")), rows.getString("reason"), rows.getLong("created_at"), expiry,
                staff == null ? null : UUID.fromString(staff), rows.getString("staff_name"));
    }

    private <T> CompletableFuture<T> query(SqlTask<T> task) {
        return enqueue(ignored -> {
            HikariDataSource current = source;
            if (current == null || current.isClosed()) throw new IllegalStateException("Database is not ready.");
            return task.run(current);
        });
    }

    private <T> CompletableFuture<T> enqueue(SqlTask<T> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Database is shutting down."));
        try {
            io.execute(() -> {
                try { result.complete(task.run(source)); }
                catch (Throwable error) { result.completeExceptionally(error); }
            });
        } catch (RuntimeException rejected) { result.completeExceptionally(rejected); }
        return result;
    }

    @Override public void close() {
        closed = true;
        try { io.execute(() -> { HikariDataSource current = source; if (current != null) current.close(); }); }
        catch (RuntimeException rejected) { Thread closer = new Thread(() -> { HikariDataSource current = source; if (current != null) current.close(); }, "LiteGuard-Database-Shutdown"); closer.setDaemon(true); closer.start(); }
        io.shutdown();
    }
}
