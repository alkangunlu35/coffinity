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

@Composable
fun MeetPurposeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectedText by animateColorAsState(
        targetValue = if (selected) Color(0xFFF5E6D3) else Color(0xB5D6BFA7),
        animationSpec = tween(180),
        label = "meetPurposeChipText"
    )

    Box(
        modifier = modifier
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.98f,
                pressedAlpha = 0.96f
            )
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (selected) {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFB67A4D),
                            Color(0xFFC58B5A)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0x1AFFFFFF),
                            Color(0x12FFFFFF)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0x44F5E6D3) else Color(0x33D6BFA7),
                shape = RoundedCornerShape(50.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = selectedText
        )
    }
}
