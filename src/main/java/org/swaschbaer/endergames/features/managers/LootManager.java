package org.swaschbaer.endergames.features.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.swaschbaer.endergames.Main;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class LootManager {

    private final Main main;
    private final Map<String, Rarity> rarities = new LinkedHashMap<>();
    private final List<LootDef> lootDefs = new ArrayList<>();
    // Kisteninventare pro Location-Key
    private final Map<String, Inventory> chestInventories = new HashMap<>();

    public LootManager(Main main) {
        this.main = main;
    }

    public void loadFromConfig() {
        ConfigurationSection base = main.getConfig().getConfigurationSection("gamesettings.loot");
        if (base == null) return;

        // Rarities laden
        rarities.clear();
        ConfigurationSection rarSec = base.getConfigurationSection("rarities");
        if (rarSec != null) {
            for (String key : rarSec.getKeys(false)) {
                double weight = rarSec.getDouble(key + ".weight", 1.0);
                String color = rarSec.getString(key + ".color", "&f");
                rarities.put(key.toUpperCase(Locale.ROOT), new Rarity(key.toUpperCase(Locale.ROOT), weight, color));
            }
        } else {
            // Defaults
            rarities.put("COMMON",    new Rarity("COMMON",    1.0, "&7"));
            rarities.put("RARE",      new Rarity("RARE",      0.5, "&9"));
            rarities.put("EPIC",      new Rarity("EPIC",      0.25,"&5"));
            rarities.put("LEGENDARY", new Rarity("LEGENDARY", 0.10,"&6"));
        }

        // Loot-Defs laden
        lootDefs.clear();
        for (Map<?, ?> raw : base.getMapList("items")) {
            LootDef def = LootDef.from(raw, rarities);
            if (def != null) lootDefs.add(def);
        }
    }

    /** Beim Platzieren einer markierten Endertruhe aufrufen. */
    public void registerChestLoot(Location loc) {
        String key = keyOf(loc);
        if (chestInventories.containsKey(key)) return;

        ConfigurationSection base = main.getConfig().getConfigurationSection("gamesettings.loot");
        int size = Math.max(9, Math.min(54, base != null ? base.getInt("inventory_size", 27) : 27));
        int rolls = base != null ? Math.max(1, base.getInt("rolls_per_chest", 5)) : 5;

        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_PURPLE + "EnderGames Loot");

        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        for (int i = 0; i < rolls; i++) {
            ItemStack stack = rollOneItem(tlr);
            if (stack != null) inv.addItem(stack);
        }

        chestInventories.put(key, inv);
    }

    public Inventory getInventoryFor(Location loc) {
        return chestInventories.get(keyOf(loc));
    }

    /* ---------------- intern ---------------- */

    private ItemStack rollOneItem(ThreadLocalRandom tlr) {
        if (lootDefs.isEmpty()) return null;

        double total = 0.0;
        for (LootDef d : lootDefs) total += d.weight;

        double pick = tlr.nextDouble() * total;
        double acc = 0.0;
        for (LootDef d : lootDefs) {
            acc += d.weight;
            if (acc >= pick) {
                return buildItem(tlr, d);
            }
        }
        return null;
    }

    private ItemStack buildItem(ThreadLocalRandom tlr, LootDef d) {
        if (d.material == null) return null;

        int amount = tlr.nextInt(d.minAmount, d.maxAmount + 1);
        amount = Math.min(Math.max(1, amount), d.material.getMaxStackSize());

        ItemStack it = new ItemStack(d.material, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            // Name
            String name = d.displayName;
            if (name == null || name.isEmpty()) {
                String pretty = d.material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
                name = ChatColor.translateAlternateColorCodes('&', d.rarityColor + " " + pretty);
            } else {
                name = ChatColor.translateAlternateColorCodes('&', d.rarityColor + " " + name);
            }
            meta.setDisplayName(name.trim());

            // Lore
            if (!d.lore.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : d.lore) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(lore);
            }

            // Enchants
            for (EnchantDef e : d.enchants) {
                int lvl = tlr.nextInt(e.minLevel, e.maxLevel + 1);
                if (lvl > 0) meta.addEnchant(e.type, lvl, true);
            }

            if (d.unbreakable) meta.setUnbreakable(true);
            if (d.hideFlags) meta.addItemFlags(ItemFlag.values());

            it.setItemMeta(meta);
        }

        return it;
    }

    private String keyOf(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /** Enchantments robust auflösen (NamespacedKey → getByKey, Fallback getByName). */
    private static Enchantment resolveEnchantment(String name) {
        if (name == null || name.isEmpty()) return null;
        String lower = name.toLowerCase(Locale.ROOT).trim().replace(' ', '_');

        // Versuche namespaced key
        NamespacedKey key = lower.contains(":") ? NamespacedKey.fromString(lower) : NamespacedKey.minecraft(lower);
        Enchantment e = (key != null) ? Enchantment.getByKey(key) : null;
        if (e != null) return e;

        // Fallback (älter/Spigot)
        return Enchantment.getByName(lower.toUpperCase(Locale.ROOT));
    }

    /* ================= DTOs ================= */

    static class Rarity {
        final String name;
        final double weightMultiplier;
        final String color;
        Rarity(String name, double weightMultiplier, String color) {
            this.name = name; this.weightMultiplier = weightMultiplier; this.color = color;
        }
    }

    static class EnchantDef {
        final Enchantment type;
        final int minLevel, maxLevel;
        EnchantDef(Enchantment type, int minLevel, int maxLevel) {
            this.type = type; this.minLevel = minLevel; this.maxLevel = maxLevel;
        }
    }

    static class LootDef {
        final Material material;
        final int minAmount, maxAmount;
        final double weight;
        final String rarityName;
        final String rarityColor;
        final String displayName;
        final boolean unbreakable, hideFlags;
        final List<String> lore;
        final List<EnchantDef> enchants;

        LootDef(Material material, int minAmount, int maxAmount, double weight, String rarityName, String rarityColor,
                String displayName, boolean unbreakable, boolean hideFlags, List<String> lore, List<EnchantDef> enchants) {
            this.material = material;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.weight = weight;
            this.rarityName = rarityName;
            this.rarityColor = rarityColor;
            this.displayName = displayName;
            this.unbreakable = unbreakable;
            this.hideFlags = hideFlags;
            this.lore = lore;
            this.enchants = enchants;
        }

        @SuppressWarnings("unchecked")
        static LootDef from(Map<?, ?> raw, Map<String, Rarity> rarities) {
            try {
                // Safes Lesen über Helper (um capture<?> zu umgehen)
                String matName   = String.valueOf(getOr(raw, "material", "BREAD"));
                Material mat     = Material.matchMaterial(matName);
                if (mat == null) return null;

                int min          = toInt(getOr(raw, "min", 1));
                int max          = toInt(getOr(raw, "max", 1));
                double baseWeight= toDouble(getOr(raw, "weight", 1.0));

                String rarityKey = String.valueOf(getOr(raw, "rarity", "COMMON")).toUpperCase(Locale.ROOT);
                Rarity r         = rarities.getOrDefault(rarityKey, new Rarity("COMMON", 1.0, "&7"));
                double weight    = baseWeight * Math.max(0.0001, r.weightMultiplier);

                String color     = r.color;
                String name      = String.valueOf(getOr(raw, "name", ""));
                boolean unbreakable = toBoolean(getOr(raw, "unbreakable", false));
                boolean hideFlags   = toBoolean(getOr(raw, "hide_flags", true));

                // Lore
                List<String> lore = new ArrayList<>();
                Object rawLore = raw.get("lore");
                if (rawLore instanceof List) {
                    for (Object o : (List<?>) rawLore) lore.add(String.valueOf(o));
                }

                // Enchants
                List<EnchantDef> enchants = new ArrayList<>();
                Object rawEnchs = raw.get("enchants");
                if (rawEnchs instanceof List) {
                    for (Object o : (List<?>) rawEnchs) {
                        if (!(o instanceof Map)) continue;
                        Map<?, ?> m = (Map<?, ?>) o;

                        String enName = String.valueOf(getOr(m, "type", "DURABILITY"));
                        Enchantment ench = resolveEnchantment(enName);
                        if (ench == null) continue;

                        int minL = toInt(getOr(m, "min", 1));
                        int maxL = Math.max(minL, toInt(getOr(m, "max", minL)));
                        enchants.add(new EnchantDef(ench, minL, maxL));
                    }
                }

                return new LootDef(
                        mat,
                        Math.max(1, min),
                        Math.max(1, Math.max(min, max)),
                        Math.max(0.0, weight),
                        rarityKey,
                        color,
                        name,
                        unbreakable,
                        hideFlags,
                        lore,
                        enchants
                );
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        /* ---------- Helper gegen capture<?>-Probleme ---------- */
        private static Object getOr(Map<?, ?> map, String key, Object def) {
            Object v = map.get(key);
            return (v != null) ? v : def;
        }
        private static int toInt(Object v) {
            try { return (int) Math.round(Double.parseDouble(String.valueOf(v))); }
            catch (Exception e) { return 0; }
        }
        private static double toDouble(Object v) {
            try { return Double.parseDouble(String.valueOf(v)); }
            catch (Exception e) { return 0.0; }
        }
        private static boolean toBoolean(Object v) {
            if (v instanceof Boolean b) return b;
            return Boolean.parseBoolean(String.valueOf(v));
        }

    }
}
