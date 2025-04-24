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
import java.util.logging.Level;

/**
 * Manages player balances and handles persistence in a YAML file.
 * All currency values are stored with 2 decimal places precision.
 */
public class EconomyManager {
    private static final String LOG_PREFIX = "[EconomyManager] ";
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final File balancesFile;
    private final FileConfiguration balancesConfig;
    private final HashMap<UUID, BigDecimal> balances = new HashMap<>();
    private final JavaPlugin plugin;

    private final BigDecimal defaultBalance;

    public EconomyManager(JavaPlugin plugin, BigDecimal defaultBalance) {
        this.plugin = plugin;
        this.balancesFile = new File(plugin.getDataFolder(), "balances.yml");
        this.defaultBalance = defaultBalance.setScale(2, RoundingMode.HALF_UP);
        this.balancesConfig = YamlConfiguration.loadConfiguration(balancesFile);
        createBalancesFile();
        loadBalances();
    }

    private void createBalancesFile() {
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

    private void initializeEmptyBalances() throws IOException {
        balancesConfig.set("balances", new HashMap<>());
        balancesConfig.save(balancesFile);
    }

    /**
     * Loads player balances from the storage file.
     */
    public void loadBalances() {
        try {
            balancesConfig.load(balancesFile);
            ConfigurationSection balancesSection = balancesConfig.getConfigurationSection("balances");

            balances.clear();
            if (balancesSection != null) {
                loadValidBalances(balancesSection);
            }

            // Add default balance for new players (optional)
            plugin.getLogger().info("Loaded " + balances.size() + " balances");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load balances", e);
        }
    }

    private void loadValidBalances(ConfigurationSection balancesSection) {
        for (String key : balancesSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                BigDecimal balance = BigDecimal.valueOf(balancesSection.getDouble(key))
                        .setScale(SCALE, ROUNDING_MODE);
                balances.put(uuid, balance);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(LOG_PREFIX + "Invalid UUID format: " + key);
            }
        }
    }

    /**
     * Saves player balances to the storage file.
     */
    public void saveBalances() {
        try {
            balancesConfig.set("balances", null);
            ConfigurationSection balancesSection = balancesConfig.createSection("balances");

            for (Map.Entry<UUID, BigDecimal> entry : balances.entrySet()) {
                balancesSection.set(entry.getKey().toString(), entry.getValue().doubleValue());
            }

            balancesConfig.save(balancesFile);
            plugin.getLogger().info(LOG_PREFIX + "Saved " + balances.size() + " balances");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, LOG_PREFIX + "Failed to save balances", e);
        }
    }

    /**
     * Gets the balance for a player.
     * @param playerUUID Player's UUID
     * @return Balance rounded to 2 decimal places, or zero if not found
     */
    public BigDecimal getBalance(UUID playerUUID) {
        return balances.getOrDefault(playerUUID, defaultBalance);
    }

    /**
     * Checks if a player has insufficient funds for a specified amount.
     * @param playerUUID Player's UUID
     * @param amount Amount to check
     * @return True if balance < amount
     */
    public boolean hasInsufficientFunds(UUID playerUUID, BigDecimal amount) {
        return getBalance(playerUUID).compareTo(amount) < 0;
    }

    /**
     * Sets a player's balance.
     * @param playerUUID Player's UUID
     * @param amount New balance value (will be rounded to 2 decimal places)
     */
    public void setBalance(UUID playerUUID, BigDecimal amount) {
        balances.put(playerUUID, amount.setScale(SCALE, ROUNDING_MODE));
    }

    /**
     * Deposits funds to a player's account.
     * @param playerUUID Player's UUID
     * @param amount Positive amount to deposit
     * @return True if successful, false if invalid amount
     */
    public boolean deposit(UUID playerUUID, BigDecimal amount) {
        if (isInvalidAmount(amount)) return false;
        setBalance(playerUUID, getBalance(playerUUID).add(amount));
        return true;
    }

    /**
     * Withdraws funds from a player's account.
     * @param playerUUID Player's UUID
     * @param amount Positive amount to withdraw
     * @return True if successful, false if invalid amount or insufficient funds
     */
    public boolean withdraw(UUID playerUUID, BigDecimal amount) {
        if (isInvalidAmount(amount) || hasInsufficientFunds(playerUUID, amount)) return false;
        setBalance(playerUUID, getBalance(playerUUID).subtract(amount));
        return true;
    }

    /**
     * Transfers funds between players.
     * @param from Sender's UUID
     * @param to Receiver's UUID
     * @param amount Positive amount to transfer
     * @return True if successful, false if invalid amount or insufficient funds
     */
    public boolean transferBalance(UUID from, UUID to, BigDecimal amount) {
        if (isInvalidAmount(amount) || hasInsufficientFunds(from, amount)) return false;

        withdraw(from, amount);
        deposit(to, amount);
        return true;
    }

    /**
     * Gets a copy of all balances.
     * @return Unmodifiable map of all balances
     */
    public Map<UUID, BigDecimal> getAllBalances() {
        return new HashMap<>(balances);
    }

    private boolean isInvalidAmount(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) <= 0;
    }
}