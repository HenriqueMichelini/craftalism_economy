package io.github.HenriqueMichelini.craftalism_economy.application.service;

import io.github.HenriqueMichelini.craftalism_economy.domain.model.Player;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.enums.PayResult;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.BalanceApiService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.PlayerApiService;
import io.github.HenriqueMichelini.craftalism_economy.infra.api.service.TransactionApiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PayApplicationService Tests")
class PayCommandApplicationServiceTest {

    @Mock
    private PlayerApplicationService playerService;
    @Mock
    private PlayerApiService playerApi;
    @Mock
    private BalanceApiService balanceApi;
    @Mock
    private TransactionApiService transactionApi;
    private PayCommandApplicationService service;

    private UUID payerUuid;
    private UUID receiverUuid;
    private String payerName;
    private String receiverName;
    private Player payerPlayer;
    private PlayerResponseDTO receiverDTO;
    private Long validAmount;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new PayCommandApplicationService(playerService, playerApi, balanceApi, transactionApi);

        payerUuid = UUID.randomUUID();
        receiverUuid = UUID.randomUUID();
        payerName = "Payer";
        receiverName = "Receiver";
        validAmount = 100_0000L;

        payerPlayer = new Player(payerUuid, payerName, Instant.now());
        receiverDTO = new PlayerResponseDTO(receiverUuid, receiverName, Instant.now());
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("Should complete successful payment")
    void shouldCompleteSuccessfulPayment() throws ExecutionException, InterruptedException {
        Long payerBalance = 500_0000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, payerBalance);

        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));
        when(balanceApi.withdraw(payerUuid, validAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(balanceApi.deposit(receiverUuid, validAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(transactionApi.register(payerUuid, receiverUuid, validAmount))
                .thenReturn(CompletableFuture.completedFuture(null));

        PayResult result = service.execute(payerUuid, payerName, receiverName, validAmount).get();

        assertEquals(PayResult.SUCCESS, result);

        verify(playerService).getCachedOrFetch(payerUuid, payerName);
        verify(playerApi).getPlayerByName(receiverName);
        verify(balanceApi).getBalance(payerUuid);
        verify(balanceApi).withdraw(payerUuid, validAmount);
        verify(balanceApi).deposit(receiverUuid, validAmount);
        verify(transactionApi).register(payerUuid, receiverUuid, validAmount);
    }

    @Test
    @DisplayName("Should handle exact balance payment")
    void shouldHandleExactBalancePayment() throws ExecutionException, InterruptedException {
        Long exactAmount = 100_0000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, exactAmount);

        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));
        when(balanceApi.withdraw(payerUuid, exactAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(balanceApi.deposit(receiverUuid, exactAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(transactionApi.register(payerUuid, receiverUuid, exactAmount))
                .thenReturn(CompletableFuture.completedFuture(null));

        PayResult result = service.execute(payerUuid, payerName, receiverName, exactAmount).get();

        assertEquals(PayResult.SUCCESS, result);
    }

    @Test
    @DisplayName("Should reject payment to self")
    void shouldRejectPaymentToSelf() throws ExecutionException, InterruptedException {
        PlayerResponseDTO selfDTO = new PlayerResponseDTO(payerUuid, payerName, Instant.now());

        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(payerName))
                .thenReturn(CompletableFuture.completedFuture(selfDTO));

        PayResult result = service.execute(payerUuid, payerName, payerName, validAmount).get();

        assertEquals(PayResult.CANNOT_PAY_SELF, result);

        verify(playerService).getCachedOrFetch(payerUuid, payerName);
        verify(playerApi).getPlayerByName(payerName);
        verify(balanceApi, never()).getBalance(any());
        verify(balanceApi, never()).withdraw(any(), anyLong());
    }

    @Test
    @DisplayName("Should reject zero amount payment")
    void shouldRejectZeroAmountPayment() throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));

        PayResult result = service.execute(payerUuid, payerName, receiverName, 0L).get();

        assertEquals(PayResult.INVALID_AMOUNT, result);

        verify(balanceApi, never()).getBalance(any());
        verify(balanceApi, never()).withdraw(any(), anyLong());
    }

    @Test
    @DisplayName("Should reject negative amount payment")
    void shouldRejectNegativeAmountPayment() throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));

        PayResult result = service.execute(payerUuid, payerName, receiverName, -100L).get();

        assertEquals(PayResult.INVALID_AMOUNT, result);

        verify(balanceApi, never()).getBalance(any());
    }

    @Test
    @DisplayName("Should reject payment when target player not found")
    void shouldRejectPaymentWhenTargetNotFound() throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Player not found")));

        PayResult result = service.execute(payerUuid, payerName, receiverName, validAmount).get();

        assertEquals(PayResult.TARGET_NOT_FOUND, result);

        verify(playerService).getCachedOrFetch(payerUuid, payerName);
        verify(playerApi).getPlayerByName(receiverName);
        verify(balanceApi, never()).getBalance(any());
    }

    @Test
    @DisplayName("Should reject payment when payer has insufficient funds")
    void shouldRejectPaymentWhenInsufficientFunds() throws ExecutionException, InterruptedException {
        Long insufficientBalance = 50_0000L; // $50.00, but trying to pay $100.00
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, insufficientBalance);


        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));

        PayResult result = service.execute(payerUuid, payerName, receiverName, validAmount).get();

        assertEquals(PayResult.NOT_ENOUGH_FUNDS, result);

        verify(balanceApi).getBalance(payerUuid);
        verify(balanceApi, never()).withdraw(any(), anyLong());
        verify(balanceApi, never()).deposit(any(), anyLong());
        verify(transactionApi, never()).register(any(), any(), anyLong());
    }

    @Test
    @DisplayName("Should reject payment when balance is exactly one less than amount")
    void shouldRejectPaymentWhenBalanceOneShort() throws ExecutionException, InterruptedException {
        Long almostEnough = validAmount - 1;
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, almostEnough);


        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));

        PayResult result = service.execute(payerUuid, payerName, receiverName, validAmount).get();

        assertEquals(PayResult.NOT_ENOUGH_FUNDS, result);
    }

    @Test
    @DisplayName("Should handle exception when getting payer")
    void shouldHandleExceptionWhenGettingPayer() throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Database error")));

        PayResult result = service.execute(payerUuid, payerName, receiverName, validAmount).get();

        assertEquals(PayResult.TARGET_NOT_FOUND, result);

        verify(playerApi, never()).getPlayerByName(any());
        verify(balanceApi, never()).getBalance(any());
    }

    @Test
    @DisplayName("Should handle exception during balance check")
    void shouldHandleExceptionDuringBalanceCheck() throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Balance API error")));

        PayResult result = service.execute(payerUuid, payerName, receiverName, validAmount).get();

        assertEquals(PayResult.TARGET_NOT_FOUND, result);

        verify(balanceApi, never()).withdraw(any(), anyLong());
    }

    @Test
    @DisplayName("Should handle exception during withdraw")
    void shouldHandleExceptionDuringWithdraw() throws ExecutionException, InterruptedException {
        Long payerBalance = 500_0000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, payerBalance);


        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));
        when(balanceApi.withdraw(payerUuid, validAmount))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Withdraw failed")));

        PayResult result = service.execute(payerUuid, payerName, receiverName, validAmount).get();

        assertEquals(PayResult.TARGET_NOT_FOUND, result);

        verify(balanceApi).withdraw(payerUuid, validAmount);
        verify(balanceApi, never()).deposit(any(), anyLong());
        verify(transactionApi, never()).register(any(), any(), anyLong());
    }

    @Test
    @DisplayName("Should handle exception during deposit")
    void shouldHandleExceptionDuringDeposit() throws ExecutionException, InterruptedException {
        Long payerBalance = 500_0000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, payerBalance);


        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));
        when(balanceApi.withdraw(payerUuid, validAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(balanceApi.deposit(receiverUuid, validAmount))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Deposit failed")));

        PayResult result = service.execute(payerUuid, payerName, receiverName, validAmount).get();

        assertEquals(PayResult.TARGET_NOT_FOUND, result);

        verify(balanceApi).withdraw(payerUuid, validAmount);
        verify(balanceApi).deposit(receiverUuid, validAmount);
        verify(transactionApi, never()).register(any(), any(), anyLong());
    }

    @Test
    @DisplayName("Should handle exception during transaction registration")
    void shouldHandleExceptionDuringTransactionRegistration() throws ExecutionException, InterruptedException {
        Long payerBalance = 500_0000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, payerBalance);


        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));
        when(balanceApi.withdraw(payerUuid, validAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(balanceApi.deposit(receiverUuid, validAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(transactionApi.register(payerUuid, receiverUuid, validAmount))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Transaction log failed")));

        PayResult result = service.execute(payerUuid, payerName, receiverName, validAmount).get();

        assertEquals(PayResult.TARGET_NOT_FOUND, result);

        verify(transactionApi).register(payerUuid, receiverUuid, validAmount);
    }

    @Test
    @DisplayName("Should handle payment with very large valid amount")
    void shouldHandlePaymentWithVeryLargeAmount() throws ExecutionException, InterruptedException {
        Long largeAmount = 1_000_000_0000L; // $1,000,000.00
        Long largerBalance = 2_000_000_0000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, largerBalance);


        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));
        when(balanceApi.withdraw(payerUuid, largeAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(balanceApi.deposit(receiverUuid, largeAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(transactionApi.register(payerUuid, receiverUuid, largeAmount))
                .thenReturn(CompletableFuture.completedFuture(null));

        PayResult result = service.execute(payerUuid, payerName, receiverName, largeAmount).get();

        assertEquals(PayResult.SUCCESS, result);
    }

    @Test
    @DisplayName("Should handle payment with minimum positive amount")
    void shouldHandlePaymentWithMinimumAmount() throws ExecutionException, InterruptedException {
        Long minAmount = 1L; // 0.0001 in currency
        Long balance = 100_0000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, balance);


        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));
        when(balanceApi.withdraw(payerUuid, minAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(balanceApi.deposit(receiverUuid, minAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(transactionApi.register(payerUuid, receiverUuid, minAmount))
                .thenReturn(CompletableFuture.completedFuture(null));

        PayResult result = service.execute(payerUuid, payerName, receiverName, minAmount).get();

        assertEquals(PayResult.SUCCESS, result);
    }

    @Test
    @DisplayName("Should handle payment when payer balance is zero")
    void shouldHandlePaymentWhenPayerBalanceIsZero() throws ExecutionException, InterruptedException {
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, 0L);


        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(receiverName))
                .thenReturn(CompletableFuture.completedFuture(receiverDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));

        PayResult result = service.execute(payerUuid, payerName, receiverName, validAmount).get();

        assertEquals(PayResult.NOT_ENOUGH_FUNDS, result);
    }

    @Test
    @DisplayName("Should handle special characters in player names")
    void shouldHandleSpecialCharactersInNames() throws ExecutionException, InterruptedException {
        String specialName = "Player_123-XYZ";
        UUID specialUuid = UUID.randomUUID();
        PlayerResponseDTO specialDTO = new PlayerResponseDTO(specialUuid, specialName, Instant.now());
        Long balance = 500_0000L;

        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, balance);

        when(playerService.getCachedOrFetch(payerUuid, payerName))
                .thenReturn(CompletableFuture.completedFuture(payerPlayer));
        when(playerApi.getPlayerByName(specialName))
                .thenReturn(CompletableFuture.completedFuture(specialDTO));
        when(balanceApi.getBalance(payerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));
        when(balanceApi.withdraw(payerUuid, validAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(balanceApi.deposit(specialUuid, validAmount))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(transactionApi.register(payerUuid, specialUuid, validAmount))
                .thenReturn(CompletableFuture.completedFuture(null));

        PayResult result = service.execute(payerUuid, payerName, specialName, validAmount).get();

        assertEquals(PayResult.SUCCESS, result);
    }
}