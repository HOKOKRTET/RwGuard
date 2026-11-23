package com.hokokrtet.rwguard.network.netty;

import com.hokokrtet.rwguard.api.PacketCategory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class NettyPacketClassifier {
    private static final Map<String, String> CANONICAL_MAP = new HashMap<>();
    private static final Map<String, PacketCategory> CATEGORY_MAP = new HashMap<>();

    static {
        // Movement
        register("PacketPlayInFlying", "FLYING", PacketCategory.MOVEMENT);
        register("PacketPlayInPosition", "POSITION", PacketCategory.MOVEMENT);
        register("PacketPlayInPositionLook", "POSITION_LOOK", PacketCategory.MOVEMENT);
        register("PacketPlayInLook", "LOOK", PacketCategory.MOVEMENT);
        register("PacketPlayInVehicleMove", "VEHICLE_MOVE", PacketCategory.MOVEMENT);
        register("PacketPlayInSteerVehicle", "STEER_VEHICLE", PacketCategory.MOVEMENT);

        // Modern 1.17+ Mojang/Paper names
        register("ServerboundMovePlayerPacket$PosRot", "POSITION_LOOK", PacketCategory.MOVEMENT);
        register("ServerboundMovePlayerPacket$Pos", "POSITION", PacketCategory.MOVEMENT);
        register("ServerboundMovePlayerPacket$Rot", "LOOK", PacketCategory.MOVEMENT);
        register("ServerboundMovePlayerPacket$StatusOnly", "FLYING", PacketCategory.MOVEMENT);
        register("ServerboundMovePlayerPacket", "FLYING", PacketCategory.MOVEMENT);
        register("ServerboundMoveVehiclePacket", "VEHICLE_MOVE", PacketCategory.MOVEMENT);
        register("ServerboundPlayerInputPacket", "STEER_VEHICLE", PacketCategory.MOVEMENT);

        // Inventory
        register("PacketPlayInWindowClick", "WINDOW_CLICK", PacketCategory.INVENTORY);
        register("ServerboundContainerClickPacket", "WINDOW_CLICK", PacketCategory.INVENTORY);
        register("PacketPlayInSetCreativeSlot", "SET_CREATIVE_SLOT", PacketCategory.INVENTORY);
        register("ServerboundSetCreativeModeSlotPacket", "SET_CREATIVE_SLOT", PacketCategory.INVENTORY);
        register("PacketPlayInCloseWindow", "CLOSE_WINDOW", PacketCategory.INVENTORY);
        register("ServerboundContainerClosePacket", "CLOSE_WINDOW", PacketCategory.INVENTORY);
        register("PacketPlayInAutoRecipe", "AUTO_RECIPE", PacketCategory.INVENTORY);
        register("ServerboundPlaceRecipePacket", "AUTO_RECIPE", PacketCategory.INVENTORY);
        register("PacketPlayInEnchantItem", "ENCHANT_ITEM", PacketCategory.INVENTORY);
        register("ServerboundContainerButtonClickPacket", "ENCHANT_ITEM", PacketCategory.INVENTORY);

        // Custom Payload
        register("PacketPlayInCustomPayload", "CUSTOM_PAYLOAD", PacketCategory.CUSTOM_PAYLOAD);
        register("ServerboundCustomPayloadPacket", "CUSTOM_PAYLOAD", PacketCategory.CUSTOM_PAYLOAD);

        // Book & Sign
        register("PacketPlayInUpdateSign", "UPDATE_SIGN", PacketCategory.SIGN);
        register("ServerboundSignUpdatePacket", "UPDATE_SIGN", PacketCategory.SIGN);
        register("PacketPlayInBEdit", "B_EDIT", PacketCategory.BOOK);
        register("ServerboundEditBookPacket", "B_EDIT", PacketCategory.BOOK);

        // Entity & Interaction
        register("PacketPlayInUseEntity", "USE_ENTITY", PacketCategory.ENTITY);
        register("ServerboundInteractPacket", "USE_ENTITY", PacketCategory.ENTITY);
        register("PacketPlayInArmAnimation", "ARM_ANIMATION", PacketCategory.ENTITY);
        register("ServerboundSwingPacket", "ARM_ANIMATION", PacketCategory.ENTITY);
        register("PacketPlayInUseItem", "USE_ITEM", PacketCategory.ENTITY);
        register("ServerboundUseItemPacket", "USE_ITEM", PacketCategory.ENTITY);
        register("PacketPlayInBlockPlace", "BLOCK_PLACE", PacketCategory.ENTITY);
        register("ServerboundUseItemOnPacket", "BLOCK_PLACE", PacketCategory.ENTITY);
        register("PacketPlayInBlockDig", "BLOCK_DIG", PacketCategory.ENTITY);
        register("ServerboundPlayerActionPacket", "BLOCK_DIG", PacketCategory.ENTITY);

        // Chat & Commands
        register("PacketPlayInChat", "CHAT", PacketCategory.CHAT);
        register("ServerboundChatPacket", "CHAT", PacketCategory.CHAT);
        register("ServerboundChatCommandPacket", "CHAT_COMMAND", PacketCategory.CHAT);
        register("PacketPlayInTabComplete", "TAB_COMPLETE", PacketCategory.CHAT);
        register("ServerboundCommandSuggestionPacket", "TAB_COMPLETE", PacketCategory.CHAT);

        // Settings / Teleport Confirm / Keepalive
        register("PacketPlayInSettings", "SETTINGS", PacketCategory.OTHER);
        register("ServerboundClientInformationPacket", "SETTINGS", PacketCategory.OTHER);
        register("PacketPlayInTeleportAccept", "TELEPORT_ACCEPT", PacketCategory.MOVEMENT);
        register("ServerboundAcceptTeleportationPacket", "TELEPORT_ACCEPT", PacketCategory.MOVEMENT);
        register("PacketPlayInKeepAlive", "KEEP_ALIVE", PacketCategory.OTHER);
        register("ServerboundKeepAlivePacket", "KEEP_ALIVE", PacketCategory.OTHER);
        register("PacketPlayInAbilities", "ABILITIES", PacketCategory.OTHER);
        register("ServerboundPlayerAbilitiesPacket", "ABILITIES", PacketCategory.OTHER);
        register("PacketPlayInHeldItemSlot", "HELD_ITEM_SLOT", PacketCategory.INVENTORY);
        register("ServerboundSetCarriedItemPacket", "HELD_ITEM_SLOT", PacketCategory.INVENTORY);

        // Modern 1.21.2+ / 26.x additions
        register("ServerboundPlayerLoadedPacket", "PLAYER_LOADED", PacketCategory.OTHER);
        register("ServerboundPaddleBoatPacket", "PADDLE_BOAT", PacketCategory.MOVEMENT);
        register("ServerboundSelectKnownPacks", "KNOWN_PACKS", PacketCategory.OTHER);
        register("ServerboundConfigurationAcknowledgedPacket", "CONFIG_ACK", PacketCategory.OTHER);
    }

    private static void register(String className, String canonical, PacketCategory category) {
        CANONICAL_MAP.put(className.toLowerCase(Locale.ROOT), canonical);
        CATEGORY_MAP.put(canonical, category);
    }

    private NettyPacketClassifier() {}

    public static String classify(Object packet) {
        if (packet == null) return "UNKNOWN";
        String simpleName = packet.getClass().getSimpleName();
        String lower = simpleName.toLowerCase(Locale.ROOT);
        String match = CANONICAL_MAP.get(lower);
        if (match != null) return match;

        // Try outer class if inner (e.g. ServerboundMovePlayerPacket$Pos)
        String canonicalName = packet.getClass().getName();
        if (canonicalName.contains("$")) {
            String shortInner = canonicalName.substring(canonicalName.lastIndexOf('.') + 1);
            match = CANONICAL_MAP.get(shortInner.toLowerCase(Locale.ROOT));
            if (match != null) return match;
        }

        return simpleName.toUpperCase(Locale.ROOT);
    }

    public static PacketCategory getCategory(String canonicalName) {
        return CATEGORY_MAP.getOrDefault(canonicalName, PacketCategory.OTHER);
    }
}
