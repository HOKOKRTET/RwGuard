package com.hokokrtet.rwguard.utils;

import org.bukkit.Bukkit;

public final class VersionUtil {
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;
    private static final String VERSION_STRING;

    static {
        VERSION_STRING = Bukkit.getBukkitVersion();
        int major = 1;
        int minor = 16;
        try {
            String[] parts = VERSION_STRING.split("-")[0].split("\\.");
            if (parts.length >= 2) {
                major = Integer.parseInt(parts[0]);
                minor = Integer.parseInt(parts[1]);
            } else if (parts.length == 1) {
                major = Integer.parseInt(parts[0]);
                minor = 0;
            }
        } catch (Exception ignored) {
            major = 1;
            minor = 16;
        }
        MAJOR_VERSION = major;
        MINOR_VERSION = minor;
    }

    private VersionUtil() {}

    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }

    public static int getMinorVersion() {
        return MINOR_VERSION;
    }

    public static String getVersionString() {
        return VERSION_STRING;
    }

    public static boolean isAtLeast(int targetMajor, int targetMinor) {
        if (MAJOR_VERSION > targetMajor) return true;
        if (MAJOR_VERSION == targetMajor) return MINOR_VERSION >= targetMinor;
        return false;
    }

    public static boolean isAtLeast(int minor) {
        if (MAJOR_VERSION > 1) return true; // 26.x+ is higher than 1.x
        return MINOR_VERSION >= minor;
    }

    public static boolean isAtLeast1_17() {
        return isAtLeast(17);
    }

    public static boolean isAtLeast1_18() {
        return isAtLeast(18);
    }

    public static boolean isAtLeast1_19() {
        return isAtLeast(19);
    }

    public static boolean isAtLeast1_20() {
        return isAtLeast(20);
    }

    public static boolean isAtLeast1_21() {
        return isAtLeast(21);
    }

    public static boolean isAtLeast26() {
        return MAJOR_VERSION >= 26;
    }
}
