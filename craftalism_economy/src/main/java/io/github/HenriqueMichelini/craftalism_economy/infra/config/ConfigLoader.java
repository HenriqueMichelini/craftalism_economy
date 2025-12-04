package io.github.HenriqueMichelini.craftalism_economy.infra.config;

import io.github.HenriqueMichelini.craftalism_economy.CraftalismEconomy;

import java.util.Locale;

final class ConfigLoader {
    private final CraftalismEconomy plugin;


    ConfigLoader(CraftalismEconomy plugin) {
        this.plugin = plugin;
    }


    Locale locale() {
        final String localeStr = plugin.getConfig().getString("locale", "en-US");
        try {
            return Locale.forLanguageTag(localeStr.replace('_', '-'));
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid locale '" + localeStr + "', defaulting to en-US");
            return Locale.US;
        }
    }


    String currencySymbol() {
        return plugin.getConfig().getString("currency-symbol", "$");
    }

    String nullRepresentation() { return plugin.getConfig().getString("null-representation", "—"); }


        long defaultBalance() {
            long value = plugin.getConfig().getLong("default-balance", 100_000_000L);
            if (value < 0) {
                plugin.getLogger().warning("Invalid negative default balance, using 100000000");
                return 100_000_000L;
            }
            return value;
        }


        String baseUrl() {
// Keep reading from config; the ConnectionConfig wrapper can be implemented as needed.
            return plugin.getConfig().getString("api-base-url", "http://localhost:8080");
        }
    }