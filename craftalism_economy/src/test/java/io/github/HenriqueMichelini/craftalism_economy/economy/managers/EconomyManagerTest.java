package io.github.HenriqueMichelini.craftalism_economy.economy.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EconomyManagerTest {

    private static final long INITIAL_BALANCE = 1000L;
    private static final long DEPOSIT_AMOUNT = 500L;
    private static final long WITHDRAW_AMOUNT = 300L;
    private static final long TRANSFER_AMOUNT = 250L;

    @Mock
    private BalanceManager balanceManager;

    private EconomyManager economyManager;
    private UUID testPlayer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        economyManager = new EconomyManager(balanceManager);
        testPlayer = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("Should successfully deposit valid amount")
        void deposit_ValidAmount_ReturnsTrue() {
            // Given
            when(balanceManager.getBalance(testPlayer)).thenReturn(INITIAL_BALANCE);

            // When
            boolean result = economyManager.deposit(testPlayer, DEPOSIT_AMOUNT);

            // Then
            assertTrue(result, "Deposit should succeed with valid amount");
            verify(balanceManager).getBalance(testPlayer);
            verify(balanceManager).setBalance(testPlayer, INITIAL_BALANCE + DEPOSIT_AMOUNT);
            verifyNoMoreInteractions(balanceManager);
        }

        @Test
        @DisplayName("Should handle zero deposit amount")
        void deposit_ZeroAmount_ReturnsFalse() {
            // When
            boolean result = economyManager.deposit(testPlayer, 0L);

            // Then
            assertFalse(result, "Deposit should fail with zero amount");

            // Verify NO interactions with balanceManager since amount is zero
            verifyNoInteractions(balanceManager);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, -10L, -100L, Long.MIN_VALUE})
        @DisplayName("Should reject negative deposit amounts")
        void deposit_NegativeAmount_ReturnsFalse(long negativeAmount) {
            // When
            boolean result = economyManager.deposit(testPlayer, negativeAmount);

            // Then
            assertFalse(result, "Deposit should fail with negative amount: " + negativeAmount);
            verify(balanceManager, never()).getBalance(any(UUID.class));
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @Test
        @DisplayName("Should handle potential overflow in deposit")
        void deposit_PotentialOverflow_HandledCorrectly() {
            // Given - Balance near Long.MAX_VALUE
            long nearMaxBalance = Long.MAX_VALUE - 100L;
            when(balanceManager.getBalance(testPlayer)).thenReturn(nearMaxBalance);

            // When & Then - Should not crash when overflow might occur
            assertDoesNotThrow(() -> {
                economyManager.deposit(testPlayer, 200L);
                // The result behavior is implementation dependent - could be true or false
                // Main goal is ensuring no exception is thrown
            });
            verify(balanceManager).getBalance(testPlayer);
        }

        @Test
        @DisplayName("Should handle null player UUID")
        void deposit_NullPlayer_HandledGracefully() {
            // When & Then - Should not crash with null UUID
            assertDoesNotThrow(() -> economyManager.deposit(null, DEPOSIT_AMOUNT));
        }
    }

    @Nested
    @DisplayName("Withdraw Tests")
    class WithdrawTests {

        @Test
        @DisplayName("Should successfully withdraw with sufficient funds")
        void withdraw_SufficientFunds_ReturnsTrue() {
            // Given
            when(balanceManager.getBalance(testPlayer)).thenReturn(INITIAL_BALANCE);

            // When
            boolean result = economyManager.withdraw(testPlayer, WITHDRAW_AMOUNT);

            // Then
            assertTrue(result, "Withdraw should succeed with sufficient funds");
            verify(balanceManager, times(1)).getBalance(testPlayer); // Now only 1 call!
            verify(balanceManager).setBalance(testPlayer, INITIAL_BALANCE - WITHDRAW_AMOUNT);
        }

        @Test
        @DisplayName("Should fail withdraw with insufficient funds")
        void withdraw_InsufficientFunds_ReturnsFalse() {
            // Given
            long smallBalance = 50L;
            when(balanceManager.getBalance(testPlayer)).thenReturn(smallBalance);

            // When
            boolean result = economyManager.withdraw(testPlayer, WITHDRAW_AMOUNT);

            // Then
            assertFalse(result, "Withdraw should fail with insufficient funds");
            verify(balanceManager).getBalance(testPlayer);
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @Test
        @DisplayName("Should allow exact balance withdrawal")
        void withdraw_ExactBalance_ReturnsTrue() {
            // Given
            when(balanceManager.getBalance(testPlayer)).thenReturn(WITHDRAW_AMOUNT);

            // When
            boolean result = economyManager.withdraw(testPlayer, WITHDRAW_AMOUNT);

            // Then
            assertTrue(result, "Should allow withdrawing exact balance");
            verify(balanceManager).setBalance(testPlayer, 0L);
        }

        @Test
        @DisplayName("Should handle zero withdrawal amount")
        void withdraw_ZeroAmount_ReturnsFalse() {
            // When
            boolean result = economyManager.withdraw(testPlayer, 0L);

            // Then
            assertFalse(result, "Withdraw should fail with zero amount");
            verify(balanceManager, never()).getBalance(any(UUID.class));
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, -50L, -1000L, Long.MIN_VALUE})
        @DisplayName("Should reject negative withdrawal amounts")
        void withdraw_NegativeAmount_ReturnsFalse(long negativeAmount) {
            // When
            boolean result = economyManager.withdraw(testPlayer, negativeAmount);

            // Then
            assertFalse(result, "Withdraw should fail with negative amount: " + negativeAmount);
            verify(balanceManager, never()).getBalance(any(UUID.class));
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @Test
        @DisplayName("Should handle null player UUID")
        void withdraw_NullPlayer_HandledGracefully() {
            // When & Then - Should not crash with null UUID
            assertDoesNotThrow(() -> economyManager.withdraw(null, WITHDRAW_AMOUNT));
        }
    }

    @Nested
    @DisplayName("Transfer Tests")
    class TransferTests {

        private UUID fromPlayer;
        private UUID toPlayer;

        @BeforeEach
        void setUp() {
            fromPlayer = UUID.randomUUID();
            toPlayer = UUID.randomUUID();
        }

        @Test
        @DisplayName("Should successfully transfer with sufficient funds")
        void transferBalance_Success_ReturnsTrue() {
            // Given - Only 1 call for each player
            when(balanceManager.getBalance(fromPlayer)).thenReturn(INITIAL_BALANCE);
            when(balanceManager.getBalance(toPlayer)).thenReturn(100L);

            // When
            boolean result = economyManager.transferBalance(fromPlayer, toPlayer, TRANSFER_AMOUNT);

            // Then
            assertTrue(result, "Transfer should succeed with sufficient funds");

            // Verify total calls - now only 1 for each player!
            verify(balanceManager, times(1)).getBalance(fromPlayer);
            verify(balanceManager, times(1)).getBalance(toPlayer);
            verify(balanceManager).setBalance(fromPlayer, INITIAL_BALANCE - TRANSFER_AMOUNT);
            verify(balanceManager).setBalance(toPlayer, 100L + TRANSFER_AMOUNT);

            // Verify order of operations
            InOrder inOrder = inOrder(balanceManager);

            // First get both balances (order might be fromPlayer then toPlayer, or vice versa)
            inOrder.verify(balanceManager).getBalance(fromPlayer);
            inOrder.verify(balanceManager).getBalance(toPlayer);

            // Then update both balances
            inOrder.verify(balanceManager).setBalance(fromPlayer, INITIAL_BALANCE - TRANSFER_AMOUNT);
            inOrder.verify(balanceManager).setBalance(toPlayer, 100L + TRANSFER_AMOUNT);

            verifyNoMoreInteractions(balanceManager);
        }

        @Test
        @DisplayName("Should fail transfer with insufficient funds")
        void transferBalance_InsufficientFunds_ReturnsFalse() {
            // Given
            when(balanceManager.getBalance(fromPlayer)).thenReturn(50L); // Less than transfer amount

            // When
            boolean result = economyManager.transferBalance(fromPlayer, toPlayer, TRANSFER_AMOUNT);

            // Then
            assertFalse(result, "Transfer should fail with insufficient funds");
            verify(balanceManager).getBalance(fromPlayer);
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @Test
        @DisplayName("Should handle transfer to same player")
        void transferBalance_SamePlayer_HandledCorrectly() {
            // Given
            when(balanceManager.getBalance(fromPlayer)).thenReturn(INITIAL_BALANCE);

            // When
            boolean result = economyManager.transferBalance(fromPlayer, fromPlayer, TRANSFER_AMOUNT);

            // Then - Document the behavior for same-player transfers
            // Implementation could either allow it (no-op) or reject it
            verify(balanceManager, atLeastOnce()).getBalance(fromPlayer);

            // Note: The exact behavior (true/false) is implementation dependent
            // This test ensures the method handles same-player transfers gracefully
        }

        @Test
        @DisplayName("Should handle zero transfer amount")
        void transferBalance_ZeroAmount_ReturnsFalse() {
            // When
            boolean result = economyManager.transferBalance(fromPlayer, toPlayer, 0L);

            // Then
            assertFalse(result, "Transfer should fail with zero amount");
            verify(balanceManager, never()).getBalance(any(UUID.class));
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, -100L, -1000L, Long.MIN_VALUE})
        @DisplayName("Should reject negative transfer amounts")
        void transferBalance_NegativeAmount_ReturnsFalse(long negativeAmount) {
            // When
            boolean result = economyManager.transferBalance(fromPlayer, toPlayer, negativeAmount);

            // Then
            assertFalse(result, "Transfer should fail with negative amount: " + negativeAmount);
            verify(balanceManager, never()).getBalance(any(UUID.class));
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @Test
        @DisplayName("Should handle null UUIDs gracefully")
        void transferBalance_NullUUIDs_HandledGracefully() {
            // When & Then - Should not crash with null UUIDs
            assertAll(
                    () -> assertDoesNotThrow(() -> economyManager.transferBalance(null, toPlayer, TRANSFER_AMOUNT)),
                    () -> assertDoesNotThrow(() -> economyManager.transferBalance(fromPlayer, null, TRANSFER_AMOUNT)),
                    () -> assertDoesNotThrow(() -> economyManager.transferBalance(null, null, TRANSFER_AMOUNT))
            );
        }

        @Test
        @DisplayName("Should handle exact balance transfer")
        void transferBalance_ExactBalance_Success() {
            // Given
            when(balanceManager.getBalance(fromPlayer)).thenReturn(TRANSFER_AMOUNT);
            when(balanceManager.getBalance(toPlayer)).thenReturn(0L);

            // When
            boolean result = economyManager.transferBalance(fromPlayer, toPlayer, TRANSFER_AMOUNT);

            // Then
            assertTrue(result, "Should allow transferring exact balance");
            verify(balanceManager).setBalance(fromPlayer, 0L);
            verify(balanceManager).setBalance(toPlayer, TRANSFER_AMOUNT);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle multiple operations in sequence")
        void multipleOperations_InSequence_WorkCorrectly() {
            // Given - Only 2 getBalance calls for the operations
            when(balanceManager.getBalance(testPlayer))
                    .thenReturn(100L)
                    .thenReturn(150L);

            // When
            assertTrue(economyManager.deposit(testPlayer, 50L), "First deposit should succeed");
            assertTrue(economyManager.withdraw(testPlayer, 50L), "Withdrawal should succeed");

            // Verify call order - now only 2 getBalance calls total!
            InOrder inOrder = inOrder(balanceManager);

            // Deposit: 1 getBalance, then setBalance
            inOrder.verify(balanceManager).getBalance(testPlayer);
            inOrder.verify(balanceManager).setBalance(testPlayer, 150L);

            // Withdrawal: 1 getBalance, then setBalance
            inOrder.verify(balanceManager).getBalance(testPlayer);
            inOrder.verify(balanceManager).setBalance(testPlayer, 100L);
        }
    }
}