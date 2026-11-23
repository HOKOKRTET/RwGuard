package com.hokokrtet.rwguard.utils;

import com.hokokrtet.rwguard.RwGuard;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class DiscordWebhookUtil {
    private DiscordWebhookUtil() {}

    public static void sendAlert(RwGuard plugin, String playerName, String checkName, String reason, int score) {
        String webhookUrl = plugin.getConfigManager().getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || !plugin.getConfigManager().isDiscordWebhookEnabled()) {
            return;
        }

        SchedulerUtil.runTaskAsync(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "RwGuard-AntiCrash");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setDoOutput(true);

                String json = "{"
                        + "\"username\":\"RwGuard Protection\","
                        + "\"embeds\":[{"
                        + "\"title\":\"⚠️ Packet Exploit Blocked\","
                        + "\"color\":15158332,"
                        + "\"fields\":["
                        + "{\"name\":\"Player\",\"value\":\"" + escapeJson(playerName) + "\",\"inline\":true},"
                        + "{\"name\":\"Check\",\"value\":\"" + escapeJson(checkName) + "\",\"inline\":true},"
                        + "{\"name\":\"Score\",\"value\":\"" + score + "\",\"inline\":true},"
                        + "{\"name\":\"Reason\",\"value\":\"" + escapeJson(reason) + "\",\"inline\":false}"
                        + "]"
                        + "}]"
                        + "}";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode >= 400 && plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().warning("Discord webhook returned code " + responseCode);
                }
                conn.disconnect();
            } catch (Throwable t) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().warning("Failed to send Discord webhook: " + t.getMessage());
                }
            }
        });
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
