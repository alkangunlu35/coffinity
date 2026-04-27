package com.icoffee.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.icoffee.app.R
import com.icoffee.app.navigation.AppRoute
import com.icoffee.app.ui.theme.CreamText
import com.icoffee.app.ui.theme.GoldAccent
import com.icoffee.app.ui.theme.GoldAccentLight
import com.icoffee.app.ui.theme.MeetBadge
import com.icoffee.app.ui.theme.SoftText
import com.icoffee.app.ui.theme.SurfaceDark
import com.icoffee.app.ui.theme.SurfaceDarkAlt
import com.icoffee.app.ui.theme.SurfaceStroke

data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(AppRoute.Home.route, R.string.nav_home, Icons.Default.Explore),
    BottomNavItem(AppRoute.Brand.route, R.string.nav_brand, Icons.Default.Storefront),
    BottomNavItem(AppRoute.Meet.route, R.string.nav_meet, Icons.Default.Groups),
    BottomNavItem(AppRoute.Scan.route, R.string.nav_scan, Icons.Default.QrCodeScanner),
    BottomNavItem(AppRoute.Profile.route, R.string.nav_profile, Icons.Default.Person)
)

@Composable
fun AppBottomBar(
    currentDestination: NavDestination?,
    nearbyMeetCount: Int,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(30.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp)
            .shadow(
                elevation = 16.dp,
                shape = barShape,
                clip = false,
                ambientColor = Color(0x4ADB9F66),
                spotColor = Color(0x402A170F)
            )
            .clip(barShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceDarkAlt.copy(alpha = 0.90f),
                        SurfaceDark.copy(alpha = 0.94f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x52FFFFFF),
                        Color(0x66D8A16A),
                        SurfaceStroke.copy(alpha = 0.62f)
                    )
                ),
                shape = barShape
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp, start = 14.dp, end = 14.dp)
                .heightIn(min = 1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x4DD8A16A),
                            Color.Transparent
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val label = stringResource(item.labelRes)
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                val badgeVisible = item.route == AppRoute.Meet.route && nearbyMeetCount > 0
                val interactionSource = remember { MutableInteractionSource() }

                val iconColor by animateColorAsState(
                    targetValue = if (selected) Color(0xFF24160E) else CreamText.copy(alpha = 0.86f),
                    animationSpec = tween(220),
                    label = "bottomIconColor"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (selected) Color(0xFF24160E) else SoftText.copy(alpha = 0.86f),
                    animationSpec = tween(220),
                    label = "bottomLabelColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .coffinityPressMotion(
                                interactionSource = interactionSource,
                                pressedScale = 0.97f,
                                pressedAlpha = 0.96f
                            )
                            .shadow(
                                elevation = if (selected) 8.dp else 2.dp,
                                shape = RoundedCornerShape(16.dp),
                                clip = false,
                                ambientColor = GoldAccentLight.copy(alpha = if (selected) 0.28f else 0.06f),
                                spotColor = GoldAccentLight.copy(alpha = if (selected) 0.26f else 0.04f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) {
                                    Brush.horizontalGradient(
                                        listOf(
                                            GoldAccentLight.copy(alpha = 0.96f),
                                            GoldAccent.copy(alpha = 0.98f)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0x1CF6E7D4),
                                            Color(0x0F1A100B)
                                        )
                                    )
                                }
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    if (selected) {
                                        listOf(
                                            Color(0x88FFF4E2),
                                            Color(0x66D8A16A)
                                        )
                                    } else {
                                        listOf(
                                            Color(0x2EFFFFFF),
                                            Color(0x26D8A16A)
                                        )
                                    }
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onNavigate(item.route) }
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (badgeVisible) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MeetBadge,
                                            contentColor = Color(0xFF2A1A10)
                                        ) {
                                            Text(
                                                text = nearbyMeetCount.coerceAtMost(9).toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = label,
                                        tint = iconColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = label,
                                    tint = iconColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = labelColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
