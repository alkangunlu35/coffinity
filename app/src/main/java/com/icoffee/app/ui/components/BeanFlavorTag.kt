package com.icoffee.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import com.icoffee.app.ui.theme.SurfaceStroke

@Composable
fun BeanFlavorTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0x3A4A2D1F),
                        Color(0x2A2D180F)
                    )
                ),
                RoundedCornerShape(999.dp)
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        SurfaceStroke.copy(alpha = 0.95f),
                        Color(0x55E2B888)
                    )
                ),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = CreamText.copy(alpha = 0.94f)
        )
    }
}
