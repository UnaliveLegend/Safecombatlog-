package dev.safecombatlog.managers;

import dev.safecombatlog.SafeCombatLog;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final SafeCombatLog plugin;
    private int combatDuration;
    private String notificationType;
    private boolean broadcastEnabled;
    private boolean logIpAddresses;
    private boolean autoCleanEnabled;
    private int autoCleanDays;

    public ConfigManager(SafeCombatLog plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        load();
    }

    private void load() {
        FileConfiguration cfg = plugin.getConfig();
        combatDuration = cfg.getInt("combat.duration-seconds", 15);
        notificationType = cfg.getString("combat.notification-type", "actionbar");
        broadcastEnabled = cfg.getBoolean("logging.broadcast-to-global-chat", false);
        logIpAddresses = cfg.getBoolean("logging.log-ip-addresses", false);
        autoCleanEnabled = cfg.getBoolean("logging.auto-clean.enabled", true);
        autoCleanDays = cfg.getInt("logging.auto-clean.days-to-keep", 30);
    }

    public int getCombatDuration() { return combatDuration; }
    public String getNotificationType() { return notificationType; }
    public boolean isBroadcastEnabled() { return broadcastEnabled; }
    public boolean isLogIpAddresses() { return logIpAddresses; }
    public boolean isAutoCleanEnabled() { return autoCleanEnabled; }
    public int getAutoCleanDays() { return autoCleanDays; }
}
