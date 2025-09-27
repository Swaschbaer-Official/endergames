package org.swaschbaer.endergames.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.swaschbaer.endergames.Main;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CustomConfigManager {
    private Plugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    public CustomConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;

    }

    public void createCustomConfig(String name, String folder) {
        File configFile = new File(folder, name + ".yml");

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.save(configFile);
                configs.put(name, config);
            } catch (IOException e) {
                Main.getInstance().getLogger().severe("Konnte die CustomConfig " + name + " nicht erstellen!");
                e.printStackTrace();
            }
        }
    }

    public FileConfiguration getCustomConfig(String name, String folder) {
        if (configs.containsKey(name)) {
            return configs.get(name); // ✅ Direkt zurückgeben, wenn sie im Cache ist
        }

        File configFile = new File(Main.getInstance().getDataFolder() + File.separator + folder, name + ".yml");

        if (!configFile.exists()) {
            Main.getInstance().getLogger().severe("❌ Fehler: Die CustomConfig " + name + ".yml existiert nicht, obwohl sie erstellt wurde!");
            return null; // Oder besser: Eine Exception werfen?
        }

        // ✅ Datei existiert, also jetzt sicher laden
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (config == null) {
            Main.getInstance().getLogger().severe("❌ Fehler: Konnte die CustomConfig " + name + ".yml nicht laden!");
        } else {
            configs.put(name, config); // Speichern, damit sie nicht immer neu geladen wird
        }

        return config;
    }

    public String getLanguageString(UUID uuid, String location, String folder){
        return getCustomConfig(Main.getInstance().getDataHandler().getLanguage(uuid.toString()), folder).getString(location);
    }
    public List<String> getLanguageArray(UUID uuid, String location, String folder){
        return (List<String>) getCustomConfig(Main.getInstance().getDataHandler().getLanguage(uuid.toString()), folder).getList(location);
    }
}
