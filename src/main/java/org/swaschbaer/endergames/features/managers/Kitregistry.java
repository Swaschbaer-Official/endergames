package org.swaschbaer.endergames.features.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class Kitregistry {
    private final Map<UUID, String> kit = new HashMap<>();

    public void put(Player player, String string) {
        if (player == null) return;
        put(player.getUniqueId(), string);
    }

    /** Fügt eine UUID/Location hinzu oder aktualisiert sie. */
    public void put(UUID uuid, String string) {
        if (uuid == null || string == null) return;

        kit.put(uuid, string);
    }

    /** Entfernt einen Eintrag; gibt true zurück, wenn es einen gab. */
    public boolean remove(UUID uuid) {
        return kit.remove(uuid) != null;
    }

    /** Liefert die gespeicherte Location oder null. */
    public String get(UUID uuid) {
        return kit.get(uuid);
    }

    /** Ob für diese UUID etwas gespeichert ist. */
    public boolean contains(UUID uuid) {
        return kit.containsKey(uuid);
    }

    /** Anzahl gespeicherter Spieler. */
    public int size() {
        return kit.size();
    }

    /** Alle UUIDs (read-only View). */
    public Set<UUID> uuids() {
        return Collections.unmodifiableSet(kit.keySet());
    }


    /** Alles löschen. */
    public void clear() {
        kit.clear();
    }
}

