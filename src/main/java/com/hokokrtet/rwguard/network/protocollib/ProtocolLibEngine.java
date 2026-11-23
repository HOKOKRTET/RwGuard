package com.hokokrtet.rwguard.network.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
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

public class ProtocolLibEngine implements PacketEngine {
    private RwGuard plugin;
    private PacketAdapter packetAdapter;

    @Override
    public void initialize(RwGuard rwGuard) {
        this.plugin = rwGuard;
        this.packetAdapter = new PacketAdapter(rwGuard, ListenerPriority.LOWEST, PacketType.Play.Client.getInstance().values()) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null || !player.isOnline()) return;

                String packetName = event.getPacketType().name();
                PlayerData data = rwGuard.getCheckManager().getPlayerData(player.getUniqueId(), player.getName());
                CheckContext context = new CheckContext(player, data, packetName, PacketCategory.OTHER, event.getPacket(), 0);

                CheckResult result = rwGuard.getCheckManager().processPacket(context);
                if (result.getAction() != PacketAction.ALLOW) {
                    event.setCancelled(true);
                    rwGuard.getMetrics().recordDrop();

                    if (result.getAction() == PacketAction.KICK || result.getAction() == PacketAction.BAN) {
                        rwGuard.getMetrics().recordKick();
                        SchedulerUtil.dispatchKick(rwGuard, player, rwGuard.getConfigManager().getKickCommand(), result.getReason());
                    }
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(packetAdapter);
    }

    @Override
    public void shutdown() {
        if (packetAdapter != null) {
            try {
                ProtocolLibrary.getProtocolManager().removePacketListener(packetAdapter);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void injectPlayer(Player player) {
        // Handled by ProtocolLib
    }

    @Override
    public void uninjectPlayer(Player player) {
        // Handled by ProtocolLib
    }

    @Override
    public String getName() {
        return "ProtocolLib";
    }
}
