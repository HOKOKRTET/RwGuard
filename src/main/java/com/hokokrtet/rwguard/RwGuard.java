package com.hokokrtet.rwguard;

import com.hokokrtet.rwguard.checks.CheckManager;
import com.hokokrtet.rwguard.commands.GuardCommand;
import com.hokokrtet.rwguard.commands.GuardTabCompleter;
import com.hokokrtet.rwguard.config.ConfigManager;
import com.hokokrtet.rwguard.config.MessageManager;
import com.hokokrtet.rwguard.listeners.BukkitExploitListener;
import com.hokokrtet.rwguard.listeners.PlayerConnectionListener;
import com.hokokrtet.rwguard.models.PacketMetrics;
import com.hokokrtet.rwguard.models.RedstoneChunkTracker;
import com.hokokrtet.rwguard.network.NetworkManager;
import com.hokokrtet.rwguard.utils.SchedulerUtil;
import com.hokokrtet.rwguard.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class RwGuard extends JavaPlugin {
    private static RwGuard instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private PacketMetrics metrics;
    private CheckManager checkManager;
    private NetworkManager networkManager;
    private BukkitTask cleanupTask;

    public static RwGuard getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();
        getLogger().info("Starting RwGuard Enterprise Engine on Minecraft " + VersionUtil.getVersionString() + "...");

        // 1. Initialize configuration & messages
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.metrics = new PacketMetrics();

        // 2. Initialize check manager
        this.checkManager = new CheckManager(this);

        // 3. Initialize network engine
        this.networkManager = new NetworkManager(this);
        this.networkManager.initialize();

        // 4. Register Bukkit listeners
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BukkitExploitListener(this), this);

        // 5. Register commands
        PluginCommand guardCmd = getCommand("guard");
        if (guardCmd != null) {
            GuardCommand executor = new GuardCommand(this);
            guardCmd.setExecutor(executor);
            guardCmd.setTabCompleter(new GuardTabCompleter());
        }

        // 6. Schedule periodic cleanup task (every 10 seconds)
        SchedulerUtil.runTimerAsync(this, () -> {
            try {
                RedstoneChunkTracker.cleanup(15000L);
            } catch (Throwable ignored) {}
        }, 200L, 200L);

        long took = System.currentTimeMillis() - startTime;
        getLogger().info("RwGuard v" + getDescription().getVersion() + " initialized successfully in " + took + "ms!");
        getLogger().info("Loaded " + checkManager.getChecks().size() + " active exploit protection modules.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down RwGuard...");

        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        if (networkManager != null) {
            networkManager.shutdown();
        }

        RedstoneChunkTracker.reset();
        getLogger().info("RwGuard disabled cleanly.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PacketMetrics getMetrics() {
        return metrics;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }
}
