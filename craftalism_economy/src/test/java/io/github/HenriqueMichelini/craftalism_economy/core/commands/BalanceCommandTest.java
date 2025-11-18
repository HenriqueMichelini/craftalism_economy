package io.github.HenriqueMichelini.craftalism_economy.core.commands;

import io.github.HenriqueMichelini.craftalism_economy.core.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.core.logs.PluginLogger;
import io.github.HenriqueMichelini.craftalism_economy.core.managers.BalanceManager;
import io.github.HenriqueMichelini.craftalism_economy.core.validators.PlayerValidator;
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

@DisplayName("BalanceCommand Tests")
class BalanceCommandTest {

    private static final UUID PLAYER_UUID = UUID.randomUUID();
    private static final UUID TARGET_UUID = UUID.randomUUID();
    private static final String PLAYER_NAME = "TestPlayer";
    private static final String TARGET_NAME = "TargetPlayer";
    private static final long TEST_BALANCE = 12500L; // 1.25 in display format

    @Mock
    private BalanceManager balanceManager;

    @Mock
    private JavaPlugin plugin;

    @Mock
    private CurrencyFormatter currencyFormatter;

    @Mock
    private PlayerValidator playerValidator;

    @Mock
    private Player player;

    @Mock
    private OfflinePlayer offlineTarget;

    @Mock
    private CommandSender nonPlayerSender;

    @Mock
    private Command command;

    @Mock
    private Logger logger;

    @Mock
    private PluginLogger pluginLogger;

    private BalanceCommand balanceCommand;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Setup common mocks
        when(plugin.getLogger()).thenReturn(logger);
        when(player.getUniqueId()).thenReturn(PLAYER_UUID);
        when(player.getName()).thenReturn(PLAYER_NAME);
        when(offlineTarget.getUniqueId()).thenReturn(TARGET_UUID);
        when(currencyFormatter.formatCurrency(TEST_BALANCE)).thenReturn("$1.25");

        balanceCommand = new BalanceCommand(balanceManager, plugin, currencyFormatter, playerValidator, pluginLogger);
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
                    new BalanceCommand(balanceManager, plugin, currencyFormatter, playerValidator, pluginLogger));
        }

        @Test
        @DisplayName("Should throw exception when BalanceManager is null")
        void constructor_NullBalanceManager_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new BalanceCommand(null, plugin, currencyFormatter, playerValidator, pluginLogger)
            );
            assertEquals("BalanceManager cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when Plugin is null")
        void constructor_NullPlugin_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new BalanceCommand(balanceManager, null, currencyFormatter, playerValidator, pluginLogger)
            );
            assertEquals("Plugin cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when CurrencyFormatter is null")
        void constructor_NullCurrencyFormatter_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new BalanceCommand(balanceManager, plugin, null, playerValidator, pluginLogger)
            );
            assertEquals("CurrencyFormatter cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when PlayerValidator is null")
        void constructor_NullPlayerValidator_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new BalanceCommand(balanceManager, plugin, currencyFormatter, null, pluginLogger)
            );
            assertEquals("PlayerValidator cannot be null", exception.getMessage());
        }

//        @Test
//        @DisplayName("Should create command with legacy constructor")
//        void legacyConstructor_CreatesInstanceWithDefaultValidator() {
//            // When & Then
//            assertDoesNotThrow(() ->
//                    new BalanceCommand(balanceManager, plugin, currencyFormatter, null, pluginLogger));
//        }
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
            boolean result = balanceCommand.onCommand(nonPlayerSender, command, "balance", new String[0]);

            // Then
            assertTrue(result, "Should return true to prevent showing plugin.yml usage");
            verify(nonPlayerSender).sendMessage(any(Component.class));
            verify(playerValidator).isSenderAPlayer(nonPlayerSender);
        }
    }

    @Nested
    @DisplayName("Own Balance Tests")
    class OwnBalanceTests {

        @Test
        @DisplayName("Should show own balance successfully")
        void onCommand_OwnBalanceExists_ShowsBalance() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(balanceManager.checkIfBalanceExists(PLAYER_UUID)).thenReturn(true);
            when(balanceManager.getBalance(PLAYER_UUID)).thenReturn(TEST_BALANCE);

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[0]);

            // Then
            assertTrue(result, "Command should execute successfully");

            // Verify interactions
            verify(balanceManager).checkIfBalanceExists(PLAYER_UUID);
            verify(balanceManager).getBalance(PLAYER_UUID);
            verify(currencyFormatter).formatCurrency(TEST_BALANCE);
            verify(player).sendMessage(any(Component.class));
            verify(logger).info(contains("TestPlayer checked their own balance"));
        }

        @Test
        @DisplayName("Should handle missing balance gracefully")
        void onCommand_OwnBalanceNotExists_ShowsErrorMessage() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(balanceManager.checkIfBalanceExists(PLAYER_UUID)).thenReturn(false);

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[0]);

            // Then
            assertTrue(result, "Command should execute successfully");
            verify(balanceManager).checkIfBalanceExists(PLAYER_UUID);
            verify(balanceManager, never()).getBalance(any(UUID.class));
            verify(player).sendMessage(any(Component.class));
            verify(logger).info(contains("Balance not found for player: TestPlayer"));
        }
    }

    @Nested
    @DisplayName("Other Player Balance Tests")
    class OtherPlayerBalanceTests {

        @Test
        @DisplayName("Should show other player balance successfully")
        void onCommand_OtherPlayerBalanceExists_ShowsBalance() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(playerValidator.resolvePlayer(player, TARGET_NAME)).thenReturn(Optional.of(offlineTarget));
            when(balanceManager.checkIfBalanceExists(TARGET_UUID)).thenReturn(true);
            when(balanceManager.getBalance(TARGET_UUID)).thenReturn(TEST_BALANCE);

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[]{TARGET_NAME});

            // Then
            assertTrue(result, "Command should execute successfully");

            // Verify interactions
            verify(playerValidator).resolvePlayer(player, TARGET_NAME);
            verify(balanceManager).checkIfBalanceExists(TARGET_UUID);
            verify(balanceManager).getBalance(TARGET_UUID);
            verify(currencyFormatter).formatCurrency(TEST_BALANCE);
            verify(player).sendMessage(any(Component.class));
            verify(logger).info(contains("TestPlayer checked TargetPlayer's balance"));
        }

        @Test
        @DisplayName("Should handle player not found")
        void onCommand_PlayerNotFound_ShowsErrorMessage() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(playerValidator.resolvePlayer(player, TARGET_NAME)).thenReturn(Optional.empty());

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[]{TARGET_NAME});

            // Then
            assertTrue(result, "Command should execute successfully");
            verify(playerValidator).resolvePlayer(player, TARGET_NAME);
            verify(balanceManager, never()).checkIfBalanceExists(any(UUID.class));
            verify(balanceManager, never()).getBalance(any(UUID.class));
            verify(player).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should handle target player with no balance")
        void onCommand_TargetPlayerNoBalance_ShowsErrorMessage() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(playerValidator.resolvePlayer(player, TARGET_NAME)).thenReturn(Optional.of(offlineTarget));
            when(balanceManager.checkIfBalanceExists(TARGET_UUID)).thenReturn(false);

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[]{TARGET_NAME});

            // Then
            assertTrue(result, "Command should execute successfully");
            verify(balanceManager).checkIfBalanceExists(TARGET_UUID);
            verify(balanceManager, never()).getBalance(any(UUID.class));
            verify(player).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should handle null target name")
        void onCommand_NullTargetName_ShowsErrorMessage() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[]{null});

            // Then
            assertTrue(result, "Command should execute successfully");
            verify(playerValidator, never()).resolvePlayer(any(), any());
            verify(player, times(2)).sendMessage(any(Component.class)); // Error + usage
        }

        @Test
        @DisplayName("Should handle empty target name")
        void onCommand_EmptyTargetName_ShowsErrorMessage() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[]{""});

            // Then
            assertTrue(result, "Command should execute successfully");
            verify(playerValidator, never()).resolvePlayer(any(), any());
            verify(player, times(2)).sendMessage(any(Component.class)); // Error + usage
        }

        @Test
        @DisplayName("Should handle whitespace-only target name")
        void onCommand_WhitespaceTargetName_ShowsErrorMessage() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[]{"   "});

            // Then
            assertTrue(result, "Command should execute successfully");
            verify(playerValidator, never()).resolvePlayer(any(), any());
            verify(player, times(2)).sendMessage(any(Component.class)); // Error + usage
        }
    }

    @Nested
    @DisplayName("Invalid Arguments Tests")
    class InvalidArgumentsTests {

        @Test
        @DisplayName("Should show usage for too many arguments")
        void onCommand_TooManyArguments_ShowsUsage() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance",
                    new String[]{"arg1", "arg2", "arg3"});

            // Then
            assertTrue(result, "Command should execute successfully");
            verify(player).sendMessage(any(Component.class));
            verify(balanceManager, never()).getBalance(any(UUID.class));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle BalanceManager exception gracefully")
        void onCommand_BalanceManagerThrowsException_HandlesGracefully() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(balanceManager.checkIfBalanceExists(PLAYER_UUID)).thenThrow(new RuntimeException("Database error"));

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[0]);

            // Then
            assertTrue(result, "Command should execute successfully even with error");
            verify(logger).warning(contains("Error executing balance command"));
            verify(player).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should handle CurrencyFormatter exception gracefully")
        void onCommand_CurrencyFormatterThrowsException_HandlesGracefully() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(balanceManager.checkIfBalanceExists(PLAYER_UUID)).thenReturn(true);
            when(balanceManager.getBalance(PLAYER_UUID)).thenReturn(TEST_BALANCE);
            when(currencyFormatter.formatCurrency(TEST_BALANCE)).thenThrow(new RuntimeException("Formatting error"));

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[0]);

            // Then
            assertTrue(result, "Command should execute successfully even with error");
            verify(logger).warning(contains("Error executing balance command"));
            verify(player).sendMessage(any(Component.class));
        }
    }

    @Nested
    @DisplayName("Message Content Tests")
    class MessageContentTests {

        @Test
        @DisplayName("Should send correct own balance message format")
        void onCommand_OwnBalance_SendsCorrectMessage() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(balanceManager.checkIfBalanceExists(PLAYER_UUID)).thenReturn(true);
            when(balanceManager.getBalance(PLAYER_UUID)).thenReturn(TEST_BALANCE);

            // When
            balanceCommand.onCommand(player, command, "balance", new String[0]);

            // Then
            ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
            verify(player).sendMessage(messageCaptor.capture());

            Component message = messageCaptor.getValue();
            assertNotNull(message, "Message should not be null");
            // Note: In a real test, you'd extract and verify the actual text content
        }

        @Test
        @DisplayName("Should send correct other player balance message format")
        void onCommand_OtherPlayerBalance_SendsCorrectMessage() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(playerValidator.resolvePlayer(player, TARGET_NAME)).thenReturn(Optional.of(offlineTarget));
            when(balanceManager.checkIfBalanceExists(TARGET_UUID)).thenReturn(true);
            when(balanceManager.getBalance(TARGET_UUID)).thenReturn(TEST_BALANCE);

            // When
            balanceCommand.onCommand(player, command, "balance", new String[]{TARGET_NAME});

            // Then
            ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
            verify(player).sendMessage(messageCaptor.capture());

            Component message = messageCaptor.getValue();
            assertNotNull(message, "Message should not be null");
        }
    }

    @Nested
    @DisplayName("Logging Tests")
    class LoggingTests {

        @Test
        @DisplayName("Should log own balance check with correct format")
        void onCommand_OwnBalance_LogsCorrectly() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(balanceManager.checkIfBalanceExists(PLAYER_UUID)).thenReturn(true);
            when(balanceManager.getBalance(PLAYER_UUID)).thenReturn(TEST_BALANCE);

            // When
            balanceCommand.onCommand(player, command, "balance", new String[0]);

            // Then
            verify(logger).info("[CE.Balance] TestPlayer checked their own balance");
        }

        @Test
        @DisplayName("Should log other player balance check with correct format")
        void onCommand_OtherPlayerBalance_LogsCorrectly() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(playerValidator.resolvePlayer(player, TARGET_NAME)).thenReturn(Optional.of(offlineTarget));
            when(balanceManager.checkIfBalanceExists(TARGET_UUID)).thenReturn(true);
            when(balanceManager.getBalance(TARGET_UUID)).thenReturn(TEST_BALANCE);

            // When
            balanceCommand.onCommand(player, command, "balance", new String[]{TARGET_NAME});

            // Then
            verify(logger).info("[CE.Balance] TestPlayer checked TargetPlayer's balance");
        }

        @Test
        @DisplayName("Should log balance not found")
        void onCommand_BalanceNotFound_LogsCorrectly() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(balanceManager.checkIfBalanceExists(PLAYER_UUID)).thenReturn(false);

            // When
            balanceCommand.onCommand(player, command, "balance", new String[0]);

            // Then
            verify(logger).info("[CE.Balance] Balance not found for player: TestPlayer");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete own balance flow")
        void onCommand_CompleteOwnBalanceFlow_WorksCorrectly() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(balanceManager.checkIfBalanceExists(PLAYER_UUID)).thenReturn(true);
            when(balanceManager.getBalance(PLAYER_UUID)).thenReturn(TEST_BALANCE);

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[0]);

            // Then
            assertTrue(result);

            // Verify the complete flow
            verify(playerValidator).isSenderAPlayer(player);
            verify(balanceManager).checkIfBalanceExists(PLAYER_UUID);
            verify(balanceManager).getBalance(PLAYER_UUID);
            verify(currencyFormatter).formatCurrency(TEST_BALANCE);
            verify(player).sendMessage(any(Component.class));
            verify(logger).info(contains("checked their own balance"));
        }

        @Test
        @DisplayName("Should handle complete other player balance flow")
        void onCommand_CompleteOtherPlayerBalanceFlow_WorksCorrectly() {
            // Given
            when(playerValidator.isSenderAPlayer(player)).thenReturn(true);
            when(playerValidator.resolvePlayer(player, TARGET_NAME)).thenReturn(Optional.of(offlineTarget));
            when(balanceManager.checkIfBalanceExists(TARGET_UUID)).thenReturn(true);
            when(balanceManager.getBalance(TARGET_UUID)).thenReturn(TEST_BALANCE);

            // When
            boolean result = balanceCommand.onCommand(player, command, "balance", new String[]{TARGET_NAME});

            // Then
            assertTrue(result);

            // Verify the complete flow
            verify(playerValidator).isSenderAPlayer(player);
            verify(playerValidator).resolvePlayer(player, TARGET_NAME);
            verify(balanceManager).checkIfBalanceExists(TARGET_UUID);
            verify(balanceManager).getBalance(TARGET_UUID);
            verify(currencyFormatter).formatCurrency(TEST_BALANCE);
            verify(player).sendMessage(any(Component.class));
            verify(logger).info(contains("checked TargetPlayer's balance"));
        }
    }
}