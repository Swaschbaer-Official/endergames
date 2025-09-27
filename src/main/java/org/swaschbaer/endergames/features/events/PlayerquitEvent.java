package org.swaschbaer.endergames.features.events;

import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.swaschbaer.endergames.Main;

public class PlayerquitEvent implements Listener {
    @EventHandler
    public void onplayerleave(PlayerQuitEvent e){
        e.setQuitMessage(null);
        Bukkit.getServer().getOnlinePlayers().forEach(player ->{
            String Join = Main.getInstance().getCustomConfigManager().getLanguageString(player.getUniqueId(), "base.leave", "language");
            Join = Join.replace("{player}", e.getPlayer().getDisplayName());
            player.sendMessage(Join);
        });
    }
}
