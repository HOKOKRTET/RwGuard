package com.hokokrtet.rwguard.checks.chat;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Locale;

public class CommandSyntaxCrashCheck extends AbstractCheck {
    public CommandSyntaxCrashCheck(RwGuard plugin) {
        super(plugin, "CommandSyntaxCrash", PacketCategory.CHAT);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("CHAT") && !context.getPacketName().contains("Command")) {
            return CheckResult.allow();
        }

        Object packet = context.getRawPacket();
        if (packet == null) return CheckResult.allow();

        int maxLen = plugin.getConfigManager().getCommandMaxLength();
        Player player = context.getPlayer();
        boolean isOp = player != null && player.isOp();

        try {
            Class<?> clazz = packet.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(packet);
                if (val instanceof String) {
                    String str = (String) val;
                    if (str.length() > maxLen) {
                        return CheckResult.drop("Command or chat length exceeded (" + str.length() + " > " + maxLen + ")", 3);
                    }

                    // Check for null bytes or illegal ASCII control characters (crash exploit)
                    for (int i = 0; i < str.length(); i++) {
                        char c = str.charAt(i);
                        if (c == 0 || (c < 32 && c != '\n' && c != '\r' && c != '\t')) {
                            return CheckResult.drop("Illegal control characters in message/command", 4);
                        }
                    }

                    // Brigadier Entity Selector NBT Crash (ParadiseClient, CrashUtils, etc.)
                    if (!isOp) {
                        String lower = str.toLowerCase(Locale.ROOT);
                        if (lower.contains("@a[") || lower.contains("@e[") || lower.contains("@p[") || lower.contains("@r[") || lower.contains("@s[")) {
                            if (lower.contains("nbt=") || lower.contains("nbt =") || lower.contains("scores=") || lower.contains("distance=")) {
                                return CheckResult.drop("Malicious Brigadier entity selector NBT exploit attempt", 5);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return CheckResult.allow();
    }
}
