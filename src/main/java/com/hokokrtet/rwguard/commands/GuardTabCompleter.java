package com.hokokrtet.rwguard.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GuardTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "stats", "check", "freeze", "unfreeze", "dump", "clear", "help");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("rwguard.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, completions);
            Collections.sort(completions);
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("check") || sub.equals("freeze") || sub.equals("unfreeze") || sub.equals("clear") || sub.equals("dump")) {
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    playerNames.add(p.getName());
                }
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
                Collections.sort(completions);
                return completions;
            }
        }

        return Collections.emptyList();
    }
}
