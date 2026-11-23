package com.hokokrtet.rwguard.checks.inventory;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.AbstractCheck;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class CreativeSlotCrashCheck extends AbstractCheck {
    public CreativeSlotCrashCheck(RwGuard plugin) {
        super(plugin, "CreativeSlotCrash", PacketCategory.INVENTORY);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigManager().isCreativeSlotValidation();
    }

    @Override
    public CheckResult process(CheckContext context) {
        if (!context.getPacketName().contains("SET_CREATIVE_SLOT") && !context.getPacketName().contains("SetCreativeModeSlot")) {
            return CheckResult.allow();
        }

        Player player = context.getPlayer();
        if (player != null && player.getGameMode() != GameMode.CREATIVE) {
            return CheckResult.cancel("Creative slot packet sent by non-creative player", 4);
        }

        Object packet = context.getRawPacket();
        if (packet == null) return CheckResult.allow();

        try {
            Class<?> clazz = packet.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(packet);
                if (val instanceof ItemStack) {
                    ItemStack item = (ItemStack) val;
                    if (item.getAmount() < 0 || item.getAmount() > plugin.getConfigManager().getCreativeMaxStackSize()) {
                        return CheckResult.cancel("Illegal item amount in creative packet: " + item.getAmount(), 3);
                    }

                    // Deep ItemMeta validation (SkullOwner, Lore, DisplayName)
                    if (item.hasItemMeta()) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            if (meta.hasDisplayName() && meta.getDisplayName().length() > 128) {
                                return CheckResult.cancel("Oversized item display name in creative packet", 3);
                            }
                            if (meta.hasLore()) {
                                List<String> lore = meta.getLore();
                                if (lore != null && (lore.size() > 20 || lore.toString().length() > 2048)) {
                                    return CheckResult.cancel("Oversized item lore in creative packet", 3);
                                }
                            }
                            if (meta instanceof SkullMeta) {
                                SkullMeta skull = (SkullMeta) meta;
                                if (skull.hasOwner() && skull.getOwner() != null && skull.getOwner().length() > 64) {
                                    return CheckResult.cancel("Oversized skull owner string crash exploit", 4);
                                }
                            }
                            if (meta instanceof PotionMeta) {
                                PotionMeta potion = (PotionMeta) meta;
                                for (PotionEffect effect : potion.getCustomEffects()) {
                                    if (effect.getDuration() > 1000000 || effect.getDuration() < 0 || effect.getAmplifier() > 127 || effect.getAmplifier() < 0) {
                                        return CheckResult.cancel("Illegal potion effect duration/amplifier crash exploit", 4);
                                    }
                                }
                            }
                        }
                    }
                } else if (val != null && val.getClass().getSimpleName().equals("ItemStack")) {
                    // NMS ItemStack inspection
                    try {
                        Method getCountMethod = val.getClass().getMethod("getCount");
                        int count = (int) getCountMethod.invoke(val);
                        if (count < 0 || count > plugin.getConfigManager().getCreativeMaxStackSize()) {
                            return CheckResult.cancel("Illegal NMS item stack count: " + count, 3);
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        return CheckResult.allow();
    }
}
