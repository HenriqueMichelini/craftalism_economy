package io.github.HenriqueMichelini.craftalism_economy.application.service;

import io.github.HenriqueMichelini.craftalism_economy.infra.api.repository.BalanceCacheRepository;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.repository.PlayerCacheRepository;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.ApiServiceFactory;

public final class ApplicationServiceFactory {
    // repositories (could be replaced by interfaces)
    private final PlayerCacheRepository playerCache = new PlayerCacheRepository();
    private final BalanceCacheRepository balanceCache = new BalanceCacheRepository();

    // application-level services
    private final PlayerApplicationService playerApp;
    private final BalanceApplicationService balanceApp;

    // command application services
    private final PayCommandApplicationService payCmdApp;
    private final BalanceCommandApplicationService balanceCmdApp;
    private final BaltopCommandApplicationService baltopCmdApp;
    private final SetBalanceCommandApplicationService setBalanceCmdApp;

    public ApplicationServiceFactory(ApiServiceFactory apis) {

        this.playerApp = new PlayerApplicationService(apis.playerApi(), playerCache);
        this.balanceApp = new BalanceApplicationService(apis.balanceApi(), balanceCache);

        this.payCmdApp = new PayCommandApplicationService(playerApp, apis.playerApi(), apis.balanceApi(), apis.transactionApi());
        this.balanceCmdApp = new BalanceCommandApplicationService(playerApp, balanceApp);
        this.baltopCmdApp = new BaltopCommandApplicationService(apis.balanceApi(), apis.playerApi());
        this.setBalanceCmdApp = new SetBalanceCommandApplicationService(apis.balanceApi(), playerApp);
    }

    public PlayerApplicationService playerApplication() { return playerApp; }
    public BalanceApplicationService balanceApplication() { return balanceApp; }
    public PayCommandApplicationService payCommandApplication() { return payCmdApp; }
    public BalanceCommandApplicationService balanceCommandApplication() { return balanceCmdApp; }
    public BaltopCommandApplicationService baltopCommandApplication() { return baltopCmdApp; }
    public SetBalanceCommandApplicationService setBalanceCommandApplication() { return setBalanceCmdApp; }

    public void shutdown() {
        // persist caches, shutdown http client, etc.
        playerCache.flush();
        balanceCache.flush();
    }
}