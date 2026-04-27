package com.icoffee.app.ui.screens.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icoffee.app.R

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val constraintsPx = constraints
        val compactPhone = maxHeight < 700.dp
        val tallPhone = maxHeight >= 860.dp
        val safeHeight = maxHeight

        val sidePadding = when {
            compactPhone -> 22.dp
            tallPhone -> 32.dp
            else -> 28.dp
        }
        val brandTopPadding = when {
            compactPhone -> (safeHeight * 0.036f).coerceIn(18.dp, 24.dp)
            tallPhone -> (safeHeight * 0.045f).coerceIn(34.dp, 46.dp)
            else -> (safeHeight * 0.041f).coerceIn(26.dp, 34.dp)
        }
        val dividerGap = when {
            compactPhone -> 9.dp
            tallPhone -> 13.dp
            else -> 11.dp
        }
        val titleTopPadding = when {
            compactPhone -> 18.dp
            tallPhone -> 30.dp
            else -> 24.dp
        }
        val subtitleGap = when {
            compactPhone -> 10.dp
            tallPhone -> 14.dp
            else -> 12.dp
        }
        val ctaBottomPadding = when {
            compactPhone -> 10.dp
            tallPhone -> 24.dp
            else -> 16.dp
        }
        val contentToCtaGap = when {
            compactPhone -> 10.dp
            tallPhone -> 22.dp
            else -> 14.dp
        }
        val ctaShape = RoundedCornerShape(34.dp)
        val ctaHeight = if (compactPhone) 58.dp else 64.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.coffinity_bg),
                contentDescription = stringResource(R.string.welcome_hero_content_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x06000000),
                                Color(0x14050302),
                                Color(0x28080403),
                                Color(0x420B0503)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x3ADFA064),
                                Color(0x17CB8344),
                                Color.Transparent
                            ),
                            center = Offset(
                                x = constraintsPx.maxWidth * 0.5f,
                                y = constraintsPx.maxHeight * 0.7f
                            ),
                            radius = constraintsPx.maxHeight * 0.64f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x24E9AA69),
                                Color.Transparent
                            ),
                            center = Offset(
                                x = constraintsPx.maxWidth * 0.22f,
                                y = constraintsPx.maxHeight * 0.2f
                            ),
                            radius = constraintsPx.maxWidth * 0.52f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = sidePadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(brandTopPadding))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dividerGap)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = if (compactPhone) 42.sp else 46.sp,
                                lineHeight = if (compactPhone) 46.sp else 50.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.sp,
                                color = Color(0xFFF8EBD9),
                                shadow = Shadow(
                                    color = Color(0x59000000),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 8f
                                )
                            )
                        )

                        DividerBean(width = if (compactPhone) 62.dp else 72.dp)
                    }

                    Spacer(modifier = Modifier.height(titleTopPadding))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(subtitleGap)
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_title),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = if (compactPhone) 35.sp else 38.sp,
                                lineHeight = if (compactPhone) 40.sp else 44.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF8ECDD),
                                shadow = Shadow(
                                    color = Color(0x7A000000),
                                    offset = Offset(0f, 4f),
                                    blurRadius = 12f
                                )
                            ),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )

                        Text(
                            text = stringResource(R.string.welcome_subtitle),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = if (compactPhone) 13.sp else 14.sp,
                                lineHeight = if (compactPhone) 20.sp else 22.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xE8E8D5C0)
                            ),
                            modifier = Modifier.fillMaxWidth(if (compactPhone) 0.9f else 0.82f)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(contentToCtaGap))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ctaHeight)
                        .shadow(
                            elevation = 18.dp,
                            shape = ctaShape,
                            ambientColor = Color(0x55B36A2F),
                            spotColor = Color(0x73000000)
                        )
                        .clip(ctaShape)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFFD78B3F),
                                    Color(0xFFB96823),
                                    Color(0xFF8F4611)
                                )
                            ),
                            shape = ctaShape
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0x99F1C48E),
                            shape = ctaShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onStartClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.TopCenter)
                            .background(Color(0x5CEFC38E))
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.46f)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x38FFF0D8),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    Text(
                        text = stringResource(R.string.welcome_cta),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF8EAD7),
                                letterSpacing = 0.1.sp
                            )
                        )
                        Text(
                            text = "›",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF8EAD7)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ctaBottomPadding))
            }
        }
    }
}

@Composable
private fun DividerBean(width: Dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(1.dp)
                .background(Color(0x86D7B28A))
        )
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(9.dp)
                .graphicsLayer { rotationZ = -18f }
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFE0B481),
                            Color(0xFFC68443)
                        )
                    )
                )
                .border(
                    width = 0.8.dp,
                    color = Color(0x76F0CAA0),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.72f)
                    .background(Color(0xA2F6D8B0), RoundedCornerShape(20.dp))
            )
        }
        Box(
            modifier = Modifier
                .width(width)
                .height(1.dp)
                .background(Color(0x86D7B28A))
        )
    }
}
