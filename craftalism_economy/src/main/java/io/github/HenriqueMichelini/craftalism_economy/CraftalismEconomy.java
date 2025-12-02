package io.github.HenriqueMichelini.craftalism_economy;

import com.google.gson.Gson;
import io.github.HenriqueMichelini.craftalism_economy.application.service.*;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.LogManager;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.PluginLogger;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.BalanceMessages;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.BaltopMessages;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.CurrencyMessages;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.PayMessages;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.repository.BalanceCacheRepository;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.repository.PlayerCacheRepository;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.BalanceApiService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.PlayerApiService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.TransactionApiService;
import io.github.HenriqueMichelini.craftalism_economy.infra.config.ConnectionConfig;
import io.github.HenriqueMichelini.craftalism_economy.presentation.commands.BalanceCommand;
import io.github.HenriqueMichelini.craftalism_economy.presentation.commands.BaltopCommand;
import io.github.HenriqueMichelini.craftalism_economy.presentation.commands.PayCommand;
import io.github.HenriqueMichelini.craftalism_economy.presentation.validation.PlayerNameCheck;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Objects;

public final class CraftalismEconomy extends JavaPlugin {
    private LogManager logManager;
    private PluginLogger logger;

    private PayMessages payMessages;
    private BalanceMessages balanceMessages;
    private BaltopMessages baltopMessages;

    private final PlayerNameCheck playerNameCheck = new PlayerNameCheck();

    private CurrencyFormatter currencyFormatter;
    private CurrencyParser currencyParser;

    private ConnectionConfig connectionConfig;
    private String baseUrl;

    private final Gson gson = new Gson();

    private HttpClientService client;

    private final PlayerCacheRepository playerCacheRepository = new PlayerCacheRepository();
    private final BalanceCacheRepository balanceCacheRepository = new BalanceCacheRepository();

    private PlayerApiService playerApiService;
    private BalanceApiService balanceApiService;
    private TransactionApiService transactionApiService;

    private PlayerApplicationService playerApplicationService;
    private PayCommandApplicationService payCommandApplicationService;
    private BalanceApplicationService balanceApplicationService;
    private BalanceCommandApplicationService balanceCommandApplicationService;
    private BaltopCommandApplicationService baltopCommandApplicationService;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        this.logManager = new LogManager(this);
        this.logger = new PluginLogger(this, logManager);

        this.payMessages = new PayMessages(logger);
        this.balanceMessages = new BalanceMessages(logger);
        this.baltopMessages = new BaltopMessages(logger);

        initializeMoneyFormat();
        this.currencyParser = new CurrencyParser(new CurrencyMessages(logger));

        this.connectionConfig = new ConnectionConfig(this);
        this.baseUrl = connectionConfig.getUrl();

        this.client = new HttpClientService(baseUrl);

        this.playerApiService = new PlayerApiService(client, gson);
        this.balanceApiService = new BalanceApiService(client);
        this.transactionApiService = new TransactionApiService(client);

        this.playerApplicationService = new PlayerApplicationService(playerApiService, playerCacheRepository);
        this.payCommandApplicationService = new PayCommandApplicationService(playerApplicationService, playerApiService, balanceApiService, transactionApiService);
        this.balanceApplicationService = new BalanceApplicationService(balanceApiService, balanceCacheRepository);
        this.balanceCommandApplicationService = new BalanceCommandApplicationService(playerApplicationService, balanceApplicationService);
        this.baltopCommandApplicationService = new BaltopCommandApplicationService(balanceApiService, playerApiService);

        registerCommands();

        getLogger().info("Plugin enabled successfully");
    }

    private void initializeMoneyFormat() {
        String localeStr = getConfig().getString("locale", "en-US");
        String currencySymbol = getConfig().getString("currency-symbol", "$");
        String nullRep = getConfig().getString("null-representation", "—");

        this.currencyFormatter = new CurrencyFormatter(
                parseLocale(localeStr),
                currencySymbol,
                nullRep,
                this

        );
    }

    private Locale parseLocale(String localeStr) {
        try {
            return Locale.forLanguageTag(localeStr.replace('_', '-'));
        } catch (Exception e) {
            getLogger().warning("Invalid locale '" + localeStr + "', defaulting to en-US");
            return Locale.US;
        }
    }

    private long parseDefaultBalance() {
        long balance = getConfig().getLong("default-balance", 100000000);
        if (balance < 0) {
            getLogger().warning("Invalid negative default balance, using 10000.0000");
            balance = 100000000;
        }
        return balance;
    }

    private void registerCommands() {
        registerCommand("pay", new PayCommand(payMessages, payCommandApplicationService, playerNameCheck));
        registerCommand("balance", new BalanceCommand(balanceMessages, playerNameCheck, balanceCommandApplicationService));
        registerCommand("baltop", new BaltopCommand(baltopMessages, baltopCommandApplicationService, currencyFormatter));
//        registerCommand("setbalance", new SetBalanceCommand(balanceManager, this, currencyFormatter, playerValidator, currencyParser));
    }

    private void registerCommand(String name, CommandExecutor executor) {
        try {
            Objects.requireNonNull(getCommand(name), "Command not registered: " + name)
                    .setExecutor(executor);
            getLogger().fine("Registered command: /" + name);
        } catch (NullPointerException e) {
            getLogger().warning("Failed to register command: /" + name);
        }
    }

    @Override
    public void onDisable() {

    }

    public CurrencyFormatter getCurrencyFormatter() {
        return currencyFormatter;
    }

    public CurrencyParser getCurrencyParser() { return currencyParser; }
}