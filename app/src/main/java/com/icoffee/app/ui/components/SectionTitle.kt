package com.icoffee.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SectionTitle(
    title: String,
    actionText: String? = null,
    titleColor: Color? = null,
    actionColor: Color? = null
) {
    SectionHeader(
        title = title,
        actionText = actionText,
        titleColor = titleColor ?: MaterialTheme.colorScheme.onBackground,
        actionColor = actionColor ?: MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f)
    )
}
