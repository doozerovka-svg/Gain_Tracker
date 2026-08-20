package com.example.workouttracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Direct numeric keypad (0-9, ., ⌫, C) with decimal validation, input sanitization, and >=48dp touch targets.
 */
@Composable
fun NumericWeightKeypad(
    currentInput: String,
    onInputChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onConfirm: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: 1, 2, 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KeypadDigitButton("1", modifier = Modifier.weight(1f)) {
                    onInputChange(KeypadSanitizer.appendDigit(currentInput, '1'))
                }
                KeypadDigitButton("2", modifier = Modifier.weight(1f)) {
                    onInputChange(KeypadSanitizer.appendDigit(currentInput, '2'))
                }
                KeypadDigitButton("3", modifier = Modifier.weight(1f)) {
                    onInputChange(KeypadSanitizer.appendDigit(currentInput, '3'))
                }
            }

            // Row 2: 4, 5, 6
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KeypadDigitButton("4", modifier = Modifier.weight(1f)) {
                    onInputChange(KeypadSanitizer.appendDigit(currentInput, '4'))
                }
                KeypadDigitButton("5", modifier = Modifier.weight(1f)) {
                    onInputChange(KeypadSanitizer.appendDigit(currentInput, '5'))
                }
                KeypadDigitButton("6", modifier = Modifier.weight(1f)) {
                    onInputChange(KeypadSanitizer.appendDigit(currentInput, '6'))
                }
            }

            // Row 3: 7, 8, 9
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KeypadDigitButton("7", modifier = Modifier.weight(1f)) {
                    onInputChange(KeypadSanitizer.appendDigit(currentInput, '7'))
                }
                KeypadDigitButton("8", modifier = Modifier.weight(1f)) {
                    onInputChange(KeypadSanitizer.appendDigit(currentInput, '8'))
                }
                KeypadDigitButton("9", modifier = Modifier.weight(1f)) {
                    onInputChange(KeypadSanitizer.appendDigit(currentInput, '9'))
                }
            }

            // Row 4: C, 0, ., ⌫
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Clear Button
                FilledTonalButton(
                    onClick = { onInputChange(KeypadSanitizer.clear()) },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "C",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Digit 0 Button
                KeypadDigitButton("0", modifier = Modifier.weight(1f)) {
                    onInputChange(KeypadSanitizer.appendDigit(currentInput, '0'))
                }

                // Decimal Dot Button
                FilledTonalButton(
                    onClick = { onInputChange(KeypadSanitizer.appendDot(currentInput)) },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = ".",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Backspace Button
                FilledTonalButton(
                    onClick = { onInputChange(KeypadSanitizer.backspace(currentInput)) },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Удалить последний символ"
                    )
                }
            }

            if (onConfirm != null) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Готово", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun KeypadDigitButton(
    digit: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
        )
    }
}

/**
 * Pure sanitized logic for direct keypad inputs with decimal point validation and limits.
 */
object KeypadSanitizer {
    private const val MAX_LENGTH = 6
    private const val MAX_WEIGHT = 999.9

    fun appendDigit(current: String, digit: Char): String {
        val sanitized = current.trim()
        if (sanitized.length >= MAX_LENGTH) return sanitized
        if (sanitized == "0" || sanitized.isEmpty()) {
            return digit.toString()
        }
        val candidate = sanitized + digit
        val parsed = candidate.toDoubleOrNull() ?: return sanitized
        return if (parsed <= MAX_WEIGHT) candidate else sanitized
    }

    fun appendDot(current: String): String {
        val sanitized = current.trim()
        if (sanitized.isEmpty()) return "0."
        if (sanitized.contains('.')) return sanitized
        if (sanitized.length >= MAX_LENGTH) return sanitized
        return "$sanitized."
    }

    fun backspace(current: String): String {
        val sanitized = current.trim()
        if (sanitized.length <= 1) return "0"
        return sanitized.substring(0, sanitized.length - 1)
    }

    fun clear(): String = "0"

    fun parseWeight(input: String): Double {
        val normalized = input.replace(',', '.').trim()
        val parsed = normalized.toDoubleOrNull() ?: 0.0
        return BigDecimal.valueOf(parsed.coerceIn(0.0, MAX_WEIGHT))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }
}
