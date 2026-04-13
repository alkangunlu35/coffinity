package com.icoffee.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.icoffee.app.ui.theme.CoffeeColorTokens
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CoffeeSpace

@Composable
fun CoffeeEmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    containerColor: Color = CoffeeColorTokens.surfaceElevated,
    borderColor: Color = CoffeeColorTokens.borderSubtle,
    titleColor: Color = CoffeeColorTokens.textPrimary,
    subtitleColor: Color = CoffeeColorTokens.textSecondary,
    iconContainerColor: Color = CoffeeColorTokens.surfaceSoft,
    iconTint: Color = CoffeeColorTokens.textSecondary
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(CoffeeRadius.lg))
            .border(1.dp, borderColor, RoundedCornerShape(CoffeeRadius.lg))
            .padding(horizontal = CoffeeSpace.lg, vertical = CoffeeSpace.lg),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpace.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeeSpace.sm)
        ) {
            Box(
                modifier = Modifier
                    .background(iconContainerColor, CircleShape)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = titleColor
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = subtitleColor
        )
    }
}
