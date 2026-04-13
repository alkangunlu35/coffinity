package com.icoffee.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.ui.theme.CreamText
import com.icoffee.app.ui.theme.GoldAccentLight
import com.icoffee.app.ui.theme.MutedText
import com.icoffee.app.ui.theme.SurfaceStroke

@Composable
fun CountrySectionHeader(
    continent: String,
    countryCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ContinentSilhouetteIcon(continent = continent)
        Text(
            text = localizedContinentName(continent),
            style = MaterialTheme.typography.titleLarge,
            color = CreamText
        )
        Box(
            modifier = Modifier
                .widthIn(min = 88.dp)
                .background(Color(0x2A1A0F0A), RoundedCornerShape(999.dp))
                .border(1.dp, SurfaceStroke.copy(alpha = 0.9f), RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = pluralStringResource(
                    id = R.plurals.beans_country_count,
                    count = countryCount,
                    countryCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(GoldAccentLight.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun localizedContinentName(continent: String): String = when (continent) {
    "South & Central America" -> stringResource(R.string.continent_americas)
    "Africa" -> stringResource(R.string.continent_africa)
    "Asia & Oceania" -> stringResource(R.string.continent_asia_oceania)
    "Caribbean" -> stringResource(R.string.continent_caribbean)
    else -> continent
}
