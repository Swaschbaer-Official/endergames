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
import org.swaschbaer.endergames.features.managers.*;

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
    private GameManager gamemanager;
    private Scoreboardmanager scoreboardmanager;
    public Boolean ingame = false;
    public String state = "waiting";
    // Variable stats for visualiser
    public Integer startingtime;
    public Integer ingametime;


    @Override
    public void onEnable() {
        instance = this;
        gamemanager = new GameManager();
        startingtime = Main.getInstance().getConfig().getInt("gamesettings.starttime");
        ingametime = Main.getInstance().getConfig().getInt("gamesettings.gametime");
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
        Main Main;
        Main = this;
        Bukkit.getServer().setMotd("waiting.....");
    }

    private void saveDataFile(String fileName, String folder) {
        File langFolder = new File(getDataFolder(), folder); // plugins/MeinPlugin/language/
        File langFile = new File(langFolder, fileName);

        if (!langFolder.exists()) {
            langFolder.mkdirs(); // Erstelle den language-Ordner falls er fehlt
        }

        if (!langFile.exists()) {
            saveResource("language/" + fileName, false); // Kopiere Datei aus JAR in den Plugin-Ordner
            getLogger().info("Erstelle Standard-Sprachdatei: " + fileName);
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

    public static List<String> applyPlaceholders(List<String> lines, Player p) {
        if (lines == null || lines.isEmpty()) return java.util.Collections.emptyList();

        List<String> out = new ArrayList<>(lines.size());

        // sichere Werte holen
        int online = Bukkit.getOnlinePlayers().size();
        int maxOnline = Main.getInstance().getConfig().getInt("gamesettings.maxplayer", 0);
        String state = String.valueOf(Main.getInstance().state == null ? "waiting" : Main.getInstance().state);

        String kit = Main.getInstance().getKitregistry().get(p.getUniqueId());
        // Wenn du eine Kill-Map hast, nimm sie hier. Sonst 0:
        int kills = 0; // z.B. Main.getInstance().getKillMap().getOrDefault(p.getUniqueId(), 0);

        String startTime = String.valueOf(Main.getInstance().startingtime);
        String ingameTime = String.valueOf(Main.getInstance().ingametime);

        for (String line : lines) {
            if (line == null) continue;
            String r = line
                    .replace("{player}", p.getName())
                    .replace("{online}", String.valueOf(online))
                    .replace("{maxonline}", String.valueOf(maxOnline))
                    .replace("{state}", state)
                    .replace("{kit}", kit)
                    .replace("{kills}", String.valueOf(kills))
                    .replace("{startingtime}", startTime)
                    .replace("{remainingtime}", ingameTime);
            out.add(r);
        }
        return out;
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

    public CustomConfigManager getCustomConfigManager() {
        return customConfigManager;
    }
    public GameManager getGamemanager(){
        return gamemanager;
    }

    public Initialize getCloudNetServices(){
        return cloudnet;
    }

    public DataHandler getDataHandler(){
        return dataHandler;
    }
}
