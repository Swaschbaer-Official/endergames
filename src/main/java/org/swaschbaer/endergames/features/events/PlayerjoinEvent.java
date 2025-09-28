package org.swaschbaer.endergames.features.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.swaschbaer.endergames.Main;


public class PlayerjoinEvent implements Listener {

    private boolean lobbyRunning = false;

    @EventHandler
    public void onjoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        Main.getInstance().getKitregistry().put(e.getPlayer().getUniqueId(), "default");

        // Join-Message an alle (lokalisiert)
        for (Player pl : Bukkit.getOnlinePlayers()) {
            String joinMsg = Main.getInstance().getCustomConfigManager()
                    .getLanguageString(pl.getUniqueId(), "base.join", "language")
                    .replace("{player}", e.getPlayer().getDisplayName());
            pl.sendMessage(joinMsg);

            // Scoreboard: warten-Ansicht
            Main.getInstance().getScoreboardmanager().updateScoreboard(pl, "scoreboard.lobby-waiting");
        }

        // Countdown nur starten, wenn genug Spieler und noch nicht laufend
        if (Bukkit.getOnlinePlayers().size() >= Main.getInstance().getConfig().getInt("gamesettings.minplayer", 2)) {// hat Guard intern
            lobbyRunning = true;
            Main.getInstance().getGamemanager().starttimer();
        } else {
            if (lobbyRunning) {
                lobbyRunning = false;
            }
        }
    }
}

