package com.icoffee.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.data.model.Coffee
import com.icoffee.app.ui.theme.Caramel400
import com.icoffee.app.ui.theme.CoffeeElevation
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CreamText
import com.icoffee.app.ui.theme.GoldAccent
import com.icoffee.app.ui.theme.GoldAccentLight
import com.icoffee.app.ui.theme.SoftText
import com.icoffee.app.ui.theme.SurfaceDark
import com.icoffee.app.ui.theme.SurfaceDarkAlt
import com.icoffee.app.ui.theme.SurfaceDarkRaised

@Composable
fun CoffeeProductCard(
    coffee: Coffee,
    onAdd: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = CoffeeElevation.lg,
                shape = RoundedCornerShape(CoffeeRadius.xl),
                clip = false
            ),
        shape = RoundedCornerShape(CoffeeRadius.xl),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkRaised),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onOpen
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box {
                Image(
                    painter = painterResource(id = coffee.imageRes),
                    contentDescription = stringResource(coffee.titleRes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xB3000000))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(CoffeeRadius.pill))
                        .background(Color(0xD9221712))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = String.format("%.1f", coffee.rating),
                        style = MaterialTheme.typography.labelLarge,
                        color = CreamText
                    )
                }
            }

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(coffee.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = CreamText
                )
                Text(
                    text = stringResource(coffee.subtitleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftText
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(coffee.typeRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = SoftText
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(6.dp, CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(GoldAccentLight, Caramel400, SurfaceDarkAlt)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onAdd,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = stringResource(R.string.home_view_details),
                                tint = SurfaceDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
