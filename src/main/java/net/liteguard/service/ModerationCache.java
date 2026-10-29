package net.liteguard.service;

import net.liteguard.db.DatabaseService;
import net.liteguard.model.Punishment;
import net.liteguard.model.PunishmentType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** UUID-keyed hot-path state. Chat and voice listeners never touch JDBC. */
public final class ModerationCache {
    private final ConcurrentHashMap<UUID, Boolean> serverMuteBypass = new ConcurrentHashMap<>();
    private final AtomicLong generation = new AtomicLong();
    private volatile CacheState state = new CacheState(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), false);
    private volatile boolean ready;

    private record CacheState(ConcurrentHashMap<UUID, Punishment> bans,
                              ConcurrentHashMap<UUID, Punishment> textMutes,
                              ConcurrentHashMap<UUID, Punishment> voiceMutes,
                              boolean serverMuted) { }

    public boolean ready() { return ready; }
    public long generation() { return generation.get(); }
    public boolean serverMuted() { return state.serverMuted(); }
    public boolean hasServerMuteBypass(UUID uuid) { return serverMuteBypass.getOrDefault(uuid, false); }
    public void setServerMuteBypass(UUID uuid, boolean value) { serverMuteBypass.put(uuid, value); }
    public void clearServerMuteBypass(UUID uuid) { serverMuteBypass.remove(uuid); }

    public void replace(DatabaseService.Snapshot snapshot) { replace(snapshot, generation()); }

    public synchronized boolean replace(DatabaseService.Snapshot snapshot, long expectedGeneration) {
        if (generation.get() != expectedGeneration) return false;
        var next = new CacheState(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), snapshot.serverMuted());
        for (Punishment punishment : snapshot.active()) putInternal(next, punishment);
        state = next;
        ready = true;
        generation.incrementAndGet();
        return true;
    }

    public synchronized void put(Punishment punishment) {
        generation.incrementAndGet();
        putInternal(state, punishment);
    }

    private static void putInternal(CacheState state, Punishment punishment) {
        map(state, punishment.type()).merge(punishment.playerId(), punishment,
                (current, incoming) -> current.createdAtMillis() > incoming.createdAtMillis()
                        || current.createdAtMillis() == incoming.createdAtMillis() && current.id() > incoming.id() ? current : incoming);
    }

    public synchronized void remove(UUID uuid, PunishmentType type) { generation.incrementAndGet(); map(state, type).remove(uuid); }

    public Punishment active(UUID uuid, PunishmentType type) {
        CacheState current = state;
        Punishment value = map(current, type).get(uuid);
        if (value != null && value.expired(System.currentTimeMillis())) {
            map(current, type).remove(uuid, value);
            return null;
        }
        return value;
    }

    public synchronized void setServerMuted(boolean value) {
        CacheState current = state;
        if (current.serverMuted() != value) {
            generation.incrementAndGet();
            state = new CacheState(current.bans(), current.textMutes(), current.voiceMutes(), value);
        }
    }

    public int size(PunishmentType type) { return map(state, type).size(); }

    private static ConcurrentHashMap<UUID, Punishment> map(CacheState state, PunishmentType type) {
        return switch (type) {
            case BAN -> state.bans();
            case TEXT_MUTE -> state.textMutes();
            case VOICE_MUTE -> state.voiceMutes();
        };
    }
}
