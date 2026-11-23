package com.hokokrtet.rwguard.checks.payload;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

import java.lang.reflect.Field;
import java.util.Set;

public class PayloadChannelValidationCheck extends AbstractCheck {
    public PayloadChannelValidationCheck(RwGuard plugin) {
        super(plugin, "PayloadChannelValidation", PacketCategory.CUSTOM_PAYLOAD);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("CUSTOM_PAYLOAD") && !context.getPacketName().contains("CustomPayload")) {
            return CheckResult.allow();
        }

        Object packet = context.getRawPacket();
        if (packet == null) return CheckResult.allow();

        String channel = extractChannel(packet);
        if (channel == null) return CheckResult.allow();

        // Length validation
        if (channel.length() > 64) {
            return CheckResult.drop("Oversized payload channel name (" + channel.length() + " chars)", 3);
        }

        // Purpur Beehive exploit protection
        if (channel.equalsIgnoreCase("purpur:beehive_c2s")) {
            PlayerData data = context.getPlayerData();
            PacketCounter counter = data.getCounter("PURPUR_BEEHIVE");
            int count = counter.increment(1);
            if (count > 5) {
                return CheckResult.drop("Purpur beehive custom payload flood (" + count + "/sec)", 4);
            }
        }

        // Whitelist validation if strict
        if (plugin.getConfigManager().isPayloadStrictChannels()) {
            Set<String> allowed = plugin.getConfigManager().getAllowedChannels();
            if (!allowed.contains(channel) && !channel.equalsIgnoreCase("minecraft:register") && !channel.equalsIgnoreCase("minecraft:unregister")) {
                return CheckResult.drop("Payload channel not whitelisted: " + channel, 2);
            }
        }

        return CheckResult.allow();
    }

    private String extractChannel(Object packet) {
        try {
            Class<?> clazz = packet.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(packet);
                if (val instanceof String) {
                    return (String) val;
                } else if (val != null && val.getClass().getSimpleName().equals("MinecraftKey")) {
                    return val.toString();
                } else if (val != null && val.getClass().getSimpleName().equals("ResourceLocation")) {
                    return val.toString();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
