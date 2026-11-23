package com.hokokrtet.rwguard.utils;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([a-fA-F0-9]{6})>");

    private ColorUtil() {}

    public static String color(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Support &#RRGGBB
        Matcher matcher1 = HEX_PATTERN_1.matcher(text);
        StringBuffer buffer1 = new StringBuffer();
        while (matcher1.find()) {
            String hex = matcher1.group(1);
            matcher1.appendReplacement(buffer1, toBungeeHex(hex));
        }
        matcher1.appendTail(buffer1);
        text = buffer1.toString();

        // Support <#RRGGBB>
        Matcher matcher2 = HEX_PATTERN_2.matcher(text);
        StringBuffer buffer2 = new StringBuffer();
        while (matcher2.find()) {
            String hex = matcher2.group(1);
            matcher2.appendReplacement(buffer2, toBungeeHex(hex));
        }
        matcher2.appendTail(buffer2);
        text = buffer2.toString();

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String toBungeeHex(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }
}
