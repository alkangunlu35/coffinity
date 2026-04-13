package com.icoffee.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.data.model.AffiliateOffer
import com.icoffee.app.data.model.AffiliateTag

/**
 * Reusable affiliate offer card.
 *
 * @param offer         The affiliate offer to display.
 * @param darkTheme     True for dark espresso backgrounds (Bean Detail, Scan Result),
 *                      false for the warm cream light theme (Brew Detail).
 */
@Composable
fun AffiliateOfferCard(
    offer: AffiliateOffer,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    val cardGradient = if (darkTheme) {
        Brush.verticalGradient(
            listOf(Color(0xFF2C1D14), Color(0xFF221508))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFFFFCF8), Color(0xFFFEF6EE))
        )
    }

    val borderColor = if (darkTheme) Color(0x33E2B888) else Color(0x40C48A58)
    val tagBg = if (darkTheme) Color(0xFF3A2419) else Color(0xFFF0E3D2)
    val tagText = if (darkTheme) Color(0xFFE2B888) else Color(0xFF6B4428)
    val titleColor = if (darkTheme) Color(0xFFF8E6D1) else Color(0xFF2E1F14)
    val subtitleColor = if (darkTheme) Color(0xCCCAAF96) else Color(0xFF7A5C48)
    val priceColor = if (darkTheme) Color(0xFFD4935A) else Color(0xFFA5784F)
    val ctaTextColor = if (darkTheme) Color(0xFF1A0D08) else Color(0xFFFFFCF8)
    val ctaBg = if (darkTheme) Color(0xFFD4935A) else Color(0xFFA5784F)
    val shadowAmbient = if (darkTheme) Color(0x1A1A0A04) else Color(0x141A120C)
    val shadowSpot = if (darkTheme) Color(0x221A0A04) else Color(0x1C1A120C)

    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = cardShape,
                clip = false,
                ambientColor = shadowAmbient,
                spotColor = shadowSpot
            )
            .clip(cardShape)
            .background(cardGradient)
            .border(1.dp, borderColor, cardShape)
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.985f,
                pressedAlpha = 0.95f
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(offer.destinationUrl))
                    context.startActivity(intent)
                }
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header row: tag pill + retailer label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AffiliateTagPill(
                    tag = offer.tag,
                    bg = tagBg,
                    textColor = tagText
                )
                Text(
                    text = offer.retailerName,
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor
                )
            }

            // Title + subtitle
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = offer.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = titleColor,
                    maxLines = 2
                )
                Text(
                    text = offer.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                    maxLines = 3
                )
            }

            // Price + CTA row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                offer.priceHint?.let { price ->
                    Text(
                        text = price,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = priceColor
                    )
                } ?: Box(modifier = Modifier)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(ctaBg)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.affiliate_cta),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ctaTextColor
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = ctaTextColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AffiliateTagPill(
    tag: AffiliateTag,
    bg: Color,
    textColor: Color
) {
    val label = when (tag) {
        AffiliateTag.BEAN -> stringResource(R.string.affiliate_tag_bean)
        AffiliateTag.GEAR -> stringResource(R.string.affiliate_tag_gear)
        AffiliateTag.SUBSCRIPTION -> stringResource(R.string.affiliate_tag_subscription)
    }
    val icon = when (tag) {
        AffiliateTag.BEAN -> Icons.Default.Coffee
        AffiliateTag.GEAR -> Icons.Default.Handyman
        AffiliateTag.SUBSCRIPTION -> Icons.Default.Subscriptions
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = textColor
            )
        }
    }
}
