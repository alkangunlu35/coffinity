package com.icoffee.app.ui.screens.scan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.icoffee.app.R
import com.icoffee.app.data.model.AcidityLevel
import com.icoffee.app.data.model.CoffeeMatchResult
import com.icoffee.app.data.model.CoffeeProfile
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.OpenFoodFactsProduct
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.StrengthLevel
import com.icoffee.app.data.model.TasteNote
import com.icoffee.app.data.model.TasteInsightSignal
import com.icoffee.app.data.model.TasteInsightState
import com.icoffee.app.data.model.UserTasteSummary
import com.icoffee.app.data.affiliate.AffiliateRepository
import com.icoffee.app.ui.components.AffiliateOfferCard
import com.icoffee.app.ui.components.PrimaryCoffeeButton
import com.icoffee.app.ui.theme.CoffeeElevation
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CoffeeSpacing
import com.icoffee.app.ui.theme.CreamText
import com.icoffee.app.ui.theme.GoldAccent
import com.icoffee.app.ui.theme.MutedText
import com.icoffee.app.ui.theme.SurfaceDark
import com.icoffee.app.ui.theme.SurfaceDarkAlt
import com.icoffee.app.ui.theme.SurfaceStroke
import com.icoffee.app.data.profile.TasteReaction
import com.icoffee.app.viewmodel.ScanResultUiState
import com.icoffee.app.viewmodel.ScanResultViewModel
import org.json.JSONObject
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanResultScreen(
    code: String,
    onBack: () -> Unit,
    onScanAgain: () -> Unit
) {
    val viewModel: ScanResultViewModel = viewModel()
    val uiState = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(code) {
        viewModel.lookup(code)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF20120D),
                        Color(0xFF2E1B14),
                        Color(0xFF3A2419)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = CoffeeSpacing.md,
                end = CoffeeSpacing.md,
                top = CoffeeSpacing.md,
                bottom = CoffeeSpacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.md)
        ) {
            item { ResultHeader(onBack = onBack) }

            when (val state = uiState) {
                ScanResultUiState.Loading -> {
                    item { LoadingStateCard() }
                }

                is ScanResultUiState.Found -> {
                    val product = state.product
                    val profile = state.profile
                    val matchResult = state.matchResult
                    val summary = state.profileSummary
                    val tasteNotes = collectTasteNotes(context, profile, product)

                    item { ProductHero(product = product) }
                    item {
                        CoffeeFindingsSection(
                            code = code,
                            product = product,
                            profile = profile,
                            notes = tasteNotes
                        )
                    }
                    item {
                        ScanInteractionActions(
                            isFavorited = state.isFavorited,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onReactionSelected = viewModel::submitQuickReaction
                        )
                    }
                    item { MatchSection(matchResult = matchResult) }
                    AffiliateRepository.forCoffeeType(profile.coffeeType)?.let { offer ->
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
                                    color = MutedText.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                    item { TasteLearningSnapshot(summary = summary) }
                    item { CoffeeProfileMeta(profile = profile) }
                    product.ingredientsText?.takeIf { it.isNotBlank() }?.let { ingredients ->
                        item { IngredientsSection(ingredients = ingredients) }
                    }
                    if (profile.confidenceScore < 55) {
                        item { LowConfidenceCard() }
                    }
                    item { BarcodeReference(code = code) }
                    item {
                        PrimaryCoffeeButton(
                            text = stringResource(R.string.scan_action_scan_again),
                            subtitle = stringResource(R.string.scan_action_scan_again_subtitle),
                            onClick = onScanAgain
                        )
                    }
                }

                ScanResultUiState.NotFound -> {
                    item { NotFoundCard(code = code) }
                    item {
                        PrimaryCoffeeButton(
                            text = stringResource(R.string.scan_action_scan_again),
                            subtitle = stringResource(R.string.scan_empty_action_subtitle),
                            onClick = onScanAgain
                        )
                    }
                }

                is ScanResultUiState.Error -> {
                    item {
                        ErrorCard(
                            onRetry = viewModel::retry
                        )
                    }
                    item {
                        PrimaryCoffeeButton(
                            text = stringResource(R.string.scan_action_scan_again),
                            subtitle = stringResource(R.string.scan_empty_action_subtitle),
                            onClick = onScanAgain
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xC2342118))
                .border(1.dp, Color(0x35F5E6D3), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.scan_back),
                tint = Color(0xFFF5E6D3)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.scan_result_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = CreamText
            )
            Text(
                text = stringResource(R.string.scan_result_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
        }
    }
}

@Composable
private fun LoadingStateCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.lg))
            .background(Color(0xB0281912))
            .border(1.dp, Color(0x2CF5E6D3), RoundedCornerShape(CoffeeRadius.lg))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = GoldAccent,
            trackColor = Color(0x55311D14)
        )
        Text(
            text = stringResource(R.string.scan_loading_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF8E8D4)
        )
        Text(
            text = stringResource(R.string.scan_loading_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD8BEA2)
        )
    }
}

@Composable
private fun ProductHero(product: OpenFoodFactsProduct) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .shadow(
                elevation = CoffeeElevation.lg,
                shape = RoundedCornerShape(CoffeeRadius.lg),
                clip = false
            )
            .clip(RoundedCornerShape(CoffeeRadius.lg))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceDarkAlt.copy(alpha = 0.93f),
                        SurfaceDark.copy(alpha = 0.96f)
                    )
                )
            )
            .border(1.dp, SurfaceStroke.copy(alpha = 0.62f), RoundedCornerShape(CoffeeRadius.lg))
    ) {
        Image(
            painter = painterResource(id = R.drawable.coffee_dark_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.35f
        )

        if (!product.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.coffee_bag),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(148.dp),
                contentScale = ContentScale.Fit,
                alpha = 0.94f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x33140B07),
                            Color(0x66120B07),
                            Color(0xD10A0604)
                        )
                    )
                )
        )

        Text(
            text = stringResource(R.string.scan_product_identified),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFFDF7EF),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xB92E1B13))
                .border(1.dp, Color(0x4AE2B888), RoundedCornerShape(999.dp))
                .padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CoffeeFindingsSection(
    code: String,
    product: OpenFoodFactsProduct,
    profile: CoffeeProfile,
    notes: List<String>
) {
    val context = LocalContext.current
    val initialFindings = remember(code, product, profile, notes) {
        buildInitialFindings(
            context = context,
            product = product,
            profile = profile,
            notes = notes
        )
    }
    val persistedFindings = remember(code, context) {
        loadSavedFindings(context = context, code = code)
    }

    var isEditing by rememberSaveable(code) { mutableStateOf(false) }
    var coffeeName by rememberSaveable(code) { mutableStateOf(persistedFindings?.coffeeName ?: initialFindings.coffeeName) }
    var origin by rememberSaveable(code) { mutableStateOf(persistedFindings?.origin ?: initialFindings.origin) }
    var roast by rememberSaveable(code) { mutableStateOf(persistedFindings?.roast ?: initialFindings.roast) }
    var notesLabel by rememberSaveable(code) { mutableStateOf(persistedFindings?.notes ?: initialFindings.notes) }
    var process by rememberSaveable(code) { mutableStateOf(persistedFindings?.process ?: initialFindings.process) }
    var showSavedHint by rememberSaveable(code) { mutableStateOf(false) }

    val fields = listOf(
        CoffeeFindingField(
            label = stringResource(R.string.scan_field_coffee_name),
            value = coffeeName,
            onValueChange = { coffeeName = it }
        ),
        CoffeeFindingField(
            label = stringResource(R.string.scan_field_origin),
            value = origin,
            onValueChange = { origin = it }
        ),
        CoffeeFindingField(
            label = stringResource(R.string.scan_field_roast),
            value = roast,
            onValueChange = { roast = it }
        ),
        CoffeeFindingField(
            label = stringResource(R.string.scan_field_notes),
            value = notesLabel,
            onValueChange = { notesLabel = it }
        ),
        CoffeeFindingField(
            label = stringResource(R.string.scan_field_process),
            value = process,
            onValueChange = { process = it }
        )
    )

    val visibleFields = if (isEditing) {
        fields
    } else {
        fields.filter { it.value.trim().isNotBlank() }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.scan_section_details),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFF6E5D1)
            )

            if (isEditing) {
                TextButton(
                    onClick = {
                        val updatedFindings = SavedCoffeeFindings(
                            coffeeName = coffeeName.trim(),
                            origin = origin.trim(),
                            roast = roast.trim(),
                            notes = notesLabel.trim(),
                            process = process.trim()
                        )
                        persistFindings(context = context, code = code, findings = updatedFindings)
                        showSavedHint = true
                        isEditing = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.scan_action_confirm_save),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = GoldAccent
                    )
                }
            } else {
                TextButton(onClick = { isEditing = true }) {
                    Text(
                        text = stringResource(R.string.scan_action_edit_fields),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF2D2A8)
                    )
                }
            }
        }

        if (showSavedHint) {
            Text(
                text = stringResource(R.string.scan_saved_confirmation),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD8BEA2)
            )
        }

        if (visibleFields.isEmpty()) {
            Text(
                text = stringResource(R.string.scan_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD8BEA2),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CoffeeRadius.md))
                    .background(Color(0x88271912))
                    .border(1.dp, Color(0x24F5E6D3), RoundedCornerShape(CoffeeRadius.md))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        } else {
            visibleFields.forEach { field ->
                CoffeeFindingCard(
                    label = field.label,
                    value = field.value,
                    isEditing = isEditing,
                    onValueChange = field.onValueChange
                )
            }
        }
    }
}

@Composable
private fun ProductIdentity(
    product: OpenFoodFactsProduct,
    profile: CoffeeProfile
) {
    val context = LocalContext.current
    val name = product.name.takeIf { it.isNotBlank() } ?: stringResource(R.string.scan_unknown_product)
    val brand = product.brand ?: stringResource(R.string.scan_value_unavailable)
    val type = profile.coffeeType.asUiLabel(context)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.md))
            .background(Color(0xAD281912))
            .border(1.dp, Color(0x24F5E6D3), RoundedCornerShape(CoffeeRadius.md))
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFF9E9D5)
        )
        Text(
            text = stringResource(R.string.scan_brand_type, brand, type),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD9BEA0)
        )
    }
}

@Composable
private fun DetailsGrid(detailPairs: List<Pair<String, String>>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.scan_section_details),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF6E5D1)
        )
        detailPairs.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { (label, value) ->
                    InfoTile(label = label, value = value, modifier = Modifier.weight(1f))
                }
                if (rowItems.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CoffeeRadius.sm))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xA02A1A13), Color(0xB2261812))
                )
            )
            .border(1.dp, Color(0x1EF5E6D3), RoundedCornerShape(CoffeeRadius.sm))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFBFA188)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFFF5E6D3)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(tags: List<String>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.scan_section_tags),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF6E5D1)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFFF7E8D5),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xAA5A3726),
                                    Color(0xAA6A412D)
                                )
                            )
                        )
                        .border(1.dp, Color(0x39E2B888), RoundedCornerShape(999.dp))
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun IngredientsSection(ingredients: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.scan_section_ingredients),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF6E5D1)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CoffeeRadius.md))
                .background(Color(0xB0281912))
                .border(1.dp, Color(0x29F5E6D3), RoundedCornerShape(CoffeeRadius.md))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = ingredients,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE9D5C0)
            )
        }
    }
}

@Composable
private fun BarcodeReference(code: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.sm))
            .background(Color(0x80231510))
            .border(1.dp, Color(0x18F5E6D3), RoundedCornerShape(CoffeeRadius.sm))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = Color(0xCCCAA98A),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = stringResource(R.string.scan_result_code, code),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xCCCAA98A)
        )
    }
}

@Composable
private fun NotFoundCard(code: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.lg))
            .background(Color(0xB0281912))
            .border(1.dp, Color(0x29F5E6D3), RoundedCornerShape(CoffeeRadius.lg))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = GoldAccent,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(R.string.scan_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF8E8D4)
        )
        Text(
            text = stringResource(R.string.scan_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD8BEA2)
        )
        Text(
            text = stringResource(R.string.scan_result_code, code),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xCCCAA98A)
        )
    }
}

@Composable
private fun ErrorCard(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.lg))
            .background(Color(0xB0281912))
            .border(1.dp, Color(0x29F5E6D3), RoundedCornerShape(CoffeeRadius.lg))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = stringResource(R.string.scan_error_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFF8E8D4)
            )
        }
        Text(
            text = stringResource(R.string.scan_error_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD8BEA2)
        )
        PrimaryCoffeeButton(
            text = stringResource(R.string.scan_action_retry_lookup),
            subtitle = stringResource(R.string.scan_action_retry_lookup_subtitle),
            onClick = onRetry
        )
    }
}

@Composable
private fun MatchSection(matchResult: CoffeeMatchResult) {
    val context = LocalContext.current
    val state = matchResult.insight.state
    val stateTitle = state.asTitle(context)
    val stateSubtitle = state.asSubtitle(context)
    val signalLines = matchResult.insight.signals
        .mapNotNull { signal -> signal.toInsightLine(context) }
        .ifEmpty { listOf(stateSubtitle) }
        .take(3)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.scan_section_match),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF6E5D1)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CoffeeRadius.md))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xCC6A3F2A),
                            Color(0xCC8B5638),
                            Color(0xCCB67A4D)
                        )
                    )
                )
                .border(1.dp, Color(0x4AF5E6D3), RoundedCornerShape(CoffeeRadius.md))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stateTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFFFF4E8)
            )
            Text(
                text = stateSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFDF3E7)
            )
            signalLines.forEach { line ->
                Text(
                    text = "\u2022 $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF7E8D5)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScanInteractionActions(
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit,
    onReactionSelected: (TasteReaction) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.scan_section_interactions),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF6E5D1)
        )

        Text(
            text = stringResource(
                if (isFavorited) {
                    R.string.scan_action_saved
                } else {
                    R.string.scan_action_save
                }
            ),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF7E8D5),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = if (isFavorited) {
                            listOf(Color(0xCC8A5638), Color(0xCCB67A4D))
                        } else {
                            listOf(Color(0xA7492F21), Color(0xA75A3A2A))
                        }
                    )
                )
                .border(
                    1.dp,
                    if (isFavorited) Color(0x4AE2B888) else Color(0x2AF5E6D3),
                    RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clickable(onClick = onToggleFavorite)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                TasteReaction.LOVED_IT,
                TasteReaction.TOO_BITTER,
                TasteReaction.TOO_ACIDIC,
                TasteReaction.TOO_STRONG
            ).forEach { reaction ->
                Text(
                    text = reactionUiLabel(reaction),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFFF7E8D5),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xAA3A251B))
                        .border(1.dp, Color(0x2EF5E6D3), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .clickable { onReactionSelected(reaction) }
                )
            }
        }
    }
}

@Composable
private fun TasteLearningSnapshot(summary: UserTasteSummary) {
    if (summary.interactionCount <= 0) return
    val context = LocalContext.current

    val topNotes = summary.topNotes.takeIf { it.isNotEmpty() }
        ?.joinToString(stringResource(R.string.scan_list_separator)) { it.asUiLabel(context) }
        ?: stringResource(R.string.scan_taste_summary_none)
    val roast = summary.likelyRoast?.asUiLabel(context) ?: stringResource(R.string.scan_value_unavailable)
    val acidity = summary.likelyAcidity?.asUiLabel(context) ?: stringResource(R.string.scan_value_unavailable)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.scan_section_taste_memory),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF6E5D1)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CoffeeRadius.md))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xB42A1A13), Color(0xC1261812))
                    )
                )
                .border(1.dp, Color(0x2CF5E6D3), RoundedCornerShape(CoffeeRadius.md))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.scan_taste_summary_notes, topNotes),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF2E0CC)
            )
            Text(
                text = stringResource(R.string.scan_taste_summary_roast_acidity, roast, acidity),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD4B99A)
            )
            Text(
                text = stringResource(R.string.scan_taste_summary_interactions, summary.interactionCount),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xC8D0B49A)
            )
        }
    }
}

@Composable
private fun CoffeeProfileMeta(profile: CoffeeProfile) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.scan_section_profile),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF6E5D1)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CoffeeRadius.md))
                .background(Color(0xB1281912))
                .border(1.dp, Color(0x24F5E6D3), RoundedCornerShape(CoffeeRadius.md))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(
                        R.string.scan_profile_line,
                        stringResource(R.string.scan_profile_strength),
                        profile.strength.asUiLabel(context)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF1DEC8)
                )
                Text(
                    text = stringResource(
                        R.string.scan_profile_line,
                        stringResource(R.string.scan_profile_acidity),
                        profile.acidity.asUiLabel(context)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF1DEC8)
                )
                Text(
                    text = stringResource(
                        R.string.scan_profile_line,
                        stringResource(R.string.scan_profile_milk_friendly),
                        if (profile.milkFriendly) {
                            stringResource(R.string.scan_yes)
                        } else {
                            stringResource(R.string.scan_no)
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD9BFA3)
                )
            }
            Text(
                text = stringResource(R.string.scan_confidence_percent, profile.confidenceScore),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = GoldAccent
            )
        }
    }
}

@Composable
private fun LowConfidenceCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.sm))
            .background(Color(0x99261711))
            .border(1.dp, Color(0x22F5E6D3), RoundedCornerShape(CoffeeRadius.sm))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFFD9B48E),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = stringResource(R.string.scan_low_confidence_note),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xD5D9BFA3)
        )
    }
}

private data class SavedCoffeeFindings(
    val coffeeName: String,
    val origin: String,
    val roast: String,
    val notes: String,
    val process: String
)

private data class CoffeeFindingField(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit
)

@Composable
private fun CoffeeFindingCard(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.md))
            .background(Color(0xB0281912))
            .border(1.dp, Color(0x29F5E6D3), RoundedCornerShape(CoffeeRadius.md))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFD2B293)
        )
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFF5E6D3)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x5A2B1B14),
                    unfocusedContainerColor = Color(0x5A2B1B14),
                    disabledContainerColor = Color(0x5A2B1B14),
                    focusedIndicatorColor = GoldAccent.copy(alpha = 0.75f),
                    unfocusedIndicatorColor = Color(0x35F5E6D3),
                    cursorColor = GoldAccent,
                    focusedTextColor = Color(0xFFF5E6D3),
                    unfocusedTextColor = Color(0xFFF5E6D3)
                )
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFFF5E6D3)
            )
        }
    }
}

private fun buildInitialFindings(
    context: android.content.Context,
    product: OpenFoodFactsProduct,
    profile: CoffeeProfile,
    notes: List<String>
): SavedCoffeeFindings {
    val coffeeName = product.name.takeIf { it.isNotBlank() }
        ?: product.genericName?.takeIf { it.isNotBlank() }
        ?: ""
    val origin = profile.originCountry ?: product.countries?.firstToken().orEmpty()
    val roast = if (profile.roastLevel == RoastLevel.UNKNOWN) {
        ""
    } else {
        profile.roastLevel.asUiLabel(context)
    }
    val notesLabel = notes.joinToString(context.getString(R.string.scan_list_separator))
    return SavedCoffeeFindings(
        coffeeName = coffeeName,
        origin = origin,
        roast = roast,
        notes = notesLabel,
        process = detectCoffeeProcess(context, product).orEmpty()
    )
}

private fun detectCoffeeProcess(
    context: android.content.Context,
    product: OpenFoodFactsProduct
): String? {
    val searchable = listOfNotNull(product.categories, product.labels, product.genericName)
        .joinToString(" ")
        .lowercase(Locale.ROOT)
    if (searchable.isBlank()) return null
    return when {
        searchable.contains("washed") || searchable.contains("yikanmis") || searchable.contains("yıkanmış") ||
            searchable.contains("lavado") || searchable.contains("lave") || searchable.contains("gewaschen") ->
            context.getString(R.string.scan_process_washed)
        searchable.contains("natural") || searchable.contains("naturally processed") ->
            context.getString(R.string.scan_process_natural)
        searchable.contains("honey process") || searchable.contains("honey processed") ->
            context.getString(R.string.scan_process_honey)
        searchable.contains("anaerobic") ->
            context.getString(R.string.scan_process_anaerobic)
        searchable.contains("wet hulled") || searchable.contains("giling basah") ->
            context.getString(R.string.scan_process_wet_hulled)
        else -> null
    }
}

private fun loadSavedFindings(
    context: android.content.Context,
    code: String
): SavedCoffeeFindings? {
    val prefs = context.getSharedPreferences(SCAN_FINDINGS_PREFS, android.content.Context.MODE_PRIVATE)
    val raw = prefs.getString(code, null) ?: return null
    return runCatching {
        val obj = JSONObject(raw)
        SavedCoffeeFindings(
            coffeeName = obj.optString("coffeeName"),
            origin = obj.optString("origin"),
            roast = obj.optString("roast"),
            notes = obj.optString("notes"),
            process = obj.optString("process")
        )
    }.getOrNull()
}

private fun persistFindings(
    context: android.content.Context,
    code: String,
    findings: SavedCoffeeFindings
) {
    val prefs = context.getSharedPreferences(SCAN_FINDINGS_PREFS, android.content.Context.MODE_PRIVATE)
    if (
        findings.coffeeName.isBlank() &&
        findings.origin.isBlank() &&
        findings.roast.isBlank() &&
        findings.notes.isBlank() &&
        findings.process.isBlank()
    ) {
        prefs.edit().remove(code).apply()
        return
    }

    val json = JSONObject()
        .put("coffeeName", findings.coffeeName)
        .put("origin", findings.origin)
        .put("roast", findings.roast)
        .put("notes", findings.notes)
        .put("process", findings.process)
        .toString()
    prefs.edit().putString(code, json).apply()
}

private const val SCAN_FINDINGS_PREFS = "scan_findings_prefs"

private fun detailPairs(
    context: android.content.Context,
    product: OpenFoodFactsProduct,
    profile: CoffeeProfile,
    originLabel: String,
    roastLabel: String,
    typeLabel: String,
    profileLabel: String,
    unavailable: String
): List<Pair<String, String>> {
    val origin = profile.originCountry ?: product.countries?.firstToken() ?: unavailable
    val roast = profile.roastLevel.asUiLabel(context)
    val format = profile.coffeeType.asUiLabel(context)
    val profileText = when {
        profile.strength == StrengthLevel.UNKNOWN && profile.acidity == AcidityLevel.UNKNOWN -> unavailable
        profile.acidity == AcidityLevel.UNKNOWN -> profile.strength.asUiLabel(context)
        profile.strength == StrengthLevel.UNKNOWN -> profile.acidity.asUiLabel(context)
        else -> context.getString(
            R.string.scan_strength_acidity_pair,
            profile.strength.asUiLabel(context),
            profile.acidity.asUiLabel(context)
        )
    }

    return listOf(
        originLabel to origin,
        roastLabel to roast,
        typeLabel to format,
        profileLabel to profileText
    )
}

private fun collectTasteNotes(
    context: android.content.Context,
    profile: CoffeeProfile,
    product: OpenFoodFactsProduct
): List<String> {
    val normalized = profile.tasteNotes.map { it.asUiLabel(context) }
    if (normalized.isNotEmpty()) return normalized

    val raw = buildList {
        addAll(product.categories.toTagList())
        addAll(product.labels.toTagList())
    }
    return raw
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)
}

private fun String?.toTagList(): List<String> {
    if (this.isNullOrBlank()) return emptyList()
    return split(",")
        .map { it.substringAfter(":").trim() }
        .filter { it.isNotBlank() }
}

private fun String.firstToken(): String =
    split(",")
        .map { it.substringAfter(":").trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()

private fun CoffeeType.asUiLabel(context: android.content.Context): String = context.getString(
    when (this) {
        CoffeeType.WHOLE_BEAN -> R.string.scan_type_whole_bean
        CoffeeType.GROUND -> R.string.scan_type_ground
        CoffeeType.INSTANT -> R.string.scan_type_instant
        CoffeeType.CAPSULE -> R.string.scan_type_capsule
        CoffeeType.READY_TO_DRINK -> R.string.scan_type_rtd
        CoffeeType.UNKNOWN -> R.string.scan_value_unknown
    }
)

private fun RoastLevel.asUiLabel(context: android.content.Context): String = context.getString(
    when (this) {
        RoastLevel.LIGHT -> R.string.scan_roast_light
        RoastLevel.MEDIUM -> R.string.scan_roast_medium
        RoastLevel.DARK -> R.string.scan_roast_dark
        RoastLevel.UNKNOWN -> R.string.scan_value_unknown
    }
)

private fun StrengthLevel.asUiLabel(context: android.content.Context): String = context.getString(
    when (this) {
        StrengthLevel.LOW -> R.string.scan_level_low
        StrengthLevel.MEDIUM -> R.string.scan_level_medium
        StrengthLevel.HIGH -> R.string.scan_level_high
        StrengthLevel.UNKNOWN -> R.string.scan_value_unknown
    }
)

private fun AcidityLevel.asUiLabel(context: android.content.Context): String = context.getString(
    when (this) {
        AcidityLevel.LOW -> R.string.scan_level_low
        AcidityLevel.MEDIUM -> R.string.scan_level_medium
        AcidityLevel.HIGH -> R.string.scan_level_high
        AcidityLevel.UNKNOWN -> R.string.scan_value_unknown
    }
)

private fun TasteNote.asUiLabel(context: android.content.Context): String = context.getString(
    when (this) {
        TasteNote.CHOCOLATE -> R.string.scan_note_chocolate
        TasteNote.NUTTY -> R.string.scan_note_nutty
        TasteNote.FRUITY -> R.string.scan_note_fruity
        TasteNote.FLORAL -> R.string.scan_note_floral
        TasteNote.CARAMEL -> R.string.scan_note_caramel
        TasteNote.BOLD -> R.string.scan_note_bold
        TasteNote.SMOOTH -> R.string.scan_note_smooth
        TasteNote.BRIGHT -> R.string.scan_note_bright
        TasteNote.SMOKY -> R.string.scan_note_smoky
    }
)

private fun TasteInsightState.asTitle(context: android.content.Context): String = context.getString(
    when (this) {
        TasteInsightState.NOT_ENOUGH_DATA -> R.string.scan_taste_state_not_enough_title
        TasteInsightState.PARTIAL_MATCH -> R.string.scan_taste_state_partial_title
        TasteInsightState.LIKELY_ALIGNED -> R.string.scan_taste_state_aligned_title
        TasteInsightState.POTENTIAL_MISMATCH -> R.string.scan_taste_state_mismatch_title
    }
)

private fun TasteInsightState.asSubtitle(context: android.content.Context): String = context.getString(
    when (this) {
        TasteInsightState.NOT_ENOUGH_DATA -> R.string.scan_taste_state_not_enough_subtitle
        TasteInsightState.PARTIAL_MATCH -> R.string.scan_taste_state_partial_subtitle
        TasteInsightState.LIKELY_ALIGNED -> R.string.scan_taste_state_aligned_subtitle
        TasteInsightState.POTENTIAL_MISMATCH -> R.string.scan_taste_state_mismatch_subtitle
    }
)

private fun TasteInsightSignal.toInsightLine(context: android.content.Context): String? = when (this) {
    is TasteInsightSignal.SharedNotes -> {
        val labels = notes.map { it.asUiLabel(context) }.joinToString(context.getString(R.string.scan_list_separator))
        context.getString(R.string.scan_taste_signal_shared_notes, labels)
    }
    is TasteInsightSignal.AvoidedNotes -> {
        val labels = notes.map { it.asUiLabel(context) }.joinToString(context.getString(R.string.scan_list_separator))
        context.getString(R.string.scan_taste_signal_avoided_notes, labels)
    }
    is TasteInsightSignal.RoastAligned -> {
        context.getString(R.string.scan_taste_signal_roast_aligned, roast.asUiLabel(context))
    }
    is TasteInsightSignal.RoastDifferent -> {
        context.getString(
            R.string.scan_taste_signal_roast_different,
            expected.asUiLabel(context),
            actual.asUiLabel(context)
        )
    }
    is TasteInsightSignal.AcidityAligned -> {
        context.getString(R.string.scan_taste_signal_acidity_aligned, acidity.asUiLabel(context))
    }
    is TasteInsightSignal.AcidityDifferent -> {
        context.getString(
            R.string.scan_taste_signal_acidity_different,
            expected.asUiLabel(context),
            actual.asUiLabel(context)
        )
    }
    is TasteInsightSignal.FamiliarOrigin -> {
        context.getString(R.string.scan_taste_signal_origin_familiar, origin)
    }
    is TasteInsightSignal.FamiliarCoffeeType -> {
        context.getString(R.string.scan_taste_signal_type_familiar, type.asUiLabel(context))
    }
    TasteInsightSignal.LimitedEvidence -> context.getString(R.string.scan_taste_signal_limited_evidence)
}

@Composable
private fun reactionUiLabel(reaction: TasteReaction): String = when (reaction) {
    TasteReaction.LOVED_IT -> stringResource(R.string.scan_reaction_loved_it)
    TasteReaction.TOO_BITTER -> stringResource(R.string.scan_reaction_too_bitter)
    TasteReaction.TOO_ACIDIC -> stringResource(R.string.scan_reaction_too_acidic)
    TasteReaction.TOO_WEAK -> stringResource(R.string.scan_reaction_too_weak)
    TasteReaction.TOO_STRONG -> stringResource(R.string.scan_reaction_too_strong)
    TasteReaction.TOO_SWEET -> stringResource(R.string.scan_reaction_too_sweet)
    TasteReaction.NOT_FOR_ME -> stringResource(R.string.scan_reaction_not_for_me)
}
