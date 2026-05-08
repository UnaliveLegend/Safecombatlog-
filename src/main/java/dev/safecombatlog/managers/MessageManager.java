package dev.safecombatlog.managers;

import dev.safecombatlog.SafeCombatLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageManager {

    private final SafeCombatLog plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer =
            LegacyComponentSerializer.builder().hexColors()
                    .useUnusualXRepeatedCharacterHexFormat().build();

    private FileConfiguration messagesConfig;
    private String combatTagged, combatEnded, combatLoggedConsole;
    private String combatLoggedStaff, combatLoggedBroadcast;
    private String noPermission, playerNotFound, reloadSuccess;
    private String notInCombat, combatStatus, historyHeader;
    private String historyEntry, historyEmpty, statusHeader;
    private String statusEntry, statusEmpty, prefix;

    public MessageManager(SafeCombatLog plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() { load(); }

    private void load() {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        if (!f.exists()) plugin.saveResource("messages.yml", false);
        messagesConfig = YamlConfiguration.loadConfiguration(f);

        prefix = messagesConfig.getString("prefix", "<gray>[<gold>SCL</gold>]</gray> ");
        combatTagged = messagesConfig.getString("combat.tagged", "<red>⚔ COMBAT vs <player> — <time>s</red>");
        combatEnded = messagesConfig.getString("combat.ended", "<green>✔ No longer in combat.</green>");
        combatLoggedConsole = messagesConfig.getString("combat.logged-console",
                "[SafeCombatLog] COMBAT LOG: <player> logged out vs <opponent> in <world> at <x>,<y>,<z>. <time>s left");
        combatLoggedStaff = messagesConfig.getString("combat.logged-staff",
                "<prefix><yellow>⚠ <player> combat-logged vs <opponent> in <world> at <x>,<y>,<z> (<time>s left)</yellow>");
        combatLoggedBroadcast = messagesConfig.getString("combat.logged-broadcast",
                "<prefix><red><player> disconnected during combat!</red>");
        noPermission = messagesConfig.getString("commands.no-permission", "<prefix><red>No permission.</red>");
        playerNotFound = messagesConfig.getString("commands.player-not-found", "<prefix><red>Player not found: <player></red>");
        reloadSuccess = messagesConfig.getString("commands.reload-success", "<prefix><green>Reloaded.</green>");
        notInCombat = messagesConfig.getString("commands.not-in-combat", "<prefix><gray><player> not in combat.</gray>");
        combatStatus = messagesConfig.getString("commands.combat-status",
                "<prefix><yellow><player> vs <opponent> — <time>s left</yellow>");
        historyHeader = messagesConfig.getString("commands.history-header", "<prefix><gold>History for <player></gold>");
        historyEntry = messagesConfig.getString("commands.history-entry",
                "  <dark_gray>#<index></dark_gray> <gray><timestamp></gray> vs <yellow><opponent></yellow> in <aqua><world></aqua> @ <white><x>,<y>,<z></white>");
        historyEmpty = messagesConfig.getString("commands.history-empty", "<prefix><gray>No history for <player>.</gray>");
        statusHeader = messagesConfig.getString("commands.status-header", "<prefix><gold>Players in combat:</gold>");
        statusEntry = messagesConfig.getString("commands.status-entry",
                "  <yellow><player></yellow> vs <yellow><opponent></yellow> (<red><time>s</red>)");
        statusEmpty = messagesConfig.getString("commands.status-empty", "<prefix><gray>No players in combat.</gray>");
    }

    private Component parse(String raw, TagResolver... resolvers) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        String converted = convertLegacy(raw);
        return miniMessage.deserialize(converted, TagResolver.resolver(resolvers));
    }

    private String stripForConsole(String raw, TagResolver... resolvers) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(parse(raw, resolvers));
    }

    private String convertLegacy(String input) {
        if (!input.contains("&") && !input.contains("§")) return input;
        try {
            return miniMessage.serialize(legacySerializer.deserialize(input));
        } catch (Exception e) {
            return input;
        }
    }

    private TagResolver ph(String key, String value) {
        return Placeholder.unparsed(key, value != null ? value : "");
    }

    public Component getCombatTagMessage(String opponent, int time) {
        return parse(combatTagged, ph("player", opponent), ph("time", String.valueOf(time)));
    }

    public Component getCombatEndedMessage() { return parse(combatEnded); }

    public String getCombatLoggedConsoleMessage(String player, String opponent,
            String world, int x, int y, int z, int time) {
        return stripForConsole(combatLoggedConsole, ph("player", player),
                ph("opponent", opponent), ph("world", world), ph("x", String.valueOf(x)),
                ph("y", String.valueOf(y)), ph("z", String.valueOf(z)), ph("time", String.valueOf(time)));
    }

    public Component getCombatLoggedStaffMessage(String player, String opponent,
            String world, int x, int y, int z, int time) {
        return parse(combatLoggedStaff, ph("prefix", prefix), ph("player", player),
                ph("opponent", opponent), ph("world", world), ph("x", String.valueOf(x)),
                ph("y", String.valueOf(y)), ph("z", String.valueOf(z)), ph("time", String.valueOf(time)));
    }

    public Component getCombatLoggedBroadcastMessage(String player) {
        return parse(combatLoggedBroadcast, ph("prefix", prefix), ph("player", player));
    }

    public Component getNoPermissionMessage() {
        return parse(noPermission, ph("prefix", prefix));
    }

    public Component getPlayerNotFoundMessage(String player) {
        return parse(playerNotFound, ph("prefix", prefix), ph("player", player));
    }

    public Component getReloadSuccessMessage() {
        return parse(reloadSuccess, ph("prefix", prefix));
    }

    public Component getNotInCombatMessage(String player) {
        return parse(notInCombat, ph("prefix", prefix), ph("player", player));
    }

    public Component getCombatStatusMessage(String player, String opponent, int time) {
        return parse(combatStatus, ph("prefix", prefix), ph("player", player),
                ph("opponent", opponent), ph("time", String.valueOf(time)));
    }

    public Component getHistoryHeaderMessage(String player) {
        return parse(historyHeader, ph("prefix", prefix), ph("player", player));
    }

    public Component getHistoryEntryMessage(int index, String timestamp, String opponent,
            String world, int x, int y, int z) {
        return parse(historyEntry, ph("index", String.valueOf(index)),
                ph("timestamp", timestamp), ph("opponent", opponent), ph("world", world),
                ph("x", String.valueOf(x)), ph("y", String.valueOf(y)), ph("z", String.valueOf(z)));
    }

    public Component getHistoryEmptyMessage(String player) {
        return parse(historyEmpty, ph("prefix", prefix), ph("player", player));
    }

    public Component getStatusHeaderMessage() { return parse(statusHeader, ph("prefix", prefix)); }

    public Component getStatusEntryMessage(String player, String opponent, int time) {
        return parse(statusEntry, ph("player", player), ph("opponent", opponent),
                ph("time", String.valueOf(time)));
    }

    public Component getStatusEmptyMessage() { return parse(statusEmpty, ph("prefix", prefix)); }
}
