package org.swaschbaer.endergames.features.loot;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.swaschbaer.endergames.Main;

import java.util.HashSet;
import java.util.Set;

public class LootListener implements Listener {

    private final Plugin plugin;
    private final Set<String> consumed = new HashSet<>();
    private static final String META_LOOT_USED = "eg_loot_used";
    private static final String META_LOOT_CHEST = "eg_loot_chest"; // optional: nur „unsere“ Chests

    public LootListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChestOpen(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block b = e.getClickedBlock();
        if (b == null || b.getType() != Material.ENDER_CHEST) return;

        // Nur unsere Chests
        if (!b.hasMetadata("eg_loot_chest")) return;

        e.setCancelled(true);

        String key = b.getWorld().getUID() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
        if (b.hasMetadata("eg_loot_used")) {
            e.getPlayer().sendMessage("§cDiese Enderchest ist leer.");
            return;
        }

        Inventory inv = LootManager.generateLootInventory(e.getPlayer().getUniqueId());
        e.getPlayer().openInventory(inv);

        // markieren & deregistrieren & verschwinden lassen
        b.setMetadata("eg_loot_used", new FixedMetadataValue(Main.getInstance(), true));
        ChestRegistry.unregister(b.getLocation());
        // 1 Tick delay verhindert seltsames Client-Verhalten beim Öffnen
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> b.setType(Material.AIR));
    }
    private String keyOf(Block b) {
        return b.getWorld().getUID() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }
}
