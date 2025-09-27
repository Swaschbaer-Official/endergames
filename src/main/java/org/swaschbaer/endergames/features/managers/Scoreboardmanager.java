package org.swaschbaer.endergames.features.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.swaschbaer.endergames.Main;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Scoreboardmanager {
    int i = 0;

    public void updateScoreboard(Player player, String  state) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("Endergames", "dummy", ChatColor.AQUA + "§lEnderGames");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        List<String> strings = Main.applyPlaceholders(Main.getInstance().getCustomConfigManager().getLanguageArray(player.getUniqueId(), state, "language"), player);
        strings.forEach(line -> {
            Score score = objective.getScore(line);
            score.setScore(i);
            i = i+1;
            if(strings.size() <= i){
                i = 0;
            }
        }
                );

        player.setScoreboard(scoreboard);
    }
}

