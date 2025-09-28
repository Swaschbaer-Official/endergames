package org.swaschbaer.endergames.features.death;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class DeathScreenManager implements Listener {

    private final Plugin plugin;
    private final Set<UUID> deadState = new HashSet<>();
    private final Map<UUID, DeathMaprenderer> renderers = new HashMap<>();

    public DeathScreenManager(Plugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();

        // Sofort respawnen (2 Ticks Delay)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            dead.spigot().respawn();
            showDeadCard(dead, 10); // 10s Countdown + Kick
        }, 2L);
    }

    private void showDeadCard(Player p, int seconds) {
        deadState.add(p.getUniqueId());

        // Hotbar leeren & Karte auf Slot 4
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        // MapView erstellen
        MapView view = Bukkit.createMap(p.getWorld());
        view.setTrackingPosition(false);
        view.getRenderers().forEach(view::removeRenderer);

        DeathMaprenderer renderer = new DeathMaprenderer(seconds);
        view.addRenderer(renderer);
        renderers.put(p.getUniqueId(), renderer);

        // Map-Item bauen
        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        meta.setDisplayName("§c§lYOU ARE DEAD");
        meta.setMapView(view);
        map.setItemMeta(meta);

        p.getInventory().setItem(4, map);
        p.updateInventory();

        // Jede Sekunde aktualisieren & nach Ablauf kicken
        final int[] left = { seconds };
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!p.isOnline() || !deadState.contains(p.getUniqueId())) { task.cancel(); return; }

            left[0]--;
            renderer.setSecondsLeft(left[0]);
            p.sendMap(view); // Re-Render triggern

            if (left[0] <= 0) {
                task.cancel();
                deadState.remove(p.getUniqueId());
                renderers.remove(p.getUniqueId());
                p.kickPlayer("§cYou died. Try again!");
            }
        }, 20L, 20L);
    }

    /* --- Schutz: Karte nicht bewegen/werfen, solange tot --- */

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!deadState.contains(p.getUniqueId())) return;
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (deadState.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    /* Optional: öffentlicher Reset, falls du vorher abbrechen willst */
    public void clearFor(Player p) {
        deadState.remove(p.getUniqueId());
        renderers.remove(p.getUniqueId());
    }
}
