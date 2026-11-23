package com.hokokrtet.rwguard.checks;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.CheckContext;
import com.hokokrtet.rwguard.api.CheckResult;
import com.hokokrtet.rwguard.api.PacketAction;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.checks.book.BookContentSanityCheck;
import com.hokokrtet.rwguard.checks.book.BookEditFloodCheck;
import com.hokokrtet.rwguard.checks.book.LecternCrashCheck;
import com.hokokrtet.rwguard.checks.chat.ChatFloodCheck;
import com.hokokrtet.rwguard.checks.chat.CommandSyntaxCrashCheck;
import com.hokokrtet.rwguard.checks.chat.TabCompleteFloodCheck;
import com.hokokrtet.rwguard.checks.entity.ArmAnimationFloodCheck;
import com.hokokrtet.rwguard.checks.entity.EntityInteractRateCheck;
import com.hokokrtet.rwguard.checks.inventory.AutoRecipeCrashCheck;
import com.hokokrtet.rwguard.checks.inventory.CreativeSlotCrashCheck;
import com.hokokrtet.rwguard.checks.inventory.InvalidSlotClickCheck;
import com.hokokrtet.rwguard.checks.inventory.ItemSwapSpamCheck;
import com.hokokrtet.rwguard.checks.inventory.WindowClickRateCheck;
import com.hokokrtet.rwguard.checks.movement.InvalidPositionCheck;
import com.hokokrtet.rwguard.checks.movement.MovementSpamCheck;
import com.hokokrtet.rwguard.checks.movement.SpectateExploitCheck;
import com.hokokrtet.rwguard.checks.movement.VehicleMovementCheck;
import com.hokokrtet.rwguard.checks.netty.ClientSettingsSanityCheck;
import com.hokokrtet.rwguard.checks.netty.RawBufferSafetyCheck;
import com.hokokrtet.rwguard.checks.netty.RawNbtDecompressorCheck;
import com.hokokrtet.rwguard.checks.payload.ChannelRegisterFloodCheck;
import com.hokokrtet.rwguard.checks.payload.PayloadChannelValidationCheck;
import com.hokokrtet.rwguard.checks.payload.PayloadDataSizeCheck;
import com.hokokrtet.rwguard.checks.ratelimit.GlobalRateLimitCheck;
import com.hokokrtet.rwguard.checks.ratelimit.PacketRateLimitCheck;
import com.hokokrtet.rwguard.checks.sign.SignLengthSanityCheck;
import com.hokokrtet.rwguard.checks.sign.SignUpdateFloodCheck;
import com.hokokrtet.rwguard.checks.world.EndGatewayExploitCheck;
import com.hokokrtet.rwguard.checks.world.ProjectileVelocityCheck;
import com.hokokrtet.rwguard.checks.world.RedstoneLimiterCheck;
import com.hokokrtet.rwguard.models.PlayerData;
import com.hokokrtet.rwguard.utils.ColorUtil;
import com.hokokrtet.rwguard.utils.DiscordWebhookUtil;
import com.hokokrtet.rwguard.utils.PacketDumpManager;
import com.hokokrtet.rwguard.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CheckManager {
    private final RwGuard plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final List<AbstractCheck> checks = new ArrayList<>();

    public CheckManager(RwGuard plugin) {
        this.plugin = plugin;
        registerChecks();
    }

    private void registerChecks() {
        // Rate Limiting
        checks.add(new GlobalRateLimitCheck(plugin));
        checks.add(new PacketRateLimitCheck(plugin));

        // Netty & Buffer
        checks.add(new RawBufferSafetyCheck(plugin));
        checks.add(new RawNbtDecompressorCheck(plugin));
        checks.add(new ClientSettingsSanityCheck(plugin));

        // Movement
        checks.add(new InvalidPositionCheck(plugin));
        checks.add(new MovementSpamCheck(plugin));
        checks.add(new VehicleMovementCheck(plugin));
        checks.add(new SpectateExploitCheck(plugin));

        // Inventory
        checks.add(new InvalidSlotClickCheck(plugin));
        checks.add(new WindowClickRateCheck(plugin));
        checks.add(new CreativeSlotCrashCheck(plugin));
        checks.add(new AutoRecipeCrashCheck(plugin));
        checks.add(new ItemSwapSpamCheck(plugin));

        // Custom Payload
        checks.add(new PayloadChannelValidationCheck(plugin));
        checks.add(new PayloadDataSizeCheck(plugin));
        checks.add(new ChannelRegisterFloodCheck(plugin));

        // Book
        checks.add(new BookEditFloodCheck(plugin));
        checks.add(new BookContentSanityCheck(plugin));
        checks.add(new LecternCrashCheck(plugin));

        // Sign
        checks.add(new SignUpdateFloodCheck(plugin));
        checks.add(new SignLengthSanityCheck(plugin));

        // Entity
        checks.add(new EntityInteractRateCheck(plugin));
        checks.add(new ArmAnimationFloodCheck(plugin));

        // Chat & Commands
        checks.add(new ChatFloodCheck(plugin));
        checks.add(new CommandSyntaxCrashCheck(plugin));
        checks.add(new TabCompleteFloodCheck(plugin));

        // World & Redstone
        checks.add(new EndGatewayExploitCheck(plugin));
        checks.add(new ProjectileVelocityCheck(plugin));
        checks.add(new RedstoneLimiterCheck(plugin));
    }

    public CheckResult processPacket(CheckContext context) {
        plugin.getMetrics().recordPacket();

        PlayerData data = context.getPlayerData();
        if (data.isFrozen()) {
            return CheckResult.cancel("Player is frozen by administrator");
        }

        for (AbstractCheck check : checks) {
            if (!check.isEnabled()) continue;
            // Match category or process global/ratelimit
            if (check.getCategory() != PacketCategory.OTHER &&
                check.getCategory() != context.getCategory() &&
                !(check instanceof GlobalRateLimitCheck) &&
                !(check instanceof PacketRateLimitCheck)) {
                continue;
            }

            CheckResult result = check.process(context);
            if (result.getAction() != PacketAction.ALLOW) {
                if (PacketDumpManager.isDumping(context.getPlayerUuid())) {
                    PacketDumpManager.recordPacket(plugin, context.getPlayerUuid(), context.getPacketName(), context.getByteSize(), result.getAction().name(), result.getReason());
                }

                plugin.getMetrics().recordViolation();
                data.addViolation(check.getName(), result.getReason(), result.getSeverity());

                // Broadcast alert if configured
                if (plugin.getConfigManager().isAlertBroadcast()) {
                    broadcastAlert(context.getPlayer(), check.getName(), result.getReason(), data.getViolationScore());
                }

                // Threshold kick check
                if (data.getViolationScore() >= plugin.getConfigManager().getMaxViolationsBeforeKick()) {
                    return CheckResult.kick("Exceeded maximum violation threshold (" + data.getViolationScore() + ")");
                }

                return result;
            }
        }

        if (PacketDumpManager.isDumping(context.getPlayerUuid())) {
            PacketDumpManager.recordPacket(plugin, context.getPlayerUuid(), context.getPacketName(), context.getByteSize(), "ALLOW", null);
        }

        return CheckResult.allow();
    }

    private void broadcastAlert(Player player, String checkName, String reason, int score) {
        String safePlayer = player != null && player.getName() != null ? player.getName() : "Unknown";
        String safeCheck = checkName != null ? checkName : "General";
        String safeReason = reason != null ? reason : "Suspicious activity";

        DiscordWebhookUtil.sendAlert(plugin, safePlayer, safeCheck, safeReason, score);

        String msg = plugin.getMessageManager().get("alerts.format",
                "%player%", safePlayer,
                "%check%", safeCheck,
                "%reason%", safeReason,
                "%score%", String.valueOf(score)
        );
        String perm = plugin.getConfigManager().getAlertPermission();
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin != null && admin.isOnline() && admin.hasPermission(perm)) {
                admin.sendMessage(msg);
                if (plugin.getConfigManager().isPlayAlertSound()) {
                    SoundUtil.playAlertSound(admin);
                }
            }
        }
    }

    public PlayerData getPlayerData(UUID uuid, String name) {
        return playerDataMap.computeIfAbsent(uuid, u -> new PlayerData(u, name));
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void removePlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public Map<UUID, PlayerData> getAllPlayerData() {
        return Collections.unmodifiableMap(playerDataMap);
    }

    public List<AbstractCheck> getChecks() {
        return Collections.unmodifiableList(checks);
    }

    public <T extends AbstractCheck> T getCheck(Class<T> checkClass) {
        for (AbstractCheck check : checks) {
            if (checkClass.isInstance(check)) {
                return checkClass.cast(check);
            }
        }
        return null;
    }
}
