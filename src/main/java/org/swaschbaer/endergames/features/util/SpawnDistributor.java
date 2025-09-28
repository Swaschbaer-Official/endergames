package org.swaschbaer.endergames.features.util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class SpawnDistributor {

    private SpawnDistributor() {}

    // Öffentlicher Entry-Point
    public static Map<Player, Location> generatePlayerSpawns(World world, int mapSize, Collection<Player> players) {
        int count = players.size();
        if (count == 0) return Collections.emptyMap();

        List<Location> spots = generateDistributedSurfaceSpawns(world, mapSize, count);
        // Falls weniger sichere Spots gefunden, kürzen:
        if (spots.size() < count) spots = spots.subList(0, spots.size());

        // Stabile Zuordnung (z. B. nach UUID sortieren, damit es deterministisch bleibt)
        List<Player> ordered = players.stream()
                .sorted(Comparator.comparing(p -> p.getUniqueId().toString()))
                .collect(Collectors.toList());

        Map<Player, Location> out = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(ordered.size(), spots.size()); i++) {
            out.put(ordered.get(i), spots.get(i));
        }
        return out;
    }

    // Erzeugt gleichmäßig verteilte Spots auf Surface
    public static List<Location> generateDistributedSurfaceSpawns(World world, int mapSize, int count) {
        final double borderRadius = mapSize / 2.0;
        final double margin = 8.0; // Abstand zur Border
        final double usableR = Math.max(0, borderRadius - margin);

        // Mindestabstand zwischen Spielern (Daumenregel)
        final double minDist = Math.max(6.0, usableR / Math.sqrt(count) * 0.9);

        List<Location> result = new ArrayList<>(count);

        // Fibonacci-Spirale für gleichmäßige Verteilung
        final double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0)); // ~2.3999632
        int attemptsPerPoint = 24; // kleine lokale Suche falls Wasser etc.

        Random rnd = new Random(1337); // deterministisch (optional)

        for (int i = 0; i < count; i++) {
            double t = (i + 0.5) / count;              // 0..1
            double r = usableR * Math.sqrt(t);         // gleichmäßige Flächendichte
            double theta = i * goldenAngle;

            int baseX = (int) Math.round(r * Math.cos(theta));
            int baseZ = (int) Math.round(r * Math.sin(theta));

            Location best = null;

            // kleine lokale Jitter-Suche (Kreis um den Basispunkt)
            for (int k = 0; k < attemptsPerPoint; k++) {
                double jitterR = (k == 0) ? 0 : (2 + k * 0.75); // 0, 2.0, 2.75, ...
                double jitterA = rnd.nextDouble() * Math.PI * 2;

                int x = baseX + (int) Math.round(jitterR * Math.cos(jitterA));
                int z = baseZ + (int) Math.round(jitterR * Math.sin(jitterA));

                // innerhalb Border?
                if (hypot(x, z) > usableR) continue;

                Location surface = findSurfaceSafe(world, x, z);
                if (surface == null) continue;

                // Mindestabstand zu bereits gesetzten Spawns
                boolean ok = true;
                for (Location l : result) {
                    if (l.getWorld() == world && l.distanceSquared(surface) < (minDist * minDist)) {
                        ok = false; break;
                    }
                }
                if (!ok) continue;

                best = surface;
                break; // guten Spot gefunden
            }

            if (best != null) {
                result.add(best);
            }
            // Falls nicht gefunden: lassen wir den Slot leer (wird später einfach weniger)
        }

        return result;
    }

    // Sucht die oberste sichere Y an X/Z
    private static Location findSurfaceSafe(World world, int x, int z) {
        // höchste solide Oberfläche (Spigot kompatibel)
        Block highest = world.getHighestBlockAt(x, z);

        // manchmal ist highest z. B. LEAVES/SNOW – wir gehen ein paar Blöcke runter bis solide begehbar
        Block base = descendToSolidGround(highest, 6);
        if (base == null) return null;

        // Sicherheit: 2 Blöcke Luft über Standfläche
        Block head = base.getRelative(0, 1, 0);
        Block head2 = base.getRelative(0, 2, 0);
        if (!isAirLike(head) || !isAirLike(head2)) return null;

        // Final-Checks: nicht gefährlich (Wasser/Lava/Kaktus/Feuer etc.)
        if (!isSafeGround(base)) return null;

        // leichte zentrierung auf Blockmitte
        double y = base.getY() + 1.0;
        return new Location(world, x + 0.5, y, z + 0.5, randomYawForSpread(x, z), 0f);
    }

    private static Block descendToSolidGround(Block start, int maxDown) {
        Block b = start;
        for (int i = 0; i <= maxDown; i++) {
            if (isSolidGround(b)) return b;
            b = b.getRelative(0, -1, 0);
            if (b.getY() <= worldMinY(b.getWorld())) break;
        }
        return null;
    }

    private static boolean isSolidGround(Block b) {
        Material m = b.getType();
        if (!m.isSolid()) return false;
        // Ausschlüsse: Blätter, Glas, Slabs/Stairs oben u.ä. (je nach Geschmack erweitern)
        if (m.name().contains("LEAVES")) return false;
        if (m.name().contains("GLASS")) return false;
        if (m.name().contains("FENCE") || m.name().contains("WALL")) return false;
        if (m.name().contains("DOOR") || m.name().contains("TRAPDOOR")) return false;
        if (m.name().contains("SLAB") || m.name().contains("STAIRS")) return false;
        return true;
    }

    private static boolean isSafeGround(Block b) {
        Material m = b.getType();
        if (m == Material.CACTUS || m == Material.MAGMA_BLOCK) return false;
        if (m == Material.CAMPFIRE || m == Material.FIRE || m == Material.SOUL_FIRE) return false;

        // Check Flüssigkeiten im Umkreis 1 (optional strenger)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Material n = b.getRelative(dx, 0, dz).getType();
                if (n == Material.WATER || n == Material.LAVA) return false;
            }
        }
        return true;
    }

    private static boolean isAirLike(Block b) {
        return b.isEmpty() || b.isPassable();
    }

    private static float randomYawForSpread(int x, int z) {
        int h = Objects.hash(x, z);
        Random r = new Random(h ^ 0x9E3779B9);
        return (float) (r.nextInt(360));
    }

    private static double hypot(int x, int z) {
        return Math.sqrt((double) x * x + (double) z * z);
    }

    private static int worldMinY(World w) {
        try {
            return w.getMinHeight(); // neuere APIs
        } catch (NoSuchMethodError ignored) {
            return -64; // fallback
        }
    }
}
