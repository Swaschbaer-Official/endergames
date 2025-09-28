package org.swaschbaer.endergames.features.death;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.map.*;

public class DeathMaprenderer extends MapRenderer {
    private volatile int secondsLeft;

    public DeathMaprenderer(int seconds) {
        super(true); // überschreibt alte Renderer
        this.secondsLeft = seconds;
    }

    public void setSecondsLeft(int seconds) {
        this.secondsLeft = Math.max(0, seconds);
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        // Hintergrund dunkel machen
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                canvas.setPixel(x, y, MapPalette.DARK_GRAY);
            }
        }

        MapFont font = MinecraftFont.Font;

        // Titel
        canvas.drawText(35, 40, font, ChatColor.RED + "YOU ARE");
        canvas.drawText(45, 55, font, ChatColor.RED + "DEAD");

        // Countdown
        canvas.drawText(20, 90, font, ChatColor.WHITE + "Disconnect in:");
        canvas.drawText(55, 105, font, ChatColor.YELLOW + String.valueOf(secondsLeft) + "s");
    }
}
