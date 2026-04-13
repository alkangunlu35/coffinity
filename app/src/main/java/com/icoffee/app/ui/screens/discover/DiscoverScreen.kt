package com.icoffee.app.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.icoffee.app.R
import com.icoffee.app.data.model.BeanRepository
import com.icoffee.app.data.model.Continent
import com.icoffee.app.data.model.CountryBeans

private val BgDark = Color(0xFF0D0602)
private val TextPrimary = Color(0xFFF5E6D3)
private val TextSecondary = Color(0xFF9A7A5A)
private val TextMuted = Color(0xFF5A3A1A)
private val Amber = Color(0xFFE69A3A)
private val AmberDim = Color(0xFFC9783A)
private val CardBg = Color(0xFF1A0D06)
private val CardBorder = Color(0xFF2A1A0A)
private val ChipSelected = Color(0xFFE69A3A)
private val ChipBg = Color(0x18FFFFFF)

@Composable
fun DiscoverScreen(onCountryClick: (String) -> Unit) {
    CountryOriginsScreen(onCountryClick = onCountryClick)
}

@Composable
private fun CountryOriginsScreen(onCountryClick: (String) -> Unit) {
    val allCountries = remember { BeanRepository.countries }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    val filters = listOf("All", "Americas", "Africa", "Asia & Oceania", "Caribbean")
    val continentOrder = listOf(Continent.AMERICAS, Continent.AFRICA, Continent.ASIA, Continent.CARIBBEAN)

    val filtered = remember(searchQuery, selectedFilter) {
        allCountries.filter { country ->
            val matchesFilter = selectedFilter == "All" || when (selectedFilter) {
                "Americas" -> country.continent == Continent.AMERICAS
                "Africa" -> country.continent == Continent.AFRICA
                "Asia & Oceania" -> country.continent == Continent.ASIA
                "Caribbean" -> country.continent == Continent.CARIBBEAN
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                country.name.contains(searchQuery, ignoreCase = true) ||
                country.varieties.any { it.name.contains(searchQuery, ignoreCase = true) }
            matchesFilter && matchesSearch
        }
    }

    val grouped = continentOrder.mapNotNull { continent ->
        val countries = filtered.filter { it.continent == continent }
        if (countries.isNotEmpty()) continent to countries else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x12E69A3A), Color.Transparent),
                        center = Offset(200f, 300f),
                        radius = 800f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nav_home).uppercase(),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                            color = AmberDim
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.beans_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.beans_subtitle),
                        style = TextStyle(fontSize = 14.sp, color = TextSecondary)
                    )
                }
            }

            // Search bar
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A0D06))
                        .border(1.dp, Color(0xFF2A1A0A), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                            cursorBrush = SolidColor(Amber),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.beans_search_placeholder),
                                        style = TextStyle(fontSize = 14.sp, color = TextMuted)
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Filter chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        val selected = filter == selectedFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (selected) Amber else ChipBg)
                                .border(
                                    1.dp,
                                    if (selected) Amber else Color(0x28FFFFFF),
                                    RoundedCornerShape(50.dp)
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { selectedFilter = filter }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = localizedFilterLabel(filter),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) Color(0xFF0D0602) else TextSecondary
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Continent sections
            grouped.forEach { (continent, countries) ->
                item {
                    ContinentHeader(continent = continent)
                }
                val pairs = countries.chunked(2)
                items(pairs) { pair ->
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { country ->
                            CountryCard(
                                country = country,
                                modifier = Modifier.weight(1f),
                                onClick = { onCountryClick(country.id) }
                            )
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun ContinentHeader(continent: Continent) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Continent silhouette shape (placeholder — refined later)
        Canvas(modifier = Modifier.size(36.dp)) {
            drawContinentShape(continent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = localizedContinentLabel(continent),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Text(
                text = stringResource(
                    R.string.discover_continent_summary,
                    continent.countryCount,
                    continent.varietyCount
                ),
                style = TextStyle(fontSize = 12.sp, color = TextMuted)
            )
        }
        // Divider line
        Box(
            modifier = Modifier
                .height(1.dp)
                .width(40.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0x40E69A3A), Color.Transparent)
                    )
                )
        )
    }
}

private fun DrawScope.drawContinentShape(continent: Continent) {
    val w = size.width
    val h = size.height
    val paint = androidx.compose.ui.graphics.Paint().apply {
        color = when (continent) {
            Continent.AMERICAS -> Color(0xFF4DB868)
            Continent.AFRICA -> Color(0xFFE86050)
            Continent.ASIA -> Color(0xFF5A9FF0)
            Continent.CARIBBEAN -> Color(0xFFE69A3A)
        }
        alpha = 0.75f
    }
    val color = when (continent) {
        Continent.AMERICAS -> Color(0xFF4DB868).copy(alpha = 0.75f)
        Continent.AFRICA -> Color(0xFFE86050).copy(alpha = 0.75f)
        Continent.ASIA -> Color(0xFF5A9FF0).copy(alpha = 0.75f)
        Continent.CARIBBEAN -> Color(0xFFE69A3A).copy(alpha = 0.75f)
    }
    when (continent) {
        Continent.AMERICAS -> {
            // Vertical teardrop shape
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.45f, h * 0.04f)
                cubicTo(w * 0.72f, h * 0.04f, w * 0.82f, h * 0.22f, w * 0.75f, h * 0.4f)
                cubicTo(w * 0.68f, h * 0.55f, w * 0.78f, h * 0.65f, w * 0.65f, h * 0.82f)
                cubicTo(w * 0.55f, h * 0.96f, w * 0.38f, h * 0.98f, w * 0.3f, h * 0.85f)
                cubicTo(w * 0.18f, h * 0.68f, w * 0.22f, h * 0.52f, w * 0.28f, h * 0.38f)
                cubicTo(w * 0.2f, h * 0.22f, w * 0.22f, h * 0.04f, w * 0.45f, h * 0.04f)
                close()
            }
            drawPath(path = path, color = color)
        }
        Continent.AFRICA -> {
            // Rounded rectangular blob
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.35f, h * 0.05f)
                cubicTo(w * 0.62f, h * 0.02f, w * 0.82f, h * 0.14f, w * 0.82f, h * 0.38f)
                cubicTo(w * 0.84f, h * 0.55f, w * 0.74f, h * 0.66f, w * 0.62f, h * 0.82f)
                cubicTo(w * 0.52f, h * 0.96f, w * 0.4f, h * 0.98f, w * 0.32f, h * 0.88f)
                cubicTo(w * 0.18f, h * 0.72f, w * 0.16f, h * 0.52f, w * 0.18f, h * 0.35f)
                cubicTo(w * 0.18f, h * 0.18f, w * 0.14f, h * 0.08f, w * 0.35f, h * 0.05f)
                close()
            }
            drawPath(path = path, color = color)
            // Horn of Africa
            val horn = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.82f, h * 0.38f)
                lineTo(w * 0.95f, h * 0.46f)
                lineTo(w * 0.86f, h * 0.56f)
                lineTo(w * 0.82f, h * 0.52f)
                close()
            }
            drawPath(path = horn, color = color.copy(alpha = 0.5f))
        }
        Continent.ASIA -> {
            // Wide horizontal blob
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.08f, h * 0.35f)
                cubicTo(w * 0.18f, h * 0.1f, w * 0.42f, h * 0.05f, w * 0.66f, h * 0.08f)
                cubicTo(w * 0.88f, h * 0.1f, w * 0.98f, h * 0.28f, w * 0.92f, h * 0.5f)
                cubicTo(w * 0.86f, h * 0.72f, w * 0.66f, h * 0.86f, w * 0.44f, h * 0.88f)
                cubicTo(w * 0.24f, h * 0.9f, w * 0.08f, h * 0.76f, w * 0.06f, h * 0.55f)
                cubicTo(w * 0.04f, h * 0.42f, w * 0.06f, h * 0.38f, w * 0.08f, h * 0.35f)
                close()
            }
            drawPath(path = path, color = color)
            // India peninsula
            val india = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.5f, h * 0.88f)
                lineTo(w * 0.55f, h * 0.98f)
                lineTo(w * 0.6f, h * 0.9f)
                close()
            }
            drawPath(path = india, color = color.copy(alpha = 0.5f))
        }
        Continent.CARIBBEAN -> {
            // Small scattered islands
            drawCircle(color = color, radius = w * 0.12f, center = Offset(w * 0.25f, h * 0.45f))
            drawCircle(color = color.copy(alpha = 0.65f), radius = w * 0.10f, center = Offset(w * 0.52f, h * 0.35f))
            drawCircle(color = color.copy(alpha = 0.55f), radius = w * 0.08f, center = Offset(w * 0.75f, h * 0.52f))
            drawCircle(color = color.copy(alpha = 0.4f), radius = w * 0.05f, center = Offset(w * 0.42f, h * 0.65f))
            drawCircle(color = color.copy(alpha = 0.35f), radius = w * 0.04f, center = Offset(w * 0.65f, h * 0.7f))
        }
    }
}

@Composable
private fun CountryCard(
    country: CountryBeans,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val continentColor = when (country.continent) {
        Continent.AMERICAS -> Color(0xFF2A6B3A)
        Continent.AFRICA -> Color(0xFF6B2A20)
        Continent.ASIA -> Color(0xFF1A3A6B)
        Continent.CARIBBEAN -> Color(0xFF6B4A10)
    }
    val continentLabel = when (country.continent) {
        Continent.AMERICAS -> stringResource(R.string.continent_americas)
        Continent.AFRICA -> stringResource(R.string.continent_africa)
        Continent.ASIA -> stringResource(R.string.continent_asia_oceania)
        Continent.CARIBBEAN -> stringResource(R.string.continent_caribbean)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = country.flag, fontSize = 30.sp)
            Text(
                text = country.name,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Text(
                text = stringResource(R.string.beans_card_varieties_count, country.varieties.size),
                style = TextStyle(fontSize = 12.sp, color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(continentColor.copy(alpha = 0.3f))
                    .border(1.dp, continentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = continentLabel,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (country.continent) {
                            Continent.AMERICAS -> Color(0xFF6DB87A)
                            Continent.AFRICA -> Color(0xFFE87060)
                            Continent.ASIA -> Color(0xFF6AAAF0)
                            Continent.CARIBBEAN -> Color(0xFFE69A3A)
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun localizedFilterLabel(filter: String): String = when (filter) {
    "All" -> stringResource(R.string.beans_filter_all)
    "Americas" -> stringResource(R.string.continent_americas)
    "Africa" -> stringResource(R.string.continent_africa)
    "Asia & Oceania" -> stringResource(R.string.continent_asia_oceania)
    "Caribbean" -> stringResource(R.string.continent_caribbean)
    else -> filter
}

@Composable
private fun localizedContinentLabel(continent: Continent): String = when (continent) {
    Continent.AMERICAS -> stringResource(R.string.continent_americas)
    Continent.AFRICA -> stringResource(R.string.continent_africa)
    Continent.ASIA -> stringResource(R.string.continent_asia_oceania)
    Continent.CARIBBEAN -> stringResource(R.string.continent_caribbean)
}
