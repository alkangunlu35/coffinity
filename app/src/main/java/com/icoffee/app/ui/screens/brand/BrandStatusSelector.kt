package com.icoffee.app.ui.screens.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.data.model.BrandLifecycleStatus
import com.icoffee.app.ui.theme.CoffeeColorTokens
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CoffeeSpace

@Composable
internal fun BrandStatusSelector(
    selectedStatus: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpace.sm)) {
        Text(
            text = stringResource(R.string.brand_management_status),
            style = MaterialTheme.typography.labelLarge,
            color = CoffeeColorTokens.textSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CoffeeSpace.sm)) {
            listOf(
                BrandLifecycleStatus.DRAFT.storageValue to R.string.brand_status_draft,
                BrandLifecycleStatus.ACTIVE.storageValue to R.string.brand_status_active
            ).forEach { (value, labelRes) ->
                Row(
                    modifier = Modifier
                        .clickable { onSelected(value) }
                        .background(
                            color = if (selectedStatus == value) {
                                CoffeeColorTokens.accentSecondary
                            } else {
                                CoffeeColorTokens.surfaceSoft
                            },
                            shape = RoundedCornerShape(CoffeeRadius.pill)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selectedStatus == value) {
                                CoffeeColorTokens.borderStrong
                            } else {
                                CoffeeColorTokens.borderSubtle
                            },
                            shape = RoundedCornerShape(CoffeeRadius.pill)
                        )
                        .padding(horizontal = CoffeeSpace.md, vertical = CoffeeSpace.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedStatus == value) {
                            CoffeeColorTokens.textPrimary
                        } else {
                            CoffeeColorTokens.textSecondary
                        }
                    )
                }
            }
        }
    }
}
