package io.github.HenriqueMichelini.craftalism_economy.core.managers;

import io.github.HenriqueMichelini.craftalism_economy.domain.service.validators.EconomyValidator;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.managers.EconomyManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("EconomyManager Tests")
class EconomyManagerTest {

    private static final long INITIAL_BALANCE = 1000L;
    private static final long DEPOSIT_AMOUNT = 500L;
    private static final long WITHDRAW_AMOUNT = 300L;
    private static final long TRANSFER_AMOUNT = 250L;

    @Mock
    private BalanceManager balanceManager;

    @Mock
    private EconomyValidator economyValidator;

    private EconomyManager economyManager;
    private UUID testPlayer;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        economyManager = new EconomyManager(balanceManager, economyValidator);
        testPlayer = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw exception when BalanceManager is null")
        void constructor_NullBalanceManager_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new EconomyManager(null, economyValidator),
                    "Should throw exception when BalanceManager is null"
            );

            assertEquals("BalanceManager cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when EconomyValidator is null")
        void constructor_NullEconomyValidator_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new EconomyManager(balanceManager, null),
                    "Should throw exception when EconomyValidator is null"
            );

            assertEquals("EconomyValidator cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should create instance with valid dependencies")
        void constructor_ValidDependencies_CreatesInstance() {
            // When & Then
            assertDoesNotThrow(() -> new EconomyManager(balanceManager, economyValidator));
        }
    }

    @Nested
    @DisplayName("Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("Should successfully deposit valid amount")
        void deposit_ValidAmount_ReturnsTrue() {
            // Given
            when(economyValidator.isGreaterThanZero(DEPOSIT_AMOUNT)).thenReturn(true);
            when(balanceManager.getBalance(testPlayer)).thenReturn(INITIAL_BALANCE);

            // When
            boolean result = economyManager.deposit(testPlayer, DEPOSIT_AMOUNT);

            // Then
            assertTrue(result, "Deposit should succeed with valid amount");

            // Verify validator was called
            verify(economyValidator).isGreaterThanZero(DEPOSIT_AMOUNT);

            // Verify balance operations
            verify(balanceManager).getBalance(testPlayer);
            verify(balanceManager).setBalance(testPlayer, INITIAL_BALANCE + DEPOSIT_AMOUNT);
            verifyNoMoreInteractions(balanceManager);
        }

        @Test
        @DisplayName("Should fail when validator rejects amount")
        void deposit_ValidatorRejectsAmount_ReturnsFalse() {
            // Given
            when(economyValidator.isGreaterThanZero(DEPOSIT_AMOUNT)).thenReturn(false);

            // When
            boolean result = economyManager.deposit(testPlayer, DEPOSIT_AMOUNT);

            // Then
            assertFalse(result, "Deposit should fail when validator rejects amount");
            verify(economyValidator).isGreaterThanZero(DEPOSIT_AMOUNT);
            verifyNoInteractions(balanceManager);
        }

        @Test
        @DisplayName("Should handle zero deposit amount")
        void deposit_ZeroAmount_ReturnsFalse() {
            // Given
            when(economyValidator.isGreaterThanZero(0L)).thenReturn(false);

            // When
            boolean result = economyManager.deposit(testPlayer, 0L);

            // Then
            assertFalse(result, "Deposit should fail with zero amount");
            verify(economyValidator).isGreaterThanZero(0L);
            verifyNoInteractions(balanceManager);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, -10L, -100L, Long.MIN_VALUE})
        @DisplayName("Should reject negative deposit amounts")
        void deposit_NegativeAmount_ReturnsFalse(long negativeAmount) {
            // Given
            when(economyValidator.isGreaterThanZero(negativeAmount)).thenReturn(false);

            // When
            boolean result = economyManager.deposit(testPlayer, negativeAmount);

            // Then
            assertFalse(result, "Deposit should fail with negative amount: " + negativeAmount);
            verify(economyValidator).isGreaterThanZero(negativeAmount);
            verifyNoInteractions(balanceManager);
        }

        @Test
        @DisplayName("Should prevent overflow in deposit")
        void deposit_PotentialOverflow_ReturnsFalse() {
            // Given - Balance near Long.MAX_VALUE that would overflow
            long nearMaxBalance = Long.MAX_VALUE - 100L;
            long largeDeposit = 200L; // This would cause overflow

            when(economyValidator.isGreaterThanZero(largeDeposit)).thenReturn(true);
            when(balanceManager.getBalance(testPlayer)).thenReturn(nearMaxBalance);

            // When
            boolean result = economyManager.deposit(testPlayer, largeDeposit);

            // Then
            assertFalse(result, "Deposit should fail to prevent overflow");
            verify(economyValidator).isGreaterThanZero(largeDeposit);
            verify(balanceManager).getBalance(testPlayer);
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @Test
        @DisplayName("Should allow deposit that doesn't cause overflow")
        void deposit_NoOverflow_ReturnsTrue() {
            // Given - Balance that won't overflow
            long balance = Long.MAX_VALUE - 1000L;
            long safeDeposit = 500L;

            when(economyValidator.isGreaterThanZero(safeDeposit)).thenReturn(true);
            when(balanceManager.getBalance(testPlayer)).thenReturn(balance);

            // When
            boolean result = economyManager.deposit(testPlayer, safeDeposit);

            // Then
            assertTrue(result, "Deposit should succeed when no overflow occurs");
            verify(balanceManager).setBalance(testPlayer, balance + safeDeposit);
        }

        @Test
        @DisplayName("Should handle null player UUID")
        void deposit_NullPlayer_ReturnsFalse() {
            // When
            boolean result = economyManager.deposit(null, DEPOSIT_AMOUNT);

            // Then
            assertFalse(result, "Deposit should fail with null player UUID");
            verifyNoInteractions(economyValidator);
            verifyNoInteractions(balanceManager);
        }
    }

    @Nested
    @DisplayName("Withdraw Tests")
    class WithdrawTests {

        @Test
        @DisplayName("Should successfully withdraw with sufficient funds")
        void withdraw_SufficientFunds_ReturnsTrue() {
            // Given
            when(economyValidator.isGreaterThanZero(WITHDRAW_AMOUNT)).thenReturn(true);
            when(balanceManager.getBalance(testPlayer)).thenReturn(INITIAL_BALANCE);
            when(economyValidator.hasSufficientFunds(INITIAL_BALANCE, WITHDRAW_AMOUNT)).thenReturn(true);

            // When
            boolean result = economyManager.withdraw(testPlayer, WITHDRAW_AMOUNT);

            // Then
            assertTrue(result, "Withdraw should succeed with sufficient funds");

            verify(economyValidator).isGreaterThanZero(WITHDRAW_AMOUNT);
            verify(balanceManager).getBalance(testPlayer);
            verify(economyValidator).hasSufficientFunds(INITIAL_BALANCE, WITHDRAW_AMOUNT);
            verify(balanceManager).setBalance(testPlayer, INITIAL_BALANCE - WITHDRAW_AMOUNT);
        }

        @Test
        @DisplayName("Should fail when validator rejects amount")
        void withdraw_ValidatorRejectsAmount_ReturnsFalse() {
            // Given
            when(economyValidator.isGreaterThanZero(WITHDRAW_AMOUNT)).thenReturn(false);

            // When
            boolean result = economyManager.withdraw(testPlayer, WITHDRAW_AMOUNT);

            // Then
            assertFalse(result, "Withdraw should fail when validator rejects amount");
            verify(economyValidator).isGreaterThanZero(WITHDRAW_AMOUNT);
            verifyNoInteractions(balanceManager);
        }

        @Test
        @DisplayName("Should fail withdraw with insufficient funds")
        void withdraw_InsufficientFunds_ReturnsFalse() {
            // Given
            long smallBalance = 50L;
            when(economyValidator.isGreaterThanZero(WITHDRAW_AMOUNT)).thenReturn(true);
            when(balanceManager.getBalance(testPlayer)).thenReturn(smallBalance);
            when(economyValidator.hasSufficientFunds(smallBalance, WITHDRAW_AMOUNT)).thenReturn(false);

            // When
            boolean result = economyManager.withdraw(testPlayer, WITHDRAW_AMOUNT);

            // Then
            assertFalse(result, "Withdraw should fail with insufficient funds");
            verify(economyValidator).isGreaterThanZero(WITHDRAW_AMOUNT);
            verify(balanceManager).getBalance(testPlayer);
            verify(economyValidator).hasSufficientFunds(smallBalance, WITHDRAW_AMOUNT);
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @Test
        @DisplayName("Should allow exact balance withdrawal")
        void withdraw_ExactBalance_ReturnsTrue() {
            // Given
            when(economyValidator.isGreaterThanZero(WITHDRAW_AMOUNT)).thenReturn(true);
            when(balanceManager.getBalance(testPlayer)).thenReturn(WITHDRAW_AMOUNT);
            when(economyValidator.hasSufficientFunds(WITHDRAW_AMOUNT, WITHDRAW_AMOUNT)).thenReturn(true);

            // When
            boolean result = economyManager.withdraw(testPlayer, WITHDRAW_AMOUNT);

            // Then
            assertTrue(result, "Should allow withdrawing exact balance");
            verify(balanceManager).setBalance(testPlayer, 0L);
        }

        @Test
        @DisplayName("Should handle null player UUID")
        void withdraw_NullPlayer_ReturnsFalse() {
            // When
            boolean result = economyManager.withdraw(null, WITHDRAW_AMOUNT);

            // Then
            assertFalse(result, "Withdraw should fail with null player UUID");
            verifyNoInteractions(economyValidator);
            verifyNoInteractions(balanceManager);
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
            // Given
            long toBalance = 100L;
            when(economyValidator.isGreaterThanZero(TRANSFER_AMOUNT)).thenReturn(true);
            when(balanceManager.getBalance(fromPlayer)).thenReturn(INITIAL_BALANCE);
            when(balanceManager.getBalance(toPlayer)).thenReturn(toBalance);
            when(economyValidator.hasSufficientFunds(INITIAL_BALANCE, TRANSFER_AMOUNT)).thenReturn(true);

            // When
            boolean result = economyManager.transferBalance(fromPlayer, toPlayer, TRANSFER_AMOUNT);

            // Then
            assertTrue(result, "Transfer should succeed with sufficient funds");

            verify(economyValidator).isGreaterThanZero(TRANSFER_AMOUNT);
            verify(balanceManager).getBalance(fromPlayer);
            verify(balanceManager).getBalance(toPlayer);
            verify(economyValidator).hasSufficientFunds(INITIAL_BALANCE, TRANSFER_AMOUNT);
            verify(balanceManager).setBalance(fromPlayer, INITIAL_BALANCE - TRANSFER_AMOUNT);
            verify(balanceManager).setBalance(toPlayer, toBalance + TRANSFER_AMOUNT);
        }

        @Test
        @DisplayName("Should fail when validator rejects amount")
        void transferBalance_ValidatorRejectsAmount_ReturnsFalse() {
            // Given
            when(economyValidator.isGreaterThanZero(TRANSFER_AMOUNT)).thenReturn(false);

            // When
            boolean result = economyManager.transferBalance(fromPlayer, toPlayer, TRANSFER_AMOUNT);

            // Then
            assertFalse(result, "Transfer should fail when validator rejects amount");
            verify(economyValidator).isGreaterThanZero(TRANSFER_AMOUNT);
            verifyNoInteractions(balanceManager);
        }

        @Test
        @DisplayName("Should fail transfer with insufficient funds")
        void transferBalance_InsufficientFunds_ReturnsFalse() {
            // Given
            long smallBalance = 50L;
            when(economyValidator.isGreaterThanZero(TRANSFER_AMOUNT)).thenReturn(true);
            when(balanceManager.getBalance(fromPlayer)).thenReturn(smallBalance);
            when(economyValidator.hasSufficientFunds(smallBalance, TRANSFER_AMOUNT)).thenReturn(false);

            // When
            boolean result = economyManager.transferBalance(fromPlayer, toPlayer, TRANSFER_AMOUNT);

            // Then
            assertFalse(result, "Transfer should fail with insufficient funds");
            verify(balanceManager).getBalance(fromPlayer);
            verify(economyValidator).hasSufficientFunds(smallBalance, TRANSFER_AMOUNT);
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @Test
        @DisplayName("Should prevent self-transfer")
        void transferBalance_SamePlayer_ReturnsFalse() {
            // Given
            when(economyValidator.isGreaterThanZero(TRANSFER_AMOUNT)).thenReturn(true);

            // When
            boolean result = economyManager.transferBalance(fromPlayer, fromPlayer, TRANSFER_AMOUNT);

            // Then
            assertFalse(result, "Transfer should fail when transferring to same player");
            verify(economyValidator).isGreaterThanZero(TRANSFER_AMOUNT);
            verifyNoInteractions(balanceManager);
        }

        @Test
        @DisplayName("Should prevent overflow in recipient's balance")
        void transferBalance_RecipientOverflow_ReturnsFalse() {
            // Given
            long nearMaxBalance = Long.MAX_VALUE - 100L;
            when(economyValidator.isGreaterThanZero(TRANSFER_AMOUNT)).thenReturn(true);
            when(balanceManager.getBalance(fromPlayer)).thenReturn(INITIAL_BALANCE);
            when(balanceManager.getBalance(toPlayer)).thenReturn(nearMaxBalance);
            when(economyValidator.hasSufficientFunds(INITIAL_BALANCE, TRANSFER_AMOUNT)).thenReturn(true);

            // When
            boolean result = economyManager.transferBalance(fromPlayer, toPlayer, TRANSFER_AMOUNT);

            // Then
            assertFalse(result, "Transfer should fail to prevent recipient overflow");
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @Test
        @DisplayName("Should handle null UUIDs")
        void transferBalance_NullUUIDs_ReturnsFalse() {
            assertAll("Should handle null UUIDs gracefully",
                    () -> assertFalse(economyManager.transferBalance(null, toPlayer, TRANSFER_AMOUNT),
                            "Should fail with null from UUID"),
                    () -> assertFalse(economyManager.transferBalance(fromPlayer, null, TRANSFER_AMOUNT),
                            "Should fail with null to UUID"),
                    () -> assertFalse(economyManager.transferBalance(null, null, TRANSFER_AMOUNT),
                            "Should fail with both UUIDs null")
            );

            verifyNoInteractions(economyValidator);
            verifyNoInteractions(balanceManager);
        }

        @Test
        @DisplayName("Should handle exact balance transfer")
        void transferBalance_ExactBalance_Success() {
            // Given
            when(economyValidator.isGreaterThanZero(TRANSFER_AMOUNT)).thenReturn(true);
            when(balanceManager.getBalance(fromPlayer)).thenReturn(TRANSFER_AMOUNT);
            when(balanceManager.getBalance(toPlayer)).thenReturn(0L);
            when(economyValidator.hasSufficientFunds(TRANSFER_AMOUNT, TRANSFER_AMOUNT)).thenReturn(true);

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
            // Given - Setup mocks for deposit operation
            when(economyValidator.isGreaterThanZero(50L)).thenReturn(true);
            when(balanceManager.getBalance(testPlayer)).thenReturn(100L, 150L);

            // Given - Setup mocks for withdraw operation
            when(economyValidator.hasSufficientFunds(150L, 50L)).thenReturn(true);

            // When
            boolean depositResult = economyManager.deposit(testPlayer, 50L);
            boolean withdrawResult = economyManager.withdraw(testPlayer, 50L);

            // Then
            assertTrue(depositResult, "First deposit should succeed");
            assertTrue(withdrawResult, "Withdrawal should succeed");

            // Verify interactions in order
            InOrder inOrder = inOrder(economyValidator, balanceManager);

            // Deposit sequence
            inOrder.verify(economyValidator).isGreaterThanZero(50L);
            inOrder.verify(balanceManager).getBalance(testPlayer);
            inOrder.verify(balanceManager).setBalance(testPlayer, 150L);

            // Withdraw sequence
            inOrder.verify(economyValidator).isGreaterThanZero(50L);
            inOrder.verify(balanceManager).getBalance(testPlayer);
            inOrder.verify(economyValidator).hasSufficientFunds(150L, 50L);
            inOrder.verify(balanceManager).setBalance(testPlayer, 100L);
        }

        @Test
        @DisplayName("Should maintain validator integration throughout operations")
        void operations_ValidatorIntegration_WorksCorrectly() {
            // Test that all operations properly integrate with the validator
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();

            // Test deposit with validator
            when(economyValidator.isGreaterThanZero(100L)).thenReturn(false);
            assertFalse(economyManager.deposit(player1, 100L));
            verify(economyValidator).isGreaterThanZero(100L);

            // Test withdraw with validator
            when(economyValidator.isGreaterThanZero(50L)).thenReturn(false);
            assertFalse(economyManager.withdraw(player1, 50L));
            verify(economyValidator).isGreaterThanZero(50L);

            // Test transfer with validator
            when(economyValidator.isGreaterThanZero(25L)).thenReturn(false);
            assertFalse(economyManager.transferBalance(player1, player2, 25L));
            verify(economyValidator).isGreaterThanZero(25L);
        }
    }
}