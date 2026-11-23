package com.hokokrtet.rwguard.utils;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectionUtil {
    private static volatile Method getHandleMethod = null;
    private static volatile Field connectionField = null;
    private static volatile Field networkManagerField = null;
    private static volatile Field channelField = null;
    private static volatile boolean initialized = false;

    private ReflectionUtil() {}

    private static synchronized void initReflection(Player player) {
        if (initialized) return;
        try {
            Class<?> craftPlayerClass = player.getClass();
            getHandleMethod = craftPlayerClass.getMethod("getHandle");
            Object entityPlayer = getHandleMethod.invoke(player);
            Class<?> entityPlayerClass = entityPlayer.getClass();

            // Search for connection/playerConnection field
            Field connField = findFieldByType(entityPlayerClass, "PlayerConnection");
            if (connField == null) {
                connField = findFieldByType(entityPlayerClass, "ServerGamePacketListenerImpl");
            }
            if (connField == null) {
                for (Field f : entityPlayerClass.getFields()) {
                    if (f.getType().getSimpleName().contains("Connection") || f.getType().getSimpleName().contains("Listener")) {
                        connField = f;
                        break;
                    }
                }
            }
            if (connField == null) {
                for (Field f : entityPlayerClass.getDeclaredFields()) {
                    if (f.getType().getSimpleName().contains("Connection") || f.getType().getSimpleName().contains("Listener")) {
                        connField = f;
                        break;
                    }
                }
            }

            if (connField != null) {
                connField.setAccessible(true);
                connectionField = connField;
                Object connectionObj = connField.get(entityPlayer);
                if (connectionObj != null) {
                    Class<?> connClass = connectionObj.getClass();

                    Field netMgrField = findFieldByType(connClass, "NetworkManager");
                    if (netMgrField == null) {
                        netMgrField = findFieldByType(connClass, "Connection");
                    }
                    if (netMgrField == null) {
                        for (Field f : connClass.getDeclaredFields()) {
                            if (f.getType().getSimpleName().equalsIgnoreCase("NetworkManager") || f.getType().getSimpleName().equalsIgnoreCase("Connection")) {
                                netMgrField = f;
                                break;
                            }
                        }
                    }

                    if (netMgrField != null) {
                        netMgrField.setAccessible(true);
                        networkManagerField = netMgrField;
                        Object netMgrObj = netMgrField.get(connectionObj);
                        if (netMgrObj != null) {
                            Class<?> netMgrClass = netMgrObj.getClass();

                            for (Field f : netMgrClass.getDeclaredFields()) {
                                if (Channel.class.isAssignableFrom(f.getType())) {
                                    f.setAccessible(true);
                                    channelField = f;
                                    break;
                                }
                            }
                            if (channelField == null) {
                                for (Field f : netMgrClass.getFields()) {
                                    if (Channel.class.isAssignableFrom(f.getType())) {
                                        f.setAccessible(true);
                                        channelField = f;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            initialized = true;
        }
    }

    private static Field findFieldByType(Class<?> clazz, String typeName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (f.getType().getSimpleName().equalsIgnoreCase(typeName)) {
                    f.setAccessible(true);
                    return f;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    public static Channel getChannel(Player player) {
        if (player == null) return null;
        if (!initialized) {
            initReflection(player);
        }
        try {
            if (getHandleMethod == null) return null;
            Object entityPlayer = getHandleMethod.invoke(player);
            if (entityPlayer == null) return null;

            if (connectionField == null) return null;
            Object connection = connectionField.get(entityPlayer);
            if (connection == null) return null;

            if (networkManagerField == null) return null;
            Object networkManager = networkManagerField.get(connection);
            if (networkManager == null) return null;

            if (channelField == null) return null;
            return (Channel) channelField.get(networkManager);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
