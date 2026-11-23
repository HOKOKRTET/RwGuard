package com.hokokrtet.rwguard.network.netty;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.models.PlayerData;
import com.hokokrtet.rwguard.utils.ReflectionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.bukkit.entity.Player;

public final class NettyInjector {
    public static final String HANDLER_NAME = "rwguard_handler";
    public static final String RAW_HANDLER_NAME = "rwguard_raw_filter";

    private NettyInjector() {}

    public static void inject(RwGuard plugin, Player player, PlayerData playerData) {
        if (player == null) return;
        Channel channel = ReflectionUtil.getChannel(player);
        if (channel == null || !channel.isOpen()) return;

        channel.eventLoop().execute(() -> {
            try {
                ChannelPipeline pipeline = channel.pipeline();

                // 1. Clean existing handlers
                if (pipeline.get(RAW_HANDLER_NAME) != null) {
                    pipeline.remove(RAW_HANDLER_NAME);
                }
                if (pipeline.get(HANDLER_NAME) != null) {
                    pipeline.remove(HANDLER_NAME);
                }

                // 2. Pre-decoder Raw Frame Filter (Intercepts before ByteBuf decompression & parsing)
                NettyRawFrameHandler rawHandler = new NettyRawFrameHandler(plugin, player, playerData);
                if (pipeline.get("decoder") != null) {
                    pipeline.addBefore("decoder", RAW_HANDLER_NAME, rawHandler);
                } else if (pipeline.get("splitter") != null) {
                    pipeline.addAfter("splitter", RAW_HANDLER_NAME, rawHandler);
                } else {
                    pipeline.addFirst(RAW_HANDLER_NAME, rawHandler);
                }

                // 3. Post-decoder Game Packet Filter
                NettyChannelHandler handler = new NettyChannelHandler(plugin, player, playerData);
                if (pipeline.get("packet_handler") != null) {
                    pipeline.addBefore("packet_handler", HANDLER_NAME, handler);
                } else {
                    pipeline.addLast(HANDLER_NAME, handler);
                }
            } catch (Throwable t) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().warning("Could not inject Netty handler for " + player.getName() + ": " + t.getMessage());
                }
            }
        });
    }

    public static void uninject(RwGuard plugin, Player player) {
        if (player == null) return;
        Channel channel = ReflectionUtil.getChannel(player);
        if (channel == null || !channel.isOpen()) return;

        channel.eventLoop().execute(() -> {
            try {
                ChannelPipeline pipeline = channel.pipeline();
                if (pipeline.get(RAW_HANDLER_NAME) != null) {
                    pipeline.remove(RAW_HANDLER_NAME);
                }
                if (pipeline.get(HANDLER_NAME) != null) {
                    pipeline.remove(HANDLER_NAME);
                }
            } catch (Throwable ignored) {}
        });
    }
}
