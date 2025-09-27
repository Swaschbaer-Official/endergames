package org.swaschbaer.endergames.features.managers;

import org.bukkit.*;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.WorldBorder;
import org.swaschbaer.endergames.Main;
import org.swaschbaer.endergames.util.SurfaceUtil;

import java.util.*;


public class EnderChestLootListener {

    private final Main main;
    private final LootManager lootManager;
    private final World world;
    private final NamespacedKey chestKey;
    private final Random rnd = new Random();


    public EnderChestLootListener(Main main, LootManager lootManager, World world, String pdcKey) {
        this.main = main;
        this.lootManager = lootManager;
        this.world = world;
        this.chestKey = new NamespacedKey(main, pdcKey);
    }

    public void placeChunkFair() {
        ConfigurationSection sec = main.getConfig().getConfigurationSection("gamesettings.loot");
        if (sec == null || !sec.getBoolean("enabled", true)) return;

        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double radius = border.getSize() / 2.0;

        int chunkRadius = Math.max(1, (int) Math.floor(radius / 16.0));
        double density = clamp01(sec.getDouble("chunk_density", 0.18));
        int maxChests = Math.max(0, sec.getInt("max_chests", 140));
        double minDistance = Math.max(4.0, sec.getDouble("min_distance", 12.0));

        java.util.List<long[]> chunks = new java.util.ArrayList<>();
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                if (cx * cx + cz * cz <= chunkRadius * chunkRadius) {
                    chunks.add(new long[]{cx, cz});
                }
            }
        }
        java.util.Collections.shuffle(chunks, rnd);

        int target = Math.min(maxChests, (int) Math.round(chunks.size() * density));
        java.util.List<Location> placed = new java.util.ArrayList<>();
        int placedCount = 0;

        for (long[] chunk : chunks) {
            if (placedCount >= target) break;

            int cx = (int) chunk[0];
            int cz = (int) chunk[1];
            boolean placedHere = false;
            int triesInChunk = 8;

            while (triesInChunk-- > 0 && !placedHere && placedCount < target) {
                int x = center.getBlockX() + (cx * 16) + java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 16);
                int z = center.getBlockZ() + (cz * 16) + java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 16);

                if (!insideCircle(center, x, z, radius - 2)) continue;

                // >>> Oberfläche erzwingen
                Location loc = SurfaceUtil.findStrictSurface(world, x, z);
                if (loc == null) {
                    loc = SurfaceUtil.findFrom100Down(world, x, z);
                }
                if (loc == null) continue;
                if (!border.isInside(loc)) continue;
                if (!isFarEnough(loc, placed, minDistance)) continue;

                // Boden + 1 = Kistenblock (wir haben 2× Luft garantiert)
                org.bukkit.block.Block floor = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
                org.bukkit.block.Block chestBlock = floor.getRelative(org.bukkit.block.BlockFace.UP);

                if (!chestBlock.getType().isAir()) continue;
                if (!chestBlock.getRelative(org.bukkit.block.BlockFace.UP).getType().isAir()) continue;

                chestBlock.setType(Material.ENDER_CHEST, false);

                if (chestBlock.getState() instanceof TileState ts) {
                    ts.getPersistentDataContainer().set(chestKey, org.bukkit.persistence.PersistentDataType.STRING, "true");
                    ts.update();
                }

                lootManager.registerChestLoot(chestBlock.getLocation());

                placed.add(chestBlock.getLocation().clone().add(0.5, 0, 0.5));
                placedCount++;
                placedHere = true;
            }
        }

        Bukkit.getLogger().info("[SW] Placed " + placedCount + " EnderChests on surface in " + world.getName());
    }

    private static boolean insideCircle(Location c, int x, int z, double r) {
        double dx = c.getX() - x, dz = c.getZ() - z;
        return dx * dx + dz * dz <= r * r;
    }





    private static boolean isFarEnough(Location loc, List<Location> placed, double minDistance) {
        double min2 = minDistance * minDistance;
        for (Location l : placed) {
            if (l.getWorld().equals(loc.getWorld()) && l.distanceSquared(loc) < min2) return false;
        }
        return true;
    }

    /** Oberfläche: solider Boden + 2x Luft (wie bei Spawns) */
    private static Location findOpenSurface(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        for (int yy = y; yy > world.getMinHeight(); yy--) {
            Material floor = world.getBlockAt(x, yy - 1, z).getType();
            Material air1  = world.getBlockAt(x, yy, z).getType();
            Material air2  = world.getBlockAt(x, yy + 1, z).getType();
            if (air1.isAir() && air2.isAir()
                    && floor.isSolid()
                    && floor != Material.WATER && floor != Material.LAVA
                    && !floor.name().endsWith("_LEAVES") && !floor.name().endsWith("_LEAF")) {
                return new Location(world, x + 0.5, yy, z + 0.5);
            }
        }
        return null;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) return 0.0;
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
