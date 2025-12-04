package io.github.HenriqueMichelini.craftalism_economy.infra.config.bootstrap;

import io.github.HenriqueMichelini.craftalism_economy.CraftalismEconomy;

import java.util.Objects;

final class BootContainer {
    private final CraftalismEconomy plugin;


    // factories / services
    private final ConfigLoader configLoader;
    private final FormatterFactory formatterFactory;
    private final ApiServiceFactory apiServiceFactory;
    private final ApplicationServiceFactory applicationServiceFactory;
    private final CommandRegistrar commandRegistrar;


    BootContainer(CraftalismEconomy plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configLoader = new ConfigLoader(plugin);
        this.formatterFactory = new FormatterFactory(configLoader, plugin);
        this.apiServiceFactory = new ApiServiceFactory(configLoader);
        this.applicationServiceFactory = new ApplicationServiceFactory(apiServiceFactory);
        this.commandRegistrar = new CommandRegistrar(plugin, applicationServiceFactory, formatterFactory);
    }


    void initialize() {
// Perform any initialization steps that may be required in the future
// e.g. start health checks, metrics, schedule tasks
    }


    void shutdown() {
// Flush caches, persist state, stop schedulers, etc
        applicationServiceFactory.shutdown();
    }


    FormatterFactory getFormatterFactory() { return formatterFactory; }
    CommandRegistrar getCommandRegistrar() { return commandRegistrar; }
}