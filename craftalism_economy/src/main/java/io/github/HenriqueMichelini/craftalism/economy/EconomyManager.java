package io.github.HenriqueMichelini.craftalism.economy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private final File balancesFile;
    private final FileConfiguration balancesConfig;
    private final HashMap<UUID, Double> balances = new HashMap<>(); // Initialize the balances map
    private final JavaPlugin plugin; // Reference to the plugin

    public EconomyManager(JavaPlugin plugin) {
        this.plugin = plugin; // Store the plugin instance
        this.balancesFile = new File(plugin.getDataFolder(), "balances.yml");
        this.balancesConfig = YamlConfiguration.loadConfiguration(balancesFile);
    }

    // Load balances from balances.yml
    public void loadBalances() {
        if (!balancesFile.exists()) {
            return;
        }

        for (String key : balancesConfig.getKeys(false)) {
            UUID playerUUID = UUID.fromString(key);
            double balance = balancesConfig.getDouble(key);
            setBalance(playerUUID, balance);
        }

        plugin.getLogger().info("[Economy] Balances loaded from file.");
    }

    // Save balances to balances.yml
    public void saveBalances() {
        for (UUID uuid : balances.keySet()) {
            balancesConfig.set(uuid.toString(), getBalance(uuid));
        }

        try {
            balancesConfig.save(balancesFile);
            plugin.getLogger().info("[Economy] Balances saved to file.");
        } catch (IOException e) {
            plugin.getLogger().severe("[Economy] Failed to save balances file!");
        }
    }

    // Get balance
    public double getBalance(UUID playerUUID) {
        return balances.getOrDefault(playerUUID, 0.0);
    }

    public void setBalance(UUID playerUUID, double amount) {
        balances.put(playerUUID, roundToTwoDecimalPlaces(Math.max(amount, 0.0))); // Prevent negative balances
    }

    private double roundToTwoDecimalPlaces(double value) {
        return Math.round(value * 100.0) / 100.0; // Rounds to 2 decimal places
    }

    // Transfer balance
    public void transferBalance(UUID from, UUID to, double amount) {
        if (amount <= 0 || getBalance(from) < amount) {
            return;
        }

        setBalance(from, getBalance(from) - amount);
        setBalance(to, getBalance(to) + amount);
    }

    public Map<UUID, Double> getAllBalances() {
        return new HashMap<>(balances); // Return a copy of the balances map
    }
}
