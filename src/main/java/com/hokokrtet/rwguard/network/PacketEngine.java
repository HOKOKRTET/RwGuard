package com.hokokrtet.rwguard.network;

import com.hokokrtet.rwguard.RwGuard;
import org.bukkit.entity.Player;

public interface PacketEngine {
    void initialize(RwGuard plugin);
    void shutdown();
    void injectPlayer(Player player);
    void uninjectPlayer(Player player);
    String getName();
}
