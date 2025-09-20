package io.github.HenriqueMichelini.craftalism_economy.economy.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EconomyValidator Tests")
class EconomyValidatorTest {

    private EconomyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EconomyValidator();
    }

    @Nested
    @DisplayName("hasSufficientFunds Tests")
    class HasSufficientFundsTests {

        @Test
        @DisplayName("Should return true when balance equals amount")
        void hasSufficientFunds_BalanceEqualsAmount_ReturnsTrue() {
            // Given
            long balance = 1000L;
            long amount = 1000L;

            // When
            boolean result = validator.hasSufficientFunds(balance, amount);

            // Then
            assertTrue(result, "Should have sufficient funds when balance equals amount");
        }

        @Test
        @DisplayName("Should return true when balance is greater than amount")
        void hasSufficientFunds_BalanceGreaterThanAmount_ReturnsTrue() {
            // Given
            long balance = 1000L;
            long amount = 500L;

            // When
            boolean result = validator.hasSufficientFunds(balance, amount);

            // Then
            assertTrue(result, "Should have sufficient funds when balance is greater than amount");
        }

        @Test
        @DisplayName("Should return false when balance is less than amount")
        void hasSufficientFunds_BalanceLessThanAmount_ReturnsFalse() {
            // Given
            long balance = 500L;
            long amount = 1000L;

            // When
            boolean result = validator.hasSufficientFunds(balance, amount);

            // Then
            assertFalse(result, "Should not have sufficient funds when balance is less than amount");
        }

        @Test
        @DisplayName("Should return false when amount is negative even with sufficient balance")
        void hasSufficientFunds_NegativeAmount_ReturnsFalse() {
            // Given
            long balance = 1000L;
            long negativeAmount = -100L;

            // When
            boolean result = validator.hasSufficientFunds(balance, negativeAmount);

            // Then
            assertFalse(result, "Should return false for negative amounts regardless of balance");
        }

        @Test
        @DisplayName("Should return true for zero amount with non-negative balance")
        void hasSufficientFunds_ZeroAmount_ReturnsTrue() {
            // Given
            long balance = 1000L;
            long zeroAmount = 0L;

            // When
            boolean result = validator.hasSufficientFunds(balance, zeroAmount);

            // Then
            assertTrue(result, "Should return true for zero amount with sufficient balance");
        }

        @Test
        @DisplayName("Should return true for zero amount with zero balance")
        void hasSufficientFunds_ZeroAmountZeroBalance_ReturnsTrue() {
            // Given
            long balance = 0L;
            long amount = 0L;

            // When
            boolean result = validator.hasSufficientFunds(balance, amount);

            // Then
            assertTrue(result, "Should return true when both balance and amount are zero");
        }

        @Test
        @DisplayName("Should handle negative balance correctly")
        void hasSufficientFunds_NegativeBalance_ReturnsFalse() {
            // Given
            long negativeBalance = -500L;
            long amount = 100L;

            // When
            boolean result = validator.hasSufficientFunds(negativeBalance, amount);

            // Then
            assertFalse(result, "Should return false when balance is negative");
        }

        @ParameterizedTest
        @CsvSource({
                "1000, 500, true",      // balance > amount
                "1000, 1000, true",     // balance = amount
                "500, 1000, false",     // balance < amount
                "0, 0, true",           // both zero
                "100, 0, true",         // zero amount
                "-100, 50, false",      // negative balance
                "1000, -50, false",     // negative amount
                "-100, -50, false"      // both negative
        })
        @DisplayName("Should handle various balance and amount combinations")
        void hasSufficientFunds_VariousCombinations(long balance, long amount, boolean expected) {
            // When
            boolean result = validator.hasSufficientFunds(balance, amount);

            // Then
            assertEquals(expected, result,
                    String.format("Balance: %d, Amount: %d should return %b", balance, amount, expected));
        }

        @Test
        @DisplayName("Should handle maximum long values")
        void hasSufficientFunds_MaximumValues_HandledCorrectly() {
            // Given
            long maxBalance = Long.MAX_VALUE;
            long largeAmount = Long.MAX_VALUE - 1000L;

            // When
            boolean result = validator.hasSufficientFunds(maxBalance, largeAmount);

            // Then
            assertTrue(result, "Should handle maximum long values correctly");
        }

        @Test
        @DisplayName("Should handle minimum long values")
        void hasSufficientFunds_MinimumValues_HandledCorrectly() {
            // Given
            long minBalance = Long.MIN_VALUE;
            long amount = 1L;

            // When
            boolean result = validator.hasSufficientFunds(minBalance, amount);

            // Then
            assertFalse(result, "Should handle minimum long values correctly");
        }
    }

    @Nested
    @DisplayName("isGreaterThanZero Tests")
    class IsGreaterThanZeroTests {

        @Test
        @DisplayName("Should return true for positive amounts")
        void isGreaterThanZero_PositiveAmount_ReturnsTrue() {
            // Given
            long positiveAmount = 100L;

            // When
            boolean result = validator.isGreaterThanZero(positiveAmount);

            // Then
            assertTrue(result, "Should return true for positive amounts");
        }

        @Test
        @DisplayName("Should return false for zero")
        void isGreaterThanZero_Zero_ReturnsFalse() {
            // Given
            long zero = 0L;

            // When
            boolean result = validator.isGreaterThanZero(zero);

            // Then
            assertFalse(result, "Should return false for zero");
        }

        @Test
        @DisplayName("Should return false for negative amounts")
        void isGreaterThanZero_NegativeAmount_ReturnsFalse() {
            // Given
            long negativeAmount = -100L;

            // When
            boolean result = validator.isGreaterThanZero(negativeAmount);

            // Then
            assertFalse(result, "Should return false for negative amounts");
        }

        @ParameterizedTest
        @ValueSource(longs = {1L, 10L, 100L, 1000L, Long.MAX_VALUE})
        @DisplayName("Should return true for various positive values")
        void isGreaterThanZero_VariousPositiveValues_ReturnsTrue(long positiveValue) {
            // When
            boolean result = validator.isGreaterThanZero(positiveValue);

            // Then
            assertTrue(result, "Should return true for positive value: " + positiveValue);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, -10L, -100L, -1000L, Long.MIN_VALUE})
        @DisplayName("Should return false for various negative values")
        void isGreaterThanZero_VariousNegativeValues_ReturnsFalse(long negativeValue) {
            // When
            boolean result = validator.isGreaterThanZero(negativeValue);

            // Then
            assertFalse(result, "Should return false for negative value: " + negativeValue);
        }

        @Test
        @DisplayName("Should handle maximum long value")
        void isGreaterThanZero_MaxLongValue_ReturnsTrue() {
            // Given
            long maxValue = Long.MAX_VALUE;

            // When
            boolean result = validator.isGreaterThanZero(maxValue);

            // Then
            assertTrue(result, "Should return true for Long.MAX_VALUE");
        }

        @Test
        @DisplayName("Should handle minimum long value")
        void isGreaterThanZero_MinLongValue_ReturnsFalse() {
            // Given
            long minValue = Long.MIN_VALUE;

            // When
            boolean result = validator.isGreaterThanZero(minValue);

            // Then
            assertFalse(result, "Should return false for Long.MIN_VALUE");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Integration Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should maintain consistency between methods")
        void methodConsistency_PositiveAmount_ConsistentResults() {
            // Given
            long amount = 100L;
            long sufficientBalance = 200L;

            // When
            boolean isGreater = validator.isGreaterThanZero(amount);
            boolean hasFunds = validator.hasSufficientFunds(sufficientBalance, amount);

            // Then
            assertTrue(isGreater, "Positive amount should be greater than zero");
            assertTrue(hasFunds, "Should have sufficient funds for positive amount with adequate balance");
        }

        @Test
        @DisplayName("Should maintain consistency for zero amounts")
        void methodConsistency_ZeroAmount_ConsistentResults() {
            // Given
            long zeroAmount = 0L;
            long balance = 100L;

            // When
            boolean isGreater = validator.isGreaterThanZero(zeroAmount);
            boolean hasFunds = validator.hasSufficientFunds(balance, zeroAmount);

            // Then
            assertFalse(isGreater, "Zero should not be greater than zero");
            assertTrue(hasFunds, "Should have sufficient funds for zero amount");
        }

        @Test
        @DisplayName("Should maintain consistency for negative amounts")
        void methodConsistency_NegativeAmount_ConsistentResults() {
            // Given
            long negativeAmount = -50L;
            long largeBalance = 1000L;

            // When
            boolean isGreater = validator.isGreaterThanZero(negativeAmount);
            boolean hasFunds = validator.hasSufficientFunds(largeBalance, negativeAmount);

            // Then
            assertFalse(isGreater, "Negative amount should not be greater than zero");
            assertFalse(hasFunds, "Should not have sufficient funds for negative amount (invalid)");
        }

        @Test
        @DisplayName("Should handle edge case where balance exactly matches Long.MAX_VALUE")
        void edgeCase_MaxValueBalance_HandledCorrectly() {
            // Given
            long maxBalance = Long.MAX_VALUE;
            long maxAmount = Long.MAX_VALUE;

            // When
            boolean hasFunds = validator.hasSufficientFunds(maxBalance, maxAmount);
            boolean isGreater = validator.isGreaterThanZero(maxAmount);

            // Then
            assertTrue(hasFunds, "Should have sufficient funds when balance equals max amount");
            assertTrue(isGreater, "Max value should be greater than zero");
        }

        @Test
        @DisplayName("Should demonstrate the compound logic in hasSufficientFunds")
        void compoundLogic_ValidAmountAndSufficientBalance_Required() {
            assertAll("hasSufficientFunds requires both conditions",
                    // Valid amount (>= 0) AND sufficient balance
                    () -> assertTrue(validator.hasSufficientFunds(100L, 50L),
                            "Valid amount with sufficient balance should return true"),

                    // Invalid amount (< 0) even with sufficient balance
                    () -> assertFalse(validator.hasSufficientFunds(100L, -50L),
                            "Invalid amount should return false regardless of balance"),

                    // Valid amount but insufficient balance
                    () -> assertFalse(validator.hasSufficientFunds(50L, 100L),
                            "Insufficient balance should return false even with valid amount"),

                    // Both conditions fail
                    () -> assertFalse(validator.hasSufficientFunds(50L, -100L),
                            "Both invalid amount and insufficient balance should return false")
            );
        }
    }
}