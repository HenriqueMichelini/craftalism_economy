package io.github.HenriqueMichelini.craftalism_economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism_economy.application.dto.SetBalanceExecutionResult;
import io.github.HenriqueMichelini.craftalism_economy.application.service.SetBalanceCommandApplicationService;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.messages.SetBalanceMessages;
import io.github.HenriqueMichelini.craftalism_economy.presentation.validation.PlayerNameCheck;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SetBalanceCommand Tests")
class SetBalanceCommandTest {

    private SetBalanceMessages messages;
    private SetBalanceCommandApplicationService service;
    private PlayerNameCheck playerNameCheck;
    private JavaPlugin plugin;
    private SetBalanceCommand command;

    private CommandSender sender;
    private Player senderPlayer;
    private Player targetPlayer;
    private Command mockCommand;
    private BukkitScheduler scheduler;

    @BeforeEach
    void setUp() {
        messages = mock(SetBalanceMessages.class);
        service = mock(SetBalanceCommandApplicationService.class);
        playerNameCheck = mock(PlayerNameCheck.class);
        plugin = mock(JavaPlugin.class);
        command = new SetBalanceCommand(playerNameCheck, messages, service, plugin);

        sender = mock(CommandSender.class);
        senderPlayer = mock(Player.class);
        targetPlayer = mock(Player.class);
        mockCommand = mock(Command.class);
        scheduler = mock(BukkitScheduler.class);

        when(senderPlayer.getName()).thenReturn("Admin");
        when(sender.getName()).thenReturn("Console");

        // Mock playerNameCheck to return true by default
        when(playerNameCheck.isValid(anyString())).thenReturn(true);

        // Mock scheduler to run task immediately
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return null;
        }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
    }

    // Successful execution tests
    @Test
    @DisplayName("Should set balance successfully")
    void shouldSetBalanceSuccessfully() {
        // Arrange
        String targetName = "TestPlayer";
        String amountStr = "10000000";
        long amount = 10000000L;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            // Mock getting player by UUID (result.uuid().get())
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            // Act
            boolean result = command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            // Assert
            assertTrue(result);
            verify(service).execute(targetName, amount);
            verify(messages).sendSetBalanceSuccessSender(sender, targetName, amountStr);
            verify(messages).sendSetBalanceSuccessReceiver(targetPlayer, amountStr, "Console");
        }
    }

    @Test
    @DisplayName("Should send message with player sender name")
    void shouldSendMessageWithPlayerSenderName() {
        // Arrange
        String targetName = "TargetPlayer";
        String amountStr = "5000000";
        long amount = 5000000L;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            // Mock getting player by UUID
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            // Act
            command.onCommand(senderPlayer, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            // Assert
            verify(messages).sendSetBalanceSuccessReceiver(targetPlayer, amountStr, "Admin");
            verify(messages).sendSetBalanceSuccessSender(senderPlayer, targetName, amountStr);
        }
    }

    @Test
    @DisplayName("Should handle null target player")
    void shouldHandleNullTargetPlayer() {
        // Arrange
        String targetName = "OfflinePlayer";
        String amountStr = "1000000";
        long amount = 1000000L;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            // Target player is offline/null
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(null);

            // Act
            boolean result = command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            // Assert
            assertTrue(result);
            verify(messages).sendSetBalanceSuccessSender(sender, targetName, amountStr);
            // IMPORTANT: Currently your code DOES send message to null,
            // so this test expects that behavior
            verify(messages).sendSetBalanceSuccessReceiver(null, amountStr, "Console");
        }
    }

    // Validation tests
    @Test
    @DisplayName("Should reject command with no arguments")
    void shouldRejectCommandWithNoArguments() {
        // Act
        boolean result = command.onCommand(sender, mockCommand, "setbalance", new String[]{});

        // Assert
        assertTrue(result);
        verify(messages).sendSetBalanceUsage(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should reject command with one argument")
    void shouldRejectCommandWithOneArgument() {
        // Act
        boolean result = command.onCommand(sender, mockCommand, "setbalance", new String[]{"Player"});

        // Assert
        assertTrue(result);
        verify(messages).sendSetBalanceUsage(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should reject command with three arguments")
    void shouldRejectCommandWithThreeArguments() {
        // Act
        boolean result = command.onCommand(sender, mockCommand, "setbalance",
                new String[]{"Player", "100", "extra"});

        // Assert
        assertTrue(result);
        verify(messages).sendSetBalanceUsage(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should reject invalid player name")
    void shouldRejectInvalidPlayerName() {
        // Arrange
        String invalidName = "Invalid@Name";
        when(playerNameCheck.isValid(invalidName)).thenReturn(false);

        // Act
        boolean result = command.onCommand(sender, mockCommand, "setbalance",
                new String[]{invalidName, "100"});

        // Assert
        assertTrue(result);
        verify(messages).sendSetBalanceInvalidName(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should reject non-numeric amount")
    void shouldRejectNonNumericAmount() {
        // Arrange
        String targetName = "Player";
        String invalidAmount = "abc";

        // Act
        boolean result = command.onCommand(sender, mockCommand, "setbalance",
                new String[]{targetName, invalidAmount});

        // Assert
        assertTrue(result);
        verify(messages).sendSetBalanceInvalidAmount(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should reject decimal amount")
    void shouldRejectDecimalAmount() {
        // Arrange
        String targetName = "Player";
        String decimalAmount = "10.50";

        // Act
        boolean result = command.onCommand(sender, mockCommand, "setbalance",
                new String[]{targetName, decimalAmount});

        // Assert
        assertTrue(result);
        verify(messages).sendSetBalanceInvalidAmount(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    // Error handling tests
    @Test
    @DisplayName("Should handle player not found error")
    void shouldHandlePlayerNotFoundError() {
        // Arrange
        String targetName = "NonExistent";
        String amountStr = "1000000";
        long amount = 1000000L;

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.playerNotFound()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            // Act
            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            // Assert
            verify(messages).sendSetBalancePlayerNotFound(sender);
            verify(messages, never()).sendSetBalanceSuccessSender(any(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("Should handle invalid amount from service")
    void shouldHandleInvalidAmountFromService() {
        // Arrange
        String targetName = "Player";
        String amountStr = "-1";
        long amount = -1L;

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.invalidAmount()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            // Act
            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            // Assert
            verify(messages).sendSetBalanceInvalidAmount(sender);
        }
    }

    @Test
    @DisplayName("Should handle update failed error")
    void shouldHandleUpdateFailedError() {
        // Arrange
        String targetName = "Player";
        String amountStr = "1000000";
        long amount = 1000000L;

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.updateFailed()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            // Act
            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            // Assert
            verify(messages).sendSetBalanceUpdateFailed(sender);
        }
    }

    @Test
    @DisplayName("Should handle general exception")
    void shouldHandleGeneralException() {
        // Arrange
        String targetName = "Player";
        String amountStr = "1000000";
        long amount = 1000000L;

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.exception()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            // Act
            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            // Assert
            verify(messages).sendSetBalanceException(sender);
        }
    }

    // Edge cases
    @Test
    @DisplayName("Should handle zero amount")
    void shouldHandleZeroAmount() {
        // Arrange
        String targetName = "Player";
        String amountStr = "0";
        long amount = 0L;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            // Act
            boolean result = command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            // Assert
            assertTrue(result);
            verify(service).execute(targetName, amount);
        }
    }

    @Test
    @DisplayName("Should handle very large amounts")
    void shouldHandleVeryLargeAmounts() {
        // Arrange
        String targetName = "Player";
        String amountStr = String.valueOf(Long.MAX_VALUE);
        long amount = Long.MAX_VALUE;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            // Act
            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            // Assert
            verify(service).execute(targetName, amount);
        }
    }

    @Test
    @DisplayName("Should handle negative amounts passed to service")
    void shouldHandleNegativeAmountsPassedToService() {
        // Arrange
        String targetName = "Player";
        String amountStr = "-100";
        long amount = -100L;
        UUID targetUuid = UUID.randomUUID();

        // Service should reject it, but command parses it
        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.invalidAmount()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            // Act
            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            // Assert
            verify(service).execute(targetName, amount);
            verify(messages).sendSetBalanceInvalidAmount(sender);
        }
    }

    @Test
    @DisplayName("Should always return true")
    void shouldAlwaysReturnTrue() {
        // All command scenarios should return true
        assertTrue(command.onCommand(sender, mockCommand, "setbalance", new String[]{}));
        assertTrue(command.onCommand(sender, mockCommand, "setbalance", new String[]{"Player"}));

        when(playerNameCheck.isValid("")).thenReturn(false);
        assertTrue(command.onCommand(sender, mockCommand, "setbalance", new String[]{"", "100"}));
    }

    @Test
    @DisplayName("Should handle amount with leading zeros")
    void shouldHandleAmountWithLeadingZeros() {
        // Arrange
        String targetName = "Player";
        String amountStr = "00100";
        long amount = 100L;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            // Act
            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            // Assert
            verify(service).execute(targetName, amount);
        }
    }

    @Test
    @DisplayName("Should not call service when validation fails")
    void shouldNotCallServiceWhenValidationFails() {
        // Arrange - no arguments
        command.onCommand(sender, mockCommand, "setbalance", new String[]{});
        verify(service, never()).execute(anyString(), anyLong());

        // Arrange - invalid name
        when(playerNameCheck.isValid("Bad")).thenReturn(false);
        command.onCommand(sender, mockCommand, "setbalance", new String[]{"Bad", "100"});
        verify(service, never()).execute(anyString(), anyLong());

        // Arrange - invalid amount
        command.onCommand(sender, mockCommand, "setbalance", new String[]{"Player", "abc"});
        verify(service, never()).execute(anyString(), anyLong());
    }
}