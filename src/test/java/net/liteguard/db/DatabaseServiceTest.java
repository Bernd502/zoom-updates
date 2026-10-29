package net.liteguard.db;

import net.liteguard.model.Punishment;
import net.liteguard.model.PunishmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseServiceTest {
    @TempDir Path temp;

    @Test void persistsPlayerPunishmentsAndLockdownOffTheCallingThread() {
        var database = new DatabaseService(temp.resolve("liteguard.db"), 1, 5000, 5000);
        try {
            String initializationThread = database.initialize().thenApply(ignored -> Thread.currentThread().getName()).join();
            assertTrue(initializationThread.startsWith("LiteGuard-Database"));

            UUID uuid = UUID.randomUUID();
            database.rememberPlayer(uuid, "TestPlayer").join();
            assertEquals(uuid, database.findPlayer("testplayer").join().orElseThrow().uuid());

            long now = System.currentTimeMillis();
            var input = new Punishment(0, uuid, "TestPlayer", PunishmentType.VOICE_MUTE, "reason", now, null, null, "Console");
            var saved = database.issue(input).join();
            assertTrue(saved.id() > 0);
            database.setServerMuted(true).join();
            var snapshot = database.loadSnapshot().join();
            assertTrue(snapshot.serverMuted());
            assertEquals(saved, snapshot.active().getFirst());

            assertTrue(database.revoke(uuid, PunishmentType.VOICE_MUTE, "Console").join());
            assertTrue(database.loadSnapshot().join().active().isEmpty());

            var permanent = new Punishment(0, uuid, "TestPlayer", PunishmentType.BAN, "first", now, null, null, "Console");
            database.issue(permanent).join();
            var alreadyExpiredReplacement = new Punishment(0, uuid, "TestPlayer", PunishmentType.BAN, "second", now + 1, now - 1, null, "Console");
            database.issue(alreadyExpiredReplacement).join();
            assertTrue(database.loadSnapshot().join().active().isEmpty(), "replacing a punishment must not expose a prior ban again after expiry");
        } finally {
            database.close();
        }
    }
}
