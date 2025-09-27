package org.swaschbaer.endergames.features.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.WorldBorder;
import org.swaschbaer.endergames.Main;
import org.swaschbaer.endergames.util.SurfaceUtil;

import java.util.*;

public class StartGame {

    public static final String PDC_CHEST_KEY = "eg_enderchest";
    private final Random random = new Random();

    private World gameWorld;
    private LootManager lootManager;

    /* === Aufruf aus GameTime: new StartGame().startWithPreparedWorld(main, StartWorldManager.getPreparedWorld()); === */
    public void startWithPreparedWorld(Main main, World world) {
        if (world == null) throw new IllegalStateException("Game world not prepared");
        this.gameWorld = world;

        // Border setzen (mapsize ist dein RADIUS -> Bordergröße = Durchmesser)
        setupWorldBorder(main, world);

        // Spieler verteilen (GARANTIERT innerhalb Border)
        distributePlayersRandomly(main);

        // Kisten platzieren (nur Oberfläche + innerhalb Border)
        if (lootManager == null) {
            lootManager = new LootManager(main);
            lootManager.loadFromConfig();
        }
        if (lootManager == null) {
            lootManager = new LootManager(main);
            lootManager.loadFromConfig();
        }

        Bukkit.getPluginManager().registerEvents(
                new org.swaschbaer.endergames.features.events.EnderChestInteractionListener(
                        main, lootManager, PDC_CHEST_KEY
                ),
                main
        );
        new EnderChestLootListener(main, lootManager, world, PDC_CHEST_KEY).placeChunkFair();
    }

    /* -------------------------------- Spawns -------------------------------- */

    private void distributePlayersRandomly(Main main) {
        WorldBorder border = gameWorld.getWorldBorder();
        double radius = border.getSize() / 2.0;
        Location center = border.getCenter();

        double minDistance = Math.max(8.0, main.getConfig().getDouble("gamesettings.spawns.min_distance", 16.0));
        java.util.List<Location> used = new java.util.ArrayList<>();

        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            Location loc = randomSurfaceLocationInBorder(gameWorld, center, radius, used, minDistance, 100);
            preparePlayerForGame(p);
            p.teleport(loc);
            used.add(loc);
            main.getScoreboardmanager().updateScoreboard(p, "scoreboard.lobby-starting");
        }
    }

    private Location randomSurfaceLocationInBorder(World world, Location center, double radius,
                                                   java.util.List<Location> used, double minDistance, int maxTries) {
        WorldBorder border = world.getWorldBorder();
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < maxTries; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(random.nextDouble()) * (radius - 3.0);

            int x = center.getBlockX() + (int) Math.round(r * Math.cos(angle));
            int z = center.getBlockZ() + (int) Math.round(r * Math.sin(angle));

            // 1) Echte Oberfläche
            Location surface = SurfaceUtil.findStrictSurface(world, x, z);
            if (surface == null) {
                surface = SurfaceUtil.findFrom100Down(world, x, z); // Fallback
            }
            if (surface == null) continue;

            // 2) Sicher in Border?
            if (!border.isInside(surface)) continue;

            // 3) Mindestabstand zu anderen Spawns?
            boolean farEnough = true;
            for (Location u : used) {
                if (u.getWorld().equals(surface.getWorld()) &&
                        u.distanceSquared(surface) < (minDistance * minDistance)) {
                    farEnough = false; break;
                }
            }
            if (farEnough) return surface;
        }

        // Fallback: Spawn zur Oberfläche snappen und ggf. in Border nudge'n
        Location spawn = world.getSpawnLocation().clone();
        Location surfaceSpawn = SurfaceUtil.findStrictSurface(world, spawn.getBlockX(), spawn.getBlockZ());
        if (surfaceSpawn != null) spawn = surfaceSpawn;

        if (!world.getWorldBorder().isInside(spawn)) {
            Location c = world.getWorldBorder().getCenter();
            Vector dir = c.toVector().subtract(spawn.toVector()).normalize();
            for (int i = 0; i < 32 && !world.getWorldBorder().isInside(spawn); i++) {
                spawn.add(dir);
            }
            Location snap = SurfaceUtil.findStrictSurface(world, spawn.getBlockX(), spawn.getBlockZ());
            if (snap != null) spawn = snap;
        }
        return spawn.add(0.5, 0, 0.5);
    }

    /* Oberflächen-Suche: solider Boden + 2x Luft */
    private Location findSafeSurface(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        // Scrolle etwas nach unten, falls HighestBlock Blätter/Gras ist
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

    private Location makeSurfaceSafe(Location loc) {
        World w = loc.getWorld();
        int x = loc.getBlockX(), z = loc.getBlockZ();
        int y = w.getHighestBlockYAt(x, z);
        return new Location(w, x + 0.5, y, z + 0.5);
    }

    private void preparePlayerForGame(Player p) {
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setSaturation(8);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.getInventory().clear();
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));

        // Beispiel: Startitems
        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 4));
    }

    /* -------------------------------- Border -------------------------------- */

    private void setupWorldBorder(Main main, World world) {
        int mapRadius = Math.max(16, main.getConfig().getInt("gamesettings.mapsize", 250));
        boolean active = main.getConfig().getBoolean("gamesettings.border.active", true);
        int shrinkStart = Math.max(0, main.getConfig().getInt("gamesettings.border.startshrink", 120));
        int minSize = Math.max(16, main.getConfig().getInt("gamesettings.border.minsize", 100));

        WorldBorder border = world.getWorldBorder();
        border.setCenter(world.getSpawnLocation());
        border.setDamageAmount(0.2);
        border.setWarningDistance(5);

        if (active) {
            border.setSize(mapRadius * 2.0); // mapsize = Radius
            // optionaler späterer Shrink
            if (minSize * 2.0 < border.getSize()) {
                new BukkitRunnable() {
                    @Override public void run() {
                        // Schrumpfe auf minSize (Durchmesser = minSize*2)
                        border.setSize(minSize * 2.0, Math.max(30, (world.getPlayers().size() > 0 ? main.ingametime : 120)));
                    }
                }.runTaskLater(main, shrinkStart * 20L);
            }
        } else {
            border.setSize(30_000_000); // effektiv aus
        }
    }
}
