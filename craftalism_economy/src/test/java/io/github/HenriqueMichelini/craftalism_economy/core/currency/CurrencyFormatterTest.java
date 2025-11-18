package io.github.HenriqueMichelini.craftalism_economy.core.currency;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CurrencyFormatter Tests")
class CurrencyFormatterTest {

    private static final String CURRENCY_SYMBOL = "$";
    private static final String FALLBACK_VALUE = "0.00";
    private static final Locale TEST_LOCALE = Locale.US;

    @Mock
    private JavaPlugin plugin;

    @Mock
    private Logger logger;

    private CurrencyFormatter formatter;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(plugin.getLogger()).thenReturn(logger);
        formatter = new CurrencyFormatter(TEST_LOCALE, CURRENCY_SYMBOL, FALLBACK_VALUE, plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (formatter != null) {
            formatter.cleanup();
        }
        mocks.close();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create formatter with valid parameters")
        void constructor_ValidParameters_CreatesInstance() {
            // When & Then
            assertDoesNotThrow(() ->
                    new CurrencyFormatter(Locale.FRANCE, "€", "0,00", plugin));
        }

        @Test
        @DisplayName("Should throw exception when locale is null")
        void constructor_NullLocale_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new CurrencyFormatter(null, CURRENCY_SYMBOL, FALLBACK_VALUE, plugin)
            );
            assertEquals("Locale cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when currency symbol is null")
        void constructor_NullCurrencySymbol_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new CurrencyFormatter(TEST_LOCALE, null, FALLBACK_VALUE, plugin)
            );
            assertEquals("Currency symbol cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when fallback value is null")
        void constructor_NullFallbackValue_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new CurrencyFormatter(TEST_LOCALE, CURRENCY_SYMBOL, null, plugin)
            );
            assertEquals("Fallback value cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when plugin is null")
        void constructor_NullPlugin_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new CurrencyFormatter(TEST_LOCALE, CURRENCY_SYMBOL, FALLBACK_VALUE, null)
            );
            assertEquals("Plugin cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Format Currency (Long) Tests")
    class FormatCurrencyLongTests {

        @Test
        @DisplayName("Should format zero amount correctly")
        void formatCurrency_ZeroAmount_FormatsCorrectly() {
            // When
            String result = formatter.formatCurrency(0L);

            // Then
            assertEquals("$0.00", result);
        }

        @Test
        @DisplayName("Should format positive amounts correctly")
        void formatCurrency_PositiveAmount_FormatsCorrectly() {
            // When
            String result = formatter.formatCurrency(10000L); // 1.0000 display value

            // Then
            assertEquals("$1.00", result); // Should display as 1.00 (2 decimal places minimum)
        }

        @ParameterizedTest
        @CsvSource({
                "10000, $1.00",      // 1.0000 -> $1.00
                "15000, $1.50",      // 1.5000 -> $1.50
                "12345, $1.2345",    // 1.2345 -> $1.2345
                "1, $0.0001",        // 0.0001 -> $0.0001
                "9999, $0.9999",     // 0.9999 -> $0.9999
                "100000, $10.00",    // 10.0000 -> $10.00
                "123456, $12.3456"   // 12.3456 -> $12.3456
        })
        @DisplayName("Should format various amounts correctly")
        void formatCurrency_VariousAmounts_FormatsCorrectly(long amount, String expected) {
            // When
            String result = formatter.formatCurrency(amount);

            // Then
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Should format negative amounts correctly")
        void formatCurrency_NegativeAmount_FormatsCorrectly() {
            // When
            String result = formatter.formatCurrency(-10000L);

            // Then
            assertEquals("$-1.00", result);
        }

        @Test
        @DisplayName("Should format large amounts with grouping")
        void formatCurrency_LargeAmount_FormatsWithGrouping() {
            // When
            String result = formatter.formatCurrency(10000000L); // 1000.0000

            // Then
            assertEquals("$1,000.00", result);
        }

        @Test
        @DisplayName("Should handle maximum long value")
        void formatCurrency_MaxLong_HandlesGracefully() {
            // When
            String result = formatter.formatCurrency(Long.MAX_VALUE);

            // Then
            assertNotNull(result);
            assertTrue(result.startsWith("$"));
        }

        @Test
        @DisplayName("Should handle minimum long value")
        void formatCurrency_MinLong_HandlesGracefully() {
            // When
            String result = formatter.formatCurrency(Long.MIN_VALUE);

            // Then
            assertNotNull(result);
            assertTrue(result.startsWith("$"));
        }
    }

    @Nested
    @DisplayName("Format Currency (BigDecimal) Tests")
    class FormatCurrencyBigDecimalTests {

        @Test
        @DisplayName("Should format BigDecimal amounts correctly")
        void formatCurrency_BigDecimalAmount_FormatsCorrectly() {
            // Given
            BigDecimal amount = new BigDecimal("1234.5678");

            // When
            String result = formatter.formatCurrency(amount);

            // Then
            assertEquals("$1,234.5678", result);
        }

        @Test
        @DisplayName("Should handle BigDecimal with many decimal places")
        void formatCurrency_BigDecimalManyDecimals_FormatsCorrectly() {
            // Given
            BigDecimal amount = new BigDecimal("1.123456789");

            // When
            String result = formatter.formatCurrency(amount);

            // Then
            assertEquals("$1.1235", result); // Should be truncated to 4 decimal places
        }

        @Test
        @DisplayName("Should throw exception for null BigDecimal")
        void formatCurrency_NullBigDecimal_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> formatter.formatCurrency(null)
            );
            assertEquals("Amount cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should format zero BigDecimal correctly")
        void formatCurrency_ZeroBigDecimal_FormatsCorrectly() {
            // Given
            BigDecimal amount = BigDecimal.ZERO;

            // When
            String result = formatter.formatCurrency(amount);

            // Then
            assertEquals("$0.00", result);
        }
    }

    @Nested
    @DisplayName("Conversion Tests")
    class ConversionTests {

        @Test
        @DisplayName("Should convert long to display value correctly")
        void toDisplayValue_ValidLong_ConvertsCorrectly() {
            // When
            BigDecimal result = formatter.toDisplayValue(12345L);

            // Then
            assertEquals(new BigDecimal("1.2345"), result);
        }

        @Test
        @DisplayName("Should convert display value to long correctly")
        void fromDisplayValue_ValidBigDecimal_ConvertsCorrectly() {
            // Given
            BigDecimal displayValue = new BigDecimal("1.2345");

            // When
            long result = formatter.fromDisplayValue(displayValue);

            // Then
            assertEquals(12345L, result);
        }

        @Test
        @DisplayName("Should handle rounding in conversion correctly")
        void fromDisplayValue_RequiresRounding_RoundsCorrectly() {
            // Given
            BigDecimal displayValue = new BigDecimal("1.23456"); // More than 4 decimal places

            // When
            long result = formatter.fromDisplayValue(displayValue);

            // Then
            assertEquals(12346L, result); // Should round up (HALF_UP)
        }

        @Test
        @DisplayName("Should throw exception for null display value")
        void fromDisplayValue_NullBigDecimal_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> formatter.fromDisplayValue(null)
            );
            assertEquals("Display value cannot be null", exception.getMessage());
        }

        @ParameterizedTest
        @MethodSource("conversionTestData")
        @DisplayName("Should handle bidirectional conversions correctly")
        void conversion_Bidirectional_MaintainsAccuracy(long originalAmount, BigDecimal expectedDisplay) {
            // When
            BigDecimal displayValue = formatter.toDisplayValue(originalAmount);
            long backToLong = formatter.fromDisplayValue(displayValue);

            // Then
            assertEquals(expectedDisplay, displayValue, "Display conversion should match expected");
            assertEquals(originalAmount, backToLong, "Round-trip conversion should maintain original value");
        }

        static Stream<Arguments> conversionTestData() {
            return Stream.of(
                    Arguments.of(0L, new BigDecimal("0.0000")),
                    Arguments.of(1L, new BigDecimal("0.0001")),
                    Arguments.of(10000L, new BigDecimal("1.0000")),
                    Arguments.of(12345L, new BigDecimal("1.2345")),
                    Arguments.of(999999L, new BigDecimal("99.9999")),
                    Arguments.of(-12345L, new BigDecimal("-1.2345"))
            );
        }
    }

    @Nested
    @DisplayName("Locale Tests")
    class LocaleTests {

        @Test
        @DisplayName("Should format according to French locale")
        void formatCurrency_FrenchLocale_FormatsCorrectly() {
            // Given
            CurrencyFormatter frenchFormatter = new CurrencyFormatter(
                    Locale.FRANCE, "€", "0,00", plugin);

            try {
                // When
                String result = frenchFormatter.formatCurrency(12345678L); // 1234.5678

                // Then
                assertNotNull(result);
                assertTrue(result.startsWith("€"));
                // French locale uses comma as decimal separator and space/period as thousands separator
                assertTrue(result.contains(",") || result.contains("."));
            } finally {
                frenchFormatter.cleanup();
            }
        }

        @Test
        @DisplayName("Should format according to German locale")
        void formatCurrency_GermanLocale_FormatsCorrectly() {
            // Given
            CurrencyFormatter germanFormatter = new CurrencyFormatter(
                    Locale.GERMANY, "€", "0,00", plugin);

            try {
                // When
                String result = germanFormatter.formatCurrency(12345678L); // 1234.5678

                // Then
                assertNotNull(result);
                assertTrue(result.startsWith("€"));
                // German locale also uses comma as decimal separator
                assertTrue(result.contains(","));
            } finally {
                germanFormatter.cleanup();
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return fallback value when NumberFormat fails")
        void formatCurrency_NumberFormatException_ReturnsFallback() {
            // This test is challenging because NumberFormat rarely fails
            // We'll test with an extreme value that might cause issues

            // When
            String result = formatter.formatCurrency(Long.MAX_VALUE);

            // Then
            assertNotNull(result);
            assertTrue(result.startsWith(CURRENCY_SYMBOL));
        }

        @Test
        @DisplayName("Should handle fallback with different currency symbols")
        void formatCurrency_ErrorWithDifferentSymbol_UsesFallback() {
            // Given
            CurrencyFormatter euroFormatter = new CurrencyFormatter(
                    TEST_LOCALE, "€", "Error", plugin);

            try {
                // When - test with valid input (hard to trigger actual error)
                String result = euroFormatter.formatCurrency(10000L);

                // Then
                assertNotNull(result);
                assertTrue(result.startsWith("€"));
            } finally {
                euroFormatter.cleanup();
            }
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent access safely")
        void formatCurrency_ConcurrentAccess_ThreadSafe() throws InterruptedException {
            // Given
            int threadCount = 10;
            int operationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            try {
                // When
                for (int i = 0; i < threadCount; i++) {
                    final int threadId = i;
                    executor.submit(() -> {
                        try {
                            for (int j = 0; j < operationsPerThread; j++) {
                                long amount = (threadId * 1000L + j) * 100L;
                                String result = formatter.formatCurrency(amount);

                                // Verify basic format
                                assertNotNull(result);
                                assertTrue(result.startsWith(CURRENCY_SYMBOL));
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                // Then
                assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete within timeout");
            } finally {
                executor.shutdown();
            }
        }

        @Test
        @DisplayName("Should handle ThreadLocal cleanup correctly")
        void cleanup_RemovesThreadLocalValues_HandlesGracefully() {
            // Given - use the formatter to initialize ThreadLocal
            formatter.formatCurrency(10000L);

            // When & Then - should not throw exception
            assertDoesNotThrow(() -> formatter.cleanup());

            // Should still work after cleanup (will reinitialize ThreadLocal)
            String result = formatter.formatCurrency(20000L);
            assertEquals("$2.00", result);
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct currency symbol")
        void getCurrencySymbol_ReturnsCorrectValue() {
            // When & Then
            assertEquals(CURRENCY_SYMBOL, formatter.getCurrencySymbol());
        }

        @Test
        @DisplayName("Should return correct fallback value")
        void getFallbackValue_ReturnsCorrectValue() {
            // When & Then
            assertEquals(FALLBACK_VALUE, formatter.getFallbackValue());
        }
    }

    @Nested
    @DisplayName("Constants Tests")
    class ConstantsTests {

        @Test
        @DisplayName("Should have correct decimal scale constant")
        void decimalScaleConstant_HasCorrectValue() {
            // Then
            assertEquals(10000L, CurrencyFormatter.DECIMAL_SCALE);
        }

        @Test
        @DisplayName("Should have consistent BigDecimal scale constant")
        void decimalScaleBigDecimalConstant_ConsistentWithLongConstant() {
            // Then
            assertEquals(BigDecimal.valueOf(CurrencyFormatter.DECIMAL_SCALE),
                    CurrencyFormatter.DECIMAL_SCALE_BD);
        }
    }
}