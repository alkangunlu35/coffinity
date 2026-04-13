package com.icoffee.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.icoffee.app.ui.theme.CreamText
import com.icoffee.app.ui.theme.MutedText
import com.icoffee.app.ui.theme.SurfaceDarkAlt
import com.icoffee.app.ui.theme.SurfaceStroke

data class BeanInfoItem(
    val label: String,
    val value: String
)

@Composable
fun BeanInfoGrid(
    items: List<BeanInfoItem>,
    modifier: Modifier = Modifier
) {
    val rows = items.chunked(2)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    BeanInfoCell(
                        item = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    BeanInfoCell(
                        item = BeanInfoItem("", ""),
                        modifier = Modifier.weight(1f),
                        empty = true
                    )
                }
            }
        }
    }
}

@Composable
private fun BeanInfoCell(
    item: BeanInfoItem,
    modifier: Modifier = Modifier,
    empty: Boolean = false
) {
    if (empty) {
        Column(modifier = modifier) {}
        return
    }

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        SurfaceDarkAlt.copy(alpha = 0.86f),
                        Color(0xFF3A2318).copy(alpha = 0.82f)
                    )
                ),
                RoundedCornerShape(15.dp)
            )
            .border(1.dp, SurfaceStroke.copy(alpha = 0.92f), RoundedCornerShape(15.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelLarge,
            color = MutedText.copy(alpha = 0.9f)
        )
        Text(
            text = item.value,
            style = MaterialTheme.typography.bodyMedium,
            color = CreamText.copy(alpha = 0.96f)
        )
    }
}
