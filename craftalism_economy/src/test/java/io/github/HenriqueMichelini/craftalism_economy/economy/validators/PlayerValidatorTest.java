package io.github.HenriqueMichelini.craftalism_economy.economy.validators;

import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerValidatorTest {

    private static final String TEST_REQUESTER_NAME = "TestRequester";
    private static final String ONLINE_PLAYER_NAME = "OnlineTestPlayer";
    private static final String OFFLINE_PLAYER_NAME = "OfflineTestPlayer";

    private ServerMock server;
    private PlayerValidator playerValidator;
    private PlayerMock requester;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        mocks = MockitoAnnotations.openMocks(this);
        playerValidator = new PlayerValidator();
        requester = server.addPlayer(TEST_REQUESTER_NAME);
    }

    @AfterEach
    void tearDown() throws Exception {
        MockBukkit.unmock();
        mocks.close();
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t\n  ", "  \r\n\t  "})
        @NullSource
        @DisplayName("Should return empty for invalid usernames")
        void resolvePlayer_InvalidInput_ReturnsEmpty(String input) {
            // When
            Optional<OfflinePlayer> result = playerValidator.resolvePlayer(requester, input);

            // Then
            assertFalse(result.isPresent(), "Should return empty for invalid input: '" + input + "'");
        }

        @Test
        @DisplayName("Should handle unicode characters in username")
        void resolvePlayer_UnicodeUsername_HandledCorrectly() {
            // Given
            String unicodeUsername = "Player_测试_🎮";

            // When
            Optional<OfflinePlayer> result = playerValidator.resolvePlayer(requester, unicodeUsername);

            // Then
            assertFalse(result.isPresent(), "Unicode username should not be found if player doesn't exist");
        }

        @Test
        @DisplayName("Should handle very long username")
        void resolvePlayer_VeryLongUsername_HandledCorrectly() {
            // Given
            String longUsername = "a".repeat(100); // Very long username

            // When
            Optional<OfflinePlayer> result = playerValidator.resolvePlayer(requester, longUsername);

            // Then
            assertFalse(result.isPresent(), "Very long username should not be found if player doesn't exist");
        }
    }

    @Nested
    @DisplayName("Online Player Resolution Tests")
    class OnlinePlayerTests {

        @Test
        @DisplayName("Should return online player when player is currently online")
        void resolvePlayer_OnlinePlayer_ReturnsPlayer() {
            // Given
            PlayerMock onlinePlayer = server.addPlayer(ONLINE_PLAYER_NAME);

            // When
            Optional<OfflinePlayer> result = playerValidator.resolvePlayer(requester, ONLINE_PLAYER_NAME);

            // Then
            assertTrue(result.isPresent(), "Should find online player");
            assertEquals(onlinePlayer, result.get(), "Should return the exact online player instance");
            assertInstanceOf(PlayerMock.class, result.get(), "Should return PlayerMock for online player");
        }

        @Test
        @DisplayName("Should return online player with exact name match (case sensitive)")
        void resolvePlayer_ExactNameMatch_ReturnsCorrectPlayer() {
            // Given
            PlayerMock player1 = server.addPlayer("TestPlayer");
            PlayerMock player2 = server.addPlayer("testplayer"); // Different case

            // When
            Optional<OfflinePlayer> result = playerValidator.resolvePlayer(requester, "TestPlayer");

            // Then
            assertTrue(result.isPresent(), "Should find player with exact name match");
            assertEquals(player1, result.get(), "Should return player with exact case match");
            assertNotEquals(player2, result.get(), "Should not return player with different case");
        }

        @Test
        @DisplayName("Should prioritize online player over offline player with same name")
        void resolvePlayer_OnlinePlayerTakesPrecedence_ReturnsOnlinePlayer() {
            // Given
            String playerName = "TestPlayer";

            // First, create a player that has played before (add and disconnect)
            PlayerMock previousPlayer = server.addPlayer(playerName);
            previousPlayer.disconnect();

            // Then add an online player with same name
            PlayerMock onlinePlayer = server.addPlayer(playerName);

            // When
            Optional<OfflinePlayer> result = playerValidator.resolvePlayer(requester, playerName);

            // Then
            assertTrue(result.isPresent(), "Should find a player");
            assertEquals(onlinePlayer, result.get(), "Should prioritize online player");
            assertInstanceOf(PlayerMock.class, result.get(), "Should be online PlayerMock");
            assertTrue(result.get().isOnline(), "Returned player should be online");
        }
    }

    @Nested
    @DisplayName("Offline Player Resolution Tests")
    class OfflinePlayerTests {

        @Test
        @DisplayName("Should return offline player when player exists but is offline")
        void resolvePlayer_OfflinePlayerExists_ReturnsOfflinePlayer() {
            // Given
            PlayerMock player = server.addPlayer(OFFLINE_PLAYER_NAME);
            UUID playerId = player.getUniqueId();
            player.disconnect(); // This makes them offline but keeps their data

            // When
            Optional<OfflinePlayer> result = playerValidator.resolvePlayer(requester, OFFLINE_PLAYER_NAME);

            // Then
            assertTrue(result.isPresent(), "Should find offline player who has played before");
            assertEquals(OFFLINE_PLAYER_NAME, result.get().getName(), "Should return correct player name");
            assertEquals(playerId, result.get().getUniqueId(), "Should return correct player UUID");
            assertTrue(result.get().hasPlayedBefore(), "Player should have played before");
        }

        @Test
        @DisplayName("Should return empty when offline player has never played before")
        void resolvePlayer_OfflinePlayerNeverPlayed_ReturnsEmpty() {
            // Given - Test with a completely unknown player name
            String playerName = "NeverPlayedPlayer_" + System.currentTimeMillis();

            // When
            Optional<OfflinePlayer> result = playerValidator.resolvePlayer(requester, playerName);

            // Then
            assertFalse(result.isPresent(), "Should not find player who has never joined the server");
        }

        @Test
        @DisplayName("Should verify UUID version validation works correctly")
        void resolvePlayer_UUIDVersionCheck_WorksCorrectly() {
            // Given - Create a real player and verify their UUID has proper version
            PlayerMock player = server.addPlayer("RealPlayer");
            UUID realUuid = player.getUniqueId();

            // Verify the UUID has a proper version (should be 4 for random UUIDs)
            assertTrue(realUuid.version() > 0, "Real player should have UUID version > 0");

            player.disconnect(); // Make them offline

            // When
            Optional<OfflinePlayer> result = playerValidator.resolvePlayer(requester, "RealPlayer");

            // Then
            assertTrue(result.isPresent(), "Should find valid offline player with proper UUID");

            // Test with a fake player (one that never joined)
            String fakePlayerName = "FakePlayer_" + System.currentTimeMillis();

            // When resolving fake player
            Optional<OfflinePlayer> fakeResult = playerValidator.resolvePlayer(requester, fakePlayerName);

            // Then - should return empty because they haven't played before
            assertFalse(fakeResult.isPresent(), "Should not find fake player who never joined");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should return empty when player doesn't exist")
        void resolvePlayer_PlayerDoesNotExist_ReturnsEmpty() {
            // Given
            String nonExistentPlayer = "NonExistentPlayer_" + System.currentTimeMillis();

            // When
            Optional<OfflinePlayer> result = playerValidator.resolvePlayer(requester, nonExistentPlayer);

            // Then
            assertFalse(result.isPresent(), "Should not find non-existent player");
        }

        @Test
        @DisplayName("Should handle method execution without exceptions")
        void resolvePlayer_NoExceptions_ExecutesSuccessfully() {
            // Given
            String playerName = "TestPlayer_" + System.currentTimeMillis();

            // When & Then - Should not throw any exceptions
            assertDoesNotThrow(() -> {
                Optional<OfflinePlayer> result = playerValidator.resolvePlayer(requester, playerName);
                assertFalse(result.isPresent(), "Unknown player should return empty");
            }, "Method should handle all cases without throwing exceptions");
        }
    }
}