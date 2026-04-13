package com.icoffee.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.icoffee.app.data.model.Coffee

@Composable
fun CoffeeCard(
    coffee: Coffee,
    onAdd: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    CoffeeProductCard(
        coffee = coffee,
        onAdd = onAdd,
        onOpen = onOpen,
        modifier = modifier
    )
}
