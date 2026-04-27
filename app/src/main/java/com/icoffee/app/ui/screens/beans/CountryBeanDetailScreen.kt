package com.icoffee.app.ui.screens.beans

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icoffee.app.R
import com.icoffee.app.data.affiliate.AffiliateRepository
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.ui.components.AffiliateOfferCard
import com.icoffee.app.ui.components.BeanFlavorTag
import com.icoffee.app.ui.components.BeanInfoGrid
import com.icoffee.app.ui.components.BeanInfoItem
import com.icoffee.app.ui.components.BeanVarietyTab
import com.icoffee.app.ui.theme.CreamText
import com.icoffee.app.ui.theme.GoldAccent
import com.icoffee.app.ui.theme.GoldAccentLight
import com.icoffee.app.ui.theme.MutedText
import com.icoffee.app.ui.theme.SurfaceDark
import com.icoffee.app.ui.theme.SurfaceDarkAlt
import com.icoffee.app.ui.theme.SurfaceStroke
import com.icoffee.app.util.BeanOriginTextLocalizer
import com.icoffee.app.util.CountryDisplayNames
import com.icoffee.app.util.ENABLE_AFFILIATE_SECTION
import com.icoffee.app.viewmodel.BeansViewModel

@Composable
fun CountryBeanDetailScreen(
    countryId: String,
    onBack: () -> Unit,
    viewModel: BeansViewModel = viewModel()
) {
    val context = LocalContext.current
    val languageCode = AppLocaleManager.currentLanguage(context).code
    val country = viewModel.getCountry(countryId)

    if (country == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A0F0A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.beans_detail_not_found),
                color = CreamText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val safeIndex = viewModel.selectedVarietyIndex(country.id)
        .coerceIn(0, country.varieties.lastIndex)
    val variety = country.varieties[safeIndex]
    val localizedContinent = localizedContinentName(country.continent)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.coffee_highlight_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE11A0F0A),
                            Color(0xC21A0F0A),
                            Color(0xEB1A0F0A)
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
                            Color(0x28E2B888),
                            Color.Transparent
                        ),
                        radius = 960f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    SurfaceDarkAlt.copy(alpha = 0.93f),
                                    SurfaceDark.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .border(1.dp, SurfaceStroke.copy(alpha = 0.9f), RoundedCornerShape(26.dp))
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        GoldAccent.copy(alpha = 0.18f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = country.flagEmoji,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = CountryDisplayNames.localizedName(country.country, languageCode),
                                style = MaterialTheme.typography.headlineLarge,
                                color = CreamText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.beans_detail_curated_varieties,
                                country.varieties.size,
                                localizedContinent
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MutedText
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.beans_detail_varieties_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = CreamText
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(country.varieties) { index, bean ->
                            BeanVarietyTab(
                                label = bean.name,
                                selected = index == safeIndex,
                                onClick = { viewModel.selectVariety(country.id, index) }
                            )
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    SurfaceDarkAlt.copy(alpha = 0.94f),
                                    SurfaceDark.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .border(1.dp, SurfaceStroke.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x20FFE1B6),
                                        Color.Transparent,
                                        Color(0x180F0704)
                                    )
                                )
                            )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = variety.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = CreamText
                            )
                            Text(
                                text = BeanOriginTextLocalizer.localizedDescription(
                                    rawDescription = variety.description,
                                    countryName = country.country,
                                    flavorNotes = variety.flavorNotes,
                                    appLanguageCode = languageCode
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = CreamText.copy(alpha = 0.9f)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.beans_detail_flavor_notes),
                                style = MaterialTheme.typography.titleMedium,
                                color = CreamText.copy(alpha = 0.92f)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(variety.flavorNotes) { note ->
                                    BeanFlavorTag(
                                        text = BeanOriginTextLocalizer.localizedFlavorNote(
                                            rawNote = note,
                                            appLanguageCode = languageCode
                                        )
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.beans_detail_profile_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = CreamText.copy(alpha = 0.92f)
                            )
                            BeanInfoGrid(
                                items = listOf(
                                    BeanInfoItem(
                                        stringResource(R.string.beans_detail_info_processing),
                                        variety.processing ?: stringResource(R.string.beans_detail_value_not_specified)
                                    ),
                                    BeanInfoItem(
                                        stringResource(R.string.beans_detail_info_altitude),
                                        variety.altitude ?: stringResource(R.string.beans_detail_value_not_specified)
                                    ),
                                    BeanInfoItem(
                                        stringResource(R.string.beans_detail_info_roast),
                                        variety.roast ?: stringResource(R.string.beans_detail_value_not_specified)
                                    ),
                                    BeanInfoItem(
                                        stringResource(R.string.beans_detail_info_species),
                                        variety.species ?: stringResource(R.string.beans_detail_value_not_specified)
                                    )
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = GoldAccentLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.beans_detail_recommended_brewing),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CreamText.copy(alpha = 0.95f)
                                )
                            }

                            if (variety.recommendedBrewing.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(variety.recommendedBrewing) { method ->
                                        BeanFlavorTag(text = method)
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.beans_detail_no_recommendation),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedText
                                )
                            }
                        }
                    }
                }
            }

            if (ENABLE_AFFILIATE_SECTION) {
                AffiliateRepository.forCountry(country.id)?.let { offer ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(R.string.affiliate_section_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = CreamText.copy(alpha = 0.80f)
                            )
                            AffiliateOfferCard(offer = offer, darkTheme = true)
                            Text(
                                text = stringResource(R.string.affiliate_disclosure),
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText.copy(alpha = 0.60f)
                            )
                        }
                    }
                }
            }
        }
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
