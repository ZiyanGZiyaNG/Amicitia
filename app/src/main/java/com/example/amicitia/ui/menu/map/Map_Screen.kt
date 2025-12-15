package com.example.amicitia.ui.menu.map

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.Locale

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var query by remember { mutableStateOf("") }
    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(25.0330, 121.5654), // 初始台北
            11f
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fine || coarse

        if (hasLocationPermission) {
            scope.launch {
                val ok = moveToCurrentLocation(
                    context = context,
                    fusedClient = fusedClient,
                    cameraState = cameraState,
                    onLatLng = { markerPosition = it },
                    onError = { snackbarMessage = it }
                )
                if (!ok) snackbarMessage = "目前無法取得位置（可能尚未有定位資料或定位服務關閉）。"
            }
        } else {
            snackbarMessage = "未授權定位權限，無法定位。"
        }
    }

    LaunchedEffect(Unit) {
        hasLocationPermission = context.hasAnyLocationPermission()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            ),
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            )
        ) {
            markerPosition?.let {
                Marker(
                    state = MarkerState(it),
                    title = "位置"
                )
            }
        }

        // 搜尋列
        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(999.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜尋地點…") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (query.isBlank()) return@KeyboardActions
                        scope.launch {
                            val latLng = geocodeFirst(geocoder, query)
                            if (latLng != null) {
                                markerPosition = latLng
                                cameraState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                            } else {
                                snackbarMessage = "找不到地點：$query"
                            }
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 定位按鈕：真正定位
        FloatingActionButton(
            onClick = {
                if (!context.isLocationEnabled()) {
                    snackbarMessage = "請先開啟系統定位服務（GPS/定位）。"
                    return@FloatingActionButton
                }

                if (!hasLocationPermission) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } else {
                    scope.launch {
                        val ok = moveToCurrentLocation(
                            context = context,
                            fusedClient = fusedClient,
                            cameraState = cameraState,
                            onLatLng = { markerPosition = it },
                            onError = { snackbarMessage = it }
                        )
                        if (!ok) snackbarMessage = "目前無法取得位置（可能尚未有定位資料）。"
                    }
                }
            },
            containerColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.MyLocation,
                contentDescription = "定位",
                tint = Color(0xFF4F46E5)
            )
        }

        // Snackbar（提示）
        snackbarMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
            ) {
                Snackbar(
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("關閉")
                        }
                    }
                ) { Text(msg) }
            }
        }
    }
}

private suspend fun moveToCurrentLocation(
    context: Context,
    fusedClient: com.google.android.gms.location.FusedLocationProviderClient,
    cameraState: CameraPositionState,
    onLatLng: (LatLng) -> Unit,
    onError: (String) -> Unit
): Boolean = withContext(Dispatchers.Main) {
    try {
        // 1) 先試 lastLocation（最快，但可能為 null）
        val loc = fusedClient.lastLocation.await()
        if (loc != null) {
            val latLng = LatLng(loc.latitude, loc.longitude)
            onLatLng(latLng)
            cameraState.position = CameraPosition.fromLatLngZoom(latLng, 16f)
            return@withContext true
        }

        // 2) 再試 currentLocation（較可靠）
        val cur = fusedClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .await()

        if (cur != null) {
            val latLng = LatLng(cur.latitude, cur.longitude)
            onLatLng(latLng)
            cameraState.position = CameraPosition.fromLatLngZoom(latLng, 16f)
            return@withContext true
        }

        false
    } catch (e: SecurityException) {
        onError("定位權限不足：${e.message ?: "SecurityException"}")
        false
    } catch (e: Exception) {
        onError("取得位置失敗：${e.message ?: "Unknown error"}")
        false
    }
}

private suspend fun geocodeFirst(geocoder: Geocoder, query: String): LatLng? =
    withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocationName(query, 1)
            val loc = results?.firstOrNull() ?: return@withContext null
            LatLng(loc.latitude, loc.longitude)
        } catch (_: Exception) {
            null
        }
    }

private fun Context.hasAnyLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private fun Context.isLocationEnabled(): Boolean {
    val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        lm.isLocationEnabled
    } else {
        @Suppress("DEPRECATION")
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}