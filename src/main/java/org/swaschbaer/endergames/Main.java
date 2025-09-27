package org.swaschbaer.endergames;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.swaschbaer.endergames.cloudnet.Initialize;
import org.swaschbaer.endergames.config.CustomConfigManager;
import org.swaschbaer.endergames.config.DataHandler;
import org.swaschbaer.endergames.features.events.InteractionEvents;
import org.swaschbaer.endergames.features.events.PlayerjoinEvent;
import org.swaschbaer.endergames.features.events.PlayerquitEvent;
import org.swaschbaer.endergames.features.managers.GameTime;
import org.swaschbaer.endergames.features.managers.Kitregistry;
import org.swaschbaer.endergames.features.managers.Scoreboardmanager;

import java.io.File;
import java.util.*;

public final class Main extends JavaPlugin {
    // Cloudnet & Plugin initialiser
    private Initialize cloudnet;
    private static Main instance;
    private Kitregistry kitregistry;
    private CustomConfigManager customConfigManager;
    private DataHandler dataHandler;
    // Game Initialiser
    private Scoreboardmanager scoreboardmanager;
    public Boolean ingame = false;
    public String state = "waiting";
    public String langstate;
    private static GameTime gametime;
    // Variable stats for visualiser
    public Integer startingtime;
    public Integer ingametime;


    @Override
    public void onEnable() {
        instance = this;
        startingtime = Main.getInstance().getConfig().getInt("gamesettings.starttime");
        ingametime = Main.getInstance().getConfig().getInt("gamesettings.gametime");
        gametime = new GameTime();
        kitregistry = new Kitregistry();
        scoreboardmanager = new Scoreboardmanager();
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
        registerEvent(new PlayerjoinEvent(), this);
        registerEvent(new PlayerquitEvent(), this);
        registerEvent(new InteractionEvents(), this);
        Bukkit.getServer().setMotd("waiting.....");
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

    public static List<String> applyPlaceholders(List<String> lines, Player p) {
        List<String> result = new ArrayList<>();

        for (String line : lines) {
            String replaced = line;

            replaced = replaced.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
            replaced = replaced.replace("{maxonline}", String.valueOf(Main.getInstance().getConfig().getInt("gamesettings.maxplayer")));
            replaced = replaced.replace("{state}", getInstance().state);
            replaced = replaced.replace("{kit}", getInstance().getKitregistry().get(p.getUniqueId()));
            replaced = replaced.replace("{kills}", getInstance().getKitregistry().get(p.getUniqueId()));
            replaced = replaced.replace("{startingtime}", Main.getInstance().startingtime.toString());
            replaced = replaced.replace("{remainingtime}", getInstance().ingametime.toString());
            result.add(replaced);
        }

        return result;
    }


    private void registerEvent(Listener listener, Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    public Scoreboardmanager getScoreboardmanager() {
        return scoreboardmanager;
    }
    public Kitregistry getKitregistry() {
        return kitregistry;
    }

    @Override
    public void onDisable() {

    }

    public static Main getInstance() {
        return instance;
    }
    public static GameTime getGameTime(){
        return gametime;
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
