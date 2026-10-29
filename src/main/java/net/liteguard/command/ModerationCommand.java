package net.liteguard.command;

import net.liteguard.LiteGuardPlugin;
import net.liteguard.model.PunishmentType;
import net.liteguard.service.DurationParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class ModerationCommand implements CommandExecutor, TabCompleter {
    private final LiteGuardPlugin plugin;

    public ModerationCommand(LiteGuardPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("servermute")) {
            if (args.length != 0) { sender.sendMessage("Usage: /servermute"); return true; }
            plugin.moderation().toggleServerMute(sender);
            return true;
        }
        if (args.length == 0) { sender.sendMessage(command.getUsage()); return true; }

        switch (name) {
            case "ban" -> plugin.moderation().issue(sender, args[0], PunishmentType.BAN, join(args, 1), null);
            case "tempban" -> {
                if (args.length < 2) { sender.sendMessage("Usage: /tempban <player> <duration> [reason]"); return true; }
                Long expiry = expiration(args[1], sender);
                if (expiry == null) return true;
                plugin.moderation().issue(sender, args[0], PunishmentType.BAN, join(args, 2), expiry);
            }
            case "mute", "vcmute" -> {
                Long expiry = null;
                int reasonStart = 1;
                if (args.length > 1 && DurationParser.parseMillis(args[1], maxDays()).isPresent()) {
                    long millis = DurationParser.parseMillis(args[1], maxDays()).getAsLong();
                    expiry = safeExpiry(millis, sender);
                    if (expiry == null) return true;
                    reasonStart = 2;
                } else if (args.length > 1 && DurationParser.looksLikeInvalidDuration(args[1])) {
                    sender.sendMessage("Invalid duration. Use values such as 30s, 15m, 12h, 7d or 1w2d.");
                    return true;
                }
                plugin.moderation().issue(sender, args[0], name.equals("mute") ? PunishmentType.TEXT_MUTE : PunishmentType.VOICE_MUTE, join(args, reasonStart), expiry);
            }
            case "unban" -> plugin.moderation().revoke(sender, args[0], PunishmentType.BAN);
            case "unmute" -> plugin.moderation().revoke(sender, args[0], PunishmentType.TEXT_MUTE);
            case "vcunmute" -> plugin.moderation().revoke(sender, args[0], PunishmentType.VOICE_MUTE);
            default -> sender.sendMessage(command.getUsage());
        }
        return true;
    }

    private Long expiration(String input, CommandSender sender) {
        var millis = DurationParser.parseMillis(input, maxDays());
        if (millis.isEmpty()) {
            sender.sendMessage("Invalid duration. Use values such as 30s, 15m, 12h, 7d or 1w2d (maximum " + maxDays() + " days).");
            return null;
        }
        return safeExpiry(millis.getAsLong(), sender);
    }

    private Long safeExpiry(long millis, CommandSender sender) {
        try { return Math.addExact(System.currentTimeMillis(), millis); }
        catch (ArithmeticException overflow) { sender.sendMessage("That duration is too long."); return null; }
    }

    private long maxDays() { return Math.clamp(plugin.getConfig().getLong("limits.maximum-duration-days", 3650), 1, 36_500); }
    private static String join(String[] args, int from) { return from >= args.length ? "No reason specified" : String.join(" ", java.util.Arrays.copyOfRange(args, from, args.length)); }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && !command.getName().equalsIgnoreCase("servermute")) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return plugin.getServer().getOnlinePlayers().stream().map(p -> p.getName()).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
        }
        if (args.length == 2 && (command.getName().equalsIgnoreCase("tempban") || command.getName().equalsIgnoreCase("mute") || command.getName().equalsIgnoreCase("vcmute"))) {
            return List.of("30s", "15m", "12h", "7d", "1w2d").stream().filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
