package com.icoffee.app.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.icoffee.app.R

@Composable
fun ParticipantStepper(
    value: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedValue = animateIntAsState(
        targetValue = value,
        animationSpec = tween(180),
        label = "participantStepperValue"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFF3E7D8),
                        Color(0xFFF8EFE5)
                    )
                )
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperActionButton(
            label = "−",
            enabled = value > minValue,
            onClick = { if (value > minValue) onValueChange(value - 1) }
        )

        Text(
            text = pluralStringResource(
                id = R.plurals.meet_people_count_plural,
                count = animatedValue.value,
                animatedValue.value
            ),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF543524)
        )

        StepperActionButton(
            label = "+",
            enabled = value < maxValue,
            onClick = { if (value < maxValue) onValueChange(value + 1) }
        )
    }
}

@Composable
private fun StepperActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = if (enabled) 0.95f else 1f,
                pressedAlpha = if (enabled) 0.96f else 1f
            )
            .size(32.dp)
            .clip(CircleShape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        listOf(Color(0xFFD9A066), Color(0xFFB67A4D))
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(Color(0xFFE2D5C8), Color(0xFFD7C8BA))
                    )
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) Color.White else Color(0xAA8B735E)
        )
    }
}
