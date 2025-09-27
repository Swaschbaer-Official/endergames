package org.swaschbaer.endergames.features.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.swaschbaer.endergames.Main;

import java.util.ArrayList;
import java.util.List;

import static org.swaschbaer.endergames.Main.applyPlaceholders;


public class PlayerjoinEvent implements Listener {
    public List<String> array;
    private boolean startet = false;
    @EventHandler
    public void onjoin(PlayerJoinEvent e) {
        Main.getInstance().getKitregistry().put(e.getPlayer().getUniqueId(), "default");
        e.setJoinMessage(null);
        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            String Join = Main.getInstance().getCustomConfigManager().getLanguageString(player.getUniqueId(), "base.join", "language");
            Join = Join.replace("{player}", e.getPlayer().getDisplayName());
            player.sendMessage(Join);
            Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
                    Player p = (Player) e.getPlayer();
                    if(!startet) {
                        Main.getInstance().getScoreboardmanager().updateScoreboard(player, "scoreboard.lobby-waiting");
            }
                    if(Bukkit.getServer().getOnlinePlayers().size() >= 2){
                        Main.getGameTime().starttimer(player);
                        startet = true;
                    } else if (startet){
                        Main.getGameTime().resettimer(player);
                        startet = false;
                    }
                    }, 20L, 20L);
        });
    }


}
