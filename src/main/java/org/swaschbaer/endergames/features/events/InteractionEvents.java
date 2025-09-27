package org.swaschbaer.endergames.features.events;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.swaschbaer.endergames.Main;

public class InteractionEvents implements Listener {

    @EventHandler
    public void onBlockBreak(PlayerHarvestBlockEvent e ){
        if(e.getPlayer().getWorld().equals(Bukkit.getWorld("world"))){
            e.setCancelled(true);
            e.getPlayer().sendMessage(Main.getInstance().getCustomConfigManager().getLanguageString(e.getPlayer().getUniqueId(), "error.break.lobby", "language"));
        }
    }
}
