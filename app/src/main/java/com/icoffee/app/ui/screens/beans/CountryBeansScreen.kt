package com.icoffee.app.ui.screens.beans

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icoffee.app.R
import com.icoffee.app.ui.components.CountryBeanCard
import com.icoffee.app.ui.components.CountryBeansSearchBar
import com.icoffee.app.ui.components.CountryFilterChip
import com.icoffee.app.ui.components.CountrySectionHeader
import com.icoffee.app.ui.theme.CreamText
import com.icoffee.app.ui.theme.MutedText
import com.icoffee.app.viewmodel.BeansViewModel

@Composable
fun CountryBeansScreen(
    onCountryClick: (String) -> Unit,
    viewModel: BeansViewModel = viewModel()
) {
    val groups = viewModel.groupedCountries

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
                            Color(0xBF1A0F0A),
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 156.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x38E2B888),
                                    Color(0x1A25130B)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x22FFDCA9),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.beans_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = CreamText
                        )
                        Text(
                            text = stringResource(R.string.beans_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MutedText
                        )
                        Text(
                            text = stringResource(R.string.beans_curated_origins, viewModel.allCountries.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = CreamText.copy(alpha = 0.76f)
                        )
                    }
                }
            }
            item {
                CountryBeansSearchBar(
                    query = viewModel.searchQuery,
                    onQueryChange = viewModel::updateQuery,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.continents) { filter ->
                        CountryFilterChip(
                            label = localizedContinentFilterLabel(filter),
                            selected = filter == viewModel.selectedContinent,
                            onClick = { viewModel.selectContinent(filter) }
                        )
                    }
                }
            }

            groups.forEach { group ->
                item {
                    CountrySectionHeader(
                        continent = group.continent,
                        countryCount = group.countries.size,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                val rows = group.countries.chunked(2)
                items(rows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { country ->
                            CountryBeanCard(
                                country = country,
                                onClick = { onCountryClick(country.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun localizedContinentFilterLabel(filter: String): String = when (filter) {
    "All" -> stringResource(R.string.beans_filter_all)
    "South & Central America" -> stringResource(R.string.continent_americas)
    "Africa" -> stringResource(R.string.continent_africa)
    "Asia & Oceania" -> stringResource(R.string.continent_asia_oceania)
    "Caribbean" -> stringResource(R.string.continent_caribbean)
    else -> filter
}
