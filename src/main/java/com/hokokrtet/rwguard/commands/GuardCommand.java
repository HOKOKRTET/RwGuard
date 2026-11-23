package com.hokokrtet.rwguard.commands;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.models.PacketCounter;
import com.hokokrtet.rwguard.models.PlayerData;
import com.hokokrtet.rwguard.models.ViolationRecord;
import com.hokokrtet.rwguard.utils.ColorUtil;
import com.hokokrtet.rwguard.utils.PacketDumpManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class GuardCommand implements CommandExecutor {
    private final RwGuard plugin;

    public GuardCommand(RwGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rwguard.admin")) {
            sender.sendMessage(plugin.getMessageManager().get("no-permission"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload": {
                plugin.getConfigManager().load();
                plugin.getMessageManager().load();
                sender.sendMessage(plugin.getMessageManager().get("reload-success"));
                return true;
            }
            case "stats": {
                sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.color("&6&lRwGuard &7Engine Statistics"));
                sender.sendMessage(ColorUtil.color("&7Active Engine: &e" + plugin.getNetworkManager().getActiveEngine().getName()));
                sender.sendMessage(ColorUtil.color("&7Current Throughput: &a" + plugin.getMetrics().getCurrentPps() + " pps"));
                sender.sendMessage(ColorUtil.color("&7Total Received: &f" + plugin.getMetrics().getTotalReceived()));
                sender.sendMessage(ColorUtil.color("&7Total Dropped: &c" + plugin.getMetrics().getTotalDropped()));
                sender.sendMessage(ColorUtil.color("&7Total Violations: &e" + plugin.getMetrics().getTotalViolations()));
                sender.sendMessage(ColorUtil.color("&7Total Kicks: &4" + plugin.getMetrics().getTotalKicks()));
                sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
                return true;
            }
            case "check": {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /guard check <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtil.color("&cPlayer not found or offline."));
                    return true;
                }
                PlayerData data = plugin.getCheckManager().getPlayerData(target.getUniqueId());
                if (data == null) {
                    sender.sendMessage(ColorUtil.color("&cNo data tracked for player yet."));
                    return true;
                }

                sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.color("&6&lRwGuard &7Status for &e" + target.getName()));
                sender.sendMessage(ColorUtil.color("&7Violation Score: " + (data.getViolationScore() > 0 ? "&c" : "&a") + data.getViolationScore()));
                sender.sendMessage(ColorUtil.color("&7Frozen: &f" + data.isFrozen()));
                sender.sendMessage(ColorUtil.color("&7Recent Violations:"));
                List<ViolationRecord> recents = data.getRecentViolations();
                if (recents.isEmpty()) {
                    sender.sendMessage(ColorUtil.color("  &aNone recorded. Clean."));
                } else {
                    int shown = 0;
                    for (int i = recents.size() - 1; i >= 0 && shown < 5; i--, shown++) {
                        ViolationRecord vr = recents.get(i);
                        sender.sendMessage(ColorUtil.color("  &c[" + vr.getCheckName() + "] &7" + vr.getDetails() + " &8(+" + vr.getSeverity() + ")"));
                    }
                }
                sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
                return true;
            }
            case "freeze": {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /guard freeze <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtil.color("&cPlayer not found."));
                    return true;
                }
                PlayerData data = plugin.getCheckManager().getPlayerData(target.getUniqueId(), target.getName());
                data.setFrozen(true);
                sender.sendMessage(ColorUtil.color("&aPlayer &e" + target.getName() + " &apackets are now frozen!"));
                return true;
            }
            case "unfreeze": {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /guard unfreeze <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtil.color("&cPlayer not found."));
                    return true;
                }
                PlayerData data = plugin.getCheckManager().getPlayerData(target.getUniqueId(), target.getName());
                data.setFrozen(false);
                sender.sendMessage(ColorUtil.color("&aPlayer &e" + target.getName() + " &ais no longer frozen."));
                return true;
            }
            case "clear": {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /guard clear <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtil.color("&cPlayer not found."));
                    return true;
                }
                PlayerData data = plugin.getCheckManager().getPlayerData(target.getUniqueId());
                if (data != null) {
                    data.resetAllCounters();
                    sender.sendMessage(ColorUtil.color("&aCleared packet counters and violations for &e" + target.getName()));
                }
                return true;
            }
            case "dump": {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /guard dump <player> [packetCount]"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtil.color("&cPlayer not found."));
                    return true;
                }
                int count = 100;
                if (args.length >= 3) {
                    try {
                        count = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {}
                }
                PacketDumpManager.startDump(target, sender, count);
                sender.sendMessage(ColorUtil.color("&aRecording next &e" + count + " &apackets from &e" + target.getName() + "&a..."));
                return true;
            }
            default: {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.color("&6&lRwGuard &7Commands Help:"));
        sender.sendMessage(ColorUtil.color("&e/guard reload &7- Reload configuration & messages"));
        sender.sendMessage(ColorUtil.color("&e/guard stats &7- View live server packet engine metrics"));
        sender.sendMessage(ColorUtil.color("&e/guard check <player> &7- Inspect player packet rates & violations"));
        sender.sendMessage(ColorUtil.color("&e/guard freeze <player> &7- Freeze all incoming packets from player"));
        sender.sendMessage(ColorUtil.color("&e/guard unfreeze <player> &7- Unfreeze player"));
        sender.sendMessage(ColorUtil.color("&e/guard dump <player> [count] &7- Dump raw packet log for analysis"));
        sender.sendMessage(ColorUtil.color("&e/guard clear <player> &7- Reset violations and counters"));
        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
    }
}
