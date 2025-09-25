package org.swaschbaer.endergames;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.swaschbaer.endergames.cloudnet.Initialize;
import org.swaschbaer.endergames.config.CustomConfigManager;
import org.swaschbaer.endergames.config.DataHandler;

import java.io.File;

public final class Main extends JavaPlugin {
    private Initialize cloudnet;
    public Boolean ingame;
    public String langstate;
    private static Main instance;
    private CustomConfigManager customConfigManager;
    private DataHandler dataHandler;

    @Override
    public void onEnable() {

        instance = this;
        cloudnet = new Initialize(this);
        customConfigManager = new CustomConfigManager(this);
        dataHandler = new DataHandler(this);
        cloudnet.initalizeBridge();
        cloudnet.InitalizeServiceProvider();
        this.reloadConfig();
        saveDefaultConfig();
        saveLanguageFile("DE_de.yml");
        saveLanguageFile("EN_en.yml");
        if(cloudnet.cloudServer == null){
            Bukkit.getServer().getLogger().warning("CloudNet Bridge is not Initialized");
        } else if(cloudnet.cloudServiceProvider == null){
            Bukkit.getServer().getLogger().warning("CloudNet ServiceProvider is not Initialized");
        } else{
            Bukkit.getServer().getLogger().info("CloudNetSystems are Initialized");
        }
        try{
            dataHandler.connect();
            dataHandler.createTable("language", "uuid VARCHAR(36) PRIMARY KEY, language_file VARCHAR(255)");
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveLanguageFile(String fileName) {
        File langFolder = new File(getDataFolder(), "language"); // plugins/MeinPlugin/language/
        File langFile = new File(langFolder, fileName);

        if (!langFolder.exists()) {
            langFolder.mkdirs(); // Erstelle den language-Ordner falls er fehlt
        }

        if (!langFile.exists()) {
            saveResource("language/" + fileName, false); // Kopiere Datei aus JAR in den Plugin-Ordner
            getLogger().info("Erstelle Standard-Sprachdatei: " + fileName);
        }
    }

    @Override
    public void onDisable() {

    }

    public static Main getInstance() {
        return instance;
    }

    public CustomConfigManager getCustomConfigManager() {
        return customConfigManager;
    }

    public Initialize getCloudNetServices(){
        return cloudnet;
    }

    public DataHandler getDataHandler(){
        return dataHandler;
    }
}
