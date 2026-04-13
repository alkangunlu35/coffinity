package com.icoffee.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.icoffee.app.ui.theme.CoffeeColorTokens
import com.icoffee.app.ui.theme.CoffeeElevation
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CoffeeSpace

@Composable
fun PrimaryCoffeeButton(
    text: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val minButtonHeight = if (subtitle.isNullOrBlank()) 52.dp else 60.dp
    val cornerShape = RoundedCornerShape(CoffeeRadius.pill)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedShadow by animateDpAsState(
        targetValue = when {
            !enabled -> CoffeeElevation.sm
            pressed -> CoffeeElevation.sm
            else -> CoffeeElevation.md
        },
        animationSpec = tween(durationMillis = if (pressed) 90 else 220),
        label = "primaryButtonShadow"
    )
    val animatedContainer by animateColorAsState(
        targetValue = when {
            !enabled -> CoffeeColorTokens.accentPrimary.copy(alpha = 0.56f)
            pressed -> CoffeeColorTokens.accentPrimaryPressed
            else -> CoffeeColorTokens.accentPrimary
        },
        animationSpec = tween(durationMillis = 140),
        label = "primaryButtonContainer"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minButtonHeight)
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = if (enabled) 0.97f else 1f,
                pressedAlpha = if (enabled) 0.98f else 1f
            )
            .shadow(
                elevation = animatedShadow,
                shape = cornerShape,
                clip = false
            )
            .clip(cornerShape)
            .background(animatedContainer)
            .border(
                width = 1.dp,
                color = if (enabled) CoffeeColorTokens.borderStrong else CoffeeColorTokens.borderStrong.copy(alpha = 0.56f),
                shape = cornerShape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = CoffeeSpace.xl,
                vertical = if (subtitle.isNullOrBlank()) CoffeeSpace.md else CoffeeSpace.sm
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = if (enabled) CoffeeColorTokens.textOnAccent else CoffeeColorTokens.textOnAccent.copy(alpha = 0.84f)
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (enabled) CoffeeColorTokens.textOnAccent.copy(alpha = 0.92f) else CoffeeColorTokens.textOnAccent.copy(alpha = 0.72f)
                )
            }
        }
    }
}
