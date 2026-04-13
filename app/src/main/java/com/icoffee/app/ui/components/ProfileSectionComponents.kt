package com.icoffee.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CoffeeSpacing

@Composable
fun ProfileSectionHeader(
    title: String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF6E5D1)
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xC7D5B99A)
            )
        }
    }
}

@Composable
fun TasteSummaryCard(
    title: String,
    subtitle: String,
    footer: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD04A2D21),
                        Color(0xD03A2419)
                    )
                ),
                shape = RoundedCornerShape(CoffeeRadius.lg)
            )
            .border(
                1.dp,
                Color(0x38F2CEAA),
                RoundedCornerShape(CoffeeRadius.lg)
            )
            .padding(CoffeeSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFF7E8D5)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFE4C8AB)
        )
        Text(
            text = footer,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xD2D9BFA3)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoriteChipRow(
    values: List<String>,
    emptyText: String
) {
    if (values.isEmpty()) {
        EmptyProfileState(text = emptyText)
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFFF7E7D3),
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xAA5D3826), Color(0xAA774B35))
                        ),
                        RoundedCornerShape(999.dp)
                    )
                    .border(
                        1.dp,
                        Color(0x38F2CEAA),
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
fun ProfileHorizontalItemCard(
    title: String,
    subtitle: String,
    meta: String,
    imageUrl: String? = null
) {
    Row(
        modifier = Modifier
            .background(
                Color(0xCC3A2419),
                RoundedCornerShape(CoffeeRadius.md)
            )
            .border(
                1.dp,
                Color(0x2EF2CEAA),
                RoundedCornerShape(CoffeeRadius.md)
            )
            .padding(horizontal = CoffeeSpacing.md, vertical = CoffeeSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFF6E5D1)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xE2D5B99A)
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xC9C7A78B)
            )
        }
    }
}

@Composable
fun EmptyProfileState(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xC9D0B49A),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0x80332118),
                RoundedCornerShape(CoffeeRadius.md)
            )
            .border(
                1.dp,
                Color(0x22F2CEAA),
                RoundedCornerShape(CoffeeRadius.md)
            )
            .padding(CoffeeSpacing.sm)
    )
}

@Composable
fun ProfileSettingsRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xCC372218),
                RoundedCornerShape(CoffeeRadius.md)
            )
            .border(
                1.dp,
                Color(0x2CF2CEAA),
                RoundedCornerShape(CoffeeRadius.md)
            )
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = CoffeeSpacing.md, vertical = CoffeeSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFF7E8D4)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xDDD5B99A)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xC9C7A78B)
        )
    }
}
