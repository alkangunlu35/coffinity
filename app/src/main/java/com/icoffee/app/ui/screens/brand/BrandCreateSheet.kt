package com.icoffee.app.ui.screens.brand

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.icoffee.app.R
import com.icoffee.app.data.model.BrandLifecycleStatus
import com.icoffee.app.ui.components.CoffeeFeedbackCard
import com.icoffee.app.ui.components.CoffeeFeedbackTone
import com.icoffee.app.ui.components.CoffeeLabelText
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.ui.components.coffeeFieldTextStyle
import com.icoffee.app.ui.components.coffeeOutlinedTextFieldColors
import com.icoffee.app.ui.theme.CoffeeColorTokens
import com.icoffee.app.ui.theme.CoffeeSpace
import com.icoffee.app.viewmodel.BrandAdminViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandCreateSheet(
    viewModel: BrandAdminViewModel,
    onDismiss: () -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var website by rememberSaveable { mutableStateOf("") }
    var instagram by rememberSaveable { mutableStateOf("") }
    var feedbackRes by rememberSaveable { mutableStateOf<Int?>(null) }
    val fieldTextStyle = coffeeFieldTextStyle()
    val fieldColors = coffeeOutlinedTextFieldColors()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CoffeeColorTokens.surfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CoffeeSpace.lg, vertical = CoffeeSpace.md),
            verticalArrangement = Arrangement.spacedBy(CoffeeSpace.md)
        ) {
            Text(
                text = stringResource(R.string.brand_admin_create_brand),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = CoffeeColorTokens.textPrimary
            )
            Text(
                text = stringResource(R.string.brand_admin_create_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = CoffeeColorTokens.textSecondary
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    feedbackRes = null
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = fieldTextStyle,
                colors = fieldColors,
                label = { CoffeeLabelText(stringResource(R.string.brand_field_brand_name)) },
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    feedbackRes = null
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = fieldTextStyle,
                colors = fieldColors,
                label = { CoffeeLabelText(stringResource(R.string.brand_field_description_optional)) },
                minLines = 3,
                maxLines = 4
            )

            Row(horizontalArrangement = Arrangement.spacedBy(CoffeeSpace.sm)) {
                OutlinedTextField(
                    value = country,
                    onValueChange = {
                        country = it
                        feedbackRes = null
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = fieldTextStyle,
                    colors = fieldColors,
                    label = { CoffeeLabelText(stringResource(R.string.brand_field_country_optional)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = {
                        city = it
                        feedbackRes = null
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = fieldTextStyle,
                    colors = fieldColors,
                    label = { CoffeeLabelText(stringResource(R.string.brand_suggest_city_optional)) },
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = website,
                onValueChange = {
                    website = it
                    feedbackRes = null
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = fieldTextStyle,
                colors = fieldColors,
                label = { CoffeeLabelText(stringResource(R.string.brand_suggest_website_optional)) },
                singleLine = true
            )

            OutlinedTextField(
                value = instagram,
                onValueChange = {
                    instagram = it
                    feedbackRes = null
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = fieldTextStyle,
                colors = fieldColors,
                label = { CoffeeLabelText(stringResource(R.string.brand_suggest_instagram_optional)) },
                singleLine = true
            )

            if (feedbackRes != null) {
                CoffeeFeedbackCard(
                    message = stringResource(feedbackRes!!),
                    tone = if (feedbackRes == R.string.brand_import_save_success) {
                        CoffeeFeedbackTone.Success
                    } else {
                        CoffeeFeedbackTone.Error
                    }
                )
            }

            PrimaryButton(
                text = stringResource(R.string.brand_import_brand_save_action),
                onClick = {
                    scope.launch {
                        val result = viewModel.createBrand(
                            name = name,
                            description = description,
                            country = country,
                            city = city,
                            website = website,
                            instagram = instagram,
                            logoUrl = "",
                            coverImageUrl = "",
                            sourceUrl = "",
                            status = BrandLifecycleStatus.ACTIVE.storageValue
                        )
                        feedbackRes = if (result.isSuccess) {
                            name = ""
                            description = ""
                            country = ""
                            city = ""
                            website = ""
                            instagram = ""
                            R.string.brand_import_save_success
                        } else {
                            val message = result.exceptionOrNull()?.message.orEmpty()
                            if (message.startsWith("duplicate:")) {
                                R.string.brand_error_duplicate_existing
                            } else if (message == "unauthorized") {
                                R.string.brand_management_error_unauthorized
                            } else {
                                R.string.brand_import_error_save
                            }
                        }
                    }
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.padding(top = CoffeeSpace.xs, bottom = CoffeeSpace.md)
            )
        }
    }
}
