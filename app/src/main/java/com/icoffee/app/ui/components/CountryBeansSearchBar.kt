package com.icoffee.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.ui.theme.CreamText
import com.icoffee.app.ui.theme.GoldAccent
import com.icoffee.app.ui.theme.MutedText
import com.icoffee.app.ui.theme.SurfaceDarkAlt
import com.icoffee.app.ui.theme.SurfaceStroke

@Composable
fun CountryBeansSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDarkAlt.copy(alpha = 0.88f), RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceStroke, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = GoldAccent
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = CreamText),
            cursorBrush = SolidColor(GoldAccent),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (query.isBlank()) {
                    Text(
                        text = stringResource(R.string.beans_search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MutedText
                    )
                }
                innerTextField()
            }
        )
    }
}
