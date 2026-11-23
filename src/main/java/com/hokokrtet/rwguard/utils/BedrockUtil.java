package com.hokokrtet.rwguard.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public final class BedrockUtil {
    private static volatile boolean floodgateChecked = false;
    private static volatile Object floodgateApiInstance = null;
    private static volatile Method isFloodgatePlayerMethod = null;

    private BedrockUtil() {}

    private static void initFloodgate() {
        if (floodgateChecked) return;
        synchronized (BedrockUtil.class) {
            if (floodgateChecked) return;
            try {
                if (Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
                    Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                    Method getInstanceMethod = apiClass.getMethod("getInstance");
                    floodgateApiInstance = getInstanceMethod.invoke(null);
                    isFloodgatePlayerMethod = apiClass.getMethod("isFloodgatePlayer", UUID.class);
                }
            } catch (Throwable ignored) {}
            floodgateChecked = true;
        }
    }

    public static boolean isBedrockPlayer(Player player) {
        if (player == null) return false;
        return isBedrockPlayer(player.getUniqueId(), player.getName());
    }

    public static boolean isBedrockPlayer(UUID uuid, String name) {
        initFloodgate();
        if (floodgateApiInstance != null && isFloodgatePlayerMethod != null && uuid != null) {
            try {
                Object result = isFloodgatePlayerMethod.invoke(floodgateApiInstance, uuid);
                if (result instanceof Boolean && (Boolean) result) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // Fallback: Floodgate Bedrock players often have 00000000-0000-0000- prefix or '.' / '*' in name
        if (name != null && (name.startsWith(".") || name.startsWith("*"))) {
            return true;
        }

        if (uuid != null) {
            // Floodgate UUIDs typically have high bits zero or specific range
            long mostSigBits = uuid.getMostSignificantBits();
            if (mostSigBits == 0L) {
                return true;
            }
        }

        return false;
    }
}
