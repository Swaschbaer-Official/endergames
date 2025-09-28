package org.swaschbaer.endergames.features.managers;

import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.swaschbaer.endergames.Main;
import org.swaschbaer.endergames.features.util.BlockDistributor;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public final class WorldManager {

    private static final String WORLD_PREFIX = "Endergmes"; // fester Name (wie bei dir)
    private static String cachedWorldName;
    public static World gameworld;

    private WorldManager() {}

    /* =========================
       Public API
       ========================= */

    /** Beim Serverstart aufrufen. Lädt/erstellt Welt, setzt Border & optional Pregen. */
    public static World initOnStartup(Plugin plugin) {
        plugin.saveDefaultConfig();
        unloadAndDeleteWorld("Endergmes");

        // Werte JETZT lesen (nicht in static Feldern!)
        final int mapSize = plugin.getConfig().getInt("gamesettings.mapsize", 500);
        final boolean pregenEnabled = plugin.getConfig().getBoolean("gamesettings.pregen.enabled", true);
        final int spawnRadiusChunks = plugin.getConfig().getInt("gamesettings.pregen.spawn_chunk_radius", 2);
        final int batchChunks = plugin.getConfig().getInt("gamesettings.pregen.batch_chunks", 32);

        String worldName = plugin.getConfig().getString("world.name", null);
        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            World w = createWorld(); // fester Name (WORLD_PREFIX)
            if (w == null) throw new IllegalStateException("Welt konnte nicht erstellt werden.");
            worldName = w.getName();
            plugin.getConfig().set("world.name", worldName);
            plugin.saveConfig();
        }

        cachedWorldName = worldName;

        World world = ensureWorldLoaded(worldName);

        // Border setzen (mapSize ist Durchmesser)
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(mapSize);

        if (pregenEnabled) {
            startSpawnPregen(plugin, world, spawnRadiusChunks, batchChunks);
        }

        Bukkit.getLogger().info("[WorldManager] Welt bereit: " + worldName + " (Border " + mapSize + ")");
        gameworld = world;
        long seed = ThreadLocalRandom.current().nextLong();
        BlockDistributor.distributeOnSurfaceAsync(
                Main.getInstance(),
                WorldManager.getGameWorldOrThrow(Main.getInstance()),
                Material.ENDER_CHEST, // Zielblock
                0.15,    // 30% Chance pro Chunk
                1, 2,   // 1..3 Vorkommen
                Main.getInstance().getConfig().getInt("gamesettings.mapsize", 500),
                seed,   // Zufallsseed
                64      // Batch pro Tick
        );
        Bukkit.getLogger().info("[BlockDistributor] Seed für diese Runde: " + seed);

        return world;
    }

    private static void unloadAndDeleteWorld(String name) {
        World old = Bukkit.getWorld(name);
        if (old != null) {
            old.getPlayers().forEach(p -> p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()));
            Bukkit.unloadWorld(old, false);
        }
        File folder = new File(Bukkit.getWorldContainer(), name);
        deleteFolder(folder);
        deleteFolder(new File(Bukkit.getWorldContainer(), name + "_nether"));
        deleteFolder(new File(Bukkit.getWorldContainer(), name + "_the_end"));
    }

    private static void deleteFolder(File f) {
        if (!f.exists()) return;
        File[] files = f.listFiles();
        if (files != null) {
            for (File c : files) deleteFolder(c);
        }
        f.delete();
    }

    /** Erstellt die Spielwelt (hier: fester Name). */
    public static World createWorld() {
        String worldName = WORLD_PREFIX;
        World world = new WorldCreator(worldName).createWorld();
        if (world == null) {
            Bukkit.getLogger().warning("[WorldManager] Konnte Welt " + worldName + " nicht erstellen.");
            return null;
        }
        return world;
    }

    /** Holt eine Welt per Name (kann null liefern). */
    public static World getWorld(String name) {
        return Bukkit.getWorld(name);
    }

    /** Setzt/aktualisiert den gecachten Welt-Namen (z. B. nach init). */
    public static void setCachedWorldName(String name) {
        cachedWorldName = name;
    }

    /** Holt die Game-Welt aus Cache/Config und lädt sie falls nötig – niemals null (wirft sonst). */
    public static World getGameWorldOrThrow(Plugin plugin) {
        String name = (cachedWorldName != null)
                ? cachedWorldName
                : plugin.getConfig().getString("world.name", WORLD_PREFIX);
        World w = ensureWorldLoaded(name);
        if (w == null) {
            throw new IllegalStateException("Game world '" + name + "' konnte nicht geladen werden.");
        }
        return w;
    }

    /** Stellt sicher, dass eine Welt geladen ist (lädt/erzeugt sie bei Bedarf). */
    public static World ensureWorldLoaded(String name) {
        Objects.requireNonNull(name, "world name");
        World w = Bukkit.getWorld(name);
        if (w != null) return w;
        return new WorldCreator(name).createWorld();
    }

    /* =========================
       Intern: Pregen
       ========================= */

    /**
     * Asynchrone Pregen der Spawn-Chunks (radius in CHUNKS, nicht Blöcken).
     * Lädt in Batches pro Tick, damit der Server flüssig bleibt.
     */
    private static void startSpawnPregen(Plugin plugin, World world, int radiusChunks, int batchSize) {
        Objects.requireNonNull(world, "world darf nicht null sein");
        final int spawnChunkX = world.getSpawnLocation().getBlockX() >> 4;
        final int spawnChunkZ = world.getSpawnLocation().getBlockZ() >> 4;

        final Queue<long[]> work = new ArrayDeque<>();
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                work.add(new long[]{spawnChunkX + dx, spawnChunkZ + dz});
            }
        }

        Bukkit.getLogger().info("[WorldManager] Starte Spawn-Pregen in " + world.getName()
                + " — Chunks: " + work.size() + " (Batch " + batchSize + ")");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (work.isEmpty()) {
                    Bukkit.getLogger().info("[WorldManager] Pregen abgeschlossen für " + world.getName());
                    this.cancel();
                    return;
                }

                int processed = 0;
                while (processed < batchSize && !work.isEmpty()) {
                    long[] c = work.poll();
                    int cx = (int) c[0];
                    int cz = (int) c[1];

                    // Paper: lädt/generiert async; blockiert den Tick nicht
                    world.getChunkAtAsync(cx, cz, true);
                    processed++;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
