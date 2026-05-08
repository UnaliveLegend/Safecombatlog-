package dev.safecombatlog.managers;

import dev.safecombatlog.SafeCombatLog;
import dev.safecombatlog.model.CombatEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {

    private final SafeCombatLog plugin;
    private final Map<UUID, CombatEntry> combatMap = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> timerTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> notifyTasks = new ConcurrentHashMap<>();

    public CombatManager(SafeCombatLog plugin) {
        this.plugin = plugin;
    }

    public void enterCombat(Player attacker, Player victim) {
        tagPlayer(attacker, victim);
        tagPlayer(victim, attacker);
    }

    public boolean isInCombat(Player player) {
        return combatMap.containsKey(player.getUniqueId());
    }

    public CombatEntry getCombatEntry(Player player) {
        return combatMap.get(player.getUniqueId());
    }

    public Set<UUID> getAllInCombat() {
        return Collections.unmodifiableSet(combatMap.keySet());
    }

    public void removeCombat(UUID uuid) {
        combatMap.remove(uuid);
        cancelTask(timerTasks.remove(uuid));
        cancelTask(notifyTasks.remove(uuid));
    }

    public void clearAllCombat() {
        timerTasks.values().forEach(this::cancelTask);
        notifyTasks.values().forEach(this::cancelTask);
        combatMap.clear();
        timerTasks.clear();
        notifyTasks.clear();
    }

    private void tagPlayer(Player player, Player opponent) {
        UUID uuid = player.getUniqueId();
        int duration = plugin.getConfigManager().getCombatDuration();

        CombatEntry entry;
        if (combatMap.containsKey(uuid)) {
            entry = combatMap.get(uuid);
            entry.setOpponent(opponent.getName(), opponent.getUniqueId());
            entry.resetTimer(duration);
        } else {
            entry = new CombatEntry(
                    player.getName(), uuid,
                    opponent.getName(), opponent.getUniqueId(),
                    player.getWorld().getName(),
                    player.getLocation().getX(),
                    player.getLocation().getY(),
                    player.getLocation().getZ(),
                    duration
            );
            combatMap.put(uuid, entry);
            startNotifyTask(player, entry);
        }

        cancelTask(timerTasks.remove(uuid));
        timerTasks.put(uuid, scheduleCountdown(player, duration));
    }

    private BukkitTask scheduleCountdown(Player player, int seconds) {
        return plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            UUID uuid = player.getUniqueId();
            if (combatMap.containsKey(uuid)) {
                removeCombat(uuid);
                if (player.isOnline()) {
                    player.sendActionBar(plugin.getMessageManager().getCombatEndedMessage());
                }
            }
        }, (long) seconds * 20L);
    }

    private void startNotifyTask(Player player, CombatEntry entry) {
        UUID uuid = player.getUniqueId();
        cancelTask(notifyTasks.remove(uuid));

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !combatMap.containsKey(uuid)) {
                cancelTask(notifyTasks.remove(uuid));
                return;
            }
            int remaining = entry.getRemainingSeconds();
            if (remaining <= 0) {
                cancelTask(notifyTasks.remove(uuid));
                return;
            }

            Component notification = plugin.getMessageManager()
                    .getCombatTagMessage(entry.getOpponentName(), remaining);

            if ("title".equalsIgnoreCase(plugin.getConfigManager().getNotificationType())) {
                player.showTitle(net.kyori.adventure.title.Title.title(
                        Component.empty(), notification,
                        net.kyori.adventure.title.Title.Times.times(
                                Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(0)
                        )
                ));
            } else {
                player.sendActionBar(notification);
            }
        }, 0L, 20L);

        notifyTasks.put(uuid, task);
    }

    private void cancelTask(BukkitTask task) {
        if (task != null && !task.isCancelled()) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
    }
}
