package com.hokokrtet.rwguard.checks.chat;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Locale;

public class TabCompleteFloodCheck extends AbstractCheck {
    public TabCompleteFloodCheck(RwGuard plugin) {
        super(plugin, "TabCompleteFlood", PacketCategory.CHAT);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getTabCompleteMaxPps() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("TAB_COMPLETE") && !context.getPacketName().contains("CommandSuggestion")) {
            return CheckResult.allow();
        }

        PlayerData data = context.getPlayerData();
        PacketCounter counter = data.getCounter("TAB_COMPLETE");
        int count = counter.increment(1);
        int max = plugin.getConfigManager().getTabCompleteMaxPps();

        if (count > max) {
            return CheckResult.drop("Tab complete flood (" + count + "/" + max + " per sec)", 2);
        }

        // Inspect TabComplete command text for Brigadier exploits
        Object packet = context.getRawPacket();
        if (packet != null) {
            Player player = context.getPlayer();
            boolean isOp = player != null && player.isOp();

            try {
                Class<?> clazz = packet.getClass();
                for (Field f : clazz.getDeclaredFields()) {
                    f.setAccessible(true);
                    Object val = f.get(packet);
                    if (val instanceof String) {
                        String query = (String) val;
                        if (query.length() > 256) {
                            return CheckResult.drop("Oversized tab-complete query (" + query.length() + " chars)", 3);
                        }

                        if (!isOp) {
                            String lower = query.toLowerCase(Locale.ROOT);
                            if (lower.contains("@a[") || lower.contains("@e[") || lower.contains("@p[") || lower.contains("@r[") || lower.contains("@s[")) {
                                if (lower.contains("nbt=") || lower.contains("nbt =") || lower.contains("scores=") || lower.contains("distance=")) {
                                    return CheckResult.drop("Brigadier entity selector crash exploit in tab-complete", 5);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        return CheckResult.allow();
    }
}
