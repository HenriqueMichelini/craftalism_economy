package io.github.HenriqueMichelini.craftalism_economy.economy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private final File balancesFile;
    private final FileConfiguration balancesConfig;
    private final HashMap<UUID, BigDecimal> balances = new HashMap<>(); // Use BigDecimal for balances
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
            BigDecimal balance = BigDecimal.valueOf(balancesConfig.getDouble(key));
            setBalance(playerUUID, balance);
        }

        plugin.getLogger().info("[Economy] Balances loaded from file.");
    }

    // Save balances to balances.yml
    public void saveBalances() {
        for (UUID uuid : balances.keySet()) {
            balancesConfig.set(uuid.toString(), getBalance(uuid).doubleValue());
        }

        try {
            balancesConfig.save(balancesFile);
            plugin.getLogger().info("[Economy] Balances saved to file.");
        } catch (IOException e) {
            plugin.getLogger().severe("[Economy] Failed to save balances file!");
        }
    }

    // Get balance
    public BigDecimal getBalance(UUID playerUUID) {
        return balances.getOrDefault(playerUUID, BigDecimal.ZERO);
    }

    public void setBalance(UUID playerUUID, BigDecimal amount) {
        balances.put(playerUUID, amount.setScale(2, RoundingMode.HALF_UP)); // Round to 2 decimal places
    }

    // Transfer balance
    public void transferBalance(UUID from, UUID to, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || getBalance(from).compareTo(amount) < 0) {
            return;
        }

        setBalance(from, getBalance(from).subtract(amount));
        setBalance(to, getBalance(to).add(amount));
    }

    public Map<UUID, BigDecimal> getAllBalances() {
        return new HashMap<>(balances); // Return a copy of the balances map
    }
}