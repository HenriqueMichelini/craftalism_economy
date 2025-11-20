package io.github.HenriqueMichelini.craftalism_economy.core.currency;

import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism_economy.domain.service.logs.currency.CurrencyParser;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CurrencyParser Tests")
class CurrencyParserTest {

    @Mock
    private Player player;

    private AutoCloseable mocks;

    private CurrencyParser currencyParser;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        this.currencyParser = new CurrencyParser();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Nested
    @DisplayName("parseAmount(Player, String) Tests")
    class ParseAmountWithPlayerTests {

        @Test
        @DisplayName("Should parse valid decimal amount correctly")
        void parseAmount_ValidDecimal_ReturnsCorrectValue() {
            // Given
            String input = "1.2345";

            // When
            Optional<Long> result = currencyParser.parseAmount(player, input);

            // Then
            assertTrue(result.isPresent(), "Should parse valid decimal");
            assertEquals(12345L, result.get(), "Should convert 1.2345 to 12345 internal units");
            verifyNoInteractions(player); // No error messages should be sent
        }

        @Test
        @DisplayName("Should parse integer amount correctly")
        void parseAmount_Integer_ReturnsCorrectValue() {
            // Given
            String input = "123";

            // When
            Optional<Long> result = currencyParser.parseAmount(player, input);

            // Then
            assertTrue(result.isPresent(), "Should parse valid integer");
            assertEquals(1230000L, result.get(), "Should convert 123 to 1230000 internal units");
            verifyNoInteractions(player);
        }

        @ParameterizedTest
        @MethodSource("validAmountData")
        @DisplayName("Should parse various valid amounts correctly")
        void parseAmount_ValidAmounts_ReturnsCorrectValues(String input, long expected) {
            // When
            Optional<Long> result = currencyParser.parseAmount(player, input);

            // Then
            assertTrue(result.isPresent(), "Should parse valid amount: " + input);
            assertEquals(expected, result.get(), "Incorrect conversion for input: " + input);
            verifyNoInteractions(player);
        }

        static Stream<Arguments> validAmountData() {
            return Stream.of(
                    Arguments.of("1", 10000L),
                    Arguments.of("1.0", 10000L),
                    Arguments.of("1.00", 10000L),
                    Arguments.of("1.5", 15000L),
                    Arguments.of("0.0001", 1L),
                    Arguments.of("0.1", 1000L),
                    Arguments.of("10.25", 102500L),
                    Arguments.of("100", 1000000L),
                    Arguments.of("0.9999", 9999L)
            );
        }

        @Test
        @DisplayName("Should handle amounts with currency symbols")
        void parseAmount_WithCurrencySymbols_ParsesCorrectly() {
            assertAll("Should parse amounts with various currency symbols",
                    () -> {
                        Optional<Long> result = currencyParser.parseAmount(player, "$1.23");
                        assertTrue(result.isPresent());
                        assertEquals(12300L, result.get());
                    },
                    () -> {
                        Optional<Long> result = currencyParser.parseAmount(player, "€5.67");
                        assertTrue(result.isPresent());
                        assertEquals(56700L, result.get());
                    },
                    () -> {
                        Optional<Long> result = currencyParser.parseAmount(player, "£10.99");
                        assertTrue(result.isPresent());
                        assertEquals(109900L, result.get());
                    }
            );
            verifyNoInteractions(player);
        }

        @Test
        @DisplayName("Should handle whitespace correctly")
        void parseAmount_WithWhitespace_ParsesCorrectly() {
            assertAll("Should handle various whitespace scenarios",
                    () -> assertTrue(currencyParser.parseAmount(player, "  1.23  ").isPresent()),
                    () -> assertTrue(currencyParser.parseAmount(player, "\t5.67\n").isPresent()),
                    () -> assertTrue(currencyParser.parseAmount(player, " $10.99 ").isPresent())
            );
            verifyNoInteractions(player);
        }

        @Test
        @DisplayName("Should reject negative amounts")
        void parseAmount_NegativeAmount_RejectsAndSendsMessage() {
            // Given
            String input = "-1.23";

            // When
            Optional<Long> result = currencyParser.parseAmount(player, input);

            // Then
            assertFalse(result.isPresent(), "Should reject negative amount");

            // Verify error message was sent
            ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
            verify(player).sendMessage(messageCaptor.capture());

            Component sentMessage = messageCaptor.getValue();
            // Note: In a real test, you'd need to extract the text from the Component
            // This is a simplified assertion
            assertNotNull(sentMessage, "Should send error message for negative amount");
        }

        @Test
        @DisplayName("Should reject zero amounts")
        void parseAmount_ZeroAmount_RejectsAndSendsMessage() {
            // Given
            String input = "0";

            // When
            Optional<Long> result = currencyParser.parseAmount(player, input);

            // Then
            assertFalse(result.isPresent(), "Should reject zero amount");
            verify(player).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should reject empty input")
        void parseAmount_EmptyInput_RejectsAndSendsMessage() {
            assertAll("Should reject various empty inputs",
                    () -> {
                        Optional<Long> result = currencyParser.parseAmount(player, "");
                        assertFalse(result.isPresent());
                    },
                    () -> {
                        Optional<Long> result = currencyParser.parseAmount(player, "   ");
                        assertFalse(result.isPresent());
                    },
                    () -> {
                        Optional<Long> result = currencyParser.parseAmount(player, "\t\n");
                        assertFalse(result.isPresent());
                    }
            );

            // Should send 3 error messages
            verify(player, times(3)).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should reject amounts with too many decimal places")
        void parseAmount_TooManyDecimals_RejectsAndSendsMessage() {
            // Given
            String input = "1.12345"; // 5 decimal places, max is 4

            // When
            Optional<Long> result = currencyParser.parseAmount(player, input);

            // Then
            assertFalse(result.isPresent(), "Should reject amount with > 4 decimal places");
            verify(player).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should reject invalid number format")
        void parseAmount_InvalidFormat_RejectsAndSendsMessage() {
            assertAll("Should reject various invalid formats",
                    () -> {
                        Optional<Long> result = currencyParser.parseAmount(player, "abc");
                        assertFalse(result.isPresent());
                    },
                    () -> {
                        Optional<Long> result = currencyParser.parseAmount(player, "1.2.3");
                        assertFalse(result.isPresent());
                    },
                    () -> {
                        Optional<Long> result = currencyParser.parseAmount(player, "1a.23");
                        assertFalse(result.isPresent());
                    }
            );

            verify(player, times(3)).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should reject amounts that are too large")
        void parseAmount_TooLarge_RejectsAndSendsMessage() {
            // Given - Create an amount that would overflow when scaled
            String input = "999999999999999999999"; // Very large number

            // When
            Optional<Long> result = currencyParser.parseAmount(player, input);

            // Then
            assertFalse(result.isPresent(), "Should reject amounts that are too large");
            verify(player).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should throw exception for null player")
        void parseAmount_NullPlayer_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> currencyParser.parseAmount(null, "1.23")
            );
            assertEquals("Player cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null input")
        void parseAmount_NullInput_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> currencyParser.parseAmount(player, null)
            );
            assertEquals("Input cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("parseAmount(Player, String, boolean) Tests")
    class ParseAmountWithFlagTests {

        @Test
        @DisplayName("Should send messages when flag is true")
        void parseAmountWithFlag_SendMessagesTrue_SendsMessages() {
            // When
            Optional<Long> result = currencyParser.parseAmount(player, "-1.23", true);

            // Then
            assertFalse(result.isPresent());
            verify(player).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should not send messages when flag is false")
        void parseAmountWithFlag_SendMessagesFalse_NoMessages() {
            // When
            Optional<Long> result = currencyParser.parseAmount(player, "-1.23", false);

            // Then
            assertFalse(result.isPresent());
            verifyNoInteractions(player);
        }
    }

    @Nested
    @DisplayName("parseAmountSilently Tests")
    class ParseAmountSilentlyTests {

        @Test
        @DisplayName("Should parse valid amounts without sending messages")
        void parseAmountSilently_ValidAmount_ParsesWithoutMessages() {
            // When
            Optional<Long> result = currencyParser.parseAmountSilently("1.23");

            // Then
            assertTrue(result.isPresent());
            assertEquals(12300L, result.get());
        }

        @Test
        @DisplayName("Should reject invalid amounts without sending messages")
        void parseAmountSilently_InvalidAmount_RejectsWithoutMessages() {
            // When
            Optional<Long> result = currencyParser.parseAmountSilently("-1.23");

            // Then
            assertFalse(result.isPresent());
            // No way to verify messages weren't sent since no player is involved
        }

        @Test
        @DisplayName("Should throw exception for null input")
        void parseAmountSilently_NullInput_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> currencyParser.parseAmountSilently(null)
            );
            assertEquals("Input cannot be null", exception.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "\t\n", "abc", "-1", "0", "1.12345"})
        @DisplayName("Should reject various invalid inputs silently")
        void parseAmountSilently_InvalidInputs_RejectsSilently(String input) {
            // When
            Optional<Long> result = currencyParser.parseAmountSilently(input);

            // Then
            assertFalse(result.isPresent(), "Should reject invalid input: " + input);
        }
    }

    @Nested
    @DisplayName("isValidAmount Tests")
    class IsValidAmountTests {

        @ParameterizedTest
        @ValueSource(strings = {"1", "1.23", "0.0001", "999.9999", "$1.23", " 5.67 "})
        @DisplayName("Should return true for valid amounts")
        void isValidAmount_ValidInputs_ReturnsTrue(String input) {
            // When & Then
            assertTrue(currencyParser.isValidAmount(input), "Should be valid: " + input);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "abc", "-1", "0", "1.12345", "999999999999999999999"})
        @DisplayName("Should return false for invalid amounts")
        void isValidAmount_InvalidInputs_ReturnsFalse(String input) {
            // When & Then
            assertFalse(currencyParser.isValidAmount(input), "Should be invalid: " + input);
        }
    }

    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodTests {

        @Test
        @DisplayName("Should return maximum allowed value")
        void getMaxAllowedValue_ReturnsCorrectValue() {
            // When
            BigDecimal maxValue = currencyParser.getMaxAllowedValue();

            // Then
            assertNotNull(maxValue, "Max allowed value should not be null");
            assertTrue(maxValue.compareTo(BigDecimal.ZERO) > 0, "Max allowed value should be positive");

            // Should be less than what would cause overflow
            BigDecimal scaled = maxValue.multiply(CurrencyFormatter.DECIMAL_SCALE_BD);
            assertTrue(scaled.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0,
                    "Max value when scaled should not exceed Long.MAX_VALUE");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle scientific notation")
        void parseAmount_ScientificNotation_HandlesCorrectly() {
            // Given
            String input = "1E-4"; // 0.0001

            // When
            Optional<Long> result = currencyParser.parseAmount(player, input);

            // Then
            assertTrue(result.isPresent(), "Should parse scientific notation");
            assertEquals(1L, result.get(), "1E-4 should equal 0.0001 = 1 internal unit");
        }

        @Test
        @DisplayName("Should handle currency symbols only")
        void parseAmount_CurrencySymbolOnly_Rejects() {
            // When
            Optional<Long> result = currencyParser.parseAmount(player, "$");

            // Then
            assertFalse(result.isPresent(), "Should reject currency symbol without number");
            verify(player).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("Should handle maximum precision correctly")
        void parseAmount_MaximumPrecision_ParsesCorrectly() {
            // Given
            String input = "1.0000"; // Exactly 4 decimal places

            // When
            Optional<Long> result = currencyParser.parseAmount(player, input);

            // Then
            assertTrue(result.isPresent(), "Should parse exactly 4 decimal places");
            assertEquals(10000L, result.get());
            verifyNoInteractions(player);
        }

        @Test
        @DisplayName("Should handle very small valid amounts")
        void parseAmount_VerySmallAmount_ParsesCorrectly() {
            // Given
            String input = "0.0001"; // Smallest possible amount

            // When
            Optional<Long> result = currencyParser.parseAmount(player, input);

            // Then
            assertTrue(result.isPresent(), "Should parse smallest valid amount");
            assertEquals(1L, result.get());
            verifyNoInteractions(player);
        }

        @Test
        @DisplayName("Should handle boundary values correctly")
        void parseAmount_BoundaryValues_HandlesCorrectly() {
            // Test values at the boundary of what should be accepted
            BigDecimal maxAllowed = currencyParser.getMaxAllowedValue();
            String maxInput = maxAllowed.toPlainString();

            // When
            Optional<Long> result = currencyParser.parseAmount(player, maxInput);

            // Then
            assertTrue(result.isPresent(), "Should parse maximum allowed value");
            verifyNoInteractions(player);
        }
    }
}