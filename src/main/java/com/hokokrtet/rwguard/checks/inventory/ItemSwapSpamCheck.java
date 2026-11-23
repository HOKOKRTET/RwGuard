package com.hokokrtet.rwguard.checks.inventory;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;

import java.lang.reflect.Field;

public class ItemSwapSpamCheck extends AbstractCheck {
    public ItemSwapSpamCheck(RwGuard plugin) {
        super(plugin, "ItemSwapSpam", PacketCategory.INVENTORY);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        String name = context.getPacketName();
        if (!name.contains("BLOCK_DIG") && !name.contains("PlayerAction")) {
            return CheckResult.allow();
        }

        Object packet = context.getRawPacket();
        if (packet == null) return CheckResult.allow();

        boolean isSwap = false;
        try {
            Class<?> clazz = packet.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(packet);
                if (val != null && val.getClass().isEnum()) {
                    String enumName = ((Enum<?>) val).name();
                    if (enumName.contains("SWAP")) {
                        isSwap = true;
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (isSwap) {
            PlayerData data = context.getPlayerData();
            PacketCounter counter = data.getCounter("ITEM_SWAP");
            int count = counter.increment(1);
            if (count > 25) {
                return CheckResult.drop("Offhand item swap flood (" + count + "/sec)", 2);
            }
        }

        return CheckResult.allow();
    }
}
