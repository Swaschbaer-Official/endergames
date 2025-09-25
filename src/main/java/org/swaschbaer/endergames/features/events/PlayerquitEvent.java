package org.swaschbaer.endergames.features.events;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.swaschbaer.endergames.Main;

public class PlayerquitEvent implements Listener {
    @EventHandler
    public void onplayerleave(PlayerQuitEvent e){
        String leave = Main.getInstance().getCustomConfigManager().getLanguageString(e.getPlayer().getUniqueId(), "base.leave", "language");
        leave = leave.replace("{player}", e.getPlayer().getDisplayName());
        if(!Main.getInstance().ingame){
            leave = leave + " [&a" + Bukkit.getServer().getOnlinePlayers() + "&7/&a" + Main.getInstance().getConfig().getInt("gamesettings.maxplayer" + "&7]");
        }
        e.setQuitMessage(leave);
    }
}
