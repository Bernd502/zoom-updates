package net.liteguard.service;

import net.liteguard.db.DatabaseService;
import net.liteguard.model.Punishment;
import net.liteguard.model.PunishmentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModerationCacheTest {
    @Test void hotLookupsAreUuidKeyedAndExpireWithoutDatabaseWork() {
        var cache = new ModerationCache();
        UUID id = UUID.randomUUID();
        long now = System.currentTimeMillis();
        var live = new Punishment(1, id, "Player", PunishmentType.TEXT_MUTE, "chat", now, now + 60_000, null, "Console");
        var expired = new Punishment(2, id, "Player", PunishmentType.VOICE_MUTE, "voice", now - 60_000, now - 1, null, "Console");
        cache.replace(new DatabaseService.Snapshot(List.of(live, expired), true));

        assertTrue(cache.ready());
        assertEquals(live, cache.active(id, PunishmentType.TEXT_MUTE));
        assertNull(cache.active(id, PunishmentType.VOICE_MUTE));
        assertEquals(0, cache.size(PunishmentType.VOICE_MUTE));
        assertTrue(cache.serverMuted());
    }

    @Test void newestActivePunishmentWinsAndTargetedRevocationIsConstantTime() {
        var cache = new ModerationCache();
        UUID id = UUID.randomUUID();
        long now = System.currentTimeMillis();
        var older = new Punishment(1, id, "Player", PunishmentType.TEXT_MUTE, "old", now, null, null, "Console");
        var newer = new Punishment(2, id, "Player", PunishmentType.TEXT_MUTE, "new", now + 1, null, null, "Console");
        cache.replace(new DatabaseService.Snapshot(List.of(newer, older), false));
        assertEquals("new", cache.active(id, PunishmentType.TEXT_MUTE).reason());
        cache.remove(id, PunishmentType.TEXT_MUTE);
        assertNull(cache.active(id, PunishmentType.TEXT_MUTE));
    }

    @Test void staleRefreshCannotOverwriteAConcurrentModerationAction() {
        var cache = new ModerationCache();
        cache.replace(new DatabaseService.Snapshot(List.of(), false));
        long refreshStartedAt = cache.generation();
        UUID id = UUID.randomUUID();
        long now = System.currentTimeMillis();
        var mute = new Punishment(1, id, "Player", PunishmentType.TEXT_MUTE, "reason", now, null, null, "Console");
        cache.put(mute);

        assertFalse(cache.replace(new DatabaseService.Snapshot(List.of(), false), refreshStartedAt));
        assertEquals(mute, cache.active(id, PunishmentType.TEXT_MUTE));
    }
}
