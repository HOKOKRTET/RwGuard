package com.hokokrtet.rwguard.utils;

import com.hokokrtet.rwguard.RwGuard;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PacketDumpManager {
    private static final Map<UUID, DumpSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private PacketDumpManager() {}

    public static boolean startDump(Player target, CommandSender requester, int packetCount) {
        if (target == null) return false;
        DumpSession session = new DumpSession(target.getUniqueId(), target.getName(), requester, Math.min(packetCount, 500));
        ACTIVE_SESSIONS.put(target.getUniqueId(), session);
        return true;
    }

    public static boolean isDumping(UUID uuid) {
        return uuid != null && ACTIVE_SESSIONS.containsKey(uuid);
    }

    public static void stopDump(UUID uuid) {
        if (uuid != null) {
            ACTIVE_SESSIONS.remove(uuid);
        }
    }

    public static void recordPacket(RwGuard plugin, UUID uuid, String packetName, int bytes, String status, String reason) {
        DumpSession session = ACTIVE_SESSIONS.get(uuid);
        if (session == null) return;

        String line = String.format("[%s] %-8s %-32s (%d bytes) %s",
                DATE_FORMAT.format(Instant.now()),
                status,
                packetName,
                bytes,
                reason != null ? "[" + reason + "]" : ""
        );
        session.lines.add(line);

        if (session.remaining.decrementAndGet() <= 0) {
            ACTIVE_SESSIONS.remove(uuid);
            saveDump(plugin, session);
        }
    }

    private static void saveDump(RwGuard plugin, DumpSession session) {
        SchedulerUtil.runTaskAsync(plugin, () -> {
            try {
                File dir = new File(plugin.getDataFolder(), "dumps");
                if (!dir.exists()) dir.mkdirs();

                String filename = String.format("dump_%s_%d.log", session.targetName, System.currentTimeMillis());
                File file = new File(dir, filename);

                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    writer.println("# RwGuard Packet Dump");
                    writer.println("# Target: " + session.targetName + " (" + session.targetUuid + ")");
                    writer.println("# Total Packets: " + session.lines.size());
                    writer.println("# Time: " + new Date());
                    writer.println("# -------------------------------------------------------------");
                    for (String l : session.lines) {
                        writer.println(l);
                    }
                }

                if (session.requester != null) {
                    session.requester.sendMessage(ColorUtil.color(
                            "&a[RwGuard] Packet dump for &e" + session.targetName +
                            " &asaved to &fdumps/" + filename +
                            " &8(&e" + session.lines.size() + " &7packets&8)"
                    ));
                }
            } catch (Throwable t) {
                if (session.requester != null) {
                    session.requester.sendMessage(ColorUtil.color("&c[RwGuard] Failed to save packet dump: " + t.getMessage()));
                }
            }
        });
    }

    private static class DumpSession {
        final UUID targetUuid;
        final String targetName;
        final CommandSender requester;
        final AtomicInteger remaining;
        final List<String> lines = Collections.synchronizedList(new ArrayList<>());

        DumpSession(UUID targetUuid, String targetName, CommandSender requester, int count) {
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.requester = requester;
            this.remaining = new AtomicInteger(count);
        }
    }
}
