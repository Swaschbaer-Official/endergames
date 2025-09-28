package org.swaschbaer.endergames.features.compass;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.swaschbaer.endergames.features.loot.ChestRegistry;

import java.util.*;

public class CompassManager implements Listener {

    public enum Mode { PLAYER, CHEST }

    private final Plugin plugin;
    private final Map<UUID, Mode> mode = new HashMap<>();
    private int updateTaskId = -1;

    private static final String NAME = "§dTracker-Kompass";
    private static final String LORE_PLAYER = "§7Ziel: §fNächster Spieler §8(§eRMB wechseln§8)";
    private static final String LORE_CHEST  = "§7Ziel: §fNächste Enderchest §8(§eRMB wechseln§8)";

    public CompassManager(Plugin plugin) { this.plugin = plugin; }

    /* ==== PUBLIC API ==== */

    /** Beim Game-Start aufrufen – verteilt die Kompasse und startet das Auto-Update. */
    public void startForPlayers(Collection<? extends Player> players) {
        players.forEach(p -> {
            mode.put(p.getUniqueId(), Mode.PLAYER);
            giveCompass(p, Mode.PLAYER);
            updateTarget(p);
        });
        startAutoUpdate(); // 1×/s Ziel aktualisieren
    }

    /** Beim Game-Ende/Reset aufrufen – Task stoppen und Status räumen. */
    public void stop() {
        if (updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId);
            updateTaskId = -1;
        }
        mode.clear();
        // optional: Kompass entfernen
        // Bukkit.getOnlinePlayers().forEach(this::removeCompass);
    }

    /* ==== EVENTS ==== */

    @EventHandler(ignoreCancelled = true)
    public void onToggle(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack it = e.getItem();
        if (it == null || it.getType() != Material.COMPASS) return;
        if (!isOurCompass(it)) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            UUID id = e.getPlayer().getUniqueId();
            Mode m = mode.getOrDefault(id, Mode.PLAYER);
            m = (m == Mode.PLAYER) ? Mode.CHEST : Mode.PLAYER;
            mode.put(id, m);
            giveCompass(e.getPlayer(), m);
            updateTarget(e.getPlayer());
        }
    }

    /* ==== INTERN ==== */

    private void startAutoUpdate() {
        if (updateTaskId != -1) return;
        updateTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (UUID id : new ArrayList<>(mode.keySet())) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) updateTarget(p);
            }
        }, 20L, 20L);
    }

    private void updateTarget(Player p) {
        Mode m = mode.getOrDefault(p.getUniqueId(), Mode.PLAYER);
        Location target = switch (m) {
            case PLAYER -> nearestPlayer(p).orElse(null);
            case CHEST  -> ChestRegistry.nearest(p.getWorld(), p.getLocation()).orElse(null);
        };
        p.setCompassTarget(target != null ? target : p.getWorld().getSpawnLocation());
    }

    private Optional<Location> nearestPlayer(Player self) {
        Location from = self.getLocation();
        double best = Double.MAX_VALUE;
        Player bestP = null;
        for (Player other : self.getWorld().getPlayers()) {
            if (other == self) continue;
            if (!other.isOnline() || other.isDead()) continue;
            double d2 = other.getLocation().distanceSquared(from);
            if (d2 < best) { best = d2; bestP = other; }
        }
        return bestP == null ? Optional.empty() : Optional.of(bestP.getLocation());
    }

    private void giveCompass(Player p, Mode m) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(NAME);
        meta.setLore(Collections.singletonList(m == Mode.PLAYER ? LORE_PLAYER : LORE_CHEST));
        if (meta instanceof CompassMeta cm) cm.setLodestoneTracked(false);
        item.setItemMeta(meta);

        p.getInventory().setItem(0, item); // Slot 0
        p.updateInventory();
    }

    private boolean isOurCompass(ItemStack it) {
        ItemMeta m = it.getItemMeta();
        return m != null && NAME.equals(m.getDisplayName());
    }

    @SuppressWarnings("unused")
    private void removeCompass(Player p) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (it != null && it.getType() == Material.COMPASS && isOurCompass(it)) {
                p.getInventory().clear(i);
            }
        }
    }
}
