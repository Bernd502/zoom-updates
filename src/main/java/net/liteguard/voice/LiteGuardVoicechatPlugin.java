package net.liteguard.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.liteguard.listener.VoiceMuteListener;

/** Discovered by Simple Voice Chat using its documented Java service-provider entry. */
public final class LiteGuardVoicechatPlugin implements VoicechatPlugin {
    @Override public String getPluginId() { return "liteguard"; }

    @Override public void registerEvents(EventRegistration registration) {
        // Registration may be requested before Bukkit calls LiteGuard's onEnable;
        // the static listener resolves the plugin instance at packet time.
        registration.registerEvent(MicrophonePacketEvent.class, VoiceMuteListener::onMicrophonePacket);
    }

    @Override public void initialize(VoicechatApi api) {
        if (!(api instanceof VoicechatServerApi)) throw new IllegalStateException("LiteGuard requires the Simple Voice Chat server API.");
    }
}
