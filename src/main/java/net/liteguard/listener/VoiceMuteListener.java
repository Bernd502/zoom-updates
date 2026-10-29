package net.liteguard.listener;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.liteguard.LiteGuardPlugin;
import net.liteguard.model.PunishmentType;

public final class VoiceMuteListener {
    private VoiceMuteListener() { }

    public static void onMicrophonePacket(MicrophonePacketEvent event) {
        LiteGuardPlugin plugin = LiteGuardPlugin.instance();
        if (plugin == null || !plugin.isEnabled()) return;
        VoicechatConnection connection = event.getSenderConnection();
        if (connection == null || connection.getPlayer() == null) return;
        var uuid = connection.getPlayer().getUuid();
        if (!plugin.cache().ready()) {
            if (event.isCancellable()) event.cancel();
            plugin.throttledActionBar(uuid, "Moderation checks are starting; voice chat is paused.");
            return;
        }
        var mute = plugin.cache().active(uuid, PunishmentType.VOICE_MUTE);
        boolean lockdown = plugin.cache().serverMuted() && !plugin.cache().hasServerMuteBypass(uuid);
        if (mute == null && !lockdown) return;
        if (event.isCancellable()) event.cancel();
        plugin.throttledActionBar(uuid, mute == null ? "Voice chat is temporarily locked down." : "Voice chat is muted: " + mute.reason());
    }
}
