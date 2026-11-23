package com.hokokrtet.rwguard.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundUtil {
    private SoundUtil() {}

    public static void playAlertSound(Player player) {
        if (player == null || !player.isOnline()) return;
        try {
            Sound sound = getSound("BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING");
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.5f);
            }
        } catch (Throwable ignored) {}
    }

    public static void playWarningSound(Player player) {
        if (player == null || !player.isOnline()) return;
        try {
            Sound sound = getSound("ENTITY_VILLAGER_NO", "VILLAGER_NO");
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1.0f, 0.8f);
            }
        } catch (Throwable ignored) {}
    }

    private static Sound getSound(String modern, String legacy) {
        try {
            return Sound.valueOf(modern);
        } catch (IllegalArgumentException e) {
            try {
                return Sound.valueOf(legacy);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
