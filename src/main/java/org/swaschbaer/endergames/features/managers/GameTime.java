package org.swaschbaer.endergames.features.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.swaschbaer.endergames.Main;

public class GameTime {

    private BukkitTask lobbyTask;
    private BukkitTask gameTask;
    private BukkitTask shutdownTask;
    private boolean lobbyRunning = false;
    private boolean gameRunning = false;

    public void startLobbyCountdown() {
        if (lobbyRunning) return;
        lobbyRunning = true;

        Main main = Main.getInstance();

        // >>> Welt früh vorbereiten (einmalig, async-batch pregeneration möglich)
        StartWorldManager.ensurePrepared(main);

        main.startingtime = main.getConfig().getInt("gamesettings.starttime");
        lobbyTask = Bukkit.getScheduler().runTaskTimer(main, () -> {
            if (main.startingtime > 0) {
                main.startingtime--;
                Bukkit.getOnlinePlayers().forEach(p ->
                        main.getScoreboardmanager().updateScoreboard(p, "scoreboard.lobby-starting"));
            } else {
                cancelSafely(lobbyTask);
                lobbyRunning = false;

                // >>> Welt ist bereits vorbereitet – jetzt nur noch Spiel starten
                new StartGame().startWithPreparedWorld(main, StartWorldManager.getPreparedWorld());
                startGameCountdown();
            }
        }, 0L, 20L);
    }

    public void resetAll() {
        cancelSafely(lobbyTask);
        cancelSafely(gameTask);
        cancelSafely(shutdownTask);
        lobbyRunning = false;
        gameRunning = false;

        Main main = Main.getInstance();
        main.startingtime = main.getConfig().getInt("gamesettings.starttime");
        main.ingametime   = main.getConfig().getInt("gamesettings.gametime");

        for (Player p : Bukkit.getOnlinePlayers()) {
            main.getScoreboardmanager().updateScoreboard(p, "scoreboard.lobby-waiting");
        }
    }

    private void startGameCountdown() {
        if (gameRunning) return;
        gameRunning = true;

        Main main = Main.getInstance();
        main.ingametime = main.getConfig().getInt("gamesettings.gametime");

        gameTask = Bukkit.getScheduler().runTaskTimer(main, () -> {
            if (main.ingametime > 0) {
                main.ingametime--;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    main.getScoreboardmanager().updateScoreboard(p, "scoreboard.game-playing");
                }
            } else {
                cancelSafely(gameTask);
                gameRunning = false;
                startShutdownPhase();
            }
        }, 0L, 20L);
    }

    private void startShutdownPhase() {
        Main main = Main.getInstance();

        // Beispiel: 5 Sekunden Shutdown-Overlay, dann Reset
        final int[] seconds = {5};
        shutdownTask = Bukkit.getScheduler().runTaskTimer(main, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                main.getScoreboardmanager().updateScoreboard(p, "scoreboard.game-shutdown");
            }
            if (--seconds[0] <= 0) {
                cancelSafely(shutdownTask);
                resetAll();
            }
        }, 0L, 20L);
    }

    private void cancelSafely(BukkitTask task) {
        if (task != null) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
    }
}
