package org.swaschbaer.endergames.features.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

public class GameManager {
    private Collection<Player> players;
    public void starttimer(){
        players.addAll(Bukkit.getOnlinePlayers());


    }
}
