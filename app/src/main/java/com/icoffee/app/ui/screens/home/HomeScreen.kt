package com.icoffee.app.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icoffee.app.R
import com.icoffee.app.data.CoffeeRepository
import com.icoffee.app.data.PhaseOneRepository
import com.icoffee.app.data.RecommendationEngine
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.model.BrewingMethod
import com.icoffee.app.data.model.Coffee
import com.icoffee.app.data.model.CoffeeCategory
import com.icoffee.app.data.model.MoodType
import com.icoffee.app.data.model.RecommendationItem
import com.icoffee.app.ui.brewing.brewingCategorySubtitleRes
import com.icoffee.app.ui.brewing.brewingCategoryTitleRes
import com.icoffee.app.ui.brewing.localizedBrewingMethod
import com.icoffee.app.ui.components.coffinityPressMotion
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.ui.theme.CoffeeSpacing
import java.util.Calendar
import java.util.Locale
import coil.compose.AsyncImage

private data class HomeCategory(
    val labelRes: Int,
    val icon: ImageVector,
    val category: CoffeeCategory
)

private val homeCategories = listOf(
    HomeCategory(R.string.home_category_hot, Icons.Default.LocalCafe, CoffeeCategory.HOT),
    HomeCategory(R.string.home_category_cold, Icons.Default.AcUnit, CoffeeCategory.COLD),
    HomeCategory(R.string.home_category_espresso, Icons.Default.FlashOn, CoffeeCategory.ESPRESSO),
    HomeCategory(R.string.home_category_milk, Icons.Default.LocalDrink, CoffeeCategory.MILK)
)

private val Space4 = 4.dp
private val Space8 = 8.dp
private val Space12 = 12.dp
private val Space16 = 16.dp
private val Space24 = 24.dp
private val UnifiedCardCorner = 24.dp
private val UnifiedCardShadow = 8.dp
private val MediaCardHeight = 220.dp

fun getGreetingByHourRes(): Int {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> R.string.home_greeting_morning
        in 12..16 -> R.string.home_greeting_afternoon
        in 17..21 -> R.string.home_greeting_evening
        else -> R.string.home_greeting_night
    }
}

@Composable
fun HomeScreen(
    onBrewingMethodClick: (String) -> Unit,
    onCoffeeClick: (String) -> Unit
) {
    val initialMood = remember { RecommendationEngine.lastMoodOrDefault() }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedMood by rememberSaveable { mutableStateOf(initialMood.name) }
    var reshuffleTick by rememberSaveable { mutableStateOf(0) }
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val activeMood = MoodType.valueOf(selectedMood)
    val selectedCategory = selectedCategoryName?.let { name ->
        CoffeeCategory.entries.firstOrNull { it.name == name }
    }
    val categoryFilteredCoffees = remember(selectedCategory) {
        selectedCategory?.let { cat ->
            CoffeeRepository.coffees.filter { it.category == cat }
        } ?: emptyList()
    }

    LaunchedEffect(activeMood) {
        RecommendationEngine.onMoodSelected(activeMood)
    }

    val recommendedItems = remember(activeMood, reshuffleTick) {
        RecommendationEngine.recommendationsForMood(
            mood = activeMood,
            reshuffle = reshuffleTick > 0
        )
    }

    val searchResults = remember(searchQuery) {
        CoffeeRepository.searchCoffees(searchQuery).take(8)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5EFE7)),
        contentPadding = PaddingValues(horizontal = Space16, vertical = Space24),
        verticalArrangement = Arrangement.spacedBy(Space24)
    ) {
        item {
            HeaderSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it }
            )
        }

        item {
            AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                SearchResultsSection(
                    query = searchQuery,
                    results = searchResults,
                    onOpenCoffee = onCoffeeClick
                )
            }
        }

        item {
            SectionEntrance(delayMs = 40) {
                HighlightCard()
            }
        }

        item {
            SectionEntrance(delayMs = 70) {
                CategoryRow(
                    selectedCategory = selectedCategory,
                    onCategoryClick = { cat ->
                        selectedCategoryName = if (selectedCategory == cat) null else cat.name
                    }
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = categoryFilteredCoffees.isNotEmpty() && searchQuery.isBlank()
            ) {
                CategoryFilterSection(
                    coffees = categoryFilteredCoffees,
                    onOpenCoffee = onCoffeeClick
                )
            }
        }

        item {
            SectionEntrance(delayMs = 100) {
                RecommendationSection(
                    moods = PhaseOneRepository.moods,
                    selectedMood = activeMood,
                    onMoodSelected = {
                        selectedMood = it.name
                        reshuffleTick = 0
                    },
                    items = recommendedItems,
                    onShuffle = {
                        reshuffleTick += 1
                    },
                    onOpenCoffee = { recommendation ->
                        RecommendationEngine.onRecommendationTapped(recommendation)
                        onCoffeeClick(recommendation.coffeeId)
                    }
                )
            }
        }

        item {
            SectionEntrance(delayMs = 130) {
                BrewingMethodsSection(
                    methods = PhaseOneRepository.brewingMethods,
                    onMethodClick = {
                        RecommendationEngine.onBrewingMethodOpened(it.id)
                        onBrewingMethodClick(it.id)
                    }
                )
            }
        }

        item { Box(modifier = Modifier.height(Space4)) }
    }
}

@Composable
private fun HeaderSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val context = LocalContext.current
    val greetingRes = remember { getGreetingByHourRes() }
    val greeting = stringResource(greetingRes)
    val user = FirebaseAuthRepository.currentUser
    val displayName = user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@")
        ?: stringResource(R.string.home_default_display_name)
    val photoUrl = user?.photoUrl?.toString()
    val initial = displayName.first().toString().uppercase(AppLocaleManager.currentLocale(context))

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = Space4),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAD7C2))
                    .border(1.dp, Color(0x66D2B492), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF6B3E26)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            val greetingInlineContent = remember {
                mapOf(
                    "coffeeIcon" to InlineTextContent(
                        Placeholder(
                            width = 16.sp,
                            height = 16.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalCafe,
                            contentDescription = null,
                            tint = Color(0xFF7A4D32),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            Text(
                text = buildAnnotatedString {
                    append(greeting)
                    append(" ")
                    appendInlineContent("coffeeIcon", "[icon]")
                },
                inlineContent = greetingInlineContent,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2E2018)
            )
        }

        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.home_search_placeholder),
                    color = Color(0xFF8A7464),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8B7563))
            },
            trailingIcon = {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF8B7563))
            },
            shape = RoundedCornerShape(22.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF0E8DE),
                unfocusedContainerColor = Color(0xFFF0E8DE),
                disabledContainerColor = Color(0xFFF0E8DE),
                focusedTextColor = Color(0xFF2F2119),
                unfocusedTextColor = Color(0xFF2F2119),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SearchResultsSection(
    query: String,
    results: List<Coffee>,
    onOpenCoffee: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.md)) {
        Text(
            text = stringResource(R.string.home_results_for, query),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF3A271B)
        )

        if (results.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_no_coffee_found),
                    modifier = Modifier.padding(CoffeeSpacing.lg),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF7E6653)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                results.forEach { coffee ->
                    SearchResultRow(
                        coffee = coffee,
                        onOpen = { onOpenCoffee(coffee.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    coffee: Coffee,
    onOpen: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val rowShadow by animateDpAsState(
        targetValue = if (pressed) 2.dp else 4.dp,
        animationSpec = tween(durationMillis = if (pressed) 90 else 220),
        label = "searchResultRowShadow"
    )

    Card(
        onClick = onOpen,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.98f,
                pressedAlpha = 0.97f
            )
            .shadow(
                elevation = rowShadow,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x1F1D130D),
                spotColor = Color(0x291D130D)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CoffeeSpacing.md, vertical = CoffeeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
        ) {
            Image(
                painter = painterResource(id = coffee.imageRes),
                contentDescription = stringResource(coffee.titleRes),
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(coffee.titleRes),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF2C1F17)
                )
                Text(
                    text = stringResource(coffee.typeRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7A6353)
                )
            }
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = stringResource(R.string.home_view_details),
                tint = Color(0xFF9A734F)
            )
        }
    }
}

@Composable
private fun HighlightCard() {
    Card(
        shape = RoundedCornerShape(UnifiedCardCorner),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(MediaCardHeight)
            .shadow(
                elevation = UnifiedCardShadow,
                shape = RoundedCornerShape(UnifiedCardCorner),
                ambientColor = Color(0x331B120C),
                spotColor = Color(0x401B120C)
            )
            .clip(RoundedCornerShape(UnifiedCardCorner))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.coffee_highlight_hero),
                contentDescription = stringResource(R.string.home_highlight_image_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x4D000000),
                                Color(0xBF000000)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Space16),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_highlight_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFF6DFC5)
                )
                Text(
                    text = stringResource(R.string.home_highlight_subtitle),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        letterSpacing = 0.sp
                    ),
                    color = Color(0xFFFFF3E8)
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    selectedCategory: CoffeeCategory?,
    onCategoryClick: (CoffeeCategory) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space12)
    ) {
        homeCategories.forEach { category ->
            val label = stringResource(category.labelRes)
            val isSelected = category.category == selectedCategory
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 84.dp, max = 90.dp)
                    .shadow(
                        elevation = if (isSelected) 5.dp else 3.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0x1A1A120C),
                        spotColor = Color(0x241A120C)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color(0xFFE8D2B8) else Color(0xFFF5EFE7))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onCategoryClick(category.category) }
                    .padding(vertical = Space12, horizontal = Space8),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = label,
                    tint = if (isSelected) Color(0xFF7A3B1E) else Color(0xFF8B5E3C),
                    modifier = Modifier.size(24.dp)
                )
                Box(modifier = Modifier.height(Space8))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) Color(0xFF5A2F1A) else Color(0xFF5A3E2E),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterSection(
    coffees: List<Coffee>,
    onOpenCoffee: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
        coffees.forEach { coffee ->
            SearchResultRow(
                coffee = coffee,
                onOpen = { onOpenCoffee(coffee.id) }
            )
        }
    }
}

@Composable
private fun RecommendationSection(
    moods: List<MoodType>,
    selectedMood: MoodType,
    onMoodSelected: (MoodType) -> Unit,
    items: List<RecommendationItem>,
    onShuffle: () -> Unit,
    onOpenCoffee: (RecommendationItem) -> Unit
) {
    val rows = remember(moods) { moods.chunked(4) }
    val visibleRecommendations = remember(items) { items.take(2) }
    val shuffleInteraction = remember { MutableInteractionSource() }

    Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
        Column(verticalArrangement = Arrangement.spacedBy(Space4)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.home_choose_mood),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                        lineHeight = 34.sp
                    ),
                    color = Color(0xFF2E2018)
                )
                Box(
                modifier = Modifier
                    .size(36.dp)
                    .coffinityPressMotion(
                        interactionSource = shuffleInteraction,
                        pressedScale = 0.96f,
                        pressedAlpha = 0.9f
                    )
                    .clip(CircleShape)
                    .background(Color(0xFFF1E3D4))
                    .clickable(
                        interactionSource = shuffleInteraction,
                        indication = null,
                        onClick = onShuffle
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = stringResource(R.string.home_reshuffle),
                    tint = Color(0xFF6B3E26),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
            Text(
                text = stringResource(R.string.home_mood_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9C7B65)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
            rows.forEach { rowMoods ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowMoods.forEach { mood ->
                        MiniMoodChip(
                            label = stringResource(mood.labelRes),
                            selected = mood == selectedMood,
                            onClick = { onMoodSelected(mood) }
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
            visibleRecommendations.forEachIndexed { index, recommendation ->
                AnimatedContent(
                    targetState = recommendation,
                    transitionSpec = {
                        (
                            fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 40)) +
                                slideInVertically(
                                    initialOffsetY = { it / 10 },
                                    animationSpec = tween(durationMillis = 240)
                                )
                            ) togetherWith (
                            fadeOut(animationSpec = tween(durationMillis = 140)) +
                                slideOutVertically(
                                    targetOffsetY = { -it / 14 },
                                    animationSpec = tween(durationMillis = 180)
                                )
                            )
                    },
                    label = "recommendationCardSwap$index"
                ) { animatedRecommendation ->
                    RecommendationCard(
                        recommendation = animatedRecommendation,
                        modifier = Modifier.fillMaxWidth(),
                        appearDelayMs = index * 30,
                        onOpen = { onOpenCoffee(animatedRecommendation) }
                    )
                }
            }

            if (items.size > 2) {
                Text(
                    text = stringResource(R.string.home_mood_top_picks_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7D6655)
                )
            }
        }
    }
}

@Composable
private fun MiniMoodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) Color(0xFFEED4B8) else Color(0xFFF8F1E9),
        animationSpec = tween(200),
        label = "miniChipBg"
    )
    val text by animateColorAsState(
        targetValue = if (selected) Color(0xFF6B3E26) else Color(0xFF8D7564),
        animationSpec = tween(200),
        label = "miniChipText"
    )
    val liftScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = tween(220),
        label = "miniMoodLiftScale"
    )
    val chipShadow by animateDpAsState(
        targetValue = if (selected) 5.dp else 2.dp,
        animationSpec = tween(220),
        label = "miniMoodShadow"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = liftScale
                scaleY = liftScale
            }
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.98f,
                pressedAlpha = 0.96f
            )
            .shadow(
                elevation = chipShadow,
                shape = RoundedCornerShape(999.dp),
                clip = false
            )
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = CoffeeSpacing.sm, vertical = CoffeeSpacing.xs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = text
        )
    }
}

@Composable
private fun RecommendationCard(
    recommendation: RecommendationItem,
    modifier: Modifier = Modifier,
    appearDelayMs: Int = 0,
    onOpen: () -> Unit
) {
    var appeared by remember(recommendation.id) { mutableStateOf(false) }
    LaunchedEffect(recommendation.id) {
        if (appearDelayMs > 0) {
            kotlinx.coroutines.delay(appearDelayMs.toLong())
        }
        appeared = true
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dynamicShadow by animateDpAsState(
        targetValue = if (pressed) 5.dp else UnifiedCardShadow,
        animationSpec = tween(durationMillis = if (pressed) 90 else 220),
        label = "recommendCardShadow"
    )
    val appearAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "recommendCardAppearAlpha"
    )
    val appearOffset by animateDpAsState(
        targetValue = if (appeared) 0.dp else 6.dp,
        animationSpec = tween(durationMillis = 260),
        label = "recommendCardAppearOffset"
    )
    val appearOffsetPx = with(LocalDensity.current) { appearOffset.toPx() }

    Card(
        shape = RoundedCornerShape(UnifiedCardCorner),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF9F4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(MediaCardHeight)
            .graphicsLayer {
                alpha = appearAlpha
                translationY = appearOffsetPx
            }
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.98f,
                pressedAlpha = 0.97f
            )
            .shadow(
                elevation = dynamicShadow,
                shape = RoundedCornerShape(UnifiedCardCorner),
                ambientColor = Color(0x2617100A),
                spotColor = Color(0x3317100A)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpen
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = recommendation.imageRes),
                contentDescription = stringResource(recommendation.coffeeNameRes),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x4D000000),
                                Color(0xBF000000)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Space16)
            ) {
                Text(
                    text = stringResource(recommendation.coffeeNameRes),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        letterSpacing = 0.sp
                    ),
                    color = Color.White,
                    maxLines = 2
                )
                Box(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(recommendation.brewingStyleRes),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    ),
                    color = Color.White.copy(alpha = 0.8f)
                )
                Box(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(recommendation.reasonRes),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2
                )
                Box(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space4)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFD6A77A),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", recommendation.rating),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                    val exploreInteraction = remember { MutableInteractionSource() }
                    val explorePressed by exploreInteraction.collectIsPressedAsState()
                    val exploreShadow by animateDpAsState(
                        targetValue = if (explorePressed) 2.dp else 4.dp,
                        animationSpec = tween(durationMillis = if (explorePressed) 90 else 220),
                        label = "exploreButtonShadow"
                    )
                    Box(
                        modifier = Modifier
                            .coffinityPressMotion(
                                interactionSource = exploreInteraction,
                                pressedScale = 0.97f,
                                pressedAlpha = 0.95f
                            )
                            .height(36.dp)
                            .shadow(exploreShadow, RoundedCornerShape(18.dp), clip = false)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFC89A6A))
                            .clickable(
                                interactionSource = exploreInteraction,
                                indication = null,
                                onClick = onOpen
                            )
                            .padding(horizontal = 14.dp, vertical = Space8),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_explore),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrewingMethodsSection(
    methods: List<BrewingMethod>,
    onMethodClick: (BrewingMethod) -> Unit
) {
    val groupedMethods = remember(methods) { PhaseOneRepository.brewingMethodsByCategory(methods) }
    var cardSequence = 0

    Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
        Text(
            text = stringResource(R.string.home_brewing_methods),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                lineHeight = 34.sp
            ),
            color = Color(0xFF2E2018)
        )

        Column(verticalArrangement = Arrangement.spacedBy(Space24)) {
            groupedMethods.forEach { (category, categoryMethods) ->
                Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
                    Text(
                        text = stringResource(brewingCategoryTitleRes(category)),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF5B3A29)
                    )
                    Text(
                        text = stringResource(brewingCategorySubtitleRes(category)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0x9A664734)
                    )
                    categoryMethods.chunked(2).forEach { rowMethods ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Space12)
                        ) {
                            rowMethods.forEach { method ->
                                val localizedMethod = localizedBrewingMethod(method)
                                MethodCard(
                                    method = localizedMethod,
                                    modifier = Modifier.weight(1f),
                                    appearDelayMs = (cardSequence * 22).coerceAtMost(240),
                                    onClick = { onMethodClick(localizedMethod) }
                                )
                                cardSequence++
                            }
                            if (rowMethods.size == 1) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodCard(
    method: BrewingMethod,
    modifier: Modifier = Modifier,
    appearDelayMs: Int = 0,
    onClick: () -> Unit
) {
    var appeared by remember(method.id) { mutableStateOf(false) }
    LaunchedEffect(method.id) {
        if (appearDelayMs > 0) {
            kotlinx.coroutines.delay(appearDelayMs.toLong())
        }
        appeared = true
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dynamicShadow by animateDpAsState(
        targetValue = if (pressed) 5.dp else UnifiedCardShadow,
        animationSpec = tween(durationMillis = if (pressed) 90 else 220),
        label = "methodCardShadow"
    )
    val appearAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "methodCardAppearAlpha"
    )
    val appearOffset by animateDpAsState(
        targetValue = if (appeared) 0.dp else 6.dp,
        animationSpec = tween(durationMillis = 260),
        label = "methodCardAppearOffset"
    )
    val appearOffsetPx = with(LocalDensity.current) { appearOffset.toPx() }

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(UnifiedCardCorner),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(MediaCardHeight)
            .graphicsLayer {
                alpha = appearAlpha
                translationY = appearOffsetPx
            }
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.98f,
                pressedAlpha = 0.97f
            )
            .shadow(
                elevation = dynamicShadow,
                shape = RoundedCornerShape(UnifiedCardCorner),
                ambientColor = Color(0x2617100A),
                spotColor = Color(0x3317100A)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = method.imageRes),
                contentDescription = method.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x4D000000),
                                Color(0xBF000000)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Space16)
            ) {
                Text(
                    text = method.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        letterSpacing = 0.sp
                    ),
                    color = Color.White,
                    maxLines = 2
                )
                Box(modifier = Modifier.height(6.dp))
                Text(
                    text = method.cardSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2
                )
                Box(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space4)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFFD6A77A),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = method.brewTime,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionEntrance(
    delayMs: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (delayMs > 0) {
            kotlinx.coroutines.delay(delayMs.toLong())
        }
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 260)) +
            slideInVertically(
                initialOffsetY = { it / 10 },
                animationSpec = tween(durationMillis = 300)
            ),
        exit = ExitTransition.None
    ) {
        content()
    }
}
