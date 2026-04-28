// FILE: app/src/main/java/com/icoffee/app/ui/screens/meet/CreateMeetScreen.kt
// FULL REPLACEMENT

package com.icoffee.app.ui.screens.meet

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icoffee.app.BuildConfig
import com.icoffee.app.R
import com.icoffee.app.data.growth.GrowthAnalytics
import com.icoffee.app.data.growth.GrowthEventNames
import com.icoffee.app.data.model.BusinessOffer
import com.icoffee.app.data.model.IncludedOfferItem
import com.icoffee.app.data.model.OfferPaymentMode
import com.icoffee.app.localization.formatter.DateTimeFormatterHelper
import com.icoffee.app.localization.formatter.TimeFormatter
import com.icoffee.app.viewmodel.MeetCreateAttemptResult
import com.icoffee.app.viewmodel.MeetCreateFailureReason
import com.icoffee.app.viewmodel.MeetViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private val MeetDeepBackground = Color(0xFF2B140A)
private val MeetCardFill = Color(0x66140A05)
private val MeetCardBorder = Color(0xFFD8A16A).copy(alpha = 0.4f)
private val MeetHeadingCream = Color(0xFFF2E4D0)
private val MeetSubCream = Color(0xFFD6BEA0)
private val MeetSectionCream = Color(0xFFE7D4BC)
private val MeetInputCream = Color(0xFFE8D5BD)
private val MeetMuted = Color(0xFFBA956F)
private val NebulaAmber = Color(0xFFFFBF00)
private val NebulaBurntOrange = Color(0xFFCC5500)
private val NebulaDeepAmber = Color(0xFFE69E3A)
private val MeetTextShadow = Shadow(
    color = Color(0xCC000000),
    offset = Offset(0f, 2f),
    blurRadius = 4f
)

private enum class ValidationTarget {
    TITLE,
    LOCATION,
    PURPOSE,
    DATE,
    TIME
}

private data class ValidationFeedback(
    val target: ValidationTarget,
    val message: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateMeetScreen(
    onBack: () -> Unit,
    isUserSignedIn: Boolean,
    onRequestSignIn: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenLocationPicker: (() -> Unit)? = null,
    meetViewModel: MeetViewModel = viewModel(),
    editMeetId: String? = null
) {
    val context = LocalContext.current
    val isDebugBuild = BuildConfig.DEBUG
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val titleShakeOffset = remember { Animatable(0f) }
    val locationShakeOffset = remember { Animatable(0f) }
    val purposeShakeOffset = remember { Animatable(0f) }
    val dateShakeOffset = remember { Animatable(0f) }
    val timeShakeOffset = remember { Animatable(0f) }
    val isEditMode = editMeetId != null
    val existingMeet = remember(editMeetId) { editMeetId?.let { meetViewModel.findMeet(it) } }

    var title by remember { mutableStateOf(existingMeet?.title ?: "") }
    var description by remember { mutableStateOf(existingMeet?.description ?: "") }
    var locationName by remember { mutableStateOf(existingMeet?.locationName ?: "") }
    var latitude by remember { mutableDoubleStateOf(existingMeet?.latitude ?: 0.0) }
    var longitude by remember { mutableDoubleStateOf(existingMeet?.longitude ?: 0.0) }

    var selectedDate by remember {
        mutableStateOf<LocalDate?>(
            existingMeet?.scheduledAt?.let {
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(it),
                    ZoneId.systemDefault()
                ).toLocalDate()
            } ?: LocalDate.now()
        )
    }

    var selectedStartTime by remember {
        mutableStateOf<LocalTime?>(
            existingMeet?.scheduledAt?.let {
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(it),
                    ZoneId.systemDefault()
                ).toLocalTime()
            } ?: LocalTime.of(14, 0)
        )
    }

    var selectedDurationMinutes by remember { mutableIntStateOf(60) }
    var selectedPurpose by remember { mutableStateOf(existingMeet?.purpose ?: "") }
    var maxParticipants by remember { mutableIntStateOf(existingMeet?.maxParticipants ?: 4) }
    var selectedBrewingType by remember { mutableStateOf(existingMeet?.brewingType ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var offerEnabled by remember { mutableStateOf(existingMeet?.businessOffer != null) }
    var offerTitle by remember { mutableStateOf(existingMeet?.businessOffer?.offerTitle ?: "") }
    var offerDescription by remember { mutableStateOf(existingMeet?.businessOffer?.offerDescription ?: "") }
    var offerPriceAmount by remember { mutableStateOf<Double?>(existingMeet?.businessOffer?.priceAmount) }
    var offerCurrency by remember { mutableStateOf(existingMeet?.businessOffer?.currency ?: "TRY") }
    var offerPaymentMode by remember {
        mutableStateOf(existingMeet?.businessOffer?.paymentMode ?: OfferPaymentMode.PAY_AT_VENUE)
    }
    var offerAvailabilityLimit by remember { mutableStateOf<Int?>(existingMeet?.businessOffer?.availabilityLimit) }
    val offerItems = remember {
        mutableStateListOf<IncludedOfferItem>().apply {
            existingMeet?.businessOffer?.includedItems?.let { addAll(it) }
        }
    }

    val titleFilled = title.isNotBlank()
    val locationFilled = locationName.isNotBlank()
    val purposeFilled = selectedPurpose.isNotBlank()
    val canSubmit = titleFilled && locationFilled && purposeFilled
    val alignedMeetFieldModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .referenceFieldRim()
    val hasLocationQuery = locationName.trim().isNotEmpty()

    suspend fun runPremiumShake(animatable: Animatable<Float, AnimationVector1D>) {
        animatable.snapTo(0f)
        val keyframes = listOf(-8f, 8f, -6f, 6f, -3f, 3f, 0f)
        keyframes.forEach { step ->
            animatable.animateTo(
                targetValue = step,
                animationSpec = tween(durationMillis = 34, easing = FastOutSlowInEasing)
            )
        }
    }

    val validationTitleMsg = stringResource(R.string.create_meet_validation_title)
    val validationLocationMsg = stringResource(R.string.create_meet_validation_location)
    val validationPurposeMsg = stringResource(R.string.create_meet_validation_purpose)
    val validationDateMsg = stringResource(R.string.create_meet_validation_date)
    val validationTimeMsg = stringResource(R.string.create_meet_validation_time)

    val getValidationFeedback: () -> ValidationFeedback? = {
        when {
            title.isBlank() -> ValidationFeedback(ValidationTarget.TITLE, validationTitleMsg)
            locationName.isBlank() -> ValidationFeedback(ValidationTarget.LOCATION, validationLocationMsg)
            selectedPurpose.isBlank() -> ValidationFeedback(ValidationTarget.PURPOSE, validationPurposeMsg)
            selectedDate == null -> ValidationFeedback(ValidationTarget.DATE, validationDateMsg)
            selectedStartTime == null -> ValidationFeedback(ValidationTarget.TIME, validationTimeMsg)
            else -> null
        }
    }

    val showCoffeeSnackbar: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    val showValidationFeedback: (ValidationFeedback) -> Unit = { feedback ->
        showCoffeeSnackbar(feedback.message)
        scope.launch {
            when (feedback.target) {
                ValidationTarget.TITLE -> runPremiumShake(titleShakeOffset)
                ValidationTarget.LOCATION -> runPremiumShake(locationShakeOffset)
                ValidationTarget.PURPOSE -> runPremiumShake(purposeShakeOffset)
                ValidationTarget.DATE -> runPremiumShake(dateShakeOffset)
                ValidationTarget.TIME -> runPremiumShake(timeShakeOffset)
            }
        }
    }

    val selectedDateLabel = remember(selectedDate, configuration) {
        selectedDate?.let { DateTimeFormatterHelper.formatMeetDateOption(context, it) }
    }
    val selectedTimeLabel = remember(selectedStartTime, configuration) {
        selectedStartTime?.let { TimeFormatter.format(context, it) }
    }

    val nebulaTransition = rememberInfiniteTransition(label = "createMeetNebula")
    val nebulaShiftX by nebulaTransition.animateFloat(
        initialValue = -140f,
        targetValue = 140f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebulaShiftX"
    )
    val nebulaShiftY by nebulaTransition.animateFloat(
        initialValue = -90f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebulaShiftY"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.coffinity_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(NebulaAmber.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(210f + nebulaShiftX, 150f + nebulaShiftY),
                        radius = 460f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(NebulaBurntOrange.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(620f - nebulaShiftX, 470f + nebulaShiftY),
                        radius = 610f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(NebulaDeepAmber.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(430f + (nebulaShiftX * 0.5f), 1170f - nebulaShiftY),
                        radius = 420f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x2EA55324), Color.Transparent),
                        center = Offset(560f, 780f),
                        radius = 520f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x1AE7C39B),
                            Color(0x14C8A98A),
                            Color(0x128E6D55),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x18E7C39B),
                            Color(0x12E7C39B),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x19C8A98A), Color.Transparent),
                        center = Offset(260f, 420f),
                        radius = 500f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x18E7C39B), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x26E7C39B), Color.Transparent),
                        center = Offset(330f + nebulaShiftX, 320f),
                        radius = 260f
                    )
                )
                .blur(42.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x22F0B36A), Color.Transparent),
                        center = Offset(330f, 980f),
                        radius = 300f
                    )
                )
                .blur(38.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x12000000),
                            Color(0x0DFFFFFF),
                            Color(0x12000000),
                            Color.Transparent
                        )
                    )
                )
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, top = 20.dp)
                .size(36.dp)
                .zIndex(2f)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.meet_back),
                tint = Color(0xFFF1E1CC),
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(94.dp))
            Text(
                text = stringResource(R.string.meet_create),
                style = MaterialTheme.typography.headlineMedium.copy(shadow = MeetTextShadow),
                fontWeight = FontWeight.Bold,
                color = MeetHeadingCream,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                letterSpacing = 0.4.sp
            )
            Text(
                text = stringResource(R.string.meet_create_subtitle),
                style = MaterialTheme.typography.bodySmall.copy(shadow = MeetTextShadow),
                fontWeight = FontWeight.Medium,
                color = MeetSubCream.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            )

            ReferenceMeetCard {
                Text(
                    text = stringResource(R.string.create_meet_section_info),
                    style = MaterialTheme.typography.titleSmall.copy(shadow = MeetTextShadow),
                    fontWeight = FontWeight.SemiBold,
                    color = MeetSectionCream,
                    letterSpacing = 0.2.sp
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = null,
                    placeholder = { Text(stringResource(R.string.meet_field_title), color = MeetInputCream) },
                    leadingIcon = {
                        ReferenceFieldIcon(Icons.Default.Edit)
                    },
                    modifier = alignedMeetFieldModifier.offset(x = titleShakeOffset.value.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = referenceFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = null,
                    placeholder = { Text(stringResource(R.string.meet_field_description), color = MeetInputCream) },
                    leadingIcon = {
                        ReferenceFieldIcon(Icons.Default.Description)
                    },
                    modifier = alignedMeetFieldModifier,
                    shape = RoundedCornerShape(12.dp),
                    colors = referenceFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    minLines = 1,
                    maxLines = 3
                )

                Box(
                    modifier = alignedMeetFieldModifier
                        .offset(x = locationShakeOffset.value.dp)
                        .clickable {
                            val handledByPicker = onOpenLocationPicker != null
                            if (handledByPicker) {
                                onOpenLocationPicker.invoke()
                            } else {
                                when (launchGoogleMapsSearch(context, locationName)) {
                                    MapsLaunchResult.LAUNCHED -> Unit
                                    MapsLaunchResult.EMPTY_QUERY -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.create_meet_location_hint_first),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    MapsLaunchResult.FAILED -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.meet_location_maps_open_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                ) {
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        readOnly = true,
                        label = null,
                        placeholder = { Text(stringResource(R.string.meet_field_location), color = MeetInputCream) },
                        leadingIcon = {
                            ReferenceFieldIcon(Icons.Default.LocationOn)
                        },
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = referenceFieldColors(),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        enabled = onOpenLocationPicker != null || hasLocationQuery,
                        onClick = {
                            val handledByPicker = onOpenLocationPicker != null
                            if (handledByPicker) {
                                onOpenLocationPicker.invoke()
                            } else {
                                when (launchGoogleMapsSearch(context, locationName)) {
                                    MapsLaunchResult.LAUNCHED -> Unit
                                    MapsLaunchResult.EMPTY_QUERY -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.meet_location_maps_enter_first),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    MapsLaunchResult.FAILED -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.meet_location_maps_open_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (onOpenLocationPicker != null) {
                                stringResource(R.string.create_meet_pick_on_map)
                            } else {
                                stringResource(R.string.meet_open_in_maps)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD4B291).copy(alpha = 0.88f)
                        )
                    }
                }
            }

            ReferenceMeetCard {
                Text(
                    text = stringResource(R.string.create_meet_section_details),
                    style = MaterialTheme.typography.titleSmall.copy(shadow = MeetTextShadow),
                    fontWeight = FontWeight.SemiBold,
                    color = MeetSectionCream,
                    letterSpacing = 0.2.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReferencePillButton(
                        modifier = Modifier
                            .weight(1f)
                            .offset(x = dateShakeOffset.value.dp),
                        text = selectedDateLabel ?: stringResource(R.string.meet_time_picker_step_date),
                        emphasized = false,
                        onClick = { showDatePicker = true }
                    )
                    ReferencePillButton(
                        modifier = Modifier
                            .weight(1f)
                            .offset(x = timeShakeOffset.value.dp),
                        text = selectedTimeLabel ?: stringResource(R.string.meet_time_picker_step_time),
                        emphasized = true,
                        onClick = { showTimePicker = true }
                    )
                }

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = purposeShakeOffset.value.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val purposes = listOf("Chill", "Productive", "Deep Talk", "Networking")
                    purposes.forEach { purpose ->
                        ReferencePurposeChip(
                            label = purpose,
                            selected = selectedPurpose == purpose,
                            onClick = { selectedPurpose = purpose }
                        )
                    }
                    ReferenceMiniActionChip(
                        onClick = {
                            val currentIndex = purposes.indexOf(selectedPurpose)
                            selectedPurpose = if (currentIndex >= 0) {
                                purposes[(currentIndex + 1) % purposes.size]
                            } else {
                                purposes.first()
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.create_meet_max_participants),
                        style = MaterialTheme.typography.bodySmall.copy(shadow = MeetTextShadow),
                        color = MeetSectionCream,
                        fontWeight = FontWeight.SemiBold
                    )
                    PremiumParticipantStepper(
                        value = maxParticipants,
                        onValueChange = { maxParticipants = it },
                        minValue = 2,
                        maxValue = meetViewModel.maxAllowedParticipants()
                    )
                }
            }

            Text(
                text = stringResource(R.string.meet_field_time_picker_hint),
                style = MaterialTheme.typography.labelSmall.copy(shadow = MeetTextShadow),
                color = MeetMuted.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp, bottom = 2.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                PremiumCreateMeetButton(
                    text = stringResource(if (isEditMode) R.string.meet_save_changes else R.string.meet_create),
                    enabled = canSubmit,
                    onClick = {
                        val validationFeedback = getValidationFeedback()
                        if (validationFeedback != null) {
                            showValidationFeedback(validationFeedback)
                            return@PremiumCreateMeetButton
                        }
                        if (isDebugBuild) {
                            Log.d(
                                "CreateMeetDebug",
                                "button_tap fired canSubmit=$canSubmit titleFilled=$titleFilled locationFilled=$locationFilled purposeFilled=$purposeFilled"
                            )
                        }
                        scope.launch {
                            if (!isEditMode) {
                                val finalDate = selectedDate ?: return@launch
                                val finalTime = selectedStartTime ?: return@launch

                                val finalTimeLabel = DateTimeFormatterHelper.formatMeetSummary(
                                    context = context,
                                    date = finalDate,
                                    startTime = finalTime,
                                    durationMinutes = selectedDurationMinutes
                                )

                                val scheduledAt = LocalDateTime.of(finalDate, finalTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()

                                val normalizedOffer = if (offerEnabled) {
                                    BusinessOffer(
                                        offerTitle = offerTitle.trim(),
                                        offerDescription = offerDescription.trim().takeIf { it.isNotBlank() },
                                        includedItems = offerItems.toList(),
                                        priceAmount = offerPriceAmount,
                                        currency = offerCurrency.takeIf { it.isNotBlank() },
                                        paymentMode = offerPaymentMode,
                                        availabilityLimit = offerAvailabilityLimit,
                                        termsNote = ""
                                    )
                                } else null

                                val result = meetViewModel.createMeet(
                                    title = title.trim(),
                                    description = description.trim(),
                                    locationName = locationName.trim(),
                                    latitude = latitude,
                                    longitude = longitude,
                                    scheduledAt = scheduledAt,
                                    timeLabel = finalTimeLabel,
                                    purpose = selectedPurpose,
                                    maxParticipants = maxParticipants,
                                    hostUserType = meetViewModel.currentUserType,
                                    brewingType = selectedBrewingType,
                                    businessOffer = normalizedOffer,
                                    debugBypassEntitlementGate = isDebugBuild
                                )

                                when (result) {
                                    MeetCreateAttemptResult.Success -> {
                                        GrowthAnalytics.log(
                                            GrowthEventNames.EVENT_CREATED,
                                            params = mapOf("eventTitle" to title.trim())
                                        )
                                        onBack()
                                    }

                                    is MeetCreateAttemptResult.Failure -> {
                                        val message = when (result.reason) {
                                            MeetCreateFailureReason.MONTHLY_LIMIT_REACHED ->
                                                context.getString(R.string.meet_entitlement_monthly_create_limit_reached)

                                            MeetCreateFailureReason.ATTENDEE_LIMIT_EXCEEDED ->
                                                context.getString(
                                                    R.string.meet_entitlement_capacity_limit_reached,
                                                    meetViewModel.maxAttendees
                                                )

                                            else -> context.getString(R.string.meet_entitlement_generic_error)
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        if (result.reason == MeetCreateFailureReason.MONTHLY_LIMIT_REACHED) {
                                            GrowthAnalytics.log(GrowthEventNames.PAYWALL_OPENED_FROM_CREATE_LIMIT)
                                            onOpenPaywall()
                                        }
                                    }
                                }
                            } else {
                                val finalDate = selectedDate ?: return@launch
                                val finalTime = selectedStartTime ?: return@launch
                                val finalTimeLabel = DateTimeFormatterHelper.formatMeetSummary(
                                    context = context,
                                    date = finalDate,
                                    startTime = finalTime,
                                    durationMinutes = selectedDurationMinutes
                                )
                                val scheduledAt = LocalDateTime.of(finalDate, finalTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()

                                meetViewModel.updateMeet(
                                    meetId = editMeetId!!,
                                    title = title.trim(),
                                    description = description.trim(),
                                    locationName = locationName.trim(),
                                    latitude = latitude,
                                    longitude = longitude,
                                    scheduledAt = scheduledAt,
                                    timeLabel = finalTimeLabel,
                                    purpose = selectedPurpose,
                                    maxParticipants = maxParticipants,
                                    brewingType = selectedBrewingType,
                                    businessOffer = null
                                )
                                onBack()
                            }
                        }
                    }
                )
                if (!canSubmit) {
                    val blockedTapInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(26.dp))
                            .clickable(
                                interactionSource = blockedTapInteraction,
                                indication = null
                            ) {
                                getValidationFeedback()?.let(showValidationFeedback)
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            snackbar = { snackbarData ->
                Snackbar(
                    containerColor = Color(0xFF2A1A12),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_coffinity_logo),
                            contentDescription = "Coffinity",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = snackbarData.visuals.message,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        )
    }

    if (showDatePicker) {
        PlatformDatePickerDialog(
            initialDate = selectedDate ?: LocalDate.now(),
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { chosen ->
                selectedDate = chosen
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        PlatformTimePickerDialog(
            initialTime = selectedStartTime ?: LocalTime.of(14, 0),
            onDismissRequest = { showTimePicker = false },
            onTimeSelected = { chosen ->
                selectedStartTime = chosen
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun referenceFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    focusedTextColor = MeetInputCream,
    unfocusedTextColor = MeetInputCream,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    cursorColor = MeetHeadingCream
)

private fun Modifier.referenceFieldRim(shape: RoundedCornerShape = RoundedCornerShape(12.dp)): Modifier {
    return this
        .clip(shape)
        .background(Color(0x4D140A05), shape)
        .drawWithContent {
            drawContent()
            val radius = 12.dp.toPx()
            val topEdge = 1.dp.toPx()
            val specular = 1.5.dp.toPx()
            drawRoundRect(
                color = Color(0x99FFDC96),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, topEdge),
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color(0xDDFFCC80), Color.Transparent),
                    startX = 0f,
                    endX = size.width
                ),
                topLeft = Offset(14.dp.toPx(), 0f),
                size = Size(size.width - 28.dp.toPx(), specular),
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x33FFD7A1), Color.Transparent),
                    startY = 0f,
                    endY = 10.dp.toPx()
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, 10.dp.toPx()),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
}

@Composable
private fun ReferenceFieldIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color(0xFFF0B36A),
        modifier = Modifier
            .size(17.dp)
            .shadow(
                elevation = 5.dp,
                shape = CircleShape,
                ambientColor = Color(0xAAFF8C00),
                spotColor = Color(0xAAFF8C00)
            )
    )
}

@Composable
private fun ReferenceMeetCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 15.dp,
                shape = RoundedCornerShape(19.dp),
                ambientColor = Color(0x44A75D2C),
                spotColor = Color(0x44A75D2C)
            )
            .clip(RoundedCornerShape(19.dp))
            .background(
                Brush.verticalGradient(
                    listOf(MeetCardFill, Color(0x664A2413), MeetCardFill)
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x80FFC880),
                        Color(0x33FFFFFF),
                        Color.Transparent,
                        Color(0x22FFCC80)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(920f, 920f)
                ),
                RoundedCornerShape(19.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0x66140A05))
                .blur(35.dp)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x2DA05A2D),
                            Color.Transparent,
                            Color(0x185A2C17)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x59FFDC96), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color(0xCCFFCC80), Color.Transparent)
                    )
                )
                .blur(1.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x2EA05A2D), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(120f, 50f),
                        radius = 280f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x1FE29B57), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(520f, 140f),
                        radius = 260f
                    )
                )
        )
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            content = content
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, Color(0x2CE7C39B), RoundedCornerShape(19.dp))
        )
    }
}

@Composable
private fun ReferencePillButton(
    modifier: Modifier = Modifier,
    text: String,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(33.dp)
            .shadow(
                elevation = if (emphasized) 6.dp else 2.dp,
                shape = RoundedCornerShape(17.dp),
                ambientColor = Color(0x2AE2A15D),
                spotColor = Color(0x2AE2A15D)
            )
            .clip(RoundedCornerShape(17.dp))
            .background(
                if (emphasized) {
                    Brush.verticalGradient(
                        listOf(Color(0x669C562B), Color(0x556D3418), Color(0x444B210E))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0x465B2E19), Color(0x305B2E19))
                    )
                }
            )
            .border(
                1.dp,
                if (emphasized) Color(0x88D8A16A) else Color(0x63D8A16A),
                RoundedCornerShape(17.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            color = MeetHeadingCream
        )
    }
}

@Composable
private fun ReferencePurposeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .shadow(
                elevation = if (selected) 5.dp else 1.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color(0x22E2A15D),
                spotColor = Color(0x22E2A15D)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    Brush.horizontalGradient(
                        listOf(Color(0xFF8F4F28), Color(0xFF7A3D1D), Color(0xFF6C3318))
                    )
                } else {
                    Brush.horizontalGradient(listOf(Color(0x475B2E19), Color(0x315B2E19)))
                }
            )
            .border(
                1.dp,
                if (selected) Color(0x8CE2A15D) else Color(0x57D8A16A),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x55FFA500), Color(0x22FFA500), Color.Transparent),
                            center = Offset(40f, 8f),
                            radius = 80f
                        )
                    )
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (selected) 6.dp else 0.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0x55F2B56A))
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    shadow = if (selected) MeetTextShadow else null
                ),
                color = if (selected) MeetHeadingCream else Color(0xFFE4CCAF),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ReferenceMiniActionChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .shadow(
                elevation = 5.dp,
                shape = CircleShape,
                ambientColor = Color(0x28E2A15D),
                spotColor = Color(0x28E2A15D)
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF8F4F28), Color(0xFF6C3318))
                )
            )
            .border(1.dp, Color(0x7CE2A15D), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalCafe,
            contentDescription = null,
            tint = MeetHeadingCream,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun PremiumParticipantStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int
) {
    Row(
        modifier = Modifier
            .width(130.dp)
            .height(35.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0x30A75D2C),
                spotColor = Color(0x30A75D2C)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF9C562B), Color(0xFF6D3418), Color(0xFF4B210E))
                )
            )
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x2EF2B56A), Color.Transparent)
                )
            )
            .border(1.dp, Color(0x7AD8A16A), RoundedCornerShape(18.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            IconButton(
                modifier = Modifier.size(24.dp),
                onClick = { if (value > minValue) onValueChange(value - 1) },
                enabled = value > minValue
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    tint = MeetHeadingCream,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = pluralStringResource(R.plurals.create_meet_participants_count, value, value),
            style = MaterialTheme.typography.labelMedium.copy(shadow = MeetTextShadow),
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF3E4D1)
        )

        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = Color(0xFFEBC99F)
        ) {
            IconButton(
                modifier = Modifier.size(24.dp),
                onClick = { if (value < maxValue) onValueChange(value + 1) },
                enabled = value < maxValue
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF6D3316),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumCreateMeetButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val pulse by rememberInfiniteTransition(label = "ctaPulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ctaPulseAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 30.dp,
                shape = RoundedCornerShape(26.dp),
                clip = false,
                ambientColor = Color(0x66FF8C00),
                spotColor = Color(0x66FF8C00)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (enabled) {
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFFFF9D00),
                            0.5f to Color(0xFFD2691E),
                            1f to Color(0xFF8B4513)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xAA9A5B2D), Color(0xAA6C3518), Color(0xAA4A210F))
                    )
                }
            )
            .border(1.dp, Color(0x85E2A15D), RoundedCornerShape(26.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x7AF0A85E), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .graphicsLayer(alpha = pulse)
                .blur(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xEEFFFFFF),
                            Color(0xFFFFC875),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .graphicsLayer(alpha = pulse * 0.8f)
                .blur(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFFD89C),
                            Color(0xFFFF8C00),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x14FFFFFF), Color.Transparent, Color(0x12FFFFFF))
                    )
                )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(shadow = MeetTextShadow),
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF7E7D2)
        )
    }
}

@Composable
private fun PlatformDatePickerDialog(
    initialDate: LocalDate,
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(initialDate, context) {
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        )
        dialog.setOnCancelListener { onDismissRequest() }
        dialog.setOnDismissListener { onDismissRequest() }
        dialog.show()

        onDispose {
            dialog.dismiss()
        }
    }
}

@Composable
private fun PlatformTimePickerDialog(
    initialTime: LocalTime,
    onDismissRequest: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(initialTime, context) {
        val dialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onTimeSelected(LocalTime.of(hourOfDay, minute))
            },
            initialTime.hour,
            initialTime.minute,
            android.text.format.DateFormat.is24HourFormat(context)
        )
        dialog.setOnCancelListener { onDismissRequest() }
        dialog.setOnDismissListener { onDismissRequest() }
        dialog.show()

        onDispose {
            dialog.dismiss()
        }
    }
}

private enum class MapsLaunchResult {
    LAUNCHED,
    EMPTY_QUERY,
    FAILED
}

private fun launchGoogleMapsSearch(context: Context, query: String): MapsLaunchResult {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return MapsLaunchResult.EMPTY_QUERY

    val encodedQuery = Uri.encode(normalizedQuery)
    val packageManager = context.packageManager

    val mapsAppIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("geo:0,0?q=$encodedQuery")
    ).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (mapsAppIntent.resolveActivity(packageManager) != null) {
        return try {
            context.startActivity(mapsAppIntent)
            MapsLaunchResult.LAUNCHED
        } catch (_: Throwable) {
            MapsLaunchResult.FAILED
        }
    }

    val browserFallbackIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
    )
    if (browserFallbackIntent.resolveActivity(packageManager) != null) {
        return try {
            context.startActivity(browserFallbackIntent)
            MapsLaunchResult.LAUNCHED
        } catch (_: Throwable) {
            MapsLaunchResult.FAILED
        }
    }

    return MapsLaunchResult.FAILED
}