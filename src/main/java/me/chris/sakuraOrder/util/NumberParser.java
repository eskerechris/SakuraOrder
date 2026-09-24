package me.chris.sakuraOrder.util;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Utility class for converting, validating, and formatting numeric and economic values using BigDecimal
 * to ensure high precision for large numbers (up to Quadrillions and beyond).
 * Supports compact suffixes: K (Thousands), M (Millions), B (Billions), T (Trillions), Q (Quadrillions).
 */
public final class NumberParser {

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private static final BigDecimal BILLION = new BigDecimal("1000000000");
    private static final BigDecimal TRILLION = new BigDecimal("1000000000000");
    private static final BigDecimal QUADRILLION = new BigDecimal("1000000000000000");

    private NumberParser() {}

    /**
     * Converts a potentially abbreviated numeric string (k, m, b, t, q) into a BigDecimal.
     * Accepts both '.' and ',' as the decimal separator (e.g. "2.5k" or "2,5k").
     * Strictly validates that the number is positive (> 0).
     *
     * @param input string to convert (e.g., "100", "1.5k", "2,25m", "3.5t", "1q")
     * @return a valid, positive BigDecimal value
     * @throws NumberFormatException if the input is blank, invalid, or <= 0
     */
    @NotNull
    public static BigDecimal parseNumber(@NotNull String input) {
        if (input.isBlank()) {
            throw new NumberFormatException("Input string cannot be null or empty");
        }

        String sanitized = input.trim().toLowerCase(Locale.ROOT).replace(',', '.');
        BigDecimal multiplier = getMultiplier(sanitized);

        if (multiplier.compareTo(BigDecimal.ONE) > 0) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }

        BigDecimal value;
        try {
            value = new BigDecimal(sanitized);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid number format: " + input);
        }

        BigDecimal result = value.multiply(multiplier);
        if (result.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NumberFormatException("Value must be a positive number: " + input);
        }

        return result;
    }

    /**
     * Parses a potentially abbreviated numeric string into a {@code long}.
     * Must be a whole number after suffix expansion and fit within the long range.
     *
     * @throws NumberFormatException if invalid, not a whole number, <= 0, or too large for a long
     */
    public static long parseLong(@NotNull String input) {
        BigDecimal value = parseNumber(input);
        requireWholeNumber(value, input);

        if (value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            throw new NumberFormatException("Value is too large: " + input);
        }

        return value.longValueExact();
    }

    /**
     * Parses a potentially abbreviated numeric string into an {@code int}.
     * Same whole-number requirement as {@link #parseLong(String)}, bounded by the int range.
     *
     * @throws NumberFormatException if invalid, not a whole number, <= 0, or too large for an int
     */
    public static int parseInt(@NotNull String input) {
        long value = parseLong(input);

        if (value > Integer.MAX_VALUE) {
            throw new NumberFormatException("Value is too large: " + input);
        }

        return (int) value;
    }

    /**
     * Parses a potentially abbreviated numeric string into a {@code double}.
     * Fractional values are allowed (e.g. "1.5k" -> 1500.0). Precision may be lost
     * on extremely large values (T/Q range) — prefer {@link #parseNumber(String)}
     * directly if exact precision matters there.
     *
     * @throws NumberFormatException if the input is invalid or <= 0
     */
    public static double parseDouble(@NotNull String input) {
        return parseNumber(input).doubleValue();
    }

    /**
     * Formats a BigDecimal value into a compact form (K, M, B, T, Q) for user interface display.
     * Non-positive values format as {@code "0"}. Decimal separator is always ','.
     */
    @NotNull
    public static String formatNumber(@NotNull BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "0";
        }
        if (amount.compareTo(QUADRILLION) >= 0) {
            return formatDecimal(amount.divide(QUADRILLION, 2, RoundingMode.HALF_UP)) + "Q";
        }
        if (amount.compareTo(TRILLION) >= 0) {
            return formatDecimal(amount.divide(TRILLION, 2, RoundingMode.HALF_UP)) + "T";
        }
        if (amount.compareTo(BILLION) >= 0) {
            return formatDecimal(amount.divide(BILLION, 2, RoundingMode.HALF_UP)) + "B";
        }
        if (amount.compareTo(MILLION) >= 0) {
            return formatDecimal(amount.divide(MILLION, 2, RoundingMode.HALF_UP)) + "M";
        }
        if (amount.compareTo(THOUSAND) >= 0) {
            return formatDecimal(amount.divide(THOUSAND, 2, RoundingMode.HALF_UP)) + "K";
        }
        return formatDecimal(amount.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * @see #formatNumber(BigDecimal)
     */
    @NotNull
    public static String formatNumber(long amount) {
        return formatNumber(BigDecimal.valueOf(amount));
    }

    /**
     * @see #formatNumber(BigDecimal)
     */
    @NotNull
    public static String formatNumber(int amount) {
        return formatNumber((long) amount);
    }

    /**
     * @see #formatNumber(BigDecimal)
     * <p>
     * Uses {@link BigDecimal#valueOf(double)} (string-based conversion) rather than
     * {@code new BigDecimal(double)}, avoiding binary floating-point artifacts.
     */
    @NotNull
    public static String formatNumber(double amount) {
        return formatNumber(BigDecimal.valueOf(amount));
    }

    @NotNull
    private static BigDecimal getMultiplier(@NotNull String sanitized) {
        if (sanitized.endsWith("q")) return QUADRILLION;
        if (sanitized.endsWith("t")) return TRILLION;
        if (sanitized.endsWith("b")) return BILLION;
        if (sanitized.endsWith("m")) return MILLION;
        if (sanitized.endsWith("k")) return THOUSAND;
        return BigDecimal.ONE;
    }

    private static void requireWholeNumber(@NotNull BigDecimal value, @NotNull String originalInput) {
        if (value.stripTrailingZeros().scale() > 0) {
            throw new NumberFormatException("Value must be a whole number: " + originalInput);
        }
    }

    /**
     * stripTrailingZeros() removes unnecessary decimal zeros (e.g. 1.50 -> 1.5, 1.00 -> 1).
     * toPlainString() avoids scientific notation (e.g. 1E+2). The '.' -> ',' replace
     * at the end enforces the comma decimal separator unconditionally.
     */
    @NotNull
    private static String formatDecimal(@NotNull BigDecimal value) {
        return value.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}