package com.icoffee.app.ui.screens.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.ui.theme.CoffeeElevation
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CoffeeSpacing

@Composable
fun MapScreen(
    featuredCoffee: Coffee,
    onViewDetails: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050D15), Color(0xFF0C1C2B), Color(0xFF120D0A))
                )
            )
            .padding(horizontal = CoffeeSpacing.lg, vertical = CoffeeSpacing.lg)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.lg)
        ) {
            Text(
                text = stringResource(R.string.map_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFFECDACA)
            )
            Text(
                text = stringResource(R.string.map_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCCB9A7)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(452.dp)
                    .shadow(CoffeeElevation.xl, RoundedCornerShape(CoffeeRadius.xl), clip = false)
                    .clip(RoundedCornerShape(CoffeeRadius.xl))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x5568C8F3), Color.Transparent)
                            )
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.coffee_dark_bg),
                    contentDescription = stringResource(R.string.map_content_desc),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x33040B11), Color(0x99040B11))
                            )
                        )
                )

                CoffeeMarker("Brazil", Modifier.align(Alignment.TopStart).offset(x = 58.dp, y = 114.dp))
                CoffeeMarker("Ethiopia", Modifier.align(Alignment.TopStart).offset(x = 228.dp, y = 132.dp))
                CoffeeMarker("Colombia", Modifier.align(Alignment.TopStart).offset(x = 110.dp, y = 222.dp))
                CoffeeMarker("Indonesia", Modifier.align(Alignment.TopStart).offset(x = 258.dp, y = 260.dp))
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(CoffeeElevation.xl, RoundedCornerShape(CoffeeRadius.xl), clip = false),
            shape = RoundedCornerShape(CoffeeRadius.xl),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF241610)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(CoffeeSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs)) {
                        Text(
                            text = stringResource(featuredCoffee.titleRes),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFFCE9D6)
                        )
                        Text(
                            text = stringResource(R.string.map_featured_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE1C4A8)
                        )
                    }
                }

                PrimaryButton(
                    text = stringResource(R.string.home_view_details),
                    onClick = { onViewDetails(featuredCoffee.id) }
                )
            }
        }
    }
}

@Composable
private fun CoffeeMarker(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(CoffeeRadius.pill))
            .background(Color(0xD6261711))
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color(0xFFFFD6A7), CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFFFE4C7)
        )
    }
}
