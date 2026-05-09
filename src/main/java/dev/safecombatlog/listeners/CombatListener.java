package dev.safecombatlog.listeners;

import dev.safecombatlog.SafeCombatLog;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final SafeCombatLog plugin;

    public CombatListener(SafeCombatLog plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

        plugin.getCombatManager().enterCombat(attacker, victim);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        org.bukkit.entity.Entity damager = event.getDamager();

        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                return shooter;
            }
        }

        return null;
    }
}
