package com.hokokrtet.rwguard.config;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.api.PacketAction;
import com.hokokrtet.rwguard.api.PacketCategory;
import com.hokokrtet.rwguard.models.PacketLimitRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private final RwGuard plugin;

    private String engine;
    private boolean debug;
    private String alertPermission;
    private boolean alertBroadcast;
    private boolean playAlertSound;
    private int globalPacketLimit;
    private int globalByteLimit;
    private int maxViolationsBeforeKick;
    private String kickCommand;

    // Discord Webhook
    private boolean discordWebhookEnabled;
    private String discordWebhookUrl;

    // Module flags and thresholds
    private int movementMaxPps;
    private double movementMaxCoord;
    private int vehicleMaxPps;

    private int inventoryMaxClicksPps;
    private boolean creativeSlotValidation;
    private int creativeMaxStackSize;
    private int creativeMaxNbtBytes;
    private boolean autoRecipeLimitEnabled;
    private int autoRecipeMaxPps;
    private boolean printerModeTolerance;

    private int payloadMaxSizeBytes;
    private int payloadMaxChannels;
    private boolean payloadStrictChannels;
    private Set<String> allowedChannels = new HashSet<>();

    private int bookMaxPages;
    private int bookMaxPageLength;
    private int bookMaxEditsPps;

    private int signMaxLineLength;
    private int signMaxUpdatesPps;

    private int entityMaxInteractsPps;
    private int entityMaxArmAnimationPps;

    private int chatMaxPps;
    private int commandMaxPps;
    private int tabCompleteMaxPps;
    private int commandMaxLength;
    private boolean stripPapiPlaceholders;

    private boolean redstoneLimiterEnabled;
    private int redstoneMaxPerChunk;
    private int redstoneWindowSeconds;

    // Bedrock & Geyser
    private boolean bedrockEnabled;
    private double bedrockMultiplier;
    private boolean bedrockRelaxVehicle;

    // NBT Decompressor Bomb Protection
    private boolean nbtDecompressorCheckEnabled;
    private int nbtMaxDecompressedBytes;
    private int nbtMaxDepth;

    // Anarchy Mitigations (AEF)
    private boolean fireworkLimiterEnabled;
    private int fireworkMaxPower;
    private int fireworkMaxEffects;
    private boolean fallingBlockLimiterEnabled;
    private int fallingBlockMaxPerChunk;
    private boolean interactSpamLimiterEnabled;
    private int interactSpamMaxPps;

    private final Map<String, PacketLimitRule> packetRules = new ConcurrentHashMap<>();

    public ConfigManager(RwGuard plugin) {
        this.plugin = plugin;
        load();
    }

    public synchronized void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.engine = config.getString("engine", "AUTO").toUpperCase(Locale.ROOT);
        this.debug = config.getBoolean("debug", false);
        this.alertPermission = config.getString("alerts.permission", "rwguard.alerts");
        this.alertBroadcast = config.getBoolean("alerts.broadcast-to-admins", true);
        this.playAlertSound = config.getBoolean("alerts.play-sound", true);
        this.globalPacketLimit = config.getInt("global-ratelimit.max-packets-per-second", 1200);
        this.globalByteLimit = config.getInt("global-ratelimit.max-bytes-per-second", 262144);
        this.maxViolationsBeforeKick = config.getInt("punishments.max-violations-before-kick", 15);
        this.kickCommand = config.getString("punishments.kick-command", "kick %player% &c[RwGuard] Malicious packet flood detected");

        // Discord Webhook
        this.discordWebhookEnabled = config.getBoolean("discord-webhook.enabled", false);
        this.discordWebhookUrl = config.getString("discord-webhook.url", "");

        // Movement
        this.movementMaxPps = config.getInt("modules.movement.max-packets-per-sec", 300);
        this.movementMaxCoord = config.getDouble("modules.movement.max-coordinate", 30000000.0);
        this.vehicleMaxPps = config.getInt("modules.movement.max-vehicle-pps", 150);

        // Inventory
        this.inventoryMaxClicksPps = config.getInt("modules.inventory.max-clicks-per-sec", 45);
        this.creativeSlotValidation = config.getBoolean("modules.inventory.creative-validation", true);
        this.creativeMaxStackSize = config.getInt("modules.inventory.creative-max-stack", 64);
        this.creativeMaxNbtBytes = config.getInt("modules.inventory.creative-max-nbt-bytes", 16384);
        this.autoRecipeLimitEnabled = config.getBoolean("modules.inventory.auto-recipe-limiter", true);
        this.autoRecipeMaxPps = config.getInt("modules.inventory.auto-recipe-max-pps", 15);
        this.printerModeTolerance = config.getBoolean("modules.inventory.printer-mode-tolerance", true);

        // Payload
        this.payloadMaxSizeBytes = config.getInt("modules.payload.max-size-bytes", 32768);
        this.payloadMaxChannels = config.getInt("modules.payload.max-registered-channels", 128);
        this.payloadStrictChannels = config.getBoolean("modules.payload.strict-channel-whitelist", false);
        this.allowedChannels = new HashSet<>(config.getStringList("modules.payload.whitelist"));

        // Book
        this.bookMaxPages = config.getInt("modules.book.max-pages", 50);
        this.bookMaxPageLength = config.getInt("modules.book.max-page-length", 512);
        this.bookMaxEditsPps = config.getInt("modules.book.max-edits-per-sec", 5);

        // Sign
        this.signMaxLineLength = config.getInt("modules.sign.max-line-length", 384);
        this.signMaxUpdatesPps = config.getInt("modules.sign.max-updates-per-sec", 10);

        // Entity
        this.entityMaxInteractsPps = config.getInt("modules.entity.max-interact-per-sec", 80);
        this.entityMaxArmAnimationPps = config.getInt("modules.entity.max-arm-animation-per-sec", 100);

        // Chat
        this.chatMaxPps = config.getInt("modules.chat.max-chat-per-sec", 10);
        this.commandMaxPps = config.getInt("modules.chat.max-command-per-sec", 20);
        this.tabCompleteMaxPps = config.getInt("modules.chat.max-tab-complete-per-sec", 30);
        this.commandMaxLength = config.getInt("modules.chat.max-command-length", 256);
        this.stripPapiPlaceholders = config.getBoolean("modules.chat.strip-placeholders", true);

        // Redstone
        this.redstoneLimiterEnabled = config.getBoolean("modules.redstone.enabled", true);
        this.redstoneMaxPerChunk = config.getInt("modules.redstone.max-updates-per-chunk", 500);
        this.redstoneWindowSeconds = config.getInt("modules.redstone.window-seconds", 2);

        // Bedrock
        this.bedrockEnabled = config.getBoolean("modules.bedrock.enabled", true);
        this.bedrockMultiplier = config.getDouble("modules.bedrock.packet-multiplier", 2.0);
        this.bedrockRelaxVehicle = config.getBoolean("modules.bedrock.relax-vehicle-checks", true);

        // NBT Decompressor Bomb
        this.nbtDecompressorCheckEnabled = config.getBoolean("modules.nbt-protection.enabled", true);
        this.nbtMaxDecompressedBytes = config.getInt("modules.nbt-protection.max-decompressed-bytes", 524288);
        this.nbtMaxDepth = config.getInt("modules.nbt-protection.max-depth", 32);

        // Anarchy Mitigations (AEF)
        this.fireworkLimiterEnabled = config.getBoolean("modules.anarchy.firework-limiter.enabled", true);
        this.fireworkMaxPower = config.getInt("modules.anarchy.firework-limiter.max-power", 3);
        this.fireworkMaxEffects = config.getInt("modules.anarchy.firework-limiter.max-effects", 8);
        this.fallingBlockLimiterEnabled = config.getBoolean("modules.anarchy.falling-block-limiter.enabled", true);
        this.fallingBlockMaxPerChunk = config.getInt("modules.anarchy.falling-block-limiter.max-per-chunk", 40);
        this.interactSpamLimiterEnabled = config.getBoolean("modules.anarchy.interact-spam-limiter.enabled", true);
        this.interactSpamMaxPps = config.getInt("modules.anarchy.interact-spam-limiter.max-clicks-per-sec", 8);

        // Load packet rules
        packetRules.clear();
        ConfigurationSection rulesSec = config.getConfigurationSection("packet-rules");
        if (rulesSec != null) {
            for (String key : rulesSec.getKeys(false)) {
                ConfigurationSection sec = rulesSec.getConfigurationSection(key);
                if (sec == null) continue;
                String packetName = key.toUpperCase(Locale.ROOT);
                int maxPerSecond = sec.getInt("max-per-second", 100);
                int maxPerWindow = sec.getInt("max-per-window", 100);
                int windowSeconds = sec.getInt("window-seconds", 1);
                String actionStr = sec.getString("action", "CANCEL").toUpperCase(Locale.ROOT);
                PacketAction action;
                try {
                    action = PacketAction.valueOf(actionStr);
                } catch (IllegalArgumentException e) {
                    action = PacketAction.CANCEL;
                }
                String catStr = sec.getString("category", "OTHER").toUpperCase(Locale.ROOT);
                PacketCategory category;
                try {
                    category = PacketCategory.valueOf(catStr);
                } catch (IllegalArgumentException e) {
                    category = PacketCategory.OTHER;
                }
                boolean alert = sec.getBoolean("alert", true);
                packetRules.put(packetName, new PacketLimitRule(packetName, maxPerSecond, maxPerWindow, windowSeconds, action, category, alert));
            }
        }
    }

    public String getEngine() { return engine; }
    public boolean isDebug() { return debug; }
    public String getAlertPermission() { return alertPermission; }
    public boolean isAlertBroadcast() { return alertBroadcast; }
    public boolean isPlayAlertSound() { return playAlertSound; }
    public int getGlobalPacketLimit() { return globalPacketLimit; }
    public int getGlobalByteLimit() { return globalByteLimit; }
    public int getMaxViolationsBeforeKick() { return maxViolationsBeforeKick; }
    public String getKickCommand() { return kickCommand; }
    public boolean isDiscordWebhookEnabled() { return discordWebhookEnabled; }
    public String getDiscordWebhookUrl() { return discordWebhookUrl; }
    public boolean isStripPapiPlaceholders() { return stripPapiPlaceholders; }

    public int getMovementMaxPps() { return movementMaxPps; }
    public double getMovementMaxCoord() { return movementMaxCoord; }
    public int getVehicleMaxPps() { return vehicleMaxPps; }

    public int getInventoryMaxClicksPps() { return inventoryMaxClicksPps; }
    public boolean isCreativeSlotValidation() { return creativeSlotValidation; }
    public int getCreativeMaxStackSize() { return creativeMaxStackSize; }
    public int getCreativeMaxNbtBytes() { return creativeMaxNbtBytes; }
    public boolean isAutoRecipeLimitEnabled() { return autoRecipeLimitEnabled; }
    public int getAutoRecipeMaxPps() { return autoRecipeMaxPps; }
    public boolean isPrinterModeTolerance() { return printerModeTolerance; }

    public int getPayloadMaxSizeBytes() { return payloadMaxSizeBytes; }
    public int getPayloadMaxChannels() { return payloadMaxChannels; }
    public boolean isPayloadStrictChannels() { return payloadStrictChannels; }
    public Set<String> getAllowedChannels() { return allowedChannels; }

    public int getBookMaxPages() { return bookMaxPages; }
    public int getBookMaxPageLength() { return bookMaxPageLength; }
    public int getBookMaxEditsPps() { return bookMaxEditsPps; }

    public int getSignMaxLineLength() { return signMaxLineLength; }
    public int getSignMaxUpdatesPps() { return signMaxUpdatesPps; }

    public int getEntityMaxInteractsPps() { return entityMaxInteractsPps; }
    public int getEntityMaxArmAnimationPps() { return entityMaxArmAnimationPps; }

    public int getChatMaxPps() { return chatMaxPps; }
    public int getCommandMaxPps() { return commandMaxPps; }
    public int getTabCompleteMaxPps() { return tabCompleteMaxPps; }
    public int getCommandMaxLength() { return commandMaxLength; }

    public boolean isRedstoneLimiterEnabled() { return redstoneLimiterEnabled; }
    public int getRedstoneMaxPerChunk() { return redstoneMaxPerChunk; }
    public int getRedstoneWindowSeconds() { return redstoneWindowSeconds; }

    public boolean isBedrockEnabled() { return bedrockEnabled; }
    public double getBedrockMultiplier() { return bedrockMultiplier; }
    public boolean isBedrockRelaxVehicle() { return bedrockRelaxVehicle; }

    public boolean isNbtDecompressorCheckEnabled() { return nbtDecompressorCheckEnabled; }
    public int getNbtMaxDecompressedBytes() { return nbtMaxDecompressedBytes; }
    public int getNbtMaxDepth() { return nbtMaxDepth; }

    public boolean isFireworkLimiterEnabled() { return fireworkLimiterEnabled; }
    public int getFireworkMaxPower() { return fireworkMaxPower; }
    public int getFireworkMaxEffects() { return fireworkMaxEffects; }
    public boolean isFallingBlockLimiterEnabled() { return fallingBlockLimiterEnabled; }
    public int getFallingBlockMaxPerChunk() { return fallingBlockMaxPerChunk; }
    public boolean isInteractSpamLimiterEnabled() { return interactSpamLimiterEnabled; }
    public int getInteractSpamMaxPps() { return interactSpamMaxPps; }

    public PacketLimitRule getRule(String packetName) {
        return packetRules.get(packetName.toUpperCase(Locale.ROOT));
    }

    public Map<String, PacketLimitRule> getAllRules() {
        return Collections.unmodifiableMap(packetRules);
    }
}
