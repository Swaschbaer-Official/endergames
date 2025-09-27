package org.swaschbaer.endergames.features.events;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.swaschbaer.endergames.Main;

public class InteractionEvents implements Listener {

    private final Main main;

    public InteractionEvents(Main main) {
        this.main = main;
    }

    /** Jeglicher Schaden (Fall, Lava, Feuer, usw.) */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        boolean lobby = !isGameRunning();
        if (lobby) {
            e.setCancelled(true);
        }

    }


    private boolean isGameRunning() {
        return Main.getInstance().ingame;
    }
}
