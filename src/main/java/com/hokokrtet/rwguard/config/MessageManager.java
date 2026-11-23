package com.hokokrtet.rwguard.config;

import com.hokokrtet.rwguard.RwGuard;
import com.hokokrtet.rwguard.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private final RwGuard plugin;
    private final Map<String, String> messages = new HashMap<>();

    public MessageManager(RwGuard plugin) {
        this.plugin = plugin;
        load();
    }

    public synchronized void load() {
        messages.clear();
        File file = new File(plugin.getDataFolder(), "message.yml");
        if (!file.exists()) {
            plugin.saveResource("message.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Copy defaults from jar if missing
        InputStream defaultStream = plugin.getResource("message.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }

        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, ColorUtil.color(config.getString(key)));
            }
        }
    }

    public String get(String key) {
        return messages.getOrDefault(key, "§c[Missing message: " + key + "]");
    }

    public String get(String key, String... placeholders) {
        String msg = get(key);
        if (placeholders == null || placeholders.length < 2) {
            return msg;
        }
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            if (placeholder != null) {
                msg = msg.replace(placeholder, value != null ? value : "");
            }
        }
        return msg;
    }
}
