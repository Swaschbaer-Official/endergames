package org.swaschbaer.endergames.features.events;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.swaschbaer.endergames.Main;


public class PlayerjoinEvent implements Listener {

    @EventHandler
    public void onjoin(PlayerJoinEvent e){
        String join = Main.getInstance().getCustomConfigManager().getLanguageString(e.getPlayer().getUniqueId(), "base.join", "language");
        join = join.replace("{player}", e.getPlayer().getDisplayName());
        if(!Main.getInstance().ingame){
            join = join + " [&a" + Bukkit.getServer().getOnlinePlayers() + "&7/&a" + Main.getInstance().getConfig().getInt("gamesettings.maxplayer" + "&7]");
        }
        e.setJoinMessage(join);
    }
}
