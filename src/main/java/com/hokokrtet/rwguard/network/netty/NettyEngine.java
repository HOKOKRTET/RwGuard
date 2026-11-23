package com.hokokrtet.rwguard.network.netty;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.models.PlayerData;
import com.hokokrtet.rwguard.network.PacketEngine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class NettyEngine implements PacketEngine {
    private RwGuard plugin;

    @Override
    public void initialize(RwGuard plugin) {
        this.plugin = plugin;
        for (Player player : Bukkit.getOnlinePlayers()) {
            injectPlayer(player);
        }
    }

    @Override
    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            uninjectPlayer(player);
        }
    }

    @Override
    public void injectPlayer(Player player) {
        if (plugin == null || player == null) return;
        PlayerData data = plugin.getCheckManager().getPlayerData(player.getUniqueId(), player.getName());
        NettyInjector.inject(plugin, player, data);
    }

    @Override
    public void uninjectPlayer(Player player) {
        if (plugin == null || player == null) return;
        NettyInjector.uninject(plugin, player);
    }

    @Override
    public String getName() {
        return "Netty-Direct";
    }
}
