package io.github.HenriqueMichelini.craftalism_economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism_economy.application.service.BaltopCommandApplicationService;
import io.github.HenriqueMichelini.craftalism_economy.application.service.BaltopCommandApplicationService.BaltopEntry;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.BaltopMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.*;

@DisplayName("BaltopCommand Tests")
class  BaltopCommandTest {

    private BaltopMessages messages;
    private BaltopCommandApplicationService service;
    private CurrencyFormatter formatter;
    private BaltopCommand command;

    private Player player;
    private CommandSender consoleSender;
    private Command mockCommand;

    @BeforeEach
    void setUp() {
        messages = mock(BaltopMessages.class);
        service = mock(BaltopCommandApplicationService.class);
        formatter = mock(CurrencyFormatter.class);
        command = new BaltopCommand(messages, service, formatter);

        player = mock(Player.class);
        consoleSender = mock(CommandSender.class);
        mockCommand = mock(Command.class);
    }

    // Successful baltop display tests
    @Test
    @DisplayName("Should display baltop successfully for player")
    void shouldDisplayBaltopSuccessfullyForPlayer() {
        // Arrange
        List<BaltopEntry> entries = createBaltopEntries(5);
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(anyLong()))
                .thenAnswer(inv -> "$" + (inv.getArgument(0, Long.class) / 10000) + ".00");

        // Act
        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        assertTrue(result);
        verify(messages).sendBaltopLoading(player);

        // Wait a bit for async completion
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopHeader(player, "5");
        verify(messages, times(5)).sendBaltopEntry(eq(player), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should display baltop with correct positions")
    void shouldDisplayBaltopWithCorrectPositions() {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        List<BaltopEntry> entries = List.of(
                new BaltopEntry("FirstPlace", 1000_0000L, uuid1),
                new BaltopEntry("SecondPlace", 500_0000L, uuid2),
                new BaltopEntry("ThirdPlace", 250_0000L, uuid3)
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(1000_0000L)).thenReturn("$1,000.00");
        when(formatter.formatCurrency(500_0000L)).thenReturn("$500.00");
        when(formatter.formatCurrency(250_0000L)).thenReturn("$250.00");

        // Act
        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        assertTrue(result);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopEntry(player, "1", "FirstPlace", "$1,000.00");
        verify(messages).sendBaltopEntry(player, "2", "SecondPlace", "$500.00");
        verify(messages).sendBaltopEntry(player, "3", "ThirdPlace", "$250.00");
    }

    @Test
    @DisplayName("Should format currency correctly for each entry")
    void shouldFormatCurrencyCorrectlyForEachEntry() {
        // Arrange
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("Player1", 123_4567L, UUID.randomUUID()),
                new BaltopEntry("Player2", 987_6543L, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(123_4567L)).thenReturn("$12.3456");
        when(formatter.formatCurrency(987_6543L)).thenReturn("$98.7654");

        // Act
        command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(formatter).formatCurrency(123_4567L);
        verify(formatter).formatCurrency(987_6543L);
        verify(messages).sendBaltopEntry(player, "1", "Player1", "$12.3456");
        verify(messages).sendBaltopEntry(player, "2", "Player2", "$98.7654");
    }

    @Test
    @DisplayName("Should display empty baltop correctly")
    void shouldDisplayEmptyBaltopCorrectly() {
        // Arrange
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        // Act
        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        assertTrue(result);
        verify(messages).sendBaltopLoading(player);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopHeader(player, "0");
        verify(messages, never()).sendBaltopEntry(eq(player), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should display baltop with 10 entries")
    void shouldDisplayBaltopWith10Entries() {
        // Arrange
        List<BaltopEntry> entries = createBaltopEntries(10);
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(anyLong()))
                .thenReturn("$100.00");

        // Act
        command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopHeader(player, "10");
        verify(messages, times(10)).sendBaltopEntry(eq(player), anyString(), anyString(), anyString());
    }

    // Command validation tests
    @Test
    @DisplayName("Should reject command from console")
    void shouldRejectCommandFromConsole() {
        // Act
        boolean result = command.onCommand(consoleSender, mockCommand, "baltop", new String[]{});

        // Assert
        assertTrue(result);
        verify(messages).sendBaltopPlayerOnly();
        verify(service, never()).getTop10();
        verify(messages, never()).sendBaltopLoading(any());
    }

    @Test
    @DisplayName("Should reject command with arguments")
    void shouldRejectCommandWithArguments() {
        // Act
        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{"extra"});

        // Assert
        assertTrue(result);
        verify(messages).sendBaltopUsage(player);
        verify(service, never()).getTop10();
        verify(messages, never()).sendBaltopLoading(player);
    }

    @Test
    @DisplayName("Should reject command with multiple arguments")
    void shouldRejectCommandWithMultipleArguments() {
        // Act
        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{"arg1", "arg2"});

        // Assert
        assertTrue(result);
        verify(messages).sendBaltopUsage(player);
        verify(service, never()).getTop10();
    }

    @Test
    @DisplayName("Should accept command with no arguments")
    void shouldAcceptCommandWithNoArguments() {
        // Arrange
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        // Act
        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        assertTrue(result);
        verify(messages).sendBaltopLoading(player);
        verify(service).getTop10();
    }

    // Error handling tests
    @Test
    @DisplayName("Should handle service exception gracefully")
    void shouldHandleServiceExceptionGracefully() {
        // Arrange
        when(service.getTop10())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        // Act
        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        assertTrue(result);
        verify(messages).sendBaltopLoading(player);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopError(player);
        verify(messages, never()).sendBaltopHeader(any(), anyString());
    }

    @Test
    @DisplayName("Should handle network timeout exception")
    void shouldHandleNetworkTimeoutException() {
        // Arrange
        when(service.getTop10())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection timeout")));

        // Act
        command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopError(player);
    }

    @Test
    @DisplayName("Should handle database exception")
    void shouldHandleDatabaseException() {
        // Arrange
        when(service.getTop10())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Database unavailable")));

        // Act
        command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopError(player);
        verify(formatter, never()).formatCurrency(anyLong());
    }

    // Edge cases
    @Test
    @DisplayName("Should handle baltop with Unknown players")
    void shouldHandleBaltopWithUnknownPlayers() {
        // Arrange
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("Unknown", 1000_0000L, UUID.randomUUID()),
                new BaltopEntry("KnownPlayer", 500_0000L, UUID.randomUUID()),
                new BaltopEntry("Unknown", 250_0000L, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(anyLong()))
                .thenReturn("$100.00");

        // Act
        command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopEntry(player, "1", "Unknown", "$100.00");
        verify(messages).sendBaltopEntry(player, "2", "KnownPlayer", "$100.00");
        verify(messages).sendBaltopEntry(player, "3", "Unknown", "$100.00");
    }

    @Test
    @DisplayName("Should handle baltop with zero balances")
    void shouldHandleBaltopWithZeroBalances() {
        // Arrange
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("Player1", 1000_0000L, UUID.randomUUID()),
                new BaltopEntry("Player2", 0L, UUID.randomUUID()),
                new BaltopEntry("Player3", 0L, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(1000_0000L)).thenReturn("$1,000.00");
        when(formatter.formatCurrency(0L)).thenReturn("$0.00");

        // Act
        command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(formatter).formatCurrency(1000_0000L);
        verify(formatter, times(2)).formatCurrency(0L);
    }

    @Test
    @DisplayName("Should handle baltop with very large balances")
    void shouldHandleBaltopWithVeryLargeBalances() {
        // Arrange
        long hugeBalance = Long.MAX_VALUE / 2;
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("RichPlayer", hugeBalance, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(hugeBalance)).thenReturn("$922,337,203,685,477.00");

        // Act
        command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(formatter).formatCurrency(hugeBalance);
        verify(messages).sendBaltopEntry(player, "1", "RichPlayer", "$922,337,203,685,477.00");
    }

    @Test
    @DisplayName("Should handle players with special characters in names")
    void shouldHandlePlayersWithSpecialCharactersInNames() {
        // Arrange
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("Player_123", 1000_0000L, UUID.randomUUID()),
                new BaltopEntry("Test-User", 500_0000L, UUID.randomUUID()),
                new BaltopEntry("Name[VIP]", 250_0000L, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(anyLong()))
                .thenReturn("$100.00");

        // Act
        command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopEntry(player, "1", "Player_123", "$100.00");
        verify(messages).sendBaltopEntry(player, "2", "Test-User", "$100.00");
        verify(messages).sendBaltopEntry(player, "3", "Name[VIP]", "$100.00");
    }

    @Test
    @DisplayName("Should always return true")
    void shouldAlwaysReturnTrue() {
        // All command scenarios should return true
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        assertTrue(command.onCommand(player, mockCommand, "baltop", new String[]{}));
        assertTrue(command.onCommand(consoleSender, mockCommand, "baltop", new String[]{}));
        assertTrue(command.onCommand(player, mockCommand, "baltop", new String[]{"extra"}));
    }

    @Test
    @DisplayName("Should send loading message before fetching data")
    void shouldSendLoadingMessageBeforeFetchingData() {
        // Arrange
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        // Act
        command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert - Loading should be called immediately, before async operation
        verify(messages).sendBaltopLoading(player);
        verify(service).getTop10();
    }

    @Test
    @DisplayName("Should handle single entry baltop")
    void shouldHandleSingleEntryBaltop() {
        // Arrange
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("OnlyPlayer", 100_0000L, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(100_0000L)).thenReturn("$10.00");

        // Act
        command.onCommand(player, mockCommand, "baltop", new String[]{});

        // Assert
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopHeader(player, "1");
        verify(messages).sendBaltopEntry(player, "1", "OnlyPlayer", "$10.00");
    }

    // Helper method to create baltop entries
    private List<BaltopEntry> createBaltopEntries(int count) {
        List<BaltopEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String playerName = "Player" + (i + 1);
            long balance = (count - i) * 100_0000L;
            UUID uuid = UUID.randomUUID();
            entries.add(new BaltopEntry(playerName, balance, uuid));
        }
        return entries;
    }
}
