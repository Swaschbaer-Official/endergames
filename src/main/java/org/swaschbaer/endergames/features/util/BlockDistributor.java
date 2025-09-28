package org.swaschbaer.endergames.features.util;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.swaschbaer.endergames.Main;
import org.swaschbaer.endergames.features.loot.ChestRegistry;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class BlockDistributor {

    private BlockDistributor() {}

    /**
     * Startet eine verteilte Platzierung von Oberflächenblöcken über alle Chunks im Border.
     *
     * @param plugin           dein Plugin
     * @param world            Zielwelt (NICHT null)
     * @param target           zu platzierender Block (z. B. Material.EMERALD_BLOCK)
     * @param chancePerChunk   0.0..1.0 – Chance, dass ein Chunk zwischen min..max Spawns bekommt
     * @param minPerChunk      mind. Anzahl pro gewonnenem Chunk (>=1)
     * @param maxPerChunk      max. Anzahl pro gewonnenem Chunk (>= min)
     * @param mapSize          Border-Durchmesser (wie bei dir), z. B. 500
     * @param seed             für deterministische Verteilung
     * @param batchChunks      wie viele Chunks pro Tick verarbeitet werden (z. B. 32–128)
     */
    public static void distributeOnSurfaceAsync(
            Plugin plugin,
            World world,
            Material target,
            double chancePerChunk,
            int minPerChunk,
            int maxPerChunk,
            int mapSize,
            long seed,
            int batchChunks
    ) {
        Objects.requireNonNull(world, "world must not be null");
        if (chancePerChunk <= 0 || maxPerChunk <= 0 || mapSize <= 0) return;

        final int radiusBlocks = mapSize / 2;
        final int minChunkX = floorDiv(-radiusBlocks, 16);
        final int maxChunkX = floorDiv(+radiusBlocks, 16);
        final int minChunkZ = floorDiv(-radiusBlocks, 16);
        final int maxChunkZ = floorDiv(+radiusBlocks, 16);

        Queue<long[]> queue = new ArrayDeque<>();
        // Ring-/Spiral-ähnliche Reihenfolge: zuerst nahe Spawn, dann nach außen (gleichmäßiger IO)
        List<long[]> all = new ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                // strenger Kreisfilter (nicht nur Quadrat)
                double centerX = (cx * 16 + 8);
                double centerZ = (cz * 16 + 8);
                if (centerX * centerX + centerZ * centerZ <= (radiusBlocks - 1) * (radiusBlocks - 1)) {
                    all.add(new long[]{cx, cz});
                }
            }
        }
        // sortiere nach Entfernung zum Zentrum (Annäherung an Ring)
        all.sort(Comparator.comparingDouble(a -> (a[0] * 16 + 8) * (a[0] * 16 + 8) + (a[1] * 16 + 8) * (a[1] * 16 + 8)));
        queue.addAll(all);

        Random rnd = new Random(seed);
        Bukkit.getLogger().info("[BlockDistributor] Beginne Verteilung in " + world.getName()
                + " – Chunks: " + queue.size() + " (Batch " + batchChunks + ", Chance " + chancePerChunk + ")");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    Bukkit.getLogger().info("[BlockDistributor] Verteilung abgeschlossen in " + world.getName());
                    cancel();
                    return;
                }

                int processed = 0;
                List<CompletableFuture<?>> futures = new ArrayList<>(batchChunks);

                while (processed < batchChunks && !queue.isEmpty()) {
                    long[] c = queue.poll();
                    int cx = (int) c[0];
                    int cz = (int) c[1];

                    // würfeln, ob dieser Chunk überhaupt was kriegt
                    if (rnd.nextDouble() >= chancePerChunk) {
                        processed++;
                        continue;
                    }

                    // Anzahl 1..3 (oder wie konfiguriert)
                    int count = minPerChunk + rnd.nextInt(maxPerChunk - minPerChunk + 1);

                    // Paper async, sonst sync fallback
                    CompletableFuture<?> fut;
                    try {
                        fut = world.getChunkAtAsync(cx, cz, true).thenRun(() -> placeInChunk(world, cx, cz, target, count, radiusBlocks, rnd));
                    } catch (NoSuchMethodError e) {
                        // Spigot: kein async – als Fallback sync laden
                        world.getChunkAt(cx, cz).load(true);
                        placeInChunk(world, cx, cz, target, count, radiusBlocks, rnd);
                        fut = CompletableFuture.completedFuture(null);
                    }
                    futures.add(fut);
                    processed++;
                }

                // Optional: throttle (warte kurz bis der Batch durch ist)
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void placeInChunk(World world, int cx, int cz, Material target, int count, int radiusBlocks, Random rnd) {
        for (int i = 0; i < count; i++) {
            // zufällige Block-Koordinate im Chunk
            int x = (cx << 4) + rnd.nextInt(16);
            int z = (cz << 4) + rnd.nextInt(16);
            // innerhalb Border halten
            if (!insideCircle(x, z, radiusBlocks)) continue;

            Block base = findSurfaceBase(world, x, z);
            if (base == null) continue;

            // Platzieren: auf die Oberfläche -> Block über base setzen
            Block place = base.getRelative(BlockFace.UP);
            // nicht überschreiben, wenn da schon was Wichtiges steht
            if (!place.isEmpty() && !place.isPassable()) continue;

            place.setType(target, false);
            place.setMetadata("eg_loot_chest", new FixedMetadataValue(Main.getInstance(), true));
            ChestRegistry.register(place.getLocation());
        }
    }

    /** Sucht eine begehbare Oberfläche: keine Blätter/Flüssigkeiten/gefährliche Blöcke. Gibt den Bodenblock zurück. */
    private static Block findSurfaceBase(World world, int x, int z) {
        Block top = world.getHighestBlockAt(x, z); // top kann Schnee/Laub sein
        Block b = top;
        for (int i = 0; i < 8; i++) {
            if (isSolidGround(b) && isAirLike(b.getRelative(BlockFace.UP)) && isAirLike(b.getRelative(0, 2, 0))) {
                // zusätzlich keine Flüssigkeit direkt nebenan (optional)
                if (nearLiquid(b, 1)) return null;
                return b;
            }
            b = b.getRelative(BlockFace.DOWN);
            if (b.getY() <= world.getMinHeight()) break;
        }
        return null;
    }

    private static boolean isSolidGround(Block b) {
        Material m = b.getType();
        if (!m.isSolid()) return false;
        String n = m.name();
        if (n.contains("LEAVES") || n.contains("GLASS") || n.contains("FENCE") || n.contains("WALL")
                || n.contains("DOOR") || n.contains("TRAPDOOR") || n.contains("SLAB") || n.contains("STAIRS"))
            return false;
        if (m == Material.CACTUS || m == Material.MAGMA_BLOCK) return false;
        return true;
    }

    private static boolean isAirLike(Block b) {
        return b.isEmpty() || b.isPassable();
    }

    private static boolean nearLiquid(Block b, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                Material n = b.getRelative(dx, 0, dz).getType();
                if (n == Material.WATER || n == Material.LAVA) return true;
            }
        }
        return false;
    }

    private static boolean insideCircle(int x, int z, int r) {
        return x * x + z * z <= r * r;
    }

    private static int floorDiv(int a, int b) {
        int q = a / b;
        int r = a % b;
        if ((a ^ b) < 0 && r != 0) q--;
        return q;
    }
}
