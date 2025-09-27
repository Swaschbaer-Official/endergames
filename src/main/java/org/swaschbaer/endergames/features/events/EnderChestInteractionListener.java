package org.swaschbaer.endergames.features.events;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.swaschbaer.endergames.Main;
import org.swaschbaer.endergames.features.managers.LootManager;

public class EnderChestInteractionListener implements Listener {

    private final Main main;
    private final LootManager lootManager;
    private final NamespacedKey chestKey;

    public EnderChestInteractionListener(Main main, LootManager lootManager, String pdcKey) {
        this.main = main;
        this.lootManager = lootManager;
        this.chestKey = new NamespacedKey(main, pdcKey);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestOpen(PlayerInteractEvent e) {
        // Nur Main-Hand Events verarbeiten, sonst doppelt (Off-Hand)
        if (e.getHand() != EquipmentSlot.HAND) return;

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = e.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.ENDER_CHEST) return;

        // Nur unsere markierten Kisten „kapern“
        if (!(clicked.getState() instanceof TileState ts)) return;
        if (!ts.getPersistentDataContainer().has(chestKey, PersistentDataType.STRING)) {
            return; // normale Enderchest → Vanilla öffnen lassen
        }

        // Custom-Loot öffnen
        e.setCancelled(true); // verhindert Vanilla-Enderchest
        Player p = e.getPlayer();

        // Inventar (falls noch nicht vorbereitet) registrieren und holen
        lootManager.registerChestLoot(clicked.getLocation());
        Inventory inv = lootManager.getInventoryFor(clicked.getLocation());

        if (inv == null) {
            // Fallback: zur Sicherheit
            lootManager.registerChestLoot(clicked.getLocation());
            inv = lootManager.getInventoryFor(clicked.getLocation());
        }

        if (inv != null) {
            p.openInventory(inv);
        }
    }
}
