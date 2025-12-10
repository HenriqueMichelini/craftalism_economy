package io.github.HenriqueMichelini.craftalism_economy;

import io.github.HenriqueMichelini.craftalism_economy.application.service.*;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.PluginLogger;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.*;
import io.github.HenriqueMichelini.craftalism_economy.infra.config.bootstrap.BootContainer;
import io.github.HenriqueMichelini.craftalism_economy.presentation.listeners.OnJoin;
import org.bukkit.plugin.java.JavaPlugin;

public final class CraftalismEconomy extends JavaPlugin {

    private BootContainer container;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        this.container = new BootContainer(this, this);

        this.container.initialize();


        getLogger().info("Plugin enabled successfully");
    }

    @Override
    public void onDisable() {
        container.shutdown();
    }

    public CurrencyFormatter getCurrencyFormatter() {
        return container.getCurrencyFormatter();
    }

    public CurrencyParser getCurrencyParser() {
        return container.getCurrencyParser();
    }

    public PluginLogger getPluginLogger() {
        return container.getPluginLogger();
    }
}
