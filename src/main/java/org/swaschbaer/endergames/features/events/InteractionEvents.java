package org.swaschbaer.endergames.features.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.swaschbaer.endergames.Main;

public class InteractionEvents implements Listener {

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
