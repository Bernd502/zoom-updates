package net.liteguard.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Monotonic UUID throttle without packet-rate allocation after first use. */
public final class ActionBarThrottle {
    private final ConcurrentHashMap<UUID, AtomicLong> lastNotice = new ConcurrentHashMap<>();

    public boolean tryAcquire(UUID uuid, long nowNanos, long intervalNanos) {
        AtomicLong last = lastNotice.computeIfAbsent(uuid, ignored -> new AtomicLong());
        while (true) {
            long previous = last.get();
            if (previous != 0 && nowNanos - previous < intervalNanos) return false;
            if (last.compareAndSet(previous, nowNanos)) return true;
        }
    }

    public void clear(UUID uuid) { lastNotice.remove(uuid); }
}
