package org.swaschbaer.endergames.features.loot;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ChestRegistry {

    private static final Map<UUID, Set<Location>> ACTIVE = new ConcurrentHashMap<>();

    private ChestRegistry() {}

    public static void register(Location loc) {
        ACTIVE.computeIfAbsent(loc.getWorld().getUID(), k -> ConcurrentHashMap.newKeySet()).add(snap(loc));
    }

    public static void unregister(Location loc) {
        Set<Location> set = ACTIVE.getOrDefault(loc.getWorld().getUID(), Collections.emptySet());
        set.remove(snap(loc));
    }

    public static Optional<Location> nearest(World world, Location from) {
        Set<Location> set = ACTIVE.get(world.getUID());
        if (set == null || set.isEmpty()) return Optional.empty();
        Location best = null; double bestD2 = Double.MAX_VALUE;
        for (Location l : set) {
            double d2 = l.distanceSquared(from);
            if (d2 < bestD2) { bestD2 = d2; best = l; }
        }
        return Optional.ofNullable(best);
    }

    private static Location snap(Location l) {
        return new Location(l.getWorld(), l.getBlockX() + 0.5, l.getBlockY(), l.getBlockZ() + 0.5);
    }
}
