package com.icoffee.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import com.icoffee.app.ui.theme.CoffeeColorTokens
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CoffeeSpace

@Composable
fun coffeeOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = CoffeeColorTokens.textPrimary,
    unfocusedTextColor = CoffeeColorTokens.textPrimary,
    disabledTextColor = CoffeeColorTokens.textMuted,
    focusedContainerColor = CoffeeColorTokens.surfaceSoft,
    unfocusedContainerColor = CoffeeColorTokens.surfaceSoft,
    disabledContainerColor = CoffeeColorTokens.surfaceSoft,
    focusedLabelColor = CoffeeColorTokens.textSecondary,
    unfocusedLabelColor = CoffeeColorTokens.textSecondary,
    disabledLabelColor = CoffeeColorTokens.textMuted,
    focusedPlaceholderColor = CoffeeColorTokens.textMuted,
    unfocusedPlaceholderColor = CoffeeColorTokens.textMuted,
    disabledPlaceholderColor = CoffeeColorTokens.textMuted,
    focusedBorderColor = CoffeeColorTokens.accentPrimary,
    unfocusedBorderColor = CoffeeColorTokens.borderSubtle,
    disabledBorderColor = CoffeeColorTokens.borderSubtle,
    cursorColor = CoffeeColorTokens.textPrimary
)

@Composable
fun coffeeFieldTextStyle() = MaterialTheme.typography.bodyLarge.copy(
    color = CoffeeColorTokens.textPrimary,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

@Composable
fun CoffeeLabelText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = CoffeeColorTokens.textSecondary
    )
}

@Composable
fun CoffeePlaceholderText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = CoffeeColorTokens.textMuted
    )
}

enum class CoffeeFeedbackTone {
    Info,
    Success,
    Warning,
    Error
}

@Composable
fun CoffeeFeedbackCard(
    message: String,
    tone: CoffeeFeedbackTone,
    modifier: Modifier = Modifier
) {
    val toneColor = when (tone) {
        CoffeeFeedbackTone.Info -> CoffeeColorTokens.accentPrimary
        CoffeeFeedbackTone.Success -> CoffeeColorTokens.success
        CoffeeFeedbackTone.Warning -> CoffeeColorTokens.warning
        CoffeeFeedbackTone.Error -> CoffeeColorTokens.error
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = toneColor.copy(alpha = 0.09f),
                shape = RoundedCornerShape(CoffeeRadius.md)
            )
            .border(
                width = 1.dp,
                color = toneColor.copy(alpha = 0.28f),
                shape = RoundedCornerShape(CoffeeRadius.md)
            )
            .padding(horizontal = CoffeeSpace.md, vertical = CoffeeSpace.sm)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = CoffeeColorTokens.textPrimary
        )
    }
}
