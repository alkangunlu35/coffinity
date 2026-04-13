package com.icoffee.app.ui.screens.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.ui.components.coffinityPressMotion
import com.icoffee.app.ui.theme.CoffeeSpacing

@Composable
fun ScanScreen(
    onScanProductClick: () -> Unit,
    onScanMenuClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5EFE7)),
        contentPadding = PaddingValues(
            start = CoffeeSpacing.lg,
            end = CoffeeSpacing.lg,
            bottom = CoffeeSpacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.md)
    ) {
        item {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.scan_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF2D2018)
                )
                Text(
                    text = stringResource(R.string.scan_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF746051)
                )
            }
        }

        item {
            ScanOptionCard(
                icon = Icons.Default.Coffee,
                title = stringResource(R.string.scan_product_title),
                subtitle = stringResource(R.string.scan_product_subtitle),
                onClick = onScanProductClick
            )
        }

        item {
            ScanOptionCard(
                icon = Icons.Default.MenuBook,
                title = stringResource(R.string.scan_menu_title),
                subtitle = stringResource(R.string.scan_menu_subtitle),
                onClick = onScanMenuClick
            )
        }
    }
}

@Composable
private fun ScanOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.98f,
                pressedAlpha = 0.96f
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFCF8), Color(0xFFF8EFE6))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFEEDCC8), RoundedCornerShape(14.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF805238)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF2D2018)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6F5A4B)
            )
        }
    }
}
