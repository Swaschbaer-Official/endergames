package org.swaschbaer.endergames.features.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.swaschbaer.endergames.Main;

import java.util.Collections;
import java.util.List;

public class StartCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if(sender instanceof Player){
            if(Bukkit.getOnlinePlayers().size() >= 2 && sender.hasPermission("swaschbaer.endergames.command.start")){
                Main.getInstance().getGamemanager().startgame();
                Bukkit.getOnlinePlayers().forEach( p -> Main.getInstance().getCustomConfigManager().getLanguageString(p.getUniqueId(), "base.startcommand", "language"));
                return true;
            } else {
                return true;
            }
        } else{
            sender.sendMessage("Pls be a Player");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("swaschbaer.endergames.command.start")) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

}
