package net.liteguard.service;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses compact compound values such as 30s, 12h, 7d, or 1w2d3h. */
public final class DurationParser {
    private static final Pattern PART = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);
    private static final Pattern DURATION_INTENT = Pattern.compile("(?i)^[-+]?\\d+[smhdw].*$");
    private static final Pattern ORDINAL = Pattern.compile("(?i)^\\d+(st|nd|rd|th)$");
    private DurationParser() { }

    public static boolean looksLikeInvalidDuration(String value) {
        return value != null && DURATION_INTENT.matcher(value).matches() && !ORDINAL.matcher(value).matches();
    }

    public static OptionalLong parseMillis(String value, long maximumDays) {
        if (value == null || value.isBlank() || maximumDays < 1) return OptionalLong.empty();
        String input = value.strip().toLowerCase(java.util.Locale.ROOT);
        Matcher matcher = PART.matcher(input);
        int offset = 0;
        long total = 0;
        try {
            while (matcher.find()) {
                if (matcher.start() != offset) return OptionalLong.empty();
                long amount = Long.parseLong(matcher.group(1));
                long unit = switch (matcher.group(2)) {
                    case "s" -> 1_000L;
                    case "m" -> 60_000L;
                    case "h" -> 3_600_000L;
                    case "d" -> 86_400_000L;
                    case "w" -> 604_800_000L;
                    default -> throw new IllegalStateException();
                };
                if (amount <= 0) return OptionalLong.empty();
                total = Math.addExact(total, Math.multiplyExact(amount, unit));
                offset = matcher.end();
            }
            if (offset != input.length() || offset == 0 || total > Math.multiplyExact(maximumDays, 86_400_000L)) return OptionalLong.empty();
            return OptionalLong.of(total);
        } catch (ArithmeticException | NumberFormatException overflow) {
            return OptionalLong.empty();
        }
    }
}
