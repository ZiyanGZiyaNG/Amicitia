package com.example.amicitia.ui.menu.map

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.*

@Composable
fun MapScreen() {

    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    var query by remember { mutableStateOf("") }
    var markerPosition by remember { mutableStateOf<LatLng?>(null) }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(25.0330, 121.5654), // 台北
            11f
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        /* ---------- Google Map（真正滿版） ---------- */
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            )
        ) {
            markerPosition?.let {
                Marker(
                    state = MarkerState(it),
                    title = "搜尋結果"
                )
            }
        }

        /* ---------- 搜尋列（單一、不重疊、圓角） ---------- */
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
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (query.isBlank()) return@KeyboardActions
                        try {
                            @Suppress("DEPRECATION")
                            val result = geocoder.getFromLocationName(query, 1)
                            if (!result.isNullOrEmpty()) {
                                val loc = result[0]
                                val latLng = LatLng(loc.latitude, loc.longitude)
                                markerPosition = latLng
                                cameraState.position =
                                    CameraPosition.fromLatLngZoom(latLng, 15f)
                            }
                        } catch (_: Exception) {}
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

        /* ---------- 定位按鈕（不跟 bottom bar 打架） ---------- */
        FloatingActionButton(
            onClick = {
                cameraState.position =
                    CameraPosition.fromLatLngZoom(
                        LatLng(25.0330, 121.5654),
                        14f
                    )
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
    }
}