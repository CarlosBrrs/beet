/**
 * Shared formatting utilities for Colombian locale.
 *
 * Usage in components:
 *   import { formatCurrency, formatPriceDisplay, parsePriceInput } from "@/lib/formatters"
 *
 * All monetary formatting follows the Colombian convention:
 *   - Thousands separator: dot (.)
 *   - Decimal separator: comma (,)
 *   - Currency: COP ($)
 *   - Example: $80.000,25
 */

// ── Currency Display ──

/**
 * Formats a numeric value as a COP currency string.
 * @example formatCurrency(80000.25) → "$80.000,25"
 */
export function formatCurrency(value: number): string {
    if (!value && value !== 0) return "$0"
    return new Intl.NumberFormat("es-CO", {
        style: "currency",
        currency: "COP",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(value)
}

/**
 * Formats a numeric value with Colombian number style (no $ sign).
 * Useful for inline cost displays.
 * @example formatNumber(1.8, 2) → "1,80"
 * @example formatNumber(45000) → "45.000,00"
 */
export function formatNumber(value: number, maxDecimals = 4): string {
    return new Intl.NumberFormat("es-CO", {
        minimumFractionDigits: 2,
        maximumFractionDigits: maxDecimals,
    }).format(value)
}

// ── Price Input Formatting (live typing) ──

/**
 * Formats a raw string into a Colombian-style display as the user types.
 * Adds thousands dots and limits decimals to 2.
 *
 * @example formatPriceDisplay("80000")   → "80.000"
 * @example formatPriceDisplay("80000,2") → "80.000,2"
 * @example formatPriceDisplay("80000,25") → "80.000,25"
 */
export function formatPriceDisplay(value: string): string {
    // Strip non-numeric except comma and dot
    const cleaned = value.replace(/[^0-9.,]/g, "")
    if (!cleaned) return ""

    // Split on comma (decimal separator in Colombian format)
    const parts = cleaned.split(",")
    const intPart = parts[0].replace(/\./g, "") // remove existing dots
    const decPart = parts[1]?.slice(0, 2) // max 2 decimals

    // Add thousands dots
    const formatted = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ".")
    return decPart !== undefined ? `${formatted},${decPart}` : formatted
}

/**
 * Parses a Colombian-formatted display string back to a number.
 *
 * @example parsePriceInput("80.000,25") → 80000.25
 * @example parsePriceInput("1.500")     → 1500
 */
export function parsePriceInput(display: string): number {
    if (!display) return 0
    // Remove dots (thousands), replace comma with dot (decimal)
    const normalized = display.replace(/\./g, "").replace(",", ".")
    return parseFloat(normalized) || 0
}
