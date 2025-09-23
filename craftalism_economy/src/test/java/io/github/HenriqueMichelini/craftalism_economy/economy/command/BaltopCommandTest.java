package io.github.HenriqueMichelini.craftalism_economy.economy.command;

import io.github.HenriqueMichelini.craftalism_economy.economy.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.economy.managers.BalanceManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@DisplayName("BaltopCommand Tests")
class BaltopCommandTest {

    private static final UUID PLAYER1_UUID = UUID.randomUUID();
    private static final UUID PLAYER2_UUID = UUID.randomUUID();
    private static final UUID PLAYER3_UUID = UUID.randomUUID();
    private static final String PLAYER1_NAME = "RichPlayer";
    private static final String PLAYER2_NAME = "MiddlePlayer";
    private static final String PLAYER3_NAME = "PoorPlayer";

    @Mock private BalanceManager balanceManager;
    @Mock private JavaPlugin plugin;
    @Mock private CurrencyFormatter currencyFormatter;
    @Mock private Player senderPlayer;
    @Mock private ConsoleCommandSender console;
    @Mock private Command command;
    @Mock private Logger logger;
    @Mock private OfflinePlayer offlinePlayer1;
    @Mock private OfflinePlayer offlinePlayer2;
    @Mock private OfflinePlayer offlinePlayer3;

    private BaltopCommand baltopCommand;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Setup common mocks
        when(plugin.getLogger()).thenReturn(logger);
        when(senderPlayer.getName()).thenReturn("CommandSender");

        // Setup offline players
        when(offlinePlayer1.getName()).thenReturn(PLAYER1_NAME);
        when(offlinePlayer2.getName()).thenReturn(PLAYER2_NAME);
        when(offlinePlayer3.getName()).thenReturn(PLAYER3_NAME);

        baltopCommand = new BaltopCommand(balanceManager, plugin, currencyFormatter);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create command with valid dependencies")
        void constructor_ValidDependencies_CreatesInstance() {
            // When & Then
            assertDoesNotThrow(() ->
                    new BaltopCommand(balanceManager, plugin, currencyFormatter));
        }

        @Nested
        @DisplayName("Sender Validation Tests")
        class SenderValidationTests {

            @Test
            @DisplayName("Should allow player sender")
            void onCommand_PlayerSender_AllowsExecution() {
                // Given
                setupDefaultBalanceData();
                setupCurrencyFormatting();

                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    setupBukkitPlayerMocks(bukkit);

                    // When
                    boolean result = baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                    // Then
                    assertTrue(result);
                    verify(senderPlayer, atLeastOnce()).sendMessage(any(Component.class));
                    verify(logger).info(contains("[CE.Baltop] CommandSender viewed balance rankings"));
                }
            }

            @Test
            @DisplayName("Should reject console sender")
            void onCommand_ConsoleSender_RejectsExecution() {
                // Given
                when(console.getName()).thenReturn("CONSOLE");

                // When
                boolean result = baltopCommand.onCommand(console, command, "baltop", new String[0]);

                // Then
                assertTrue(result);
                verify(console).sendMessage(any(Component.class));
                verify(balanceManager, never()).getAllBalances();
                verify(logger, never()).info(anyString());
            }
        }

        @Nested
        @DisplayName("Balance Data Tests")
        class BalanceDataTests {

            @Test
            @DisplayName("Should display top balances correctly")
            void onCommand_WithBalanceData_DisplaysCorrectly() {
                // Given
                ConcurrentHashMap<UUID, Long> balances = createTestBalanceData();
                when(balanceManager.getAllBalances()).thenReturn(balances);
                setupCurrencyFormatting();

                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    setupBukkitPlayerMocks(bukkit);

                    // When
                    boolean result = baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                    // Then
                    assertTrue(result);

                    // Verify header was sent
                    ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
                    verify(senderPlayer, times(4)).sendMessage(messageCaptor.capture()); // Header + 3 entries

                    List<Component> messages = messageCaptor.getAllValues();
                    assertNotNull(messages);
                    assertEquals(4, messages.size());

                    verify(logger).info("[CE.Baltop] CommandSender viewed balance rankings");
                }
            }

            @Test
            @DisplayName("Should handle empty balance data")
            void onCommand_EmptyBalanceData_ShowsNoDataMessage() {
                // Given
                when(balanceManager.getAllBalances()).thenReturn(new ConcurrentHashMap<>());

                // When
                boolean result = baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                // Then
                assertTrue(result);
                verify(senderPlayer).sendMessage(any(Component.class)); // "No data available" message
                verify(logger).info("[CE.Baltop] CommandSender viewed balance rankings");
            }

            @Test
            @DisplayName("Should filter out zero and negative balances")
            void onCommand_WithZeroAndNegativeBalances_FiltersCorrectly() {
                // Given
                ConcurrentHashMap<UUID, Long> balances = new ConcurrentHashMap<>();
                balances.put(PLAYER1_UUID, 50000L); // Positive balance
                balances.put(PLAYER2_UUID, 0L);     // Zero balance - should be filtered
                balances.put(PLAYER3_UUID, -1000L); // Negative balance - should be filtered

                when(balanceManager.getAllBalances()).thenReturn(balances);
                setupCurrencyFormatting();

                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    bukkit.when(() -> Bukkit.getOfflinePlayer(PLAYER1_UUID)).thenReturn(offlinePlayer1);

                    // When
                    boolean result = baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                    // Then
                    assertTrue(result);

                    // Verify only 2 messages sent (header + 1 entry)
                    verify(senderPlayer, times(2)).sendMessage(any(Component.class));
                }
            }

            @Test
            @DisplayName("Should sort balances in descending order")
            void onCommand_UnsortedBalances_SortsDescending() {
                // Given - balances in random order
                ConcurrentHashMap<UUID, Long> balances = new ConcurrentHashMap<>();
                balances.put(PLAYER2_UUID, 25000L); // Middle
                balances.put(PLAYER1_UUID, 50000L); // Highest
                balances.put(PLAYER3_UUID, 10000L); // Lowest

                when(balanceManager.getAllBalances()).thenReturn(balances);
                when(currencyFormatter.formatCurrency(50000L)).thenReturn("$500.00");
                when(currencyFormatter.formatCurrency(25000L)).thenReturn("$250.00");
                when(currencyFormatter.formatCurrency(10000L)).thenReturn("$100.00");

                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    setupBukkitPlayerMocks(bukkit);

                    // When
                    baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                    // Then
                    // Verify currency formatter was called in descending order
                    InOrder inOrder = inOrder(currencyFormatter);
                    inOrder.verify(currencyFormatter).formatCurrency(50000L); // First (highest)
                    inOrder.verify(currencyFormatter).formatCurrency(25000L); // Second
                    inOrder.verify(currencyFormatter).formatCurrency(10000L); // Third (lowest)
                }
            }


            @Test
            @DisplayName("Should limit results to top 10")
            void onCommand_MoreThan10Players_LimitsToTop10() {
                // Given - 15 players with balances
                ConcurrentHashMap<UUID, Long> balances = new ConcurrentHashMap<>();
                for (int i = 1; i <= 15; i++) {
                    UUID uuid = UUID.randomUUID();
                    balances.put(uuid, (long) i * 1000); // Different balances
                }

                when(balanceManager.getAllBalances()).thenReturn(balances);
                when(currencyFormatter.formatCurrency(anyLong())).thenReturn("$X.XX");

                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    bukkit.when(() -> Bukkit.getOfflinePlayer(any(UUID.class)))
                            .thenReturn(mock(OfflinePlayer.class));

                    OfflinePlayer mockPlayer = mock(OfflinePlayer.class);
                    when(mockPlayer.getName()).thenReturn("TestPlayer");
                    bukkit.when(() -> Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(mockPlayer);

                    // When
                    baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                    // Then
                    // Verify exactly 11 messages sent (header + 10 entries + footer + next page = 13)
                    verify(senderPlayer, times(13)).sendMessage(any(Component.class));
                }
            }
        }

        @Nested
        @DisplayName("Player Name Resolution Tests")
        class PlayerNameResolutionTests {

            @Test
            @DisplayName("Should handle players with valid names")
            void onCommand_PlayersWithNames_DisplaysCorrectly() {
                // Given
                setupDefaultBalanceData();
                setupCurrencyFormatting();

                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    setupBukkitPlayerMocks(bukkit);

                    // When
                    baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                    // Then
                    verify(senderPlayer, times(4)).sendMessage(any(Component.class));
                    bukkit.verify(() -> Bukkit.getOfflinePlayer(PLAYER1_UUID));
                    bukkit.verify(() -> Bukkit.getOfflinePlayer(PLAYER2_UUID));
                    bukkit.verify(() -> Bukkit.getOfflinePlayer(PLAYER3_UUID));
                }
            }

            @Test
            @DisplayName("Should handle players with null names")
            void onCommand_PlayersWithNullNames_ShowsUnknownPlayer() {
                // Given
                ConcurrentHashMap<UUID, Long> balances = new ConcurrentHashMap<>(); // Changed from Map.of()
                balances.put(PLAYER1_UUID, 50000L);
                when(balanceManager.getAllBalances()).thenReturn(balances);
                when(currencyFormatter.formatCurrency(50000L)).thenReturn("$500.00");

                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    OfflinePlayer nullNamePlayer = mock(OfflinePlayer.class);
                    when(nullNamePlayer.getName()).thenReturn(null);
                    bukkit.when(() -> Bukkit.getOfflinePlayer(PLAYER1_UUID)).thenReturn(nullNamePlayer);

                    // When
                    baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                    // Then
                    verify(senderPlayer, times(2)).sendMessage(any(Component.class)); // Header + 1 entry
                    // The "Unknown Player" text should be used, but we can't directly verify the component content
                }
            }

            @Test
            @DisplayName("Should handle Bukkit.getOfflinePlayer exceptions")
            void onCommand_BukkitException_HandlesGracefully() {
                // Given
                ConcurrentHashMap<UUID, Long> balances = new ConcurrentHashMap<>(); // Changed from Map.of()
                balances.put(PLAYER1_UUID, 50000L);
                when(balanceManager.getAllBalances()).thenReturn(balances);
                when(currencyFormatter.formatCurrency(50000L)).thenReturn("$500.00");

                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    bukkit.when(() -> Bukkit.getOfflinePlayer(PLAYER1_UUID))
                            .thenThrow(new RuntimeException("Server error"));

                    // When
                    boolean result = baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                    // Then
                    assertTrue(result);
                    verify(senderPlayer, times(2)).sendMessage(any(Component.class)); // Still shows entry with "Unknown Player"
                }
            }
        }

        @Nested
        @DisplayName("Error Handling Tests")
        class ErrorHandlingTests {

            @Test
            @DisplayName("Should handle BalanceManager exception gracefully")
            void onCommand_BalanceManagerException_HandlesGracefully() {
                // Given
                when(balanceManager.getAllBalances()).thenThrow(new RuntimeException("Database error"));

                // When
                boolean result = baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                // Then
                assertTrue(result);
                verify(logger).warning(contains("[CE.Baltop] Error executing baltop command"));
                verify(senderPlayer).sendMessage(any(Component.class));
                verify(logger, never()).info(anyString()); // Command usage should not be logged on error
            }

            @Test
            @DisplayName("Should handle CurrencyFormatter exception gracefully")
            void onCommand_CurrencyFormatterException_HandlesGracefully() {
                // Given
                setupDefaultBalanceData();
                when(currencyFormatter.formatCurrency(anyLong()))
                        .thenThrow(new RuntimeException("Formatting error"));

                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    setupBukkitPlayerMocks(bukkit);

                    // When
                    boolean result = baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                    // Then
                    assertTrue(result);
                    verify(logger, atLeastOnce()).warning(contains("[CE.Baltop] Error formatting currency for balance"));
                    verify(senderPlayer, atLeast(1)).sendMessage(any(Component.class));
                    verify(senderPlayer).sendMessage(
                            ArgumentMatchers.<net.kyori.adventure.text.Component>argThat(comp -> comp.toString().contains("Top"))
                    );
                }
            }
        }

        @Nested
        @DisplayName("Logging Tests")
        class LoggingTests {

            @Test
            @DisplayName("Should log successful command execution")
            void onCommand_Success_LogsCorrectly() {
                // Given
                setupDefaultBalanceData();
                setupCurrencyFormatting();

                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    setupBukkitPlayerMocks(bukkit);

                    // When
                    baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                    // Then
                    verify(logger).info("[CE.Baltop] CommandSender viewed balance rankings");
                }
            }

            @Test
            @DisplayName("Should log errors when exceptions occur")
            void onCommand_Exception_LogsError() {
                // Given
                when(balanceManager.getAllBalances()).thenThrow(new RuntimeException("Test error"));

                // When
                baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                // Then
                verify(logger).warning("[CE.Baltop] Error executing baltop command: Test error");
            }
        }

        @Nested
        @DisplayName("Integration Tests")
        class IntegrationTests {

            @Test
            @DisplayName("Should handle complete successful flow")
            void onCommand_CompleteFlow_WorksCorrectly() {
                // Given
                setupDefaultBalanceData();
                setupCurrencyFormatting();

                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    setupBukkitPlayerMocks(bukkit);

                    // When
                    boolean result = baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                    // Then
                    assertTrue(result);

                    // Verify complete flow
                    verify(balanceManager).getAllBalances();
                    verify(currencyFormatter, times(3)).formatCurrency(anyLong());
                    verify(senderPlayer, times(4)).sendMessage(any(Component.class)); // Header + 3 entries
                    verify(logger).info(contains("CommandSender viewed balance rankings"));

                    // Verify Bukkit calls for player name resolution
                    bukkit.verify(() -> Bukkit.getOfflinePlayer(PLAYER1_UUID));
                    bukkit.verify(() -> Bukkit.getOfflinePlayer(PLAYER2_UUID));
                    bukkit.verify(() -> Bukkit.getOfflinePlayer(PLAYER3_UUID));
                }
            }

            @Test
            @DisplayName("Should handle empty server gracefully")
            void onCommand_EmptyServer_HandlesGracefully() {
                // Given
                when(balanceManager.getAllBalances()).thenReturn(new ConcurrentHashMap<>());

                // When
                boolean result = baltopCommand.onCommand(senderPlayer, command, "baltop", new String[0]);

                // Then
                assertTrue(result);
                verify(senderPlayer).sendMessage(any(Component.class)); // "No data available" message
                verify(logger).info(contains("CommandSender viewed balance rankings"));
                verify(currencyFormatter, never()).formatCurrency(anyLong());
            }
        }

        // Helper methods

        private void setupDefaultBalanceData() {
            ConcurrentHashMap<UUID, Long> balances = createTestBalanceData();
            when(balanceManager.getAllBalances()).thenReturn(balances);
        }

        private ConcurrentHashMap<UUID, Long> createTestBalanceData() {
            ConcurrentHashMap<UUID, Long> balances = new ConcurrentHashMap<>();
            balances.put(PLAYER1_UUID, 50000L); // $500.00
            balances.put(PLAYER2_UUID, 25000L); // $250.00
            balances.put(PLAYER3_UUID, 10000L); // $100.00
            return balances;
        }

        private void setupCurrencyFormatting() {
            when(currencyFormatter.formatCurrency(50000L)).thenReturn("$500.00");
            when(currencyFormatter.formatCurrency(25000L)).thenReturn("$250.00");
            when(currencyFormatter.formatCurrency(10000L)).thenReturn("$100.00");
        }

        private void setupBukkitPlayerMocks(MockedStatic<Bukkit> bukkit) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(PLAYER1_UUID)).thenReturn(offlinePlayer1);
            bukkit.when(() -> Bukkit.getOfflinePlayer(PLAYER2_UUID)).thenReturn(offlinePlayer2);
            bukkit.when(() -> Bukkit.getOfflinePlayer(PLAYER3_UUID)).thenReturn(offlinePlayer3);
        }
    }
}