package com.icoffee.app.ui.screens.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UpgradePaywallScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A120B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Premium Yakında",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFF5E6D3)
                )

                Text(
                    text = "Premium ve Business planları yakında geliyor. Şu anda herkes ücretsiz planı kullanıyor — limitlere takıldıysan bize geri bildirim yollayabilirsin.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD7C2A8)
                )

                PlanCard(
                    title = "FREE",
                    features = listOf(
                        "Aylık 4 meet katılım",
                        "Aylık 1 meet oluşturma",
                        "Maksimum 10 katılımcı"
                    ),
                    isActive = true
                )

                PlanCard(
                    title = "PREMIUM",
                    features = listOf(
                        "Sınırsız meet katılım",
                        "Aylık 10 meet oluşturma",
                        "Maksimum 20 katılımcı"
                    ),
                    highlight = true
                )

                PlanCard(
                    title = "BUSINESS",
                    features = listOf(
                        "Sınırsız meet katılım",
                        "Sınırsız meet oluşturma",
                        "Maksimum 100 katılımcı",
                        "İşletme odaklı büyüme avantajları"
                    )
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryPaywallButton(
                    text = "Premium’a Geç",
                    onClick = {
                        // Billing entegrasyonu geldiğinde buraya bağlanacak
                    }
                )

                Text(
                    text = "Şimdilik geri dön",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onBack() },
                    color = Color(0xFFBCA58E),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    features: List<String>,
    isActive: Boolean = false,
    highlight: Boolean = false
) {
    val backgroundBrush = when {
        highlight -> Brush.horizontalGradient(
            listOf(Color(0xFFB67A4D), Color(0xFFE69A3A))
        )
        isActive -> Brush.horizontalGradient(
            listOf(Color(0xFF6B4F3A), Color(0xFF4E3726))
        )
        else -> Brush.horizontalGradient(
            listOf(Color(0xFF2A1E16), Color(0xFF1F150F))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundBrush, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        features.forEach { feature ->
            Text(
                text = "• $feature",
                color = Color(0xFFEADDCF),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PrimaryPaywallButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFB67A4D), Color(0xFFE69A3A))
                ),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}