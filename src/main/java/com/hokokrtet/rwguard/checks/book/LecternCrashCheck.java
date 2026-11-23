package com.hokokrtet.rwguard.checks.book;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class LecternCrashCheck extends AbstractCheck {
    public LecternCrashCheck(RwGuard plugin) {
        super(plugin, "LecternCrash", PacketCategory.INVENTORY);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("WINDOW_CLICK") && !context.getPacketName().contains("ContainerButtonClick")) {
            return CheckResult.allow();
        }

        Player player = context.getPlayer();
        if (player == null) return CheckResult.allow();

        // Check if player is interacting with a Lectern and spamming button clicks
        if (player.getOpenInventory() != null && player.getOpenInventory().getTitle().toLowerCase().contains("lectern")) {
            PlayerData data = context.getPlayerData();
            PacketCounter counter = data.getCounter("LECTERN_CLICK");
            int count = counter.increment(1);
            if (count > 15) {
                return CheckResult.drop("Lectern interaction spam (" + count + "/sec)", 3);
            }
        }

        return CheckResult.allow();
    }
}
