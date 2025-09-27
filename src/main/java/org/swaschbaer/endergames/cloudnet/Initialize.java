package org.swaschbaer.endergames.cloudnet;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Initialize {
    private Plugin plugin;
    public BridgeServiceHelper cloudServer;
    public CloudServiceProvider cloudServiceProvider;
    public ServiceRegistry serviceRegistery;
    public PlayerManager pLayerManager;
    public Initialize(JavaPlugin plugin) {
        this.plugin = plugin;

    }

    public void initalizeBridge(){
        cloudServer = InjectionLayer.boot().instance(BridgeServiceHelper.class);
        serviceRegistery = InjectionLayer.boot().instance(ServiceRegistry.class);
        pLayerManager = serviceRegistery.defaultInstance(PlayerManager.class);
    }

    public void InitalizeServiceProvider(){
        cloudServiceProvider = InjectionLayer.boot().instance(CloudServiceProvider.class);
    }

    public void changetoIngame(){
        cloudServer.changeToIngame();
    }
}
