package org.swaschbaer.endergames.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public final class SurfaceUtil {

    private SurfaceUtil() {}

    /** Strikte Oberfläche: Top-Block an X/Z, darunter solide (kein Wasser/Laub), darüber 2× Luft. */
    public static Location findStrictSurface(World world, int x, int z) {
        int highest = world.getHighestBlockYAt(x, z);
        // Bis zu 48 Blöcke nach unten „feintunen“, falls Highest z. B. Blätter/Gras ist
        for (int y = highest; y > world.getMinHeight() && y >= highest - 48; y--) {
            Material floor = world.getBlockAt(x, y - 1, z).getType();
            Material air1  = world.getBlockAt(x, y, z).getType();
            Material air2  = world.getBlockAt(x, y + 1, z).getType();

            if (air1.isAir() && air2.isAir()
                    && floor.isSolid()
                    && floor != Material.WATER && floor != Material.LAVA
                    && !isLeaf(floor)) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    /** Optional: Wunschvariante – von Y=100 abwärts. Nutze ich nur als Fallback. */
    public static Location findFrom100Down(World world, int x, int z) {
        int startY = Math.min(100, world.getMaxHeight() - 2);
        for (int y = startY; y > world.getMinHeight(); y--) {
            Material floor = world.getBlockAt(x, y - 1, z).getType();
            Material air1  = world.getBlockAt(x, y, z).getType();
            Material air2  = world.getBlockAt(x, y + 1, z).getType();

            if (air1.isAir() && air2.isAir()
                    && floor.isSolid()
                    && floor != Material.WATER && floor != Material.LAVA
                    && !isLeaf(floor)) {
                // Trotzdem auf echte Oberfläche snappen:
                Location strict = findStrictSurface(world, x, z);
                return strict != null ? strict : new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    private static boolean isLeaf(Material m) {
        String n = m.name();
        return n.endsWith("_LEAVES") || n.endsWith("_LEAF");
    }
}
