package com.hokokrtet.rwguard.models;

import com.hokokrtet.rwguard.api.PacketAction;
import com.hokokrtet.rwguard.api.PacketCategory;

public class PacketLimitRule {
    private final String packetName;
    private final int maxPerSecond;
    private final int maxPerWindow;
    private final int windowSeconds;
    private final PacketAction action;
    private final PacketCategory category;
    private final boolean alert;

    public PacketLimitRule(String packetName, int maxPerSecond, int maxPerWindow, int windowSeconds, PacketAction action, PacketCategory category, boolean alert) {
        this.packetName = packetName;
        this.maxPerSecond = maxPerSecond;
        this.maxPerWindow = maxPerWindow;
        this.windowSeconds = windowSeconds > 0 ? windowSeconds : 1;
        this.action = action != null ? action : PacketAction.CANCEL;
        this.category = category != null ? category : PacketCategory.OTHER;
        this.alert = alert;
    }

    public PacketLimitRule(int limit, boolean cancelOnly) {
        this("UNKNOWN", limit, limit, 1, cancelOnly ? PacketAction.CANCEL : PacketAction.KICK, PacketCategory.OTHER, true);
    }

    public String getPacketName() {
        return packetName;
    }

    public int getMaxPerSecond() {
        return maxPerSecond;
    }

    public int getMaxPerWindow() {
        return maxPerWindow;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public PacketAction getAction() {
        return action;
    }

    public PacketCategory getCategory() {
        return category;
    }

    public boolean isAlert() {
        return alert;
    }
}
