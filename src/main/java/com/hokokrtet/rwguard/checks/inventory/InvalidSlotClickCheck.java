package com.hokokrtet.rwguard.checks.inventory;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

import java.lang.reflect.Field;

public class InvalidSlotClickCheck extends AbstractCheck {
    public InvalidSlotClickCheck(RwGuard plugin) {
        super(plugin, "InvalidSlotClick", PacketCategory.INVENTORY);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("WINDOW_CLICK") && !context.getPacketName().contains("ContainerClick")) {
            return CheckResult.allow();
        }

        Object packet = context.getRawPacket();
        if (packet == null) return CheckResult.allow();

        int slot = -999;
        int windowId = 0;
        try {
            Class<?> clazz = packet.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                String name = f.getName().toLowerCase();
                if (f.getType() == int.class) {
                    if (name.contains("slot")) {
                        slot = f.getInt(packet);
                    } else if (name.contains("container") || name.contains("window")) {
                        windowId = f.getInt(packet);
                    }
                } else if (f.getType() == short.class && name.contains("slot")) {
                    slot = f.getShort(packet);
                }
            }
        } catch (Throwable ignored) {
            return CheckResult.allow();
        }

        Player player = context.getPlayer();
        if (player != null) {
            InventoryView openInventory = player.getOpenInventory();
            if (openInventory != null) {
                int totalSlots = openInventory.countSlots();
                // slot -999 is clicking outside inventory (valid), but other negative numbers or numbers exceeding 128/total are crash exploits
                if (slot != -999 && slot != -1 && (slot < 0 || slot > totalSlots + 36 || slot > 256)) {
                    return CheckResult.cancel("Out of bounds inventory slot (" + slot + ")", 3);
                }
            }
        }

        return CheckResult.allow();
    }
}
