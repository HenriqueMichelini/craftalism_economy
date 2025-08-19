package io.github.HenriqueMichelini.craftalism_economy.economy.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class BalanceManager {
    private static final String LOG_PREFIX = "[BalanceManager] ";

    private final File balancesFile;
    private final FileConfiguration balancesConfig;
    private final HashMap<UUID, Long> allBalances = new HashMap<>();
    private final long defaultBalance;
    private final JavaPlugin plugin;


    public BalanceManager(File balancesFile, FileConfiguration balancesConfig, long defaultBalance, JavaPlugin plugin) {
        this.balancesFile = balancesFile;
        this.balancesConfig = balancesConfig;
        this.defaultBalance = defaultBalance;
        this.plugin = plugin;

        createBalancesFile();
        loadBalances();
    }

    public void createBalancesFile() {
        try {
            if (!balancesFile.exists()) {
                if (balancesFile.createNewFile()) {
                    plugin.getLogger().info(LOG_PREFIX + "Created new balances.yml file");
                    initializeEmptyBalances();
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, LOG_PREFIX + "Failed to create balances.yml", e);
        }
    }

    public void initializeEmptyBalances() throws IOException {
        balancesConfig.set("balances", new HashMap<>());
        balancesConfig.save(balancesFile);
    }

    public void loadBalances() {
        try {
            balancesConfig.load(balancesFile);
            ConfigurationSection balancesSection = balancesConfig.getConfigurationSection("balances");

            allBalances.clear();
            if (balancesSection != null) {
                loadValidBalances(balancesSection);
            }

            plugin.getLogger().info("Loaded " + allBalances.size() + " balances");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load balances", e);
        }
    }

    public void loadValidBalances(ConfigurationSection balancesSection) {
        for (String key : balancesSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long balance = balancesSection.getLong(key);
                allBalances.put(uuid, balance);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(LOG_PREFIX + "Invalid UUID format: " + key);
            }
        }
    }

    public void saveBalances() {
        try {
            balancesConfig.set("balances", null);
            ConfigurationSection balancesSection = balancesConfig.createSection("balances");

            for (Map.Entry<UUID, Long> entry : allBalances.entrySet()) {
                balancesSection.set(entry.getKey().toString(), entry.getValue().doubleValue());
            }

            balancesConfig.save(balancesFile);
            plugin.getLogger().info(LOG_PREFIX + "Saved " + allBalances.size() + " balances");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, LOG_PREFIX + "Failed to save balances", e);
        }
    }

    public boolean checkIfBalanceExists(UUID playerUUID) {
        return allBalances.containsKey(playerUUID);
    }

    public Long getBalance(UUID playerUUID) {
        return allBalances.get(playerUUID);
    }

    public void setBalance(UUID playerUUID, long amount) {
        allBalances.put(playerUUID, amount);
    }

    public HashMap<UUID, Long> getAllBalances() {
        return this.allBalances;
    }
}
