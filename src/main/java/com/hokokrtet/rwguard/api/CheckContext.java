package com.hokokrtet.rwguard.api;

import com.hokokrtet.rwguard.models.PlayerData;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CheckContext {
    private final Player player;
    private final PlayerData playerData;
    private final UUID playerUuid;
    private final String packetName;
    private final PacketCategory category;
    private final Object rawPacket;
    private final int byteSize;
    private final long timestamp;
    private boolean cancelled;

    public CheckContext(Player player, PlayerData playerData, String packetName, PacketCategory category, Object rawPacket, int byteSize) {
        this.player = player;
        this.playerData = playerData;
        this.playerUuid = player != null ? player.getUniqueId() : null;
        this.packetName = packetName;
        this.category = category;
        this.rawPacket = rawPacket;
        this.byteSize = byteSize;
        this.timestamp = System.currentTimeMillis();
        this.cancelled = false;
    }

    public CheckContext(Player player, String packetType, Object rawPacket, PacketCategory category) {
        this(player, null, packetType, category, rawPacket, 0);
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPacketName() {
        return packetName;
    }

    public String getPacketType() {
        return packetName;
    }

    public PacketCategory getCategory() {
        return category;
    }

    public Object getRawPacket() {
        return rawPacket;
    }

    public int getByteSize() {
        return byteSize;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
