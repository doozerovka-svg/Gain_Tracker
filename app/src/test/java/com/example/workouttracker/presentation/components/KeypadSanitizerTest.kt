package com.example.workouttracker.presentation.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeypadSanitizerTest {

    @Test
    fun `appendDigit replaces initial 0 or empty with single digit`() {
        assertThat(KeypadSanitizer.appendDigit("0", '5')).isEqualTo("5")
        assertThat(KeypadSanitizer.appendDigit("", '7')).isEqualTo("7")
    }

    @Test
    fun `appendDigit concatenates subsequent digits`() {
        assertThat(KeypadSanitizer.appendDigit("10", '5')).isEqualTo("105")
    }

    @Test
    fun `appendDot adds single dot only once`() {
        assertThat(KeypadSanitizer.appendDot("100")).isEqualTo("100.")
        assertThat(KeypadSanitizer.appendDot("100.")).isEqualTo("100.")
        assertThat(KeypadSanitizer.appendDot("100.5")).isEqualTo("100.5")
    }

    @Test
    fun `backspace removes last character or resets to 0`() {
        assertThat(KeypadSanitizer.backspace("100")).isEqualTo("10")
        assertThat(KeypadSanitizer.backspace("5")).isEqualTo("0")
        assertThat(KeypadSanitizer.backspace("0")).isEqualTo("0")
    }

    @Test
    fun `clear resets to 0`() {
        assertThat(KeypadSanitizer.clear()).isEqualTo("0")
    }

    @Test
    fun `parseWeight handles integer and decimal formats cleanly`() {
        assertThat(KeypadSanitizer.parseWeight("100")).isEqualTo(100.0)
        assertThat(KeypadSanitizer.parseWeight("82.5")).isEqualTo(82.5)
        assertThat(KeypadSanitizer.parseWeight("82,5")).isEqualTo(82.5)
        assertThat(KeypadSanitizer.parseWeight("invalid")).isEqualTo(0.0)
        assertThat(KeypadSanitizer.parseWeight("1200")).isEqualTo(999.9) // Clamped to max weight
    }

    @Test
    fun `getRirDescription returns correct Russian text for all values`() {
        assertThat(getRirDescription(0)).isEqualTo("0 — До отказа (0 в запасе)")
        assertThat(getRirDescription(1)).isEqualTo("1 — Предельно тяжело (1 в запасе)")
        assertThat(getRirDescription(2)).isEqualTo("2 — Тяжело (2 в запасе)")
        assertThat(getRirDescription(3)).isEqualTo("3 — Умеренно (3 в запасе)")
        assertThat(getRirDescription(4)).isEqualTo("4 — Легко (4 в запасе)")
        assertThat(getRirDescription(5)).isEqualTo("5 — Разминка / Запас ≥ 5")
    }
}
