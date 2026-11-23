package com.hokokrtet.rwguard.network;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.network.netty.NettyEngine;
import com.hokokrtet.rwguard.network.packetevents.PacketEventsEngine;
import com.hokokrtet.rwguard.network.protocollib.ProtocolLibEngine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class NetworkManager {
    private final RwGuard plugin;
    private PacketEngine activeEngine;

    public NetworkManager(RwGuard plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        String configuredEngine = plugin.getConfigManager().getEngine();

        if ("PACKETEVENTS".equalsIgnoreCase(configuredEngine)) {
            if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
                activeEngine = new PacketEventsEngine();
            } else {
                plugin.getLogger().warning("PacketEvents requested but plugin not found! Falling back to Netty.");
                activeEngine = new NettyEngine();
            }
        } else if ("PROTOCOLLIB".equalsIgnoreCase(configuredEngine)) {
            if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                activeEngine = new ProtocolLibEngine();
            } else {
                plugin.getLogger().warning("ProtocolLib requested but plugin not found! Falling back to Netty.");
                activeEngine = new NettyEngine();
            }
        } else if ("AUTO".equalsIgnoreCase(configuredEngine)) {
            if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
                activeEngine = new PacketEventsEngine();
            } else if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                activeEngine = new ProtocolLibEngine();
            } else {
                activeEngine = new NettyEngine();
            }
        } else {
            // Default to direct Netty
            activeEngine = new NettyEngine();
        }

        plugin.getLogger().info("RwGuard Packet Engine: " + activeEngine.getName());
        activeEngine.initialize(plugin);
    }

    public void shutdown() {
        if (activeEngine != null) {
            activeEngine.shutdown();
        }
    }

    public void injectPlayer(Player player) {
        if (activeEngine != null) {
            activeEngine.injectPlayer(player);
        }
    }

    public void uninjectPlayer(Player player) {
        if (activeEngine != null) {
            activeEngine.uninjectPlayer(player);
        }
    }

    public PacketEngine getActiveEngine() {
        return activeEngine;
    }
}
