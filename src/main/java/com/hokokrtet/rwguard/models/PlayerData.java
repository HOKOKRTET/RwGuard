package com.hokokrtet.rwguard.models;

import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerData {
    private final UUID uuid;
    private final String name;
    private final long joinTime;

    private final Map<String, PacketCounter> packetCounters = new ConcurrentHashMap<>();
    private final PacketCounter totalPacketCounter = new PacketCounter();
    private final AtomicInteger violationScore = new AtomicInteger(0);
    private final List<ViolationRecord> recentViolations = Collections.synchronizedList(new LinkedList<>());

    private volatile Location lastLocation;
    private volatile long lastLocationTime;
    private final AtomicLong lastWindowClick = new AtomicLong(0);
    private final AtomicLong lastBookEdit = new AtomicLong(0);
    private final AtomicLong lastSignEdit = new AtomicLong(0);
    private final AtomicLong lastChat = new AtomicLong(0);
    private final AtomicLong lastCommand = new AtomicLong(0);

    private final Set<String> registeredChannels = ConcurrentHashMap.newKeySet();
    private volatile boolean frozen = false;
    private volatile boolean bedrock = false;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.joinTime = System.currentTimeMillis();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public PacketCounter getCounter(String packetName) {
        return packetCounters.computeIfAbsent(packetName, k -> new PacketCounter());
    }

    public PacketCounter getTotalPacketCounter() {
        return totalPacketCounter;
    }

    public Map<String, PacketCounter> getAllCounters() {
        return packetCounters;
    }

    public void addViolation(String check, String details, int severity) {
        violationScore.addAndGet(severity);
        recentViolations.add(new ViolationRecord(check, details, severity));
        if (recentViolations.size() > 50) {
            recentViolations.remove(0);
        }
    }

    public int getViolationScore() {
        return violationScore.get();
    }

    public List<ViolationRecord> getRecentViolations() {
        return new ArrayList<>(recentViolations);
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
        this.lastLocationTime = System.currentTimeMillis();
    }

    public long getLastLocationTime() {
        return lastLocationTime;
    }

    public AtomicLong getLastWindowClick() {
        return lastWindowClick;
    }

    public AtomicLong getLastBookEdit() {
        return lastBookEdit;
    }

    public AtomicLong getLastSignEdit() {
        return lastSignEdit;
    }

    public AtomicLong getLastChat() {
        return lastChat;
    }

    public AtomicLong getLastCommand() {
        return lastCommand;
    }

    public Set<String> getRegisteredChannels() {
        return registeredChannels;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public boolean isBedrock() {
        return bedrock;
    }

    public void setBedrock(boolean bedrock) {
        this.bedrock = bedrock;
    }

    public void resetAllCounters() {
        totalPacketCounter.reset();
        for (PacketCounter counter : packetCounters.values()) {
            counter.reset();
        }
    }
}
