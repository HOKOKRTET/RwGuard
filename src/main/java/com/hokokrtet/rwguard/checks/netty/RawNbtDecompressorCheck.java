package com.hokokrtet.rwguard.checks.netty;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.utils.NbtSafetyUtil;
import io.netty.buffer.ByteBuf;

import java.lang.reflect.Field;

public class RawNbtDecompressorCheck extends AbstractCheck {
    public RawNbtDecompressorCheck(RwGuard plugin) {
        super(plugin, "RawNbtDecompressor", PacketCategory.NETTY);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().isNbtDecompressorCheckEnabled();
    }

    @Override
    public CheckResult process(CheckContext context) {
        Object packet = context.getRawPacket();
        if (packet == null) return CheckResult.allow();

        // 1. Direct ByteBuf inspection
        if (packet instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) packet;
            if (NbtSafetyUtil.isPotentiallyCompressed(buf)) {
                int maxBytes = plugin.getConfigManager().getNbtMaxDecompressedBytes();
                int maxDepth = plugin.getConfigManager().getNbtMaxDepth();
                if (!NbtSafetyUtil.isSafeNbt(buf, maxBytes, maxDepth)) {
                    return CheckResult.drop("Malicious compressed NBT decompression bomb detected", 6);
                }
            }
        }

        // 2. Custom Payload or Packet byte buffer inspection
        String name = context.getPacketName();
        if (name.contains("CUSTOM_PAYLOAD") || name.contains("SET_CREATIVE_SLOT") || name.contains("B_EDIT")) {
            ByteBuf innerBuf = extractByteBuf(packet);
            if (innerBuf != null && innerBuf.readableBytes() > 4) {
                if (NbtSafetyUtil.isPotentiallyCompressed(innerBuf)) {
                    int maxBytes = plugin.getConfigManager().getNbtMaxDecompressedBytes();
                    int maxDepth = plugin.getConfigManager().getNbtMaxDepth();
                    if (!NbtSafetyUtil.isSafeNbt(innerBuf, maxBytes, maxDepth)) {
                        return CheckResult.drop("Exploit NBT bomb in payload packet", 6);
                    }
                }
            }
        }

        return CheckResult.allow();
    }

    private ByteBuf extractByteBuf(Object packet) {
        try {
            Class<?> clazz = packet.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(packet);
                if (val instanceof ByteBuf) {
                    return (ByteBuf) val;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
