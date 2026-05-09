package dev.safecombatlog;

import dev.safecombatlog.commands.CombatLogCommand;
import dev.safecombatlog.listeners.CombatListener;
import dev.safecombatlog.listeners.PlayerQuitListener;
import dev.safecombatlog.managers.CombatManager;
import dev.safecombatlog.managers.ConfigManager;
import dev.safecombatlog.managers.LogManager;
import dev.safecombatlog.managers.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SafeCombatLog extends JavaPlugin {

    private static SafeCombatLog instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private LogManager logManager;
    private CombatManager combatManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.logManager = new LogManager(this);
        this.combatManager = new CombatManager(this);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);

        CombatLogCommand commandExecutor = new CombatLogCommand(this);
        getCommand("combatlog").setExecutor(commandExecutor);
        getCommand("combatlog").setTabCompleter(commandExecutor);

        scheduleLogCleanup();

        getLogger().info("SafeCombatLog has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (combatManager != null) combatManager.clearAllCombat();
        if (logManager != null) logManager.flushAndClose();
        getLogger().info("SafeCombatLog has been disabled.");
        instance = null;
    }

    private void scheduleLogCleanup() {
        if (!configManager.isAutoCleanEnabled()) return;
        long periodTicks = 20L * 60 * 60 * 24;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () ->
                logManager.cleanOldLogs(configManager.getAutoCleanDays()),
                periodTicks, periodTicks);
    }

    public void reloadPlugin() {
        reloadConfig();
        configManager.reload();
        messageManager.reload();
        logManager.reload();
        getLogger().info("SafeCombatLog configuration reloaded.");
    }

    public static SafeCombatLog getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public LogManager getLogManager() { return logManager; }
    public CombatManager getCombatManager() { return combatManager; }
}
