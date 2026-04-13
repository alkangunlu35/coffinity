package com.icoffee.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icoffee.app.R

@Composable
fun PremiumRoomCard(
    title: String,
    subtitle: String,
    tag: String,
    imageRes: Int,
    joined: Boolean = false,
    isFull: Boolean = false,
    actionLabel: String? = null,
    onJoinClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val joinBackground by animateColorAsState(
        targetValue = when {
            joined -> Color(0xB3664A36)
            isFull -> Color(0xB38D7B6C)
            else -> Color(0xFFE2A66E)
        },
        animationSpec = tween(220),
        label = "premiumRoomJoinBg"
    )
    val joinTextColor by animateColorAsState(
        targetValue = if (joined || isFull) Color(0xFFF5E6D3) else Color.White,
        animationSpec = tween(220),
        label = "premiumRoomJoinText"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp)
            .padding(vertical = 6.dp)
    ) {

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x66FFFFFF),
                            Color(0x22FFFFFF)
                        )
                    )
                )
                .border(
                    1.dp,
                    Color(0x22FFFFFF),
                    RoundedCornerShape(24.dp)
                )
        )

        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color(0xFFF5E6D3),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    color = Color(0xCCF5E6D3),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .background(
                            Color(0x33FFFFFF),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tag,
                        color = Color(0xFFF5E6D3),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    joinBackground.copy(alpha = 0.88f),
                                    joinBackground
                                )
                            ),
                            RoundedCornerShape(50)
                        )
                        .clickable(
                            enabled = onJoinClick != null && (!isFull || joined),
                            interactionSource = interactionSource,
                            indication = null
                        ) { onJoinClick?.invoke() }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (joined && actionLabel == null) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = joinTextColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = actionLabel ?: when {
                                joined -> stringResource(R.string.meet_joined)
                                isFull -> stringResource(R.string.meet_full)
                                else -> stringResource(R.string.meet_join)
                            },
                            color = joinTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
