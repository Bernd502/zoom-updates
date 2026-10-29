package net.liteguard.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DurationParserTest {
    @Test void parsesSingleAndCompoundUnits() {
        assertEquals(30_000L, DurationParser.parseMillis("30s", 3650).orElseThrow());
        assertEquals(900_000L, DurationParser.parseMillis("15m", 3650).orElseThrow());
        assertEquals(43_200_000L, DurationParser.parseMillis("12h", 3650).orElseThrow());
        assertEquals(604_800_000L, DurationParser.parseMillis("7d", 3650).orElseThrow());
        assertEquals(788_400_000L, DurationParser.parseMillis("1w2d3h", 3650).orElseThrow());
    }

    @Test void rejectsMalformedZeroAndOverflowValues() {
        assertTrue(DurationParser.parseMillis("", 3650).isEmpty());
        assertTrue(DurationParser.parseMillis("1h-2m", 3650).isEmpty());
        assertTrue(DurationParser.parseMillis("1x", 3650).isEmpty());
        assertTrue(DurationParser.parseMillis("0s", 3650).isEmpty());
        assertTrue(DurationParser.parseMillis("999999999999999999999d", 3650).isEmpty());
        assertTrue(DurationParser.parseMillis("8d", 7).isEmpty());
        assertTrue(DurationParser.looksLikeInvalidDuration("15mx"));
        assertTrue(DurationParser.looksLikeInvalidDuration("1h-2m"));
        assertFalse(DurationParser.looksLikeInvalidDuration("1st"));
        assertFalse(DurationParser.looksLikeInvalidDuration("1984"));
    }
}
