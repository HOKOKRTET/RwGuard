package com.hokokrtet.rwguard.checks.netty;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;

import java.lang.reflect.Field;

public class ClientSettingsSanityCheck extends AbstractCheck {
    public ClientSettingsSanityCheck(RwGuard plugin) {
        super(plugin, "ClientSettingsSanity", PacketCategory.OTHER);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        String name = context.getPacketName();
        if (!name.contains("SETTINGS") && !name.contains("ClientInformation")) {
            return CheckResult.allow();
        }

        Object packet = context.getRawPacket();
        if (packet == null) return CheckResult.allow();

        try {
            Class<?> clazz = packet.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(packet);
                if (val instanceof String) {
                    String locale = (String) val;
                    if (locale.length() > 16) {
                        return CheckResult.drop("Oversized client settings locale string (" + locale.length() + ")", 3);
                    }
                } else if (f.getType() == int.class && f.getName().toLowerCase().contains("view")) {
                    int viewDist = f.getInt(packet);
                    if (viewDist < 0 || viewDist > 64) {
                        return CheckResult.drop("Illegal view distance in client settings (" + viewDist + ")", 3);
                    }
                }
            }
        } catch (Throwable ignored) {}

        return CheckResult.allow();
    }
}
