package io.github.HenriqueMichelini.craftalism_economy.economy;

import org.bukkit.configuration.ConfigurationSection;
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
    private final HashMap<UUID, BigDecimal> balances = new HashMap<>();
    private final JavaPlugin plugin;

    public EconomyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.balancesFile = new File(plugin.getDataFolder(), "balances.yml");
        this.balancesConfig = YamlConfiguration.loadConfiguration(balancesFile);
        createBalancesFile();
        loadBalances();
    }

    private void createBalancesFile() {
        try {
            if (!balancesFile.exists()) {
                plugin.getDataFolder().mkdirs();
                balancesFile.createNewFile();
                plugin.getLogger().info("[Economy] Created new balances.yml file");

                // Initialize with empty balances section
                balancesConfig.set("balances", new HashMap<>());
                balancesConfig.save(balancesFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("[Economy] Failed to create balances.yml: " + e.getMessage());
        }
    }

    public void loadBalances() {
        try {
            balancesConfig.load(balancesFile); // Reload to get latest changes
            ConfigurationSection balancesSection = balancesConfig.getConfigurationSection("balances");
            if (balancesSection != null) {
                for (String key : balancesSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        BigDecimal balance = BigDecimal.valueOf(balancesSection.getDouble(key));
                        balances.put(uuid, balance);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID: " + key);
                    }
                }
            }
            plugin.getLogger().info("[Economy] Loaded " + balances.size() + " balances");
        } catch (Exception e) {
            plugin.getLogger().severe("[Economy] Failed to load balances: " + e.getMessage());
        }
    }

    public void saveBalances() {
        try {
            // Clear existing balances
            balancesConfig.set("balances", null);

            // Create new balances section
            ConfigurationSection balancesSection = balancesConfig.createSection("balances");
            for (Map.Entry<UUID, BigDecimal> entry : balances.entrySet()) {
                balancesSection.set(entry.getKey().toString(), entry.getValue().doubleValue());
            }

            balancesConfig.save(balancesFile);
            plugin.getLogger().info("[Economy] Saved " + balances.size() + " balances");
        } catch (IOException e) {
            plugin.getLogger().severe("[Economy] Failed to save balances: " + e.getMessage());
        }
    }

    // Get balance
    public BigDecimal getBalance(UUID playerUUID) {
        return balances.getOrDefault(playerUUID, BigDecimal.ZERO);
    }

    // Check if player has sufficient balance
    public boolean hasBalance(UUID playerUUID, BigDecimal amount) {
        return getBalance(playerUUID).compareTo(amount) >= 0;
    }

    // Set balance
    public void setBalance(UUID playerUUID, BigDecimal amount) {
        balances.put(playerUUID, amount.setScale(2, RoundingMode.HALF_UP)); // Round to 2 decimal places
    }

    // Deposit money
    public boolean deposit(UUID playerUUID, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        setBalance(playerUUID, getBalance(playerUUID).add(amount));
        return true;
    }

    // Withdraw money
    public boolean withdraw(UUID playerUUID, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || !hasBalance(playerUUID, amount)) {
            return false;
        }
        setBalance(playerUUID, getBalance(playerUUID).subtract(amount));
        return true;
    }

    // Transfer balance
    public boolean transferBalance(UUID from, UUID to, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || !hasBalance(from, amount)) {
            return false;
        }

        setBalance(from, getBalance(from).subtract(amount));
        setBalance(to, getBalance(to).add(amount));
        return true;
    }

    // Get all balances
    public Map<UUID, BigDecimal> getAllBalances() {
        return new HashMap<>(balances); // Return a copy of the balances map
    }
}