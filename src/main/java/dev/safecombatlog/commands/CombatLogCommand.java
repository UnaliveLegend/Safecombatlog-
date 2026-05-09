package dev.safecombatlog.commands;

import dev.safecombatlog.SafeCombatLog;
import dev.safecombatlog.managers.CombatManager;
import dev.safecombatlog.managers.LogManager;
import dev.safecombatlog.managers.MessageManager;
import dev.safecombatlog.model.CombatEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CombatLogCommand implements CommandExecutor, TabCompleter {

    private final SafeCombatLog plugin;
    private static final int HISTORY_PAGE_SIZE = 10;

    public CombatLogCommand(SafeCombatLog plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload"  -> handleReload(sender);
            case "check"   -> handleCheck(sender, args);
            case "history" -> handleHistory(sender, args);
            case "status"  -> handleStatus(sender);
            default        -> sendHelp(sender);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("combatlog.reload")) {
            sender.sendMessage(plugin.getMessageManager().getNoPermissionMessage());
            return;
        }
        plugin.reloadPlugin();
        sender.sendMessage(plugin.getMessageManager().getReloadSuccessMessage());
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("combatlog.check") && !sender.hasPermission("combatlog.admin")) {
            sender.sendMessage(plugin.getMessageManager().getNoPermissionMessage());
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /combatlog check <player>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageManager().getPlayerNotFoundMessage(args[1]));
            return;
        }

        CombatManager cm = plugin.getCombatManager();
        if (!cm.isInCombat(target)) {
            sender.sendMessage(plugin.getMessageManager().getNotInCombatMessage(target.getName()));
            return;
        }

        CombatEntry entry = cm.getCombatEntry(target);
        if (entry == null) {
            sender.sendMessage(plugin.getMessageManager().getNotInCombatMessage(target.getName()));
            return;
        }

        sender.sendMessage(plugin.getMessageManager().getCombatStatusMessage(
                target.getName(), entry.getOpponentName(), entry.getRemainingSeconds()));
    }

    private void handleHistory(CommandSender sender, String[] args) {
        if (!sender.hasPermission("combatlog.check") && !sender.hasPermission("combatlog.admin")) {
            sender.sendMessage(plugin.getMessageManager().getNoPermissionMessage());
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /combatlog history <player>", NamedTextColor.RED));
            return;
        }

        String targetName = args[1];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            sender.sendMessage(plugin.getMessageManager().getPlayerNotFoundMessage(targetName));
            return;
        }

        List<Map<?, ?>> logs = plugin.getLogManager().getPlayerLogs(targetUUID);
        MessageManager mm = plugin.getMessageManager();

        if (logs.isEmpty()) {
            sender.sendMessage(mm.getHistoryEmptyMessage(targetName));
            return;
        }

        sender.sendMessage(mm.getHistoryHeaderMessage(targetName));
        int start = Math.max(0, logs.size() - HISTORY_PAGE_SIZE);

        for (int i = start; i < logs.size(); i++) {
            Map<?, ?> entry = logs.get(i);
            sender.sendMessage(mm.getHistoryEntryMessage(
                    i + 1,
                    formatTimestamp(safeString(entry.get("timestamp"), "unknown")),
                    safeString(entry.get("opponent"), "unknown"),
                    safeString(entry.get("world"), "world"),
                    safeInt(entry.get("x"), 0),
                    safeInt(entry.get("y"), 0),
                    safeInt(entry.get("z"), 0)
            ));
        }

        if (logs.size() > HISTORY_PAGE_SIZE) {
            sender.sendMessage(Component.text(
                    "  ... and " + (logs.size() - HISTORY_PAGE_SIZE) + " more entries.",
                    NamedTextColor.GRAY));
        }
    }

    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("combatlog.admin")) {
            sender.sendMessage(plugin.getMessageManager().getNoPermissionMessage());
            return;
        }

        CombatManager cm = plugin.getCombatManager();
        MessageManager mm = plugin.getMessageManager();
        Set<UUID> inCombat = cm.getAllInCombat();

        if (inCombat.isEmpty()) {
            sender.sendMessage(mm.getStatusEmptyMessage());
            return;
        }

        sender.sendMessage(mm.getStatusHeaderMessage());
        for (UUID uuid : inCombat) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            CombatEntry entry = cm.getCombatEntry(player);
            if (entry == null) continue;
            sender.sendMessage(mm.getStatusEntryMessage(
                    player.getName(), entry.getOpponentName(), entry.getRemainingSeconds()));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("combatlog.reload")) subs.add("reload");
            if (sender.hasPermission("combatlog.check") || sender.hasPermission("combatlog.admin")) {
                subs.add("check");
                subs.add("history");
            }
            if (sender.hasPermission("combatlog.admin")) subs.add("status");
            return filterByPrefix(subs, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("history"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return filterByPrefix(names, args[1]);
        }

        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("─── SafeCombatLog Commands ───", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/combatlog reload", NamedTextColor.YELLOW)
                .append(Component.text(" — Reload config", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/combatlog check <player>", NamedTextColor.YELLOW)
                .append(Component.text(" — Check combat status", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/combatlog history <player>", NamedTextColor.YELLOW)
                .append(Component.text(" — View combat log history", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/combatlog status", NamedTextColor.YELLOW)
                .append(Component.text(" — List all players in combat", NamedTextColor.GRAY)));
    }

    @SuppressWarnings("deprecation")
    private UUID resolveUUID(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) return offline.getUniqueId();
        return null;
    }

    private List<String> filterByPrefix(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) return list;
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String s : list) if (s.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(s);
        return result;
    }

    private String safeString(Object obj, String fallback) {
        return obj != null ? obj.toString() : fallback;
    }

    private int safeInt(Object obj, int fallback) {
        if (obj == null) return fallback;
        try { return (int) Double.parseDouble(obj.toString()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private String formatTimestamp(String iso) {
        try {
            LocalDateTime ldt = LocalDateTime.ofInstant(Instant.parse(iso), ZoneOffset.UTC);
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(ldt) + " UTC";
        } catch (Exception e) { return iso; }
    }
          }
