package net.liteguard.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActionBarThrottleTest {
    @Test void repeatedPacketsDoNotMoveTheThrottleDeadline() {
        var throttle = new ActionBarThrottle();
        UUID id = UUID.randomUUID();
        assertTrue(throttle.tryAcquire(id, 1_000, 1_000));
        assertFalse(throttle.tryAcquire(id, 1_500, 1_000));
        assertFalse(throttle.tryAcquire(id, 1_999, 1_000));
        assertTrue(throttle.tryAcquire(id, 2_000, 1_000));
        throttle.clear(id);
        assertTrue(throttle.tryAcquire(id, 2_001, 1_000));
    }
}
