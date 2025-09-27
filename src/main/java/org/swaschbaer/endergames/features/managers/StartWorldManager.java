package org.swaschbaer.endergames.features.managers;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.scheduler.BukkitTask;
import org.swaschbaer.endergames.Main;

import java.util.*;

/**
 * Erstellt und wärmt die Spielwelt vor (Spawn-Chunks laden, optional Bereich vorgenerieren in Batches).
 * Nutze ensurePrepared(main) früh (z.B. wenn minplayer erreicht), damit beim Spielstart alles smooth ist.
 */
public class StartWorldManager {

    private static World preparedWorld;
    private static boolean preparing = false;
    private static BukkitTask pregenerateTask;


    public static World getPreparedWorld() {
        return preparedWorld;
    }

    /** Einmalig vorbereiten – idempotent. */
    public static synchronized void ensurePrepared(Main main) {
        if (preparedWorld != null || preparing) return;
        preparing = true;

        // Weltname (konfigurierbar oder generiert)
        String worldName = main.getConfig().getString("gamesettings.world_name");
        if (worldName == null || worldName.isEmpty()) {
            worldName = "endergames-" + System.currentTimeMillis();
        }

        // Welt erstellen/laden
        WorldCreator creator = new WorldCreator(worldName)
                .environment(World.Environment.NORMAL)
                .type(WorldType.NORMAL)
                .generateStructures(false);
        preparedWorld = creator.createWorld();

        if (preparedWorld == null) {
            preparing = false;
            throw new IllegalStateException("Failed to create game world: " + worldName);
        }

        // Basiseinstellungen
        preparedWorld.setTime(1000L);
        preparedWorld.setStorm(false);
        preparedWorld.setThundering(false);
        preparedWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        preparedWorld.setPVP(true);

        // Spawn grob in der Mitte
        Location spawn = new Location(preparedWorld, 0.5, preparedWorld.getHighestBlockYAt(0, 0), 0.5);
        preparedWorld.setSpawnLocation(spawn);
        recenterToLandRichArea(main, preparedWorld);

        // Spawn-Chunks laden
        int spawnChunkRadius = Math.max(0, main.getConfig().getInt("gamesettings.pregen.spawn_chunk_radius", 2));
        for (int cx = -spawnChunkRadius; cx <= spawnChunkRadius; cx++) {
            for (int cz = -spawnChunkRadius; cz <= spawnChunkRadius; cz++) {
                preparedWorld.getChunkAt(cx, cz).load(true);
            }
        }

        // Optional: Bereich vorgenerieren (leicht, in Batches)
        if (main.getConfig().getBoolean("gamesettings.pregen.enabled", false)) {
            startLightPregeneration(main, preparedWorld);
        } else {
            preparing = false;
        }
    }

    /** Batch-Pregeneration im Kreis um (0,0) – ressourcenschonend. */
    private static void startLightPregeneration(Main main, World world) {
        int radiusBlocks = Math.max(64, main.getConfig().getInt("gamesettings.mapsize", 500));
        int chunkRadius  = Math.max(1, radiusBlocks / 16);
        int batchSize    = Math.max(1, main.getConfig().getInt("gamesettings.pregen.batch_chunks", 32));

        // Chunks im Kreis sammeln und mischen
        List<long[]> queue = new ArrayList<>();
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                if ((cx*cx + cz*cz) <= (chunkRadius*chunkRadius)) {
                    queue.add(new long[]{cx, cz});
                }
            }
        }
        Collections.shuffle(queue, new Random());

        pregenerateTask = Bukkit.getScheduler().runTaskTimer(main, () -> {
            int processed = 0;
            while (processed < batchSize && !queue.isEmpty()) {
                long[] c = queue.remove(queue.size() - 1);
                world.getChunkAt((int)c[0], (int)c[1]).load(true);
                processed++;
            }
            if (queue.isEmpty()) {
                stopPregeneration();
                preparing = false;
                Bukkit.getLogger().info("[SW] Pregeneration complete for " + world.getName());
            }
        }, 20L, 20L); // alle 1s
    }

    public static void stopPregeneration() {
        if (pregenerateTask != null) {
            try { pregenerateTask.cancel(); } catch (Exception ignored) {}
            pregenerateTask = null;
        }
    }

    /** Sauberes Entladen/Löschen nach dem Match (optional). */
    public static void cleanup(Main main, boolean deleteWorldFolder) {
        stopPregeneration();
        if (preparedWorld != null) {
            // Spieler raus (zur Lobby)
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p.getWorld().equals(preparedWorld)) {
                    p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }
            });
            String folder = preparedWorld.getName();
            Bukkit.unloadWorld(preparedWorld, false);
            preparedWorld = null;
            preparing = false;

            if (deleteWorldFolder) {
                // Achtung: rekursives Löschen nur machen, wenn du sicher bist
                java.io.File f = new java.io.File(folder);
                deleteRec(f);
            }
        }
    }

    private static void deleteRec(java.io.File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            java.io.File[] kids = f.listFiles();
            if (kids != null) for (java.io.File k : kids) deleteRec(k);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    private static void recenterToLandRichArea(Main main, World world) {
        // Konfigurierbar
        int targetRadius = Math.max(64, main.getConfig().getInt("gamesettings.mapsize", 500));
        int scanStep     = Math.max(16, main.getConfig().getInt("gamesettings.landscan.step", 32)); // in Blöcken
        int sampleRadius = Math.max(64, main.getConfig().getInt("gamesettings.landscan.sample_radius", 192));
        double maxOceanRatio = Math.min(1.0, Math.max(0.0, main.getConfig().getDouble("gamesettings.landscan.max_ocean_ratio", 0.15)));
        int searchRadius = Math.max(sampleRadius, main.getConfig().getInt("gamesettings.landscan.search_radius", 2048));

        // Wir durchsuchen ein Raster um 0/0 bis searchRadius, bis wir eine Region mit wenig Ozean finden.
        Location best = null;
        double bestOceanRatio = 1.0;

        outer:
        for (int ring = scanStep; ring <= searchRadius; ring += scanStep) {
            for (int x = -ring; x <= ring; x += scanStep) {
                int z1 = -ring, z2 = ring;

                Location[] candidates = new Location[] {
                        new Location(world, x, 0, z1),
                        new Location(world, x, 0, z2),
                        new Location(world, -ring, 0, x),
                        new Location(world, ring, 0, x)
                };

                for (Location c : candidates) {
                    double oceanRatio = estimateOceanRatio(world, c, sampleRadius, scanStep);
                    if (oceanRatio < bestOceanRatio) {
                        bestOceanRatio = oceanRatio;
                        best = c.clone();
                    }
                    if (oceanRatio <= maxOceanRatio) {
                        best = c.clone();
                        break outer; // gutes Center gefunden
                    }
                }
            }
        }

        if (best == null) {
            // kein besseres Zentrum gefunden – bleib bei (0,0) aber warnen
            Bukkit.getLogger().warning("[SW] No land-rich center found, staying at 0,0. Ocean ratio near spawn may be high.");
            return;
        }

        // Auf sichere Oberfläche heben
        int sx = best.getBlockX();
        int sz = best.getBlockZ();
        int sy = world.getHighestBlockYAt(sx, sz);
        Location center = new Location(world, sx + 0.5, sy, sz + 0.5);

        // Spawn & (später) Border-Center hierher legen
        world.setSpawnLocation(center);
        Bukkit.getLogger().info(String.format(Locale.ROOT,
                "[SW] Chose land-rich center at (%d, %d) with ocean ratio ~%.2f%% in r=%d.",
                sx, sz, bestOceanRatio * 100.0, sampleRadius));
    }

    /** Grobe Schätzung: Anteil an Ozean-Biomen um 'center' innerhalb sampleRadius (in Blöcken), Rasterabstand 'step'. */

    private static boolean isOceanBiome(org.bukkit.block.Biome biome) {
        return biome != null && biome.name().contains("OCEAN");
    }

    private static double estimateOceanRatio(org.bukkit.World world, org.bukkit.Location center, int sampleRadius, int step) {
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        int ocean = 0, total = 0;
        for (int x = cx - sampleRadius; x <= cx + sampleRadius; x += step) {
            for (int z = cz - sampleRadius; z <= cz + sampleRadius; z += step) {
                org.bukkit.block.Biome b = world.getBiome(x, world.getMinHeight() + 1, z);
                total++;
                if (isOceanBiome(b)) ocean++;
            }
        }
        return total == 0 ? 1.0 : (double) ocean / (double) total;
    }



}
