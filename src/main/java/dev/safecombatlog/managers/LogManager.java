package dev.safecombatlog.managers;

import dev.safecombatlog.SafeCombatLog;
import dev.safecombatlog.model.CombatEntry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class LogManager {

    private final SafeCombatLog plugin;
    private File logFile;
    private FileConfiguration logConfig;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public LogManager(SafeCombatLog plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() { load(); }

    private void load() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        logFile = new File(dataFolder, "combatlogs.yml");
        if (!logFile.exists()) {
            try { logFile.createNewFile(); }
            catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create combatlogs.yml", e);
            }
        }

        lock.writeLock().lock();
        try { logConfig = YamlConfiguration.loadConfiguration(logFile); }
        finally { lock.writeLock().unlock(); }
    }

    public void saveCombatLog(CombatEntry entry, String ipAddress) {
        String uuid = entry.getPlayerUUID().toString();
        String playerName = entry.getPlayerName();
        String opponentName = entry.getOpponentName();
        String timestamp = Instant.now().toString();
        String world = entry.getWorldName();
        double x = entry.getX();
        double y = entry.getY();
        double z = entry.getZ();
        int timeRemaining = entry.getRemainingSeconds();
        boolean logIp = plugin.getConfigManager().isLogIpAddresses();

        CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                String basePath = "logs." + uuid;
                int totalLogs = logConfig.getInt(basePath + ".total-logs", 0) + 1;

                logConfig.set(basePath + ".player-name", playerName);
                logConfig.set(basePath + ".uuid", uuid);
                logConfig.set(basePath + ".total-logs", totalLogs);

                Map<String, Object> entryMap = new LinkedHashMap<>();
                entryMap.put("opponent", opponentName);
                entryMap.put("timestamp", timestamp);
                entryMap.put("world", world);
                entryMap.put("x", Math.round(x * 10.0) / 10.0);
                entryMap.put("y", Math.round(y * 10.0) / 10.0);
                entryMap.put("z", Math.round(z * 10.0) / 10.0);
                entryMap.put("time-remaining", timeRemaining);

                if (logIp && ipAddress != null && !ipAddress.isEmpty()) {
                    entryMap.put("ip", ipAddress);
                }

                List<Map<?, ?>> entries = logConfig.getMapList(basePath + ".entries");
                entries.add(entryMap);
                logConfig.set(basePath + ".entries", entries);
                saveConfigToDisk();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save combat log for " + playerName, e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    public List<Map<?, ?>> getPlayerLogs(UUID playerUUID) {
        lock.readLock().lock();
        try {
            return new ArrayList<>(logConfig.getMapList("logs." + playerUUID + ".entries"));
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getTotalLogs(UUID playerUUID) {
        lock.readLock().lock();
        try {
            return logConfig.getInt("logs." + playerUUID + ".total-logs", 0);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void cleanOldLogs(int daysToKeep) {
        CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                long cutoffMillis = System.currentTimeMillis() - ((long) daysToKeep * 86400 * 1000);
                if (!logConfig.contains("logs")) return;
                boolean modified = false;

                for (String uuid : logConfig.getConfigurationSection("logs").getKeys(false)) {
                    String basePath = "logs." + uuid;
                    List<Map<?, ?>> entries = logConfig.getMapList(basePath + ".entries");
                    List<Map<?, ?>> kept = new ArrayList<>();

                    for (Map<?, ?> e : entries) {
                        Object ts = e.get("timestamp");
                        if (ts == null) continue;
                        try {
                            if (Instant.parse(ts.toString()).toEpochMilli() >= cutoffMillis) {
                                kept.add(e);
                            }
                        } catch (Exception ignored) {
                            kept.add(e);
                        }
                    }

                    if (kept.size() != entries.size()) {
                        logConfig.set(basePath + ".entries", kept);
                        logConfig.set(basePath + ".total-logs", kept.size());
                        modified = true;
                    }
                }

                if (modified) {
                    saveConfigToDisk();
                    plugin.getLogger().info("[SafeCombatLog] Old log entries cleaned.");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed during log cleanup", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    public void flushAndClose() {
        lock.writeLock().lock();
        try { saveConfigToDisk(); }
        finally { lock.writeLock().unlock(); }
    }

    private void saveConfigToDisk() {
        try { logConfig.save(logFile); }
        catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save combatlogs.yml", e);
        }
    }
                                   }
