package com.icoffee.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.icoffee.app.ui.theme.CreamText
import com.icoffee.app.ui.theme.GoldAccent
import com.icoffee.app.ui.theme.GoldAccentLight
import com.icoffee.app.ui.theme.MutedText
import com.icoffee.app.ui.theme.SurfaceDarkAlt
import com.icoffee.app.ui.theme.SurfaceStroke

@Composable
fun BeanVarietyTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.985f,
                pressedAlpha = 0.96f
            )
            .shadow(
                elevation = if (selected) 7.dp else 2.dp,
                shape = RoundedCornerShape(999.dp),
                clip = false,
                ambientColor = Color(0x330E0704),
                spotColor = Color(0x330E0704)
            )
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) {
                    Brush.horizontalGradient(
                        listOf(
                            GoldAccentLight,
                            GoldAccent,
                            Color(0xFFB17A48)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            SurfaceDarkAlt.copy(alpha = 0.92f),
                            Color(0xFF3C2419).copy(alpha = 0.86f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0x7DFFE2C7) else SurfaceStroke,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 15.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color(0xFF2E1C12) else CreamText.copy(alpha = 0.86f)
        )
    }
}
