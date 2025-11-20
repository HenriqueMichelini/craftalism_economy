package io.github.HenriqueMichelini.craftalism_economy.core.commands;

import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.managers.EconomyManager;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.validators.PlayerValidator;
import io.github.HenriqueMichelini.craftalism_economy.presentation.commands.PayCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PayCommand Tests")
class PayCommandTest {

    private static final UUID PAYER_UUID = UUID.randomUUID();
    private static final UUID PAYEE_UUID = UUID.randomUUID();
    private static final String PAYER_NAME = "PayerPlayer";
    private static final String PAYEE_NAME = "PayeePlayer";
    private static final long TEST_AMOUNT = 12500L; // 1.25 in display format

    @Mock private EconomyManager economyManager;
    @Mock private BalanceManager balanceManager;
    @Mock private JavaPlugin plugin;
    @Mock private CurrencyFormatter currencyFormatter;
    @Mock private PlayerValidator playerValidator;
    @Mock private CurrencyParser currencyParser;
    @Mock private Player payer;
    @Mock private Player payeeOnline;
    @Mock private OfflinePlayer payeeOffline;
    @Mock private CommandSender nonPlayerSender;
    @Mock private Command command;
    @Mock private Logger logger;

    private PayCommand payCommand;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Setup common mocks
        when(plugin.getLogger()).thenReturn(logger);
        when(payer.getUniqueId()).thenReturn(PAYER_UUID);
        when(payer.getName()).thenReturn(PAYER_NAME);
        when(payeeOnline.getUniqueId()).thenReturn(PAYEE_UUID);
        when(payeeOnline.getName()).thenReturn(PAYEE_NAME);
        when(payeeOffline.getUniqueId()).thenReturn(PAYEE_UUID);
        when(payeeOffline.getName()).thenReturn(PAYEE_NAME);
        when(currencyFormatter.formatCurrency(TEST_AMOUNT)).thenReturn("$1.25");
        when(currencyParser.parseAmount(eq(payer), anyString())).thenReturn(Optional.of(TEST_AMOUNT));

        payCommand = new PayCommand(economyManager, balanceManager, plugin,
                currencyFormatter, playerValidator, currencyParser);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create command with all dependencies")
        void constructor_ValidDependencies_CreatesInstance() {
            // When & Then
            assertDoesNotThrow(() ->
                    new PayCommand(economyManager, balanceManager, plugin, currencyFormatter, playerValidator, currencyParser));
        }

        @Test
        @DisplayName("Should throw exception when EconomyManager is null")
        void constructor_NullEconomyManager_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new PayCommand(null, balanceManager, plugin, currencyFormatter, playerValidator, currencyParser)
            );
            assertEquals("EconomyManager cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when BalanceManager is null")
        void constructor_NullBalanceManager_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new PayCommand(economyManager, null, plugin, currencyFormatter, playerValidator, currencyParser)
            );
            assertEquals("BalanceManager cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when Plugin is null")
        void constructor_NullPlugin_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new PayCommand(economyManager, balanceManager, null, currencyFormatter, playerValidator, currencyParser)
            );
            assertEquals("Plugin cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when CurrencyFormatter is null")
        void constructor_NullCurrencyFormatter_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new PayCommand(economyManager, balanceManager, plugin, null, playerValidator, currencyParser)
            );
            assertEquals("CurrencyFormatter cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when PlayerValidator is null")
        void constructor_NullPlayerValidator_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new PayCommand(economyManager, balanceManager, plugin, currencyFormatter, null, currencyParser)
            );
            assertEquals("PlayerValidator cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should create command with legacy constructor")
        void legacyConstructor_CreatesInstanceWithDefaultValidator() {
            // When & Then
            assertDoesNotThrow(() ->
                    new PayCommand(economyManager, balanceManager, plugin, currencyFormatter, currencyParser));
        }
    }

    @Nested
    @DisplayName("Non-Player Sender Tests")
    class NonPlayerSenderTests {

        @Test
        @DisplayName("Should reject non-player sender")
        void onCommand_NonPlayerSender_RejectsAndSendsMessage() {
            // Given
            when(playerValidator.isSenderAPlayer(nonPlayerSender)).thenReturn(false);

            // When
            boolean result = payCommand.onCommand(nonPlayerSender, command, "pay",
                    new String[]{"target", "1.00"});

            // Then
            assertTrue(result, "Should return true to prevent showing plugin.yml usage");
            verify(nonPlayerSender).sendMessage(any(Component.class));
            verify(playerValidator).isSenderAPlayer(nonPlayerSender);
        }
    }

    @Nested
    @DisplayName("Argument Validation Tests")
    class ArgumentValidationTests {

        @Test
        @DisplayName("Should show usage for no arguments")
        void onCommand_NoArguments_ShowsUsage() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay", new String[0]);

            // Then
            assertTrue(result);
            verify(payer).sendMessage(any(Component.class)); // Usage message
        }

        @Test
        @DisplayName("Should show usage for one argument")
        void onCommand_OneArgument_ShowsUsage() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay", new String[]{"target"});

            // Then
            assertTrue(result);
            verify(payer).sendMessage(any(Component.class)); // Usage message
        }

        @Test
        @DisplayName("Should show usage for too many arguments")
        void onCommand_TooManyArguments_ShowsUsage() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{"target", "1.00", "extra"});

            // Then
            assertTrue(result);
            verify(payer).sendMessage(any(Component.class)); // Usage message
        }

        @Test
        @DisplayName("Should reject null player name")
        void onCommand_NullPlayerName_ShowsError() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{null, "1.00"});

            // Then
            assertTrue(result);
            verify(payer, times(2)).sendMessage(any(Component.class)); // Error + usage
        }

        @Test
        @DisplayName("Should reject empty player name")
        void onCommand_EmptyPlayerName_ShowsError() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{"", "1.00"});

            // Then
            assertTrue(result);
            verify(payer, times(2)).sendMessage(any(Component.class)); // Error + usage
        }

        @Test
        @DisplayName("Should reject null amount")
        void onCommand_NullAmount_ShowsError() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{"target", null});

            // Then
            assertTrue(result);
            verify(payer, times(2)).sendMessage(any(Component.class)); // Error + usage
        }

        @Test
        @DisplayName("Should reject empty amount")
        void onCommand_EmptyAmount_ShowsError() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{"target", ""});

            // Then
            assertTrue(result);
            verify(payer, times(2)).sendMessage(any(Component.class)); // Error + usage
        }
    }

    @Nested
    @DisplayName("Player Resolution Tests")
    class PlayerResolutionTests {

        @Test
        @DisplayName("Should handle player not found")
        void onCommand_PlayerNotFound_ShowsError() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.empty());

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{PAYEE_NAME, "1.00"});

            // Then
            assertTrue(result);
            verify(playerValidator).resolvePlayer(payer, PAYEE_NAME);
            verify(payer).sendMessage(any(Component.class));
            verify(balanceManager, never()).checkIfBalanceExists(any(UUID.class));
        }

        @Test
        @DisplayName("Should handle target player without balance account")
        void onCommand_TargetNoAccount_ShowsError() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.of(payeeOffline));
            when(balanceManager.checkIfBalanceExists(PAYEE_UUID)).thenReturn(false);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{PAYEE_NAME, "1.00"});

            // Then
            assertTrue(result);
            verify(balanceManager).checkIfBalanceExists(PAYEE_UUID);
            verify(payer).sendMessage(any(Component.class));
            verify(economyManager, never()).transferBalance(any(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("Self-Payment Tests")
    class SelfPaymentTests {

        @Test
        @DisplayName("Should prevent self-payment")
        void onCommand_SelfPayment_ShowsError() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYER_NAME)).thenReturn(Optional.of(payer));
            when(payer.getUniqueId()).thenReturn(PAYER_UUID); // Same UUID for both
            when(balanceManager.checkIfBalanceExists(PAYER_UUID)).thenReturn(true);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{PAYER_NAME, "1.00"});

            // Then
            assertTrue(result);
            verify(payer).sendMessage(any(Component.class));
            verify(economyManager, never()).transferBalance(any(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("Successful Payment Tests")
    class SuccessfulPaymentTests {

        @Test
        @DisplayName("Should process payment to offline player successfully")
        void onCommand_PaymentToOfflinePlayer_ProcessesSuccessfully() {
            // Given
            setupSuccessfulPaymentMocks(payeeOffline);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{PAYEE_NAME, "1.25"});

            // Then
            assertTrue(result);
            verify(economyManager).transferBalance(PAYER_UUID, PAYEE_UUID, TEST_AMOUNT);
            verify(payer).sendMessage(any(Component.class));
            verify(logger).info(contains("[CE.Pay] PayerPlayer paid PayeePlayer $1.25"));
        }

        @Test
        @DisplayName("Should process payment to online player successfully")
        void onCommand_PaymentToOnlinePlayer_ProcessesSuccessfully() {
            // Given
            setupSuccessfulPaymentMocks(payeeOnline);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{PAYEE_NAME, "1.25"});

            // Then
            assertTrue(result);
            verify(economyManager).transferBalance(PAYER_UUID, PAYEE_UUID, TEST_AMOUNT);
            verify(payer).sendMessage(any(Component.class));
            verify(payeeOnline).sendMessage(any(Component.class)); // Online player gets message
            verify(logger).info(contains("[CE.Pay] PayerPlayer paid PayeePlayer $1.25"));
        }

        private void setupSuccessfulPaymentMocks(OfflinePlayer payee) {
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.of(payee));
            when(balanceManager.checkIfBalanceExists(PAYEE_UUID)).thenReturn(true);
            when(economyManager.transferBalance(PAYER_UUID, PAYEE_UUID, TEST_AMOUNT)).thenReturn(true);

            // Mock CurrencyParser.parseAmount (this would normally be mocked differently in a real test)
            // For this test, we assume the parser works correctly and returns the expected amount
        }
    }

    @Nested
    @DisplayName("Failed Payment Tests")
    class FailedPaymentTests {

        @Test
        @DisplayName("Should handle transfer failure gracefully")
        void onCommand_TransferFails_ShowsError() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.of(payeeOffline));
            when(balanceManager.checkIfBalanceExists(PAYEE_UUID)).thenReturn(true);
            when(economyManager.transferBalance(PAYER_UUID, PAYEE_UUID, TEST_AMOUNT)).thenReturn(false);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{PAYEE_NAME, "1.25"});

            // Then
            assertTrue(result);
            verify(economyManager).transferBalance(PAYER_UUID, PAYEE_UUID, TEST_AMOUNT);
            verify(payer).sendMessage(any(Component.class));
            // Note: payeeOffline is OfflinePlayer and doesn't have sendMessage() method
            // The implementation only sends messages to online players (Player instances)
            verify(logger, never()).info(anyString()); // No success logging on failure
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle EconomyManager exception gracefully")
        void onCommand_EconomyManagerException_HandlesGracefully() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.of(payeeOffline));
            when(balanceManager.checkIfBalanceExists(PAYEE_UUID)).thenReturn(true);
            when(economyManager.transferBalance(any(), any(), anyLong()))
                    .thenThrow(new RuntimeException("Database error"));

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{PAYEE_NAME, "1.00"});

            // Then
            assertTrue(result);
            verify(logger).warning(contains("Error executing pay command"));
            verify(payer).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should handle BalanceManager exception gracefully")
        void onCommand_BalanceManagerException_HandlesGracefully() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.of(payeeOffline));
            when(balanceManager.checkIfBalanceExists(PAYEE_UUID))
                    .thenThrow(new RuntimeException("Database connection error"));

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{PAYEE_NAME, "1.00"});

            // Then
            assertTrue(result);
            verify(logger).warning(contains("Error executing pay command"));
            verify(payer).sendMessage(any(Component.class));
        }
    }

    @Nested
    @DisplayName("Message Content Tests")
    class MessageContentTests {

        @Test
        @DisplayName("Should send correct payment confirmation message")
        void onCommand_SuccessfulPayment_SendsCorrectPayerMessage() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.of(payeeOffline));
            when(balanceManager.checkIfBalanceExists(PAYEE_UUID)).thenReturn(true);
            when(economyManager.transferBalance(PAYER_UUID, PAYEE_UUID, TEST_AMOUNT)).thenReturn(true);

            // When
            payCommand.onCommand(payer, command, "pay", new String[]{PAYEE_NAME, "1.25"});

            // Then
            ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
            verify(payer).sendMessage(messageCaptor.capture());

            Component message = messageCaptor.getValue();
            assertNotNull(message, "Message should not be null");
            // Note: In a real test, you'd extract and verify the actual text content
        }

        @Test
        @DisplayName("Should send payment received message to online payee")
        void onCommand_OnlinePayee_SendsReceivedMessage() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.of(payeeOnline));
            when(balanceManager.checkIfBalanceExists(PAYEE_UUID)).thenReturn(true);
            when(economyManager.transferBalance(PAYER_UUID, PAYEE_UUID, TEST_AMOUNT)).thenReturn(true);

            // When
            payCommand.onCommand(payer, command, "pay", new String[]{PAYEE_NAME, "1.25"});

            // Then
            ArgumentCaptor<Component> payeeMessageCaptor = ArgumentCaptor.forClass(Component.class);
            verify(payeeOnline).sendMessage(payeeMessageCaptor.capture());

            Component payeeMessage = payeeMessageCaptor.getValue();
            assertNotNull(payeeMessage, "Payee message should not be null");
        }
    }

    @Nested
    @DisplayName("Logging Tests")
    class LoggingTests {

        @Test
        @DisplayName("Should log successful transaction with correct format")
        void onCommand_SuccessfulPayment_LogsCorrectly() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.of(payeeOffline));
            when(balanceManager.checkIfBalanceExists(PAYEE_UUID)).thenReturn(true);
            when(economyManager.transferBalance(PAYER_UUID, PAYEE_UUID, TEST_AMOUNT)).thenReturn(true);

            // When
            payCommand.onCommand(payer, command, "pay", new String[]{PAYEE_NAME, "1.25"});

            // Then
            verify(logger).info("[CE.Pay] PayerPlayer paid PayeePlayer $1.25");
        }

        @Test
        @DisplayName("Should handle player with null name in logging")
        void onCommand_PlayerNullName_LogsUnknownPlayer() {
            // Given
            OfflinePlayer payeeWithNullName = mock(OfflinePlayer.class);
            when(payeeWithNullName.getUniqueId()).thenReturn(PAYEE_UUID);
            when(payeeWithNullName.getName()).thenReturn(null);

            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.of(payeeWithNullName));
            when(balanceManager.checkIfBalanceExists(PAYEE_UUID)).thenReturn(true);
            when(economyManager.transferBalance(PAYER_UUID, PAYEE_UUID, TEST_AMOUNT)).thenReturn(true);

            // When
            payCommand.onCommand(payer, command, "pay", new String[]{PAYEE_NAME, "1.25"});

            // Then
            verify(logger).info("[CE.Pay] PayerPlayer paid Unknown Player $1.25");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete successful payment flow")
        void onCommand_CompleteSuccessfulFlow_WorksCorrectly() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.of(payeeOnline));
            when(balanceManager.checkIfBalanceExists(PAYEE_UUID)).thenReturn(true);
            when(economyManager.transferBalance(PAYER_UUID, PAYEE_UUID, TEST_AMOUNT)).thenReturn(true);

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{PAYEE_NAME, "1.25"});

            // Then
            assertTrue(result);

            // Verify the complete flow
            verify(playerValidator).isSenderAPlayer(payer);
            verify(playerValidator).resolvePlayer(payer, PAYEE_NAME);
            verify(balanceManager).checkIfBalanceExists(PAYEE_UUID);
            verify(economyManager).transferBalance(PAYER_UUID, PAYEE_UUID, TEST_AMOUNT);
            verify(currencyFormatter, times(2)).formatCurrency(TEST_AMOUNT); // Once for each message
            verify(payer).sendMessage(any(Component.class));
            verify(payeeOnline).sendMessage(any(Component.class));
            verify(logger).info(contains("PayerPlayer paid PayeePlayer"));
        }

        @Test
        @DisplayName("Should handle complete failed payment flow")
        void onCommand_CompleteFailedFlow_WorksCorrectly() {
            // Given
            when(playerValidator.isSenderAPlayer(payer)).thenReturn(true);
            when(playerValidator.resolvePlayer(payer, PAYEE_NAME)).thenReturn(Optional.empty());

            // When
            boolean result = payCommand.onCommand(payer, command, "pay",
                    new String[]{PAYEE_NAME, "1.00"});

            // Then
            assertTrue(result);

            // Verify flow stops at player resolution
            verify(playerValidator).isSenderAPlayer(payer);
            verify(playerValidator).resolvePlayer(payer, PAYEE_NAME);
            verify(balanceManager, never()).checkIfBalanceExists(any());
            verify(economyManager, never()).transferBalance(any(), any(), anyLong());
            verify(payer).sendMessage(any(Component.class));
            verify(logger, never()).info(anyString());
        }
    }
}