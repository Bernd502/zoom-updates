package net.liteguard.model;

import java.util.UUID;

public record Punishment(
        long id,
        UUID playerId,
        String playerName,
        PunishmentType type,
        String reason,
        long createdAtMillis,
        Long expiresAtMillis,
        UUID staffId,
        String staffName
) {
    public boolean expired(long nowMillis) {
        return expiresAtMillis != null && expiresAtMillis <= nowMillis;
    }

    public String expiryText() {
        return expiresAtMillis == null ? "Permanent" : java.time.Instant.ofEpochMilli(expiresAtMillis).toString();
    }
}
