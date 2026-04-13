package com.icoffee.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.data.model.beans.CountryBeans

@Composable
fun CountryBeanCard(
    country: CountryBeans,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val signatureFallback = stringResource(R.string.beans_card_signature_fallback)
    val cardElevation by animateDpAsState(
        targetValue = if (isPressed) 5.dp else 11.dp,
        animationSpec = tween(durationMillis = if (isPressed) 90 else 220),
        label = "countryCardElevation"
    )

    val highlightedVariety = country.varieties.firstOrNull()
    val metadataLine = buildString {
        append(highlightedVariety?.name ?: signatureFallback)
        highlightedVariety?.flavorNotes?.firstOrNull()?.let { note ->
            append(" · ")
            append(note)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.97f,
                pressedAlpha = 0.96f
            )
            .shadow(
                elevation = cardElevation,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color(0x40150907),
                spotColor = Color(0x4A150907)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2A1710),
                        Color(0xFF1E100B)
                    )
                )
            )
            .border(1.dp, Color(0x22F5E6D3), RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x1AF5E6D3),
                            Color(0x12150C08),
                            Color(0x260E0705)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0x26B67A4D),
                            Color.Transparent
                        ),
                        radius = 420f
                    )
                )
        )

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.matchParentSize()
        ) {
            Text(
                text = country.flagEmoji,
                style = MaterialTheme.typography.titleMedium
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = country.country,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFF5E6D3),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(R.string.beans_card_varieties_count, country.varieties.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD6BFA7)
                )

                Text(
                    text = metadataLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD6BFA7).copy(alpha = 0.94f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
