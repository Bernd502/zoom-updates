package net.liteguard.service;

import net.liteguard.LiteGuardPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Rank hierarchy check using LuckPerms' effective inherited group weights. */
public final class LuckPermsHook {
    private final LuckPerms luckPerms;

    public LuckPermsHook(LiteGuardPlugin plugin) {
        LuckPerms found = null;
        if (plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            try { found = LuckPermsProvider.get(); }
            catch (IllegalStateException ignored) { plugin.getLogger().warning("LuckPerms is enabled but its API provider is not ready."); }
        }
        luckPerms = found;
        if (found == null) plugin.getLogger().warning("LuckPerms was not found. Player-issued punishments will fail closed because staff/target rank weights cannot be checked.");
    }

    public boolean available() { return luckPerms != null; }

    /** A null actor UUID means trusted console; player staff must strictly outrank the target. */
    public CompletableFuture<Boolean> canPunish(UUID staffId, UUID targetId) {
        if (staffId == null) return CompletableFuture.completedFuture(true);
        if (luckPerms == null) return CompletableFuture.completedFuture(false);
        User staffUser = luckPerms.getUserManager().getUser(staffId);
        CompletableFuture<User> staffFuture = staffUser == null
                ? luckPerms.getUserManager().loadUser(staffId)
                : CompletableFuture.completedFuture(staffUser);
        CompletableFuture<User> targetFuture = luckPerms.getUserManager().loadUser(targetId);
        return staffFuture.thenCombineAsync(targetFuture, (actor, target) -> weight(actor) > weight(target));
    }

    private int weight(User user) {
        var options = luckPerms.getContextManager().getQueryOptions(user).orElse(net.luckperms.api.query.QueryOptions.nonContextual());
        return user.getInheritedGroups(options).stream()
                .mapToInt(group -> group.getWeight().orElse(0))
                .max().orElse(0);
    }
}
