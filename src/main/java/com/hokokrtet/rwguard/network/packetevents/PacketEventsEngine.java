package com.hokokrtet.rwguard.network.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketAction;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.models.PlayerData;
import com.hokokrtet.rwguard.network.PacketEngine;
import com.hokokrtet.rwguard.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PacketEventsEngine extends PacketListenerAbstract implements PacketEngine {
    private RwGuard plugin;

    public PacketEventsEngine() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void initialize(RwGuard plugin) {
        this.plugin = plugin;
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void shutdown() {
        try {
            PacketEvents.getAPI().getEventManager().unregisterListener(this);
        } catch (Throwable ignored) {}
    }

    @Override
    public void injectPlayer(Player player) {
        // Handled internally by PacketEvents
    }

    @Override
    public void uninjectPlayer(Player player) {
        // Handled internally by PacketEvents
    }

    @Override
    public String getName() {
        return "PacketEvents-2.x";
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Object playerObj = event.getPlayer();
        if (!(playerObj instanceof Player)) return;
        Player player = (Player) playerObj;

        String packetName = event.getPacketType().getName();
        if (packetName == null) packetName = "UNKNOWN";

        PlayerData data = plugin.getCheckManager().getPlayerData(player.getUniqueId(), player.getName());
        CheckContext context = new CheckContext(player, data, packetName, PacketCategory.OTHER, event, 0);

        CheckResult result = plugin.getCheckManager().processPacket(context);
        if (result.getAction() != PacketAction.ALLOW) {
            event.setCancelled(true);
            plugin.getMetrics().recordDrop();

            if (result.getAction() == PacketAction.KICK || result.getAction() == PacketAction.BAN) {
                plugin.getMetrics().recordKick();
                SchedulerUtil.dispatchKick(plugin, player, plugin.getConfigManager().getKickCommand(), result.getReason());
            }
        }
    }
}
