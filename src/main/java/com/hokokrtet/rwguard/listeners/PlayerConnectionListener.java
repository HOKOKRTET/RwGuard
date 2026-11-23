package com.hokokrtet.rwguard.listeners;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.models.PlayerData;
import com.hokokrtet.rwguard.utils.BedrockUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
    private final RwGuard plugin;

    public PlayerConnectionListener(RwGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        PlayerData data = plugin.getCheckManager().getPlayerData(event.getUniqueId(), event.getName());
        data.setBedrock(BedrockUtil.isBedrockPlayer(event.getUniqueId(), event.getName()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getCheckManager().getPlayerData(player.getUniqueId(), player.getName());
        if (!data.isBedrock()) {
            data.setBedrock(BedrockUtil.isBedrockPlayer(player));
        }
        plugin.getNetworkManager().injectPlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getNetworkManager().uninjectPlayer(player);
        plugin.getCheckManager().removePlayerData(player.getUniqueId());
        com.hokokrtet.rwguard.utils.PacketDumpManager.stopDump(player.getUniqueId());
    }
}
