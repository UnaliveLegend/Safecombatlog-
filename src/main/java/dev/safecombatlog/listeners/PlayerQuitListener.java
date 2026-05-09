package dev.safecombatlog.listeners;

import dev.safecombatlog.SafeCombatLog;
import dev.safecombatlog.managers.CombatManager;
import dev.safecombatlog.managers.MessageManager;
import dev.safecombatlog.model.CombatEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.InetSocketAddress;

public class PlayerQuitListener implements Listener {

    private final SafeCombatLog plugin;

    public PlayerQuitListener(SafeCombatLog plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CombatManager combatManager = plugin.getCombatManager();

        if (!combatManager.isInCombat(player)) return;

        CombatEntry entry = combatManager.getCombatEntry(player);
        if (entry == null) return;

        combatManager.removeCombat(player.getUniqueId());

        int timeRemaining = entry.getRemainingSeconds();
        int x = (int) player.getLocation().getX();
        int y = (int) player.getLocation().getY();
        int z = (int) player.getLocation().getZ();
        String world = player.getWorld().getName();
        String opponentName = entry.getOpponentName();
        String playerName = player.getName();
        String ipAddress = resolveIp(player);

        plugin.getLogManager().saveCombatLog(entry, ipAddress);

        MessageManager mm = plugin.getMessageManager();
        String consoleMsg = mm.getCombatLoggedConsoleMessage(
                playerName, opponentName, world, x, y, z, timeRemaining);
        plugin.getLogger().warning(consoleMsg);

        Component staffMsg = mm.getCombatLoggedStaffMessage(
                playerName, opponentName, world, x, y, z, timeRemaining);
        notifyStaff(staffMsg, player);

        if (plugin.getConfigManager().isBroadcastEnabled()) {
            Component broadcastMsg = mm.getCombatLoggedBroadcastMessage(playerName);
            Bukkit.broadcast(broadcastMsg);
        }
    }

    private void notifyStaff(Component message, Player combatLogger) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(combatLogger.getUniqueId())) continue;
            if (online.hasPermission("combatlog.notify") || online.hasPermission("combatlog.admin")) {
                online.sendMessage(message);
            }
        }
    }

    private String resolveIp(Player player) {
        if (!plugin.getConfigManager().isLogIpAddresses()) return "";
        try {
            InetSocketAddress address = player.getAddress();
            if (address == null) return "";
            return address.getAddress().getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }
}
