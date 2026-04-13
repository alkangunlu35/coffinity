package com.icoffee.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.icoffee.app.ui.theme.GoldAccentLight

@Composable
fun ContinentSilhouetteIcon(
    continent: String,
    modifier: Modifier = Modifier
) {
    val tint = when (continent) {
        "South & Central America" -> Color(0xFFD4A071)
        "Africa" -> Color(0xFFC8875D)
        "Asia & Oceania" -> Color(0xFFBE8A63)
        "Caribbean" -> Color(0xFFD7AF7F)
        else -> GoldAccentLight
    }

    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        when (continent) {
            "South & Central America" -> {
                val p = Path().apply {
                    moveTo(w * 0.45f, h * 0.04f)
                    cubicTo(w * 0.7f, h * 0.03f, w * 0.82f, h * 0.24f, w * 0.74f, h * 0.4f)
                    cubicTo(w * 0.68f, h * 0.55f, w * 0.78f, h * 0.64f, w * 0.64f, h * 0.82f)
                    cubicTo(w * 0.54f, h * 0.96f, w * 0.38f, h * 0.96f, w * 0.3f, h * 0.84f)
                    cubicTo(w * 0.18f, h * 0.66f, w * 0.22f, h * 0.5f, w * 0.28f, h * 0.38f)
                    cubicTo(w * 0.2f, h * 0.22f, w * 0.22f, h * 0.05f, w * 0.45f, h * 0.04f)
                    close()
                }
                drawPath(path = p, color = tint.copy(alpha = 0.9f))
            }
            "Africa" -> {
                val p = Path().apply {
                    moveTo(w * 0.36f, h * 0.04f)
                    cubicTo(w * 0.62f, h * 0.01f, w * 0.82f, h * 0.14f, w * 0.82f, h * 0.38f)
                    cubicTo(w * 0.84f, h * 0.55f, w * 0.74f, h * 0.66f, w * 0.62f, h * 0.82f)
                    cubicTo(w * 0.52f, h * 0.95f, w * 0.4f, h * 0.97f, w * 0.32f, h * 0.88f)
                    cubicTo(w * 0.18f, h * 0.72f, w * 0.16f, h * 0.52f, w * 0.18f, h * 0.34f)
                    cubicTo(w * 0.19f, h * 0.18f, w * 0.14f, h * 0.08f, w * 0.36f, h * 0.04f)
                    close()
                }
                drawPath(path = p, color = tint.copy(alpha = 0.9f))
            }
            "Asia & Oceania" -> {
                val p = Path().apply {
                    moveTo(w * 0.08f, h * 0.36f)
                    cubicTo(w * 0.18f, h * 0.1f, w * 0.42f, h * 0.05f, w * 0.66f, h * 0.08f)
                    cubicTo(w * 0.88f, h * 0.1f, w * 0.98f, h * 0.28f, w * 0.92f, h * 0.5f)
                    cubicTo(w * 0.86f, h * 0.72f, w * 0.66f, h * 0.86f, w * 0.44f, h * 0.88f)
                    cubicTo(w * 0.24f, h * 0.88f, w * 0.08f, h * 0.7f, w * 0.08f, h * 0.36f)
                    close()
                }
                drawPath(path = p, color = tint.copy(alpha = 0.88f))
                drawCircle(
                    color = tint.copy(alpha = 0.75f),
                    radius = w * 0.09f,
                    center = Offset(w * 0.76f, h * 0.84f)
                )
            }
            "Caribbean" -> {
                drawCircle(
                    color = tint.copy(alpha = 0.9f),
                    radius = w * 0.18f,
                    center = Offset(w * 0.3f, h * 0.45f)
                )
                drawCircle(
                    color = tint.copy(alpha = 0.8f),
                    radius = w * 0.13f,
                    center = Offset(w * 0.56f, h * 0.6f)
                )
                drawCircle(
                    color = tint.copy(alpha = 0.75f),
                    radius = w * 0.1f,
                    center = Offset(w * 0.76f, h * 0.36f)
                )
            }
            else -> {
                drawCircle(color = tint.copy(alpha = 0.8f), radius = w * 0.32f, center = center)
            }
        }
    }
}
