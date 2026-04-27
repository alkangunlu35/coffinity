package com.icoffee.app.ui.screens.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import java.util.Locale
import com.icoffee.app.data.model.Coffee
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.ui.components.TastingNoteChip
import com.icoffee.app.ui.theme.Caramel500
import com.icoffee.app.ui.theme.CoffeeElevation
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CoffeeSpacing
import com.icoffee.app.ui.theme.Espresso700
import com.icoffee.app.ui.theme.Espresso900
import com.icoffee.app.util.BeanOriginTextLocalizer
import com.icoffee.app.util.CountryDisplayNames

@Composable
fun CoffeeDetailScreen(
    coffee: Coffee,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val languageCode = AppLocaleManager.currentLanguage(context).code
    val coffeeNotes = stringArrayResource(coffee.notesRes)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFFBF6), Color(0xFFF4E8DB))
                )
            ),
        contentPadding = PaddingValues(bottom = CoffeeSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.lg)
    ) {
        item {
            Box {
                Image(
                    painter = painterResource(id = coffee.imageRes),
                    contentDescription = stringResource(coffee.titleRes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x00000000), Color(0x00000000), Color(0xBB160D08))
                            )
                        )
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = CoffeeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs)
            ) {
                Text(
                    text = stringResource(coffee.titleRes),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF2E1F14)
                )
                Text(
                    text = stringResource(coffee.subtitleRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF7A5C48)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = CoffeeSpacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Caramel500
                    )
                    Text(
                        text = stringResource(
                            R.string.coffee_detail_rating_reviews,
                            String.format(Locale.getDefault(), "%.1f", coffee.rating),
                            coffee.reviewCount
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF5C3D28)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CoffeeSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs)
            ) {
                coffeeNotes.forEach { note ->
                    TastingNoteChip(
                        text = BeanOriginTextLocalizer.localizedFlavorNote(
                            rawNote = note,
                            appLanguageCode = languageCode
                        )
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CoffeeSpacing.lg)
                    .shadow(CoffeeElevation.md, RoundedCornerShape(CoffeeRadius.xl), ambientColor = Color(0x221A120C), spotColor = Color(0x2B1A120C), clip = false),
                shape = RoundedCornerShape(CoffeeRadius.xl),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2E4D3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(CoffeeSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.md)
                ) {
                    Text(
                        text = stringResource(R.string.coffee_detail_section_bean_details),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF4C3220)
                    )
                    InfoRow(
                        label = stringResource(R.string.coffee_detail_label_origin),
                        value = CountryDisplayNames.localizedName(coffee.origin, languageCode)
                    )
                    InfoRow(label = stringResource(R.string.coffee_detail_label_roast), value = coffee.roast)
                    InfoRow(label = stringResource(R.string.coffee_detail_label_altitude), value = coffee.altitude)
                }
            }
        }

        item {
            Text(
                text = stringResource(coffee.descriptionRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CoffeeSpacing.lg)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CoffeeSpacing.lg)
                    .shadow(CoffeeElevation.md, RoundedCornerShape(CoffeeRadius.xl), ambientColor = Color(0x221A120C), spotColor = Color(0x2B1A120C), clip = false),
                shape = RoundedCornerShape(CoffeeRadius.xl),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF0E2)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(CoffeeSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.coffee_detail_section_cup_profile),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Espresso700
                    )
                    Text(
                        text = stringResource(coffee.typeRes),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFFA5784F)
                    )
                    Text(
                        text = stringResource(
                            R.string.coffee_detail_cup_profile_body,
                            coffeeNotes.firstOrNull() ?: stringResource(R.string.coffee_detail_balanced_fallback)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6F5545)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF7A5C48)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF3A2416)
        )
    }
}
