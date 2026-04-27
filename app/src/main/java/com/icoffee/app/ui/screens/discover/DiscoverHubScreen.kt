package com.icoffee.app.ui.screens.discover

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.data.CountryBeansRepository
import com.icoffee.app.data.PhaseOneRepository
import com.icoffee.app.data.model.BrewingMethod
import com.icoffee.app.data.model.beans.CountryBeans
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.ui.brewing.brewingCategorySubtitleRes
import com.icoffee.app.ui.brewing.brewingCategoryTitleRes
import com.icoffee.app.ui.brewing.localizedBrewingMethod
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.util.CountryDisplayNames

private enum class DiscoverHubCategory {
    BREW_METHODS,
    ORIGINS
}

@Composable
fun DiscoverHubScreen(
    onBrewingMethodClick: (String) -> Unit,
    onCountryClick: (String) -> Unit,
    onOpenAllOrigins: () -> Unit
) {
    var selectedCategory by rememberSaveable { mutableStateOf(DiscoverHubCategory.BREW_METHODS.name) }
    val activeCategory = DiscoverHubCategory.valueOf(selectedCategory)
    val brewingMethodGroups = remember { PhaseOneRepository.brewingMethodsByCategory() }
    val originCountries = remember { CountryBeansRepository.getAllCountries().take(10) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.coffinity_bg),
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
                            Color(0xD91A0F0A),
                            Color(0xC626150F),
                            Color(0xE61A0F0A)
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 156.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.discover_hub_title),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF8E6D1)
                    )
                    Text(
                        text = stringResource(R.string.discover_hub_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xE6D9BA9A)
                    )
                }
            }

            item {
                DiscoverHubSelector(
                    selected = activeCategory,
                    onSelect = { category -> selectedCategory = category.name }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.discover_hub_future_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xBFD8B391)
                )
            }

            when (activeCategory) {
                DiscoverHubCategory.BREW_METHODS -> {
                    item {
                        Text(
                            text = stringResource(R.string.discover_hub_brew_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE7CDAF)
                        )
                    }

                    brewingMethodGroups.forEach { (category, methods) ->
                        item {
                            BrewingCategoryHeader(
                                title = stringResource(brewingCategoryTitleRes(category)),
                                subtitle = stringResource(brewingCategorySubtitleRes(category))
                            )
                        }

                        items(methods, key = { it.id }) { method ->
                            val localizedMethod = localizedBrewingMethod(method)
                            BrewMethodHubCard(
                                method = localizedMethod,
                                onClick = { onBrewingMethodClick(localizedMethod.id) }
                            )
                        }
                    }
                }

                DiscoverHubCategory.ORIGINS -> {
                    item {
                        Text(
                            text = stringResource(R.string.discover_hub_origins_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE7CDAF)
                        )
                    }

                    items(originCountries, key = { it.id }) { country ->
                        OriginHubCard(
                            country = country,
                            onClick = { onCountryClick(country.id) }
                        )
                    }

                    item {
                        PrimaryButton(
                            text = stringResource(R.string.discover_hub_open_all_origins),
                            subtitle = stringResource(R.string.discover_hub_open_all_origins_subtitle),
                            onClick = onOpenAllOrigins,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverHubSelector(
    selected: DiscoverHubCategory,
    onSelect: (DiscoverHubCategory) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x2E523423))
            .border(1.dp, Color(0x45E5C49D), RoundedCornerShape(18.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiscoverHubSelectorItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.discover_hub_category_brew_methods),
                subtitle = stringResource(R.string.discover_hub_category_brew_methods_subtitle),
                selected = selected == DiscoverHubCategory.BREW_METHODS,
                onClick = { onSelect(DiscoverHubCategory.BREW_METHODS) }
            )
            DiscoverHubSelectorItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.discover_hub_category_origins),
                subtitle = stringResource(R.string.discover_hub_category_origins_subtitle),
                selected = selected == DiscoverHubCategory.ORIGINS,
                onClick = { onSelect(DiscoverHubCategory.ORIGINS) }
            )
        }
    }
}

@Composable
private fun DiscoverHubSelectorItem(
    modifier: Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) {
        Brush.horizontalGradient(listOf(Color(0xE6E2B888), Color(0xE6C58B5A)))
    } else {
        Brush.horizontalGradient(listOf(Color(0x24361E12), Color(0x24361E12)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .border(
                width = 1.dp,
                color = if (selected) Color(0x66FFF0DD) else Color(0x2BF8E6D1),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) Color(0xFF2C190F) else Color(0xFFF8E6D1)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color(0xD62C190F) else Color(0xBFD5B18D)
            )
        }
    }
}

@Composable
private fun BrewMethodHubCard(
    method: BrewingMethod,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xA33A2419))
            .border(1.dp, Color(0x46E5C49D), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = method.imageRes),
                contentDescription = method.title,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = method.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF8E6D1)
                )
                Text(
                    text = method.cardSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD5B18D)
                )
                Text(
                    text = method.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xBFEAD6BF),
                    maxLines = 2
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFFE5C49D)
            )
        }
    }
}

@Composable
private fun BrewingCategoryHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF5DFC8)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xC9D8B79A)
        )
    }
}

@Composable
private fun OriginHubCard(
    country: CountryBeans,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val languageCode = AppLocaleManager.currentLanguage(context).code
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x96352318))
            .border(1.dp, Color(0x42E5C49D), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33231911)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = country.flagEmoji,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = CountryDisplayNames.localizedName(country.country, languageCode),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF8E6D1)
                )
                Text(
                    text = localizedOriginRegionLabel(country.continent),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD5B18D)
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.beans_card_varieties_count, country.varieties.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFEBD9C4)
                )
                Icon(
                    imageVector = Icons.Default.LocalCafe,
                    contentDescription = null,
                    tint = Color(0xFFE5C49D),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun localizedOriginRegionLabel(region: String): String = when (region) {
    "South & Central America" -> stringResource(R.string.continent_americas)
    "Africa" -> stringResource(R.string.continent_africa)
    "Asia & Oceania" -> stringResource(R.string.continent_asia_oceania)
    "Caribbean" -> stringResource(R.string.continent_caribbean)
    else -> region
}
