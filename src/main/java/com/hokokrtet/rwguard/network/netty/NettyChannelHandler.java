package com.hokokrtet.rwguard.network.netty;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketAction;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.models.PlayerData;
import com.hokokrtet.rwguard.utils.SchedulerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;

public class NettyChannelHandler extends ChannelDuplexHandler {
    private final RwGuard plugin;
    private final WeakReference<Player> playerRef;
    private final PlayerData playerData;

    public NettyChannelHandler(RwGuard plugin, Player player, PlayerData playerData) {
        this.plugin = plugin;
        this.playerRef = new WeakReference<>(player);
        this.playerData = playerData;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Player player = playerRef.get();
        if (player == null || !player.isOnline()) {
            super.channelRead(ctx, msg);
            return;
        }

        // 1. Raw buffer inspection before decoder (if ByteBuf)
        int byteSize = 0;
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            byteSize = buf.readableBytes();
            if (byteSize > plugin.getConfigManager().getGlobalByteLimit()) {
                plugin.getMetrics().recordDrop();
                plugin.getMetrics().recordViolation();
                playerData.addViolation("RAW_BUFFER", "ByteBuf size exceeded: " + byteSize, 5);
                ReferenceCountUtil.safeRelease(msg);
                return; // Drop immediately
            }
        }

        // 2. Classify packet
        String packetName = NettyPacketClassifier.classify(msg);
        PacketCategory category = NettyPacketClassifier.getCategory(packetName);

        // 3. Process packet through checks
        CheckContext context = new CheckContext(player, playerData, packetName, category, msg, byteSize);
        CheckResult result = plugin.getCheckManager().processPacket(context);

        if (result.getAction() == PacketAction.ALLOW) {
            super.channelRead(ctx, msg);
            return;
        }

        // Action is CANCEL, DROP, KICK, or BAN
        plugin.getMetrics().recordDrop();

        if (result.getAction() == PacketAction.KICK || result.getAction() == PacketAction.BAN) {
            plugin.getMetrics().recordKick();
            SchedulerUtil.dispatchKick(plugin, player, plugin.getConfigManager().getKickCommand(), result.getReason());
        }
        // Packet is dropped (safe release reference count to prevent direct memory leak)
        ReferenceCountUtil.safeRelease(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Prevent disconnects from malformed pipeline errors; log only if debug
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().warning("Netty exception in channel pipeline: " + cause.getMessage());
        }
    }
}
