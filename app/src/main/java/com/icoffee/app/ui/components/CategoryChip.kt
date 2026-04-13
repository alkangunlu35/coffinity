package com.icoffee.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CreamText
import com.icoffee.app.ui.theme.GoldAccent
import com.icoffee.app.ui.theme.GoldAccentLight
import com.icoffee.app.ui.theme.MutedText
import com.icoffee.app.ui.theme.SurfaceDarkAlt
import com.icoffee.app.ui.theme.SurfaceStroke

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = tween(220),
        label = "categorySelectedScale"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = if (pressed) 90 else 220),
        label = "categoryPressScale"
    )
    val chipShadow by animateDpAsState(
        targetValue = if (selected) 6.dp else 3.dp,
        animationSpec = tween(220),
        label = "categoryChipShadow"
    )
    val selectedContainer by animateColorAsState(
        targetValue = GoldAccent,
        animationSpec = tween(220),
        label = "categorySelectedContainer"
    )
    val unselectedContainer by animateColorAsState(
        targetValue = SurfaceDarkAlt.copy(alpha = 0.9f),
        animationSpec = tween(220),
        label = "categoryUnselectedContainer"
    )
    val selectedLabel by animateColorAsState(
        targetValue = CreamText,
        animationSpec = tween(220),
        label = "categorySelectedLabel"
    )
    val unselectedLabel by animateColorAsState(
        targetValue = MutedText,
        animationSpec = tween(220),
        label = "categoryUnselectedLabel"
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(CoffeeRadius.pill),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        },
        leadingIcon = if (selected) {
            {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(CreamText, CircleShape)
                )
            }
        } else {
            null
        },
        modifier = Modifier
            .graphicsLayer {
                val scale = selectedScale * pressScale
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = chipShadow,
                shape = RoundedCornerShape(CoffeeRadius.pill),
                clip = false
            ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                GoldAccentLight.copy(alpha = 0.5f)
            } else {
                SurfaceStroke
            }
        ),
        elevation = FilterChipDefaults.filterChipElevation(
            elevation = if (selected) 5.dp else 1.dp
        ),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedContainer,
            selectedLabelColor = selectedLabel,
            containerColor = unselectedContainer,
            labelColor = unselectedLabel
        )
    )
}
