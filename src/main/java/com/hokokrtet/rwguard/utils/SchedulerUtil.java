package com.hokokrtet.rwguard.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public final class SchedulerUtil {
    private static final boolean IS_FOLIA;
    private static Object globalRegionScheduler = null;
    private static Method globalRunMethod = null;
    private static Object asyncScheduler = null;
    private static Method asyncRunAtFixedRateMethod = null;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
            Method getGlobalSchedulerMethod = Bukkit.class.getMethod("getGlobalRegionScheduler");
            globalRegionScheduler = getGlobalSchedulerMethod.invoke(null);
            globalRunMethod = globalRegionScheduler.getClass().getMethod("run", Plugin.class, java.util.function.Consumer.class);

            Method getAsyncSchedulerMethod = Bukkit.class.getMethod("getAsyncScheduler");
            asyncScheduler = getAsyncSchedulerMethod.invoke(null);
            asyncRunAtFixedRateMethod = asyncScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class, TimeUnit.class);
        } catch (Throwable ignored) {
            folia = false;
        }
        IS_FOLIA = folia;
    }

    private SchedulerUtil() {}

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void runTask(Plugin plugin, Runnable task) {
        if (IS_FOLIA && globalRegionScheduler != null && globalRunMethod != null) {
            try {
                java.util.function.Consumer<Object> consumer = o -> task.run();
                globalRunMethod.invoke(globalRegionScheduler, plugin, consumer);
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runTaskAsync(Plugin plugin, Runnable task) {
        if (IS_FOLIA && asyncScheduler != null) {
            try {
                Method runNowMethod = asyncScheduler.getClass().getMethod("runNow", Plugin.class, java.util.function.Consumer.class);
                java.util.function.Consumer<Object> consumer = o -> task.run();
                runNowMethod.invoke(asyncScheduler, plugin, consumer);
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static void runTimerAsync(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (IS_FOLIA && asyncScheduler != null && asyncRunAtFixedRateMethod != null) {
            try {
                java.util.function.Consumer<Object> consumer = o -> task.run();
                long initialDelayMs = delayTicks * 50L;
                long periodMs = periodTicks * 50L;
                asyncRunAtFixedRateMethod.invoke(asyncScheduler, plugin, consumer, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    public static void dispatchKick(Plugin plugin, Player player, String commandTemplate, String reason) {
        if (player == null) return;

        String safeName = player.getName() != null ? player.getName() : "unknown";
        String safeReason = reason != null ? reason : "Malicious packet behavior";

        // Sanitize reason to prevent command injection
        safeReason = safeReason.replace("\n", " ")
                               .replace("\r", " ")
                               .replace(";", "")
                               .replace("\"", "'")
                               .trim();

        String rawCommand = commandTemplate != null && !commandTemplate.isEmpty()
                ? commandTemplate
                : "kick %player% [RwGuard] %reason%";

        String finalCmd = rawCommand
                .replace("%player%", safeName)
                .replace("%reason%", safeReason);

        // Translate color codes
        finalCmd = org.bukkit.ChatColor.translateAlternateColorCodes('&', finalCmd);

        final String commandToExecute = finalCmd;
        final String fallbackReason = org.bukkit.ChatColor.RED + "[RwGuard] " + safeReason;

        runTask(plugin, () -> {
            if (player.isOnline()) {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute);
                } catch (Throwable t) {
                    try {
                        player.kickPlayer(fallbackReason);
                    } catch (Throwable ignored) {}
                }
            }
        });
    }

    public static void dispatchKick(Plugin plugin, Player player, String command) {
        dispatchKick(plugin, player, command, null);
    }
}
