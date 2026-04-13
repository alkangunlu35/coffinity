package com.icoffee.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PrimaryButton(
    text: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    PrimaryCoffeeButton(
        text = text,
        subtitle = subtitle,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    )
}
