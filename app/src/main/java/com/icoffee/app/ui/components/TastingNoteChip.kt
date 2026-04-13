package com.icoffee.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.icoffee.app.ui.theme.CoffeeElevation
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.Cream100

@Composable
fun TastingNoteChip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
) {
    Surface(
        modifier = modifier.shadow(
            elevation = CoffeeElevation.sm,
            shape = RoundedCornerShape(CoffeeRadius.pill),
            clip = false
        ),
        shape = RoundedCornerShape(CoffeeRadius.pill),
        color = backgroundColor,
        border = BorderStroke(1.dp, Cream100.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .background(Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}
