package org.swaschbaer.endergames.features.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitTask;
import org.swaschbaer.endergames.Main;
import org.swaschbaer.endergames.features.death.EndgameMapRenderer;
import org.swaschbaer.endergames.features.util.BlockDistributor;
import org.swaschbaer.endergames.features.util.SpawnDistributor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GameManager {
    public final Set<Player> activeplayers = new HashSet<>();
    private BukkitTask task;
    public void starttimer(){
        task = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                Main.getInstance().getScoreboardmanager().updateScoreboard(player,"scoreboard.lobby-starting");
            });
            if(Main.getInstance().startingtime > 0){
                Main.getInstance().startingtime--;
            } else{
                startgame();
            }

        }, 0L, 20L);

    }

    public void startgame(){
        task.cancel();
        Main.getInstance().ingame = true;
        activeplayers.addAll(Bukkit.getOnlinePlayers());
        activeplayers.forEach(p -> {
            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(20);
            p.setSaturation(20);
            p.setLevel(0);
            p.getInventory().clear();
        });
        WorldManager.gameworld.setDifficulty(Difficulty.HARD);
        Map<Player, Location> spawns = SpawnDistributor.generatePlayerSpawns(WorldManager.gameworld, Main.getInstance().getConfig().getInt("gamesettings.mapsize"), activeplayers);
        spawns.forEach((p, loc) -> p.teleport(loc));
        Main.getInstance().getCompassManager().startForPlayers(activeplayers);
        task = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
        if(Main.getInstance().ingametime > 0){
            Main.getInstance().ingametime--;
            activeplayers.forEach(player -> {
                Main.getInstance().getScoreboardmanager().updateScoreboard(player,"scoreboard.game-playing");
            });
        } else {
            endGame(true);
        }

        }, 0L, 20L);
    }

    public void reset(){
        Bukkit.getOnlinePlayers().forEach(player -> {
            Main.getInstance().getScoreboardmanager().updateScoreboard(player,"scoreboard.lobby-waiting");
        });
        Main.getInstance().startingtime = Main.getInstance().getConfig().getInt("gamesettings.starttime");
    }

    public void endGame(boolean timeOut) {
        int countdown = 10;

        if (timeOut) {
            // Alle Spieler: Time run out
            for (Player p : Bukkit.getOnlinePlayers()) {
                giveEndgameCard(p, "TIME IS UP", "You ran out of time!", countdown);
            }
        } else {
            // Nur ein Spieler übrig → Gewinner
            if (activeplayers.size() == 1) {
                Player winner = activeplayers.iterator().next();
                winner.teleport(Bukkit.getWorld("world").getSpawnLocation());
                giveEndgameCard(winner, "YOU HAVE WON", "Congratulations!", countdown);

                // Alle anderen auch ne Map? (optional)
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(winner)) {
                        giveEndgameCard(p, "GAME OVER", "You lost!", countdown);
                    }
                }
            }
        }

        // Shutdown nach 10s
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), Bukkit::shutdown, countdown * 20L);
    }


    private void giveEndgameCard(Player p, String title, String subtitle, int seconds) {
        MapView view = Bukkit.createMap(Bukkit.getWorld("world"));
        view.setTrackingPosition(false);
        view.getRenderers().forEach(view::removeRenderer);

        EndgameMapRenderer renderer = new EndgameMapRenderer(title, subtitle, seconds);
        view.addRenderer(renderer);

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + title);
        meta.setMapView(view);
        map.setItemMeta(meta);

        p.getInventory().clear();
        p.getInventory().setItem(4, map);
        p.updateInventory();

        final int[] left = { seconds };
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), task -> {
            if (!p.isOnline()) { task.cancel(); return; }

            left[0]--;
            renderer.setSecondsLeft(left[0]);
            p.sendMap(view);

            if (left[0] <= 0) {
                task.cancel();
                // nichts mehr → warten auf Shutdown
            }
        }, 20L, 20L);
    }

}
