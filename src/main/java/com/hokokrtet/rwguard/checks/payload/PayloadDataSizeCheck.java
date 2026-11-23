package com.hokokrtet.rwguard.checks.payload;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import io.netty.buffer.ByteBuf;

import java.lang.reflect.Field;

public class PayloadDataSizeCheck extends AbstractCheck {
    public PayloadDataSizeCheck(RwGuard plugin) {
        super(plugin, "PayloadDataSize", PacketCategory.CUSTOM_PAYLOAD);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getPayloadMaxSizeBytes() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("CUSTOM_PAYLOAD") && !context.getPacketName().contains("CustomPayload")) {
            return CheckResult.allow();
        }

        Object packet = context.getRawPacket();
        if (packet == null) return CheckResult.allow();

        int dataLength = -1;
        try {
            Class<?> clazz = packet.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(packet);
                if (val instanceof ByteBuf) {
                    dataLength = ((ByteBuf) val).readableBytes();
                    break;
                } else if (val instanceof byte[]) {
                    dataLength = ((byte[]) val).length;
                    break;
                }
            }
        } catch (Throwable ignored) {}

        if (dataLength > plugin.getConfigManager().getPayloadMaxSizeBytes()) {
            return CheckResult.drop("Oversized payload data (" + dataLength + " > " + plugin.getConfigManager().getPayloadMaxSizeBytes() + " bytes)", 4);
        }

        return CheckResult.allow();
    }
}
