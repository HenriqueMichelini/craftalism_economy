package io.github.HenriqueMichelini.craftalism_economy.core.logs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LogManager {

    private final JavaPlugin plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public LogManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createLogsFileIfNotExists();
        loadLogs();
    }

    private void createLogsFileIfNotExists() {
        messagesFile = new File(plugin.getDataFolder(), "logs.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("logs.yml", false);
        }
    }

    public void loadLogs() {
        messagesFile = new File(plugin.getDataFolder(), "logs.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String path, Placeholder... placeholders) {
        if (messagesConfig == null) return "Logs not loaded!";
        String template = messagesConfig.getString(path, "Message not found: " + path);

        if (placeholders != null) {
            for (Placeholder placeholder : placeholders) {
                template = template.replace("{" + placeholder.key() + "}", placeholder.value());
            }
        }

        String prefix = messagesConfig.getString("prefix", "");
        return prefix + template;
    }

    public record Placeholder(String key, String value) {}
}
