package org.swaschbaer.endergames.features.death;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.map.*;

public class EndgameMapRenderer extends MapRenderer {
    private final String title;
    private final String subtitle;
    private volatile int secondsLeft;

    public EndgameMapRenderer(String title, String subtitle, int seconds) {
        super(true);
        this.title = title;
        this.subtitle = subtitle;
        this.secondsLeft = seconds;
    }

    public void setSecondsLeft(int seconds) {
        this.secondsLeft = Math.max(0, seconds);
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                canvas.setPixel(x, y, MapPalette.DARK_GRAY);
            }
        }

        MapFont font = MinecraftFont.Font;
        canvas.drawText(25, 40, font, ChatColor.GOLD + title);
        canvas.drawText(20, 60, font, ChatColor.YELLOW + subtitle);

        canvas.drawText(15, 90, font, ChatColor.WHITE + "Shutdown in:");
        canvas.drawText(55, 105, font, ChatColor.YELLOW + String.valueOf(secondsLeft) + "s");
    }
}
