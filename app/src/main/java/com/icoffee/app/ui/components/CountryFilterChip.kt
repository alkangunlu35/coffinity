package com.icoffee.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.icoffee.app.ui.theme.CreamText
import com.icoffee.app.ui.theme.GoldAccent
import com.icoffee.app.ui.theme.GoldAccentLight
import com.icoffee.app.ui.theme.MutedText
import com.icoffee.app.ui.theme.SurfaceDarkAlt
import com.icoffee.app.ui.theme.SurfaceStroke

@Composable
fun CountryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedText by animateColorAsState(
        targetValue = if (selected) Color(0xFF2E1C12) else MutedText,
        animationSpec = tween(180),
        label = "countryFilterText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) {
                    Brush.horizontalGradient(listOf(GoldAccentLight, GoldAccent))
                } else {
                    Brush.horizontalGradient(
                        listOf(SurfaceDarkAlt.copy(alpha = 0.92f), SurfaceDarkAlt.copy(alpha = 0.82f))
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0x66FFE2C7) else SurfaceStroke,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) selectedText else CreamText.copy(alpha = 0.84f)
        )
    }
}
