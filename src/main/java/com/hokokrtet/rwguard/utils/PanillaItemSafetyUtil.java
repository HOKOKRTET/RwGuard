package com.hokokrtet.rwguard.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.BlockState;
import org.bukkit.loot.Lootable;

import java.lang.reflect.Method;
import java.util.List;

public final class PanillaItemSafetyUtil {

    private PanillaItemSafetyUtil() {}

    /**
     * Checks whether an ItemStack has suspicious or malicious NBT exploits
     * ported from Panilla (FAWE brush exploit, illegal Crossbow charged fireworks,
     * ShulkerBox loot table crashes, etc.).
     *
     * @return null if safe, or a description of the detected exploit.
     */
    public static String validateItemExploits(ItemStack item, int maxFireworkPower, int maxFireworkEffects) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        // 1. FAWE weBrushJson Exploit (Panilla NbtCheck_weBrushJson)
        // Check if item contains weBrushJson in NBT or serialized tag
        if (hasWeBrushJsonTag(item, meta)) {
            return "FAWE brush NBT exploit (weBrushJson)";
        }

        // 2. Crossbow Charged Projectiles Crash (Panilla NbtCheck_ChargedProjectiles)
        if (meta instanceof CrossbowMeta) {
            CrossbowMeta cm = (CrossbowMeta) meta;
            if (cm.hasChargedProjectiles()) {
                List<ItemStack> charged = cm.getChargedProjectiles();
                if (charged != null && charged.size() > 3) {
                    return "Crossbow excessive charged projectiles (" + charged.size() + ")";
                }
                if (charged != null) {
                    for (ItemStack proj : charged) {
                        if (proj != null && proj.hasItemMeta()) {
                            ItemMeta pMeta = proj.getItemMeta();
                            if (pMeta instanceof FireworkMeta) {
                                FireworkMeta fm = (FireworkMeta) pMeta;
                                if (fm.getPower() > maxFireworkPower || fm.getPower() < 0 ||
                                    (fm.getEffects() != null && fm.getEffects().size() > maxFireworkEffects)) {
                                    return "Crossbow charged with illegal crash firework";
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. ShulkerBox / Container Corrupted LootTable Crash (Panilla TileLootTableListener / ContainerCheck)
        if (meta instanceof BlockStateMeta) {
            BlockStateMeta bsm = (BlockStateMeta) meta;
            try {
                BlockState state = bsm.getBlockState();
                if (state instanceof Lootable) {
                    Lootable lootable = (Lootable) state;
                    if (lootable.getLootTable() != null) {
                        try {
                            lootable.getLootTable().getKey();
                        } catch (Throwable t) {
                            // Corrupted loot table detected
                            lootable.setLootTable(null);
                            bsm.setBlockState(state);
                            item.setItemMeta(bsm);
                            return "Corrupted LootTable container crash";
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    /**
     * Inspects item for weBrushJson tag via reflection on NMS or string serialization.
     */
    public static boolean hasWeBrushJsonTag(ItemStack item, ItemMeta meta) {
        try {
            // Fast check in meta / item string representations
            String itemStr = item.toString();
            if (itemStr.contains("weBrushJson")) return true;

            String metaStr = meta.toString();
            if (metaStr.contains("weBrushJson")) return true;

            // Deep reflection check on CraftItemStack -> NMS handle -> CompoundTag
            Class<?> craftItemClass = item.getClass();
            if (craftItemClass.getName().endsWith("CraftItemStack")) {
                Method asNmsMethod = craftItemClass.getMethod("asNMSCopy", ItemStack.class);
                Object nmsItem = asNmsMethod.invoke(null, item);
                if (nmsItem != null) {
                    Method getTagMethod = null;
                    try {
                        getTagMethod = nmsItem.getClass().getMethod("getTag");
                    } catch (NoSuchMethodException e) {
                        // 1.20.5+ components or older methods
                        for (Method m : nmsItem.getClass().getMethods()) {
                            if (m.getName().equals("v") || m.getName().equals("getTag") || m.getName().equals("getComponents")) {
                                getTagMethod = m;
                                break;
                            }
                        }
                    }
                    if (getTagMethod != null) {
                        Object tag = getTagMethod.invoke(nmsItem);
                        if (tag != null && tag.toString().contains("weBrushJson")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
