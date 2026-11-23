package com.hokokrtet.rwguard.checks.movement;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;
import org.bukkit.entity.Player;

public class VehicleMovementCheck extends AbstractCheck {
    public VehicleMovementCheck(RwGuard plugin) {
        super(plugin, "VehicleMovement", PacketCategory.MOVEMENT);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().getVehicleMaxPps() > 0;
    }

    @Override
    public CheckResult process(CheckContext context) {
        String packetName = context.getPacketName();
        if (!packetName.contains("VEHICLE")) {
            return CheckResult.allow();
        }

        Player player = context.getPlayer();
        PlayerData data = context.getPlayerData();

        if (data.isBedrock() && plugin.getConfigManager().isBedrockEnabled() && plugin.getConfigManager().isBedrockRelaxVehicle()) {
            return CheckResult.allow();
        }

        if (player != null && !player.isInsideVehicle()) {
            return CheckResult.cancel("Vehicle movement packet sent while not in vehicle", 2);
        }

        PacketCounter counter = data.getCounter("VEHICLE_MOVE");
        int count = counter.increment(1);
        int max = plugin.getConfigManager().getVehicleMaxPps();

        if (count > max) {
            return CheckResult.drop("Vehicle packet spam (" + count + "/" + max + " pps)", 2);
        }

        return CheckResult.allow();
    }
}
