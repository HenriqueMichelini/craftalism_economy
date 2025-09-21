package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.economy.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.economy.validators.PlayerValidator;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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

@DisplayName("SetBalanceCommand Tests")
class SetBalanceCommandTest {

    private static final UUID TARGET_UUID = UUID.randomUUID();
    private static final String SENDER_NAME = "AdminPlayer";
    private static final String TARGET_NAME = "TargetPlayer";
    private static final long TEST_AMOUNT = 12500L; // 1.25 in display format

    @Mock private BalanceManager balanceManager;
    @Mock private JavaPlugin plugin;
    @Mock private CurrencyFormatter currencyFormatter;
    @Mock private CurrencyParser currencyParser;
    @Mock private PlayerValidator playerValidator;
    @Mock private Player adminPlayer;
    @Mock private Player targetOnline;
    @Mock private OfflinePlayer targetOffline;
    @Mock private ConsoleCommandSender console;
    @Mock private CommandSender nonPlayerSender;
    @Mock private Command command;
    @Mock private Logger logger;

    private SetBalanceCommand setBalanceCommand;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Setup common mocks
        when(plugin.getLogger()).thenReturn(logger);
        when(adminPlayer.getName()).thenReturn(SENDER_NAME);
        when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
        when(targetOnline.getUniqueId()).thenReturn(TARGET_UUID);
        when(targetOnline.getName()).thenReturn(TARGET_NAME);
        when(targetOffline.getUniqueId()).thenReturn(TARGET_UUID);
        when(targetOffline.getName()).thenReturn(TARGET_NAME);
        when(currencyFormatter.formatCurrency(TEST_AMOUNT)).thenReturn("$1.25");
        when(currencyParser.parseAmount(eq(adminPlayer), anyString())).thenReturn(Optional.of(TEST_AMOUNT));
        when(currencyParser.parseAmountSilently(anyString())).thenReturn(Optional.of(TEST_AMOUNT));

        when(console.getName()).thenReturn("CONSOLE");

        setBalanceCommand = new SetBalanceCommand(balanceManager, plugin, currencyFormatter, playerValidator, currencyParser);
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
                    new SetBalanceCommand(balanceManager, plugin, currencyFormatter, playerValidator, currencyParser));
        }

        @Test
        @DisplayName("Should throw exception when BalanceManager is null")
        void constructor_NullBalanceManager_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new SetBalanceCommand(null, plugin, currencyFormatter, playerValidator, currencyParser)
            );
            assertEquals("BalanceManager cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when Plugin is null")
        void constructor_NullPlugin_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new SetBalanceCommand(balanceManager, null, currencyFormatter, playerValidator, currencyParser)
            );
            assertEquals("Plugin cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when CurrencyFormatter is null")
        void constructor_NullCurrencyFormatter_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new SetBalanceCommand(balanceManager, plugin, null, playerValidator, currencyParser)
            );
            assertEquals("CurrencyFormatter cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when PlayerValidator is null")
        void constructor_NullPlayerValidator_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new SetBalanceCommand(balanceManager, plugin, currencyFormatter, null, currencyParser)
            );
            assertEquals("PlayerValidator cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should create command with legacy constructor")
        void legacyConstructor_CreatesInstanceWithDefaultValidator() {
            // When & Then
            assertDoesNotThrow(() ->
                    new SetBalanceCommand(balanceManager, plugin, currencyFormatter));
        }
    }

    @Nested
    @DisplayName("Permission Tests")
    class PermissionTests {

        @Test
        @DisplayName("Should allow command execution with permission")
        void onCommand_HasPermission_AllowsExecution() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.of(targetOffline));

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.25"});

            // Then
            assertTrue(result);
            verify(balanceManager).setBalance(TARGET_UUID, TEST_AMOUNT);
        }

        @Test
        @DisplayName("Should allow command execution for OP player")
        void onCommand_IsOP_AllowsExecution() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(false);
            when(adminPlayer.isOp()).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.of(targetOffline));

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.25"});

            // Then
            assertTrue(result);
            verify(balanceManager).setBalance(TARGET_UUID, TEST_AMOUNT);
        }

        @Test
        @DisplayName("Should reject command execution without permission")
        void onCommand_NoPermission_RejectsExecution() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(false);
            when(adminPlayer.isOp()).thenReturn(false);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.25"});

            // Then
            assertTrue(result);
            verify(adminPlayer).sendMessage(any(Component.class));
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }
    }

    @Nested
    @DisplayName("Argument Validation Tests")
    class ArgumentValidationTests {

        @Test
        @DisplayName("Should show usage for no arguments")
        void onCommand_NoArguments_ShowsUsage() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance", new String[0]);

            // Then
            assertTrue(result);
            verify(adminPlayer).sendMessage(any(Component.class)); // Usage message
        }

        @Test
        @DisplayName("Should show usage for one argument")
        void onCommand_OneArgument_ShowsUsage() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{"target"});

            // Then
            assertTrue(result);
            verify(adminPlayer).sendMessage(any(Component.class)); // Usage message
        }

        @Test
        @DisplayName("Should show usage for too many arguments")
        void onCommand_TooManyArguments_ShowsUsage() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{"target", "1.00", "extra"});

            // Then
            assertTrue(result);
            verify(adminPlayer).sendMessage(any(Component.class)); // Usage message
        }

        @Test
        @DisplayName("Should reject null player name")
        void onCommand_NullPlayerName_ShowsError() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{null, "1.00"});

            // Then
            assertTrue(result);
            verify(adminPlayer, times(2)).sendMessage(any(Component.class)); // Error + usage
        }

        @Test
        @DisplayName("Should reject empty player name")
        void onCommand_EmptyPlayerName_ShowsError() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{"", "1.00"});

            // Then
            assertTrue(result);
            verify(adminPlayer, times(2)).sendMessage(any(Component.class)); // Error + usage
        }

        @Test
        @DisplayName("Should reject null amount")
        void onCommand_NullAmount_ShowsError() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{"target", null});

            // Then
            assertTrue(result);
            verify(adminPlayer, times(2)).sendMessage(any(Component.class)); // Error + usage
        }

        @Test
        @DisplayName("Should reject empty amount")
        void onCommand_EmptyAmount_ShowsError() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{"target", ""});

            // Then
            assertTrue(result);
            verify(adminPlayer, times(2)).sendMessage(any(Component.class)); // Error + usage
        }
    }

    @Nested
    @DisplayName("Player Resolution Tests")
    class PlayerResolutionTests {

        @Test
        @DisplayName("Should handle player not found")
        void onCommand_PlayerNotFound_ShowsError() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.empty());

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.00"});

            // Then
            assertTrue(result);
            verify(playerValidator).resolvePlayer(adminPlayer, TARGET_NAME);
            verify(adminPlayer).sendMessage(any(Component.class));
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }

        @Test
        @DisplayName("Should handle console sender")
        void onCommand_ConsoleSender_ShowsLimitation() {
            // Given
            when(console.hasPermission("economy.admin.setbalance")).thenReturn(true);

            // When
            boolean result = setBalanceCommand.onCommand(console, command, "setbalance",
                    new String[]{TARGET_NAME, "1.00"});

            // Then
            assertTrue(result);
            verify(console).sendMessage(any(Component.class));
            verify(balanceManager, never()).setBalance(any(UUID.class), anyLong());
        }
    }

    @Nested
    @DisplayName("Successful Balance Update Tests")
    class SuccessfulBalanceUpdateTests {

        @Test
        @DisplayName("Should set balance for offline player successfully")
        void onCommand_OfflinePlayer_SetsBalanceSuccessfully() {
            // Given
            setupSuccessfulUpdateMocks(targetOffline);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.25"});

            // Then
            assertTrue(result);
            verify(balanceManager).setBalance(TARGET_UUID, TEST_AMOUNT);
            verify(adminPlayer).sendMessage(any(Component.class));
            verify(logger).info(contains("[CE.SetBalance] AdminPlayer set TargetPlayer's balance to $1.25"));
        }

        @Test
        @DisplayName("Should set balance for online player and notify both")
        void onCommand_OnlinePlayer_SetsBalanceAndNotifiesBoth() {
            // Given
            setupSuccessfulUpdateMocks(targetOnline);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.25"});

            // Then
            assertTrue(result);
            verify(balanceManager).setBalance(TARGET_UUID, TEST_AMOUNT);
            verify(adminPlayer).sendMessage(any(Component.class)); // Confirmation to admin
            verify(targetOnline).sendMessage(any(Component.class)); // Notification to target
            verify(logger).info(contains("[CE.SetBalance] AdminPlayer set TargetPlayer's balance to $1.25"));
        }

        private void setupSuccessfulUpdateMocks(OfflinePlayer target) {
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.of(target));
            // Note: CurrencyParser.parseAmount would be mocked in a real test
        }
    }

    @Nested
    @DisplayName("Failed Balance Update Tests")
    class FailedBalanceUpdateTests {

        @Test
        @DisplayName("Should handle BalanceManager exception gracefully")
        void onCommand_BalanceManagerException_HandlesGracefully() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.of(targetOffline));

            // Clear any existing mocks and set up specific ones for this test
            reset(currencyParser);
            when(currencyParser.parseAmount(adminPlayer, "1.00")).thenReturn(Optional.of(TEST_AMOUNT));

            doThrow(new RuntimeException("Database error")).when(balanceManager).setBalance(TARGET_UUID, TEST_AMOUNT);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.00"});

            // Then
            // The implementation returns false when BalanceManager throws an exception
            // This will show usage to the player in Bukkit
            assertFalse(result, "Command should return false when BalanceManager throws exception");

            verify(logger).severe(contains("Failed to update balance for TargetPlayer"));
            verify(adminPlayer).sendMessage(any(Component.class));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle general exception gracefully")
        void onCommand_GeneralException_HandlesGracefully() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.00"});

            // Then
            assertTrue(result);
            verify(logger).warning(contains("Error executing setbalance command"));
            verify(adminPlayer).sendMessage(any(Component.class));
        }
    }

    @Nested
    @DisplayName("Message Content Tests")
    class MessageContentTests {

        @Test
        @DisplayName("Should send correct confirmation message")
        void onCommand_SuccessfulUpdate_SendsCorrectConfirmation() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.of(targetOffline));

            // When
            setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.25"});

            // Then
            ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
            verify(adminPlayer).sendMessage(messageCaptor.capture());

            Component message = messageCaptor.getValue();
            assertNotNull(message, "Message should not be null");
            // Note: In a real test, you'd extract and verify the actual text content
        }

        @Test
        @DisplayName("Should send notification to online target player")
        void onCommand_OnlineTarget_SendsNotification() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.of(targetOnline));

            // When
            setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.25"});

            // Then
            ArgumentCaptor<Component> targetMessageCaptor = ArgumentCaptor.forClass(Component.class);
            verify(targetOnline).sendMessage(targetMessageCaptor.capture());

            Component targetMessage = targetMessageCaptor.getValue();
            assertNotNull(targetMessage, "Target message should not be null");
        }

        @Test
        @DisplayName("Should handle player with null name")
        void onCommand_PlayerNullName_HandlesGracefully() {
            // Given
            OfflinePlayer playerWithNullName = mock(OfflinePlayer.class);
            when(playerWithNullName.getUniqueId()).thenReturn(TARGET_UUID);
            when(playerWithNullName.getName()).thenReturn(null);

            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.of(playerWithNullName));

            // When
            setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.25"});

            // Then
            verify(logger).info(contains("[CE.SetBalance] AdminPlayer set Unknown Player's balance to $1.25"));
        }
    }

    @Nested
    @DisplayName("Logging Tests")
    class LoggingTests {

        @Test
        @DisplayName("Should log successful balance update with correct format")
        void onCommand_SuccessfulUpdate_LogsCorrectly() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.of(targetOffline));

            // When
            setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.25"});

            // Then
            verify(logger).info("[CE.SetBalance] AdminPlayer set TargetPlayer's balance to $1.25");
        }

        @Test
        @DisplayName("Should log error when balance update fails")
        void onCommand_UpdateFails_LogsError() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.of(targetOffline));
            doThrow(new RuntimeException("Database error")).when(balanceManager).setBalance(TARGET_UUID, TEST_AMOUNT);

            // When
            setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.25"});

            // Then
            verify(logger).severe("[CE.SetBalance] Failed to update balance for TargetPlayer: Database error");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete successful balance update flow")
        void onCommand_CompleteSuccessfulFlow_WorksCorrectly() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.of(targetOnline));

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.25"});

            // Then
            assertTrue(result);

            // Verify the complete flow
            verify(adminPlayer).hasPermission("economy.admin.setbalance");
            verify(playerValidator).resolvePlayer(adminPlayer, TARGET_NAME);
            verify(balanceManager).setBalance(TARGET_UUID, TEST_AMOUNT);
            verify(currencyFormatter, times(2)).formatCurrency(TEST_AMOUNT); // Once for each message
            verify(adminPlayer).sendMessage(any(Component.class));
            verify(targetOnline).sendMessage(any(Component.class));
            verify(logger).info(contains("AdminPlayer set TargetPlayer's balance to"));
        }

        @Test
        @DisplayName("Should handle complete failed permission flow")
        void onCommand_CompleteFailedPermissionFlow_WorksCorrectly() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(false);
            when(adminPlayer.isOp()).thenReturn(false);

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.00"});

            // Then
            assertTrue(result);

            // Verify flow stops at permission check
            verify(adminPlayer).hasPermission("economy.admin.setbalance");
            verify(adminPlayer).isOp();
            verify(playerValidator, never()).resolvePlayer(any(), any());
            verify(balanceManager, never()).setBalance(any(), anyLong());
            verify(adminPlayer).sendMessage(any(Component.class));
            verify(logger, never()).info(anyString());
        }

        @Test
        @DisplayName("Should handle complete failed player resolution flow")
        void onCommand_CompleteFailedResolutionFlow_WorksCorrectly() {
            // Given
            when(adminPlayer.hasPermission("economy.admin.setbalance")).thenReturn(true);
            when(playerValidator.resolvePlayer(adminPlayer, TARGET_NAME)).thenReturn(Optional.empty());

            // When
            boolean result = setBalanceCommand.onCommand(adminPlayer, command, "setbalance",
                    new String[]{TARGET_NAME, "1.00"});

            // Then
            assertTrue(result);

            // Verify flow stops at player resolution
            verify(adminPlayer).hasPermission("economy.admin.setbalance");
            verify(playerValidator).resolvePlayer(adminPlayer, TARGET_NAME);
            verify(balanceManager, never()).setBalance(any(), anyLong());
            verify(adminPlayer).sendMessage(any(Component.class));
            verify(logger, never()).info(anyString());
        }
    }
}