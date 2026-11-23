package com.hokokrtet.rwguard.network.netty;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;
import com.hokokrtet.rwguard.utils.NbtSafetyUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;

public class NettyRawFrameHandler extends ChannelInboundHandlerAdapter {
    private final RwGuard plugin;
    private final WeakReference<Player> playerRef;
    private final PlayerData playerData;

    public NettyRawFrameHandler(RwGuard plugin, Player player, PlayerData playerData) {
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

        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            int bytes = buf.readableBytes();

            // 1. Raw ByteBuf size limit before decoder runs (prevents CPU exhaustion on decompression)
            int maxBytes = plugin.getConfigManager().getGlobalByteLimit();
            if (bytes > maxBytes) {
                plugin.getMetrics().recordDrop();
                plugin.getMetrics().recordViolation();
                playerData.addViolation("RAW_FRAME_OVERSIZE", "Oversized raw ByteBuf before decoder (" + bytes + " > " + maxBytes + ")", 5);
                ReferenceCountUtil.safeRelease(msg);
                return;
            }

            // 2. Raw incoming frame rate limiter (prevents Attack Client Netty-thread spam)
            PacketCounter rawCounter = playerData.getCounter("RAW_FRAMES");
            int rawPps = rawCounter.increment(1);
            int maxPps = plugin.getConfigManager().getGlobalPacketLimit();
            if (rawPps > maxPps) {
                plugin.getMetrics().recordDrop();
                ReferenceCountUtil.safeRelease(msg);
                return;
            }

            // 3. Raw NBT Decompression Bomb Shield before Minecraft decoder/inflater
            if (plugin.getConfigManager().isNbtDecompressorCheckEnabled() && NbtSafetyUtil.isPotentiallyCompressed(buf)) {
                int maxDecompressed = plugin.getConfigManager().getNbtMaxDecompressedBytes();
                int maxDepth = plugin.getConfigManager().getNbtMaxDepth();
                if (!NbtSafetyUtil.isSafeNbt(buf, maxDecompressed, maxDepth)) {
                    plugin.getMetrics().recordDrop();
                    plugin.getMetrics().recordViolation();
                    playerData.addViolation("RAW_NBT_BOMB", "Malicious NBT bomb frame dropped before decoder", 6);
                    ReferenceCountUtil.safeRelease(msg);
                    return;
                }
            }
        }

        super.channelRead(ctx, msg);
    }
}
