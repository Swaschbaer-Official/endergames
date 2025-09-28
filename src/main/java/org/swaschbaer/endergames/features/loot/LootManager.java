package org.swaschbaer.endergames.features.loot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.swaschbaer.endergames.Main;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class LootManager {

    private static int invSize = 27;
    private static int rollsPerChest = 5;

    private static final List<LootDef> weighted = new ArrayList<>();
    private static double totalWeight = 0.0;

    private LootManager() {}

    public static void load() {
        FileConfiguration cfg = Main.getInstance().getCustomConfigManager().getCustomConfig("loot", "");
        if (cfg == null) {
            Bukkit.getLogger().severe("[Loot] loot.yml fehlt oder konnte nicht geladen werden!");
            return;
        }

        invSize = cfg.getInt("loot.inventory_size", 27);
        rollsPerChest = cfg.getInt("loot.rolls_per_chest", 5);

        weighted.clear();
        totalWeight = 0.0;

        List<Map<?, ?>> items = cfg.getMapList("loot.items");
        for (Map<?, ?> raw : items) {
            try {
                String matStr = raw.get("material") != null ? String.valueOf(raw.get("material")) : null;
                if (matStr == null) continue;
                Material mat = Material.matchMaterial(matStr);
                if (mat == null) continue;

                int min = raw.containsKey("min") ? ((Number) raw.get("min")).intValue() : 1;
                int max = raw.containsKey("max") ? ((Number) raw.get("max")).intValue() : 1;
                double weight = raw.containsKey("weight") ? ((Number) raw.get("weight")).doubleValue() : 1.0;
                String rarity = raw.containsKey("rarity") ? String.valueOf(raw.get("rarity")) : "COMMON";
                String name = raw.containsKey("name") ? String.valueOf(raw.get("name")) : null;

                @SuppressWarnings("unchecked")
                List<String> lore = raw.containsKey("lore") ? (List<String>) raw.get("lore") : Collections.emptyList();

                boolean unbreakable = raw.containsKey("unbreakable") && Boolean.TRUE.equals(raw.get("unbreakable"));
                boolean hideFlags = raw.containsKey("hide_flags") && Boolean.TRUE.equals(raw.get("hide_flags"));

                // Enchants
                List<EnchDef> ench = new ArrayList<>();
                Object enchObj = raw.get("enchants");
                if (enchObj instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof Map<?, ?> m) {
                            String type = m.get("type") != null ? String.valueOf(m.get("type")) : null;
                            if (type == null) continue;
                            int emin = m.containsKey("min") ? ((Number) m.get("min")).intValue() : 1;
                            int emax = m.containsKey("max") ? ((Number) m.get("max")).intValue() : emin;
                            ench.add(new EnchDef(type, emin, emax));
                        }
                    }
                }

                // Rarity-Farbe
                String color = cfg.isString("loot.rarities." + rarity + ".color")
                        ? cfg.getString("loot.rarities." + rarity + ".color")
                        : "&7";

                LootDef def = new LootDef(mat, min, max, weight, rarity, color, name, lore, ench, unbreakable, hideFlags);
                weighted.add(def);
                totalWeight += weight;
            } catch (Exception ex) {
                Bukkit.getLogger().warning("[Loot] Fehler beim Item-Laden: " + ex.getMessage());
            }
        }
        Bukkit.getLogger().info("[Loot] Items=" + weighted.size() + ", rolls/chest=" + rollsPerChest + ", invSize=" + invSize);
    }

    public static Inventory generateLootInventory(UUID forPlayer) {
        Inventory inv = Bukkit.createInventory(null, invSize, "§5Loot");
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < rollsPerChest; i++) {
            LootDef def = pick(rnd);
            if (def == null) continue;

            int amount = clamp(def.min + rnd.nextInt(def.max - def.min + 1), 1, 64);
            ItemStack it = new ItemStack(def.material, amount);
            ItemMeta meta = it.getItemMeta();

            // Name
            if (def.name != null) meta.setDisplayName(color(def.name));
            else meta.setDisplayName(color(def.color) + nice(def.material.name()));

            // Lore
            if (!def.lore.isEmpty()) {
                List<String> loreC = new ArrayList<>(def.lore.size());
                for (String s : def.lore) loreC.add(color(s));
                meta.setLore(loreC);
            }

            // Enchants
            for (EnchDef e : def.enchants) {
                Enchantment ench = Enchantment.getByName(e.type.toUpperCase(Locale.ROOT));
                if (ench == null) continue;
                int lvl = rnd.nextInt(Math.min(e.min, e.max), Math.max(e.min, e.max) + 1);
                meta.addEnchant(ench, lvl, true);
            }

            meta.setUnbreakable(def.unbreakable);
            if (def.hideFlags) meta.addItemFlags(ItemFlag.values());
            it.setItemMeta(meta);

            // Slot verteilen
            int tries = 8;
            boolean placed = false;
            while (tries-- > 0) {
                int slot = rnd.nextInt(inv.getSize());
                if (inv.getItem(slot) == null) {
                    inv.setItem(slot, it);
                    placed = true;
                    break;
                }
            }
            if (!placed) inv.addItem(it);
        }
        return inv;
    }

    /* ---------- intern ---------- */

    private static LootDef pick(Random rnd) {
        if (weighted.isEmpty() || totalWeight <= 0) return null;
        double r = rnd.nextDouble() * totalWeight;
        double acc = 0;
        for (LootDef d : weighted) {
            acc += d.weight;
            if (r <= acc) return d;
        }
        return weighted.get(weighted.size() - 1);
    }

    private static int clamp(int v, int a, int b) { return Math.max(a, Math.min(b, v)); }
    private static String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    private static String nice(String mat) {
        String[] parts = mat.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        return sb.toString().trim();
    }

    private record EnchDef(String type, int min, int max) {}
    private record LootDef(
            Material material, int min, int max, double weight,
            String rarity, String color, String name, List<String> lore,
            List<EnchDef> enchants, boolean unbreakable, boolean hideFlags
    ) {}
}
