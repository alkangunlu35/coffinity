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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.data.model.beans.CountryBeans
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.util.BeanOriginTextLocalizer
import com.icoffee.app.util.CountryDisplayNames

@Composable
fun CountryBeanCard(
    country: CountryBeans,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val languageCode = AppLocaleManager.currentLanguage(context).code
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val signatureFallback = stringResource(R.string.beans_card_signature_fallback)
    val cardElevation by animateDpAsState(
        targetValue = if (isPressed) 6.dp else 14.dp,
        animationSpec = tween(durationMillis = if (isPressed) 90 else 220),
        label = "countryCardElevation"
    )

    val highlightedVariety = country.varieties.firstOrNull()
    val metadataLine = buildString {
        append(highlightedVariety?.name ?: signatureFallback)
        highlightedVariety?.flavorNotes?.firstOrNull()?.let { note ->
            append(" · ")
            append(BeanOriginTextLocalizer.localizedFlavorNote(note, languageCode))
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
                ambientColor = Color(0x26DBA15E),
                spotColor = Color(0x33DBA15E)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF4A2A1A),
                        Color(0xFF2B140A)
                    )
                )
            )
            .border(1.dp, Color(0x66D8A16A), RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x14FFFFFF),
                            Color(0x06FFFFFF),
                            Color.Transparent
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
                    text = CountryDisplayNames.localizedName(country.country, languageCode),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF3E6D2),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(R.string.beans_card_varieties_count, country.varieties.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFEADBC8).copy(alpha = 0.7f)
                )

                Text(
                    text = metadataLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEADBC8).copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
