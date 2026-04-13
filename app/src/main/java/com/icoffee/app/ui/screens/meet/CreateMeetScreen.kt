package com.icoffee.app.ui.screens.meet

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icoffee.app.R
import com.icoffee.app.data.growth.GrowthAnalytics
import com.icoffee.app.data.growth.GrowthEventNames
import com.icoffee.app.data.model.BusinessOffer
import com.icoffee.app.data.model.IncludedOfferItem
import com.icoffee.app.data.model.OfferPaymentMode
import com.icoffee.app.localization.formatter.DateTimeFormatterHelper
import com.icoffee.app.localization.formatter.TimeFormatter
import com.icoffee.app.ui.components.MeetPurposeChip
import com.icoffee.app.ui.components.ParticipantStepper
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.viewmodel.MeetCreateAttemptResult
import com.icoffee.app.viewmodel.MeetCreateFailureReason
import com.icoffee.app.viewmodel.MeetViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateMeetScreen(
    onBack: () -> Unit,
    isUserSignedIn: Boolean,
    onRequestSignIn: () -> Unit,
    onOpenPaywall: () -> Unit,
    meetViewModel: MeetViewModel = viewModel(),
    editMeetId: String? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val isEditMode = editMeetId != null
    val existingMeet = remember(editMeetId) { editMeetId?.let { meetViewModel.findMeet(it) } }

    var title by remember { mutableStateOf(existingMeet?.title ?: "") }
    var description by remember { mutableStateOf(existingMeet?.description ?: "") }
    var locationName by remember { mutableStateOf(existingMeet?.locationName ?: "") }
    var latitude by remember { mutableDoubleStateOf(existingMeet?.latitude ?: 0.0) }
    var longitude by remember { mutableDoubleStateOf(existingMeet?.longitude ?: 0.0) }
    
    var selectedDate by remember { mutableStateOf<LocalDate?>(existingMeet?.scheduledAt?.let { 
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), ZoneId.systemDefault()).toLocalDate()
    } ?: LocalDate.now()) }
    
    var selectedStartTime by remember { mutableStateOf<LocalTime?>(existingMeet?.scheduledAt?.let {
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), ZoneId.systemDefault()).toLocalTime()
    } ?: LocalTime.of(14, 0)) }
    
    var selectedDurationMinutes by remember { mutableIntStateOf(60) }
    var selectedPurpose by remember { mutableStateOf(existingMeet?.purpose ?: "") }
    var maxParticipants by remember { mutableIntStateOf(existingMeet?.maxParticipants ?: 4) }
    var selectedBrewingType by remember { mutableStateOf(existingMeet?.brewingType ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Business Offer state
    var offerEnabled by remember { mutableStateOf(existingMeet?.businessOffer != null) }
    var offerTitle by remember { mutableStateOf(existingMeet?.businessOffer?.offerTitle ?: "") }
    var offerDescription by remember { mutableStateOf(existingMeet?.businessOffer?.offerDescription ?: "") }
    var offerPriceAmount by remember { mutableStateOf<Double?>(existingMeet?.businessOffer?.priceAmount) }
    var offerCurrency by remember { mutableStateOf(existingMeet?.businessOffer?.currency ?: "TRY") }
    var offerPaymentMode by remember { mutableStateOf(existingMeet?.businessOffer?.paymentMode ?: OfferPaymentMode.PAY_AT_VENUE) }
    var offerAvailabilityLimit by remember { mutableStateOf<Int?>(existingMeet?.businessOffer?.availabilityLimit) }
    val offerItems = remember { mutableStateListOf<IncludedOfferItem>().apply { 
        existingMeet?.businessOffer?.includedItems?.let { addAll(it) }
    } }

    val canSubmit = title.isNotBlank() && locationName.isNotBlank() && selectedPurpose.isNotBlank()
    val hasLocationQuery = locationName.trim().isNotEmpty()
    val selectedDateLabel = remember(selectedDate, configuration) {
        selectedDate?.let { DateTimeFormatterHelper.formatMeetDateOption(context, it) }
    }
    val selectedTimeLabel = remember(selectedStartTime, configuration) {
        selectedStartTime?.let { TimeFormatter.format(context, it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = stringResource(if (isEditMode) R.string.meet_edit_title else R.string.meet_create_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.meet_field_title)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.meet_field_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = locationName,
                onValueChange = {
                    if (locationName != it) {
                        latitude = 0.0
                        longitude = 0.0
                    }
                    locationName = it
                },
                label = { Text(stringResource(R.string.meet_field_location)) },
                placeholder = { Text(stringResource(R.string.meet_field_location_picker_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    enabled = hasLocationQuery,
                    onClick = {
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
                ) {
                    Text(stringResource(R.string.meet_open_in_maps))
                }
            }

            Text(
                text = stringResource(R.string.meet_field_time),
                style = MaterialTheme.typography.titleSmall
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        selectedDateLabel
                            ?: stringResource(R.string.meet_time_picker_step_date)
                    )
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        selectedTimeLabel
                            ?: stringResource(R.string.meet_time_picker_step_time)
                    )
                }
            }
            Text(
                text = stringResource(R.string.meet_field_time_picker_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(stringResource(R.string.meet_field_purpose), style = MaterialTheme.typography.titleSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val purposes = listOf("Chill", "Productive", "Deep Talk", "Networking")
                purposes.forEach { purpose ->
                    MeetPurposeChip(
                        label = purpose,
                        selected = selectedPurpose == purpose,
                        onClick = { selectedPurpose = purpose }
                    )
                }
            }

            Text(stringResource(R.string.meet_field_max_participants), style = MaterialTheme.typography.titleSmall)
            ParticipantStepper(
                value = maxParticipants,
                onValueChange = { maxParticipants = it },
                minValue = 2,
                maxValue = meetViewModel.maxAllowedParticipants()
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = stringResource(if (isEditMode) R.string.meet_save_changes else R.string.meet_create),
                enabled = canSubmit,
                onClick = {
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
                                businessOffer = normalizedOffer
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
                                            context.getString(R.string.meet_entitlement_capacity_limit_reached, meetViewModel.maxAttendees)
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
                            // Update logic
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
        }
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
