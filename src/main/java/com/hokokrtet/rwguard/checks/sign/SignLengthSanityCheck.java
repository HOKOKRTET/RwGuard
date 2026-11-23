package com.hokokrtet.rwguard.checks.sign;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;

import java.lang.reflect.Field;

public class SignLengthSanityCheck extends AbstractCheck {
    public SignLengthSanityCheck(RwGuard plugin) {
        super(plugin, "SignLengthSanity", PacketCategory.SIGN);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("UPDATE_SIGN") && !context.getPacketName().contains("SignUpdate")) {
            return CheckResult.allow();
        }

        Object packet = context.getRawPacket();
        if (packet == null) return CheckResult.allow();

        int maxLen = plugin.getConfigManager().getSignMaxLineLength();
        try {
            Class<?> clazz = packet.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(packet);
                if (val instanceof String[]) {
                    String[] lines = (String[]) val;
                    for (String line : lines) {
                        if (line != null && line.length() > maxLen) {
                            return CheckResult.drop("Sign line length exceeded (" + line.length() + " > " + maxLen + ")", 3);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return CheckResult.allow();
    }
}
