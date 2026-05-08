package dev.safecombatlog.model;

import java.time.Instant;
import java.util.UUID;

public class CombatEntry {

    private final String playerName;
    private final UUID playerUUID;
    private String opponentName;
    private UUID opponentUUID;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final long combatStartMillis;
    private long expiryMillis;
    private int durationSeconds;

    public CombatEntry(String playerName, UUID playerUUID, String opponentName,
                       UUID opponentUUID, String worldName, double x, double y,
                       double z, int durationSeconds) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.opponentName = opponentName;
        this.opponentUUID = opponentUUID;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.durationSeconds = durationSeconds;
        this.combatStartMillis = Instant.now().toEpochMilli();
        this.expiryMillis = combatStartMillis + (durationSeconds * 1000L);
    }

    public void resetTimer(int durationSeconds) {
        this.durationSeconds = durationSeconds;
        this.expiryMillis = Instant.now().toEpochMilli() + (durationSeconds * 1000L);
    }

    public int getRemainingSeconds() {
        long remaining = expiryMillis - Instant.now().toEpochMilli();
        return (int) Math.max(0, remaining / 1000);
    }

    public void setOpponent(String name, UUID uuid) {
        this.opponentName = name;
        this.opponentUUID = uuid;
    }

    public String getPlayerName() { return playerName; }
    public UUID getPlayerUUID() { return playerUUID; }
    public String getOpponentName() { return opponentName; }
    public UUID getOpponentUUID() { return opponentUUID; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public int getDurationSeconds() { return durationSeconds; }
    public long getCombatStartMillis() { return combatStartMillis; }
}
