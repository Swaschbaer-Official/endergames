package org.swaschbaer.endergames.features.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.swaschbaer.endergames.Main;

import static org.swaschbaer.endergames.Main.applyPlaceholders;

public class GameTime {
    public BukkitTask taskid;
    public BukkitTask taskid2;
    public void starttimer(Player player){
        taskid = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
        if(Main.getInstance().startingtime > 0){
            Main.getInstance().startingtime = Main.getInstance().startingtime - 1;
            Main.getInstance().getScoreboardmanager().updateScoreboard(player, "scoreboard.lobby-starting");
        } else {
            startgame(player);
        }
        }, 20L, 20L);
    }

    public void resettimer(Player player) {

        Main.getInstance().startingtime = Main.getInstance().getConfig().getInt("gamesettings.starttime");
        Main.getInstance().ingametime = Main.getInstance().getConfig().getInt("gamesettings.gametime");
    }

    public void startgame(Player player) {
        taskid.cancel();

        taskid2 = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            if(Main.getInstance().ingametime > 0){
                Main.getInstance().ingametime = Main.getInstance().ingametime - 1;
                Main.getInstance().getScoreboardmanager().updateScoreboard(player, "scoreboard.lobby-playing");
            } else {
                shutdowntask(player);
            }
            }, 20L, 20L);

    }

    public void shutdowntask(Player player){
        taskid2.cancel();
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            Main.getInstance().getScoreboardmanager().updateScoreboard(player, "scoreboard.lobby-shutdown");
        }, 20L, 20L);
    }

}
