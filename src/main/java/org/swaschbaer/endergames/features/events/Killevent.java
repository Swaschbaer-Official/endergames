package org.swaschbaer.endergames.features.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.swaschbaer.endergames.Main;

import java.util.HashMap;
import java.util.Map;

public class Killevent implements Listener {

    @EventHandler
    public void onKill(PlayerDeathEvent e){
        Main.getInstance().getGamemanager().activeplayers.remove(e.getPlayer());
        Player dead = e.getEntity();
        Player killer = dead.getKiller();
        int i = Main.getInstance().kills.get(killer) + 1;
        Main.getInstance().kills.put(killer, i);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            dead.spigot().respawn();
        }, 2L);
        if(Main.getInstance().getGamemanager().activeplayers.size() == 1){
            Main.getInstance().getGamemanager().endGame(false);
        }
    }

}
