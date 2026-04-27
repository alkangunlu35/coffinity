// FILE: app/src/main/java/com/icoffee/app/ui/location/LocationPickerScreen.kt
// FULL REPLACEMENT

package com.icoffee.app.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun LocationPickerScreen(
    initialLocation: LatLng = LatLng(38.4237, 27.1428),
    onLocationSelected: (Double, Double, String) -> Unit,
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    var selectedLocation by remember {
        mutableStateOf(initialLocation)
    }

    var locationName by remember {
        mutableStateOf("Konum seçiliyor...")
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 14f)
    }

    val hasLocationPermission = remember {
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 🔥 ADRES ÇÖZME
    fun resolveAddress(latLng: LatLng) {
        try {
            val addresses = geocoder.getFromLocation(
                latLng.latitude,
                latLng.longitude,
                1
            )
            if (!addresses.isNullOrEmpty()) {
                locationName = addresses[0].getAddressLine(0) ?: "Seçilen Konum"
            } else {
                locationName = "Seçilen Konum"
            }
        } catch (e: Exception) {
            locationName = "Seçilen Konum"
        }
    }

    // ilk açılışta da çöz
    LaunchedEffect(Unit) {
        resolveAddress(initialLocation)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konum Seç") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Geri")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Text(
                    text = locationName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        onLocationSelected(selectedLocation.latitude, selectedLocation.longitude, locationName)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Konumu Onayla")
                }
            }
        }
    ) { padding ->

        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),

            cameraPositionState = cameraPositionState,

            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            ),

            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                zoomControlsEnabled = true
            ),

            onMapClick = { latLng ->
                selectedLocation = latLng
                resolveAddress(latLng)
            }
        ) {

            Marker(
                state = MarkerState(position = selectedLocation),
                title = locationName
            )
        }
    }
}