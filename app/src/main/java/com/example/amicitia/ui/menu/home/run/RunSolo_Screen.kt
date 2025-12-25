package com.example.amicitia.ui.menu.home.run

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.max

// =========================
// Theme
// =========================
private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

private val GlassDark = Color(0x2BFFFFFF)
private val TextPrimary = Color.White
private val TextMuted = Color(0xB3FFFFFF)
private val TextSubtle = Color(0x80FFFFFF)

// 跟 Home 的 SolidColorCard 一致
private val SolidGray = Color(0xFF2A2A2A)
private val SolidBlack = Color(0xFF000000)

// =========================
// State
// =========================
enum class RunState { IDLE, RUNNING, PAUSED }

data class SoloRunUiState(
    val distanceKm: Double = 0.0,
    val elapsedMillis: Long = 0L,
    val steps: Int = 0,
    val state: RunState = RunState.IDLE,
    val route: List<LatLng> = emptyList(),
    val lastLatLng: LatLng? = null
)

// =========================
// ViewModel
// =========================
class SoloRunViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var timerJob: Job? = null
    private var accumulatedTime = 0L
    private var startTick = 0L

    private var totalDistanceMeters = 0f
    private var lastLocation: android.location.Location? = null

    private var baseStepCount: Int? = null
    private var stepListener: SensorEventListener? = null

    private val _uiState = MutableStateFlow(SoloRunUiState())
    val uiState: StateFlow<SoloRunUiState> = _uiState

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            if (loc.accuracy > 25f) return

            lastLocation?.let {
                val d = it.distanceTo(loc)
                if (d >= 1.5f) totalDistanceMeters += d
            }
            lastLocation = loc

            val p = LatLng(loc.latitude, loc.longitude)
            _uiState.update {
                it.copy(
                    distanceKm = totalDistanceMeters / 1000.0,
                    route = it.route + p,
                    lastLatLng = p
                )
            }
        }
    }

    fun onStart() {
        totalDistanceMeters = 0f
        lastLocation = null
        accumulatedTime = 0L
        baseStepCount = null
        _uiState.value = SoloRunUiState(state = RunState.RUNNING)
        startTimer()
    }

    fun onPause() {
        stopTimer(true)
        stopLocationUpdates()
        stopStepCounter()
        _uiState.update { it.copy(state = RunState.PAUSED) }
    }

    fun onResume() {
        _uiState.update { it.copy(state = RunState.RUNNING) }
        startTimer()
    }

    fun onStop() {
        stopTimer(false)
        stopLocationUpdates()
        stopStepCounter()
        _uiState.value = SoloRunUiState()
    }

    private fun startTimer() {
        timerJob?.cancel()
        startTick = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (_uiState.value.state == RunState.RUNNING) {
                val elapsed = accumulatedTime + (System.currentTimeMillis() - startTick)
                _uiState.update { it.copy(elapsedMillis = elapsed) }
                delay(1000)
            }
        }
    }

    private fun stopTimer(keep: Boolean) {
        timerJob?.cancel()
        timerJob = null
        accumulatedTime = if (keep) _uiState.value.elapsedMillis else 0L
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        ).setMinUpdateIntervalMillis(500L).build()
        fusedClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        fusedClient.removeLocationUpdates(locationCallback)
    }

    fun startStepCounter() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        if (stepListener != null) return

        stepListener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val raw = e.values[0].toInt()
                if (baseStepCount == null) baseStepCount = raw
                _uiState.update { it.copy(steps = max(0, raw - baseStepCount!!)) }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(stepListener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopStepCounter() {
        stepListener?.let { sensorManager.unregisterListener(it) }
        stepListener = null
    }
}

// =========================
// UI
// =========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSoloScreen(navController: NavController) {

    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: SoloRunViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(c: Class<T>): T =
            SoloRunViewModel(app) as T
    })
    val ui by vm.uiState.collectAsState()

    var hasPermission by remember { mutableStateOf(context.hasAnyLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = context.hasAnyLocationPermission()
        if (hasPermission && ui.state == RunState.RUNNING) {
            vm.startLocationUpdates()
            vm.startStepCounter()
        }
    }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(25.0330, 121.5654), 16f)
    }

    var followMode by remember { mutableStateOf(true) }

    LaunchedEffect(ui.lastLatLng, followMode) {
        val last = ui.lastLatLng ?: return@LaunchedEffect
        if (!followMode) return@LaunchedEffect
        cameraState.position = CameraPosition.fromLatLngZoom(last, 17f)
    }

    LaunchedEffect(cameraState.isMoving) {
        if (cameraState.isMoving && followMode) followMode = false
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("SOLO 跑步", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(BgDark)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {

            // 1) 儀表板：改成 SolidCard（你截圖那塊也要）
            DashboardCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                ui = ui,
                gpsText = gpsText(hasPermission, ui.lastLatLng)
            )

            // 2) 地圖：不要卡片化
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f)
                    .shadow(18.dp, RoundedCornerShape(28.dp), clip = false)
                    .clip(
                        RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraState,
                    properties = MapProperties(isMyLocationEnabled = hasPermission),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = false,
                        mapToolbarEnabled = false
                    )
                ) {
                    if (ui.route.size >= 2) Polyline(points = ui.route, width = 10f)
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallRoundFab(
                        icon = Icons.Filled.MyLocation,
                        onClick = {
                            val last = ui.lastLatLng
                            if (last != null) {
                                followMode = true
                                cameraState.position = CameraPosition.fromLatLngZoom(last, 17f)
                            }
                        }
                    )
                    SmallRoundFab(
                        icon = if (followMode) Icons.Filled.GpsFixed else Icons.Filled.GpsNotFixed,
                        active = followMode,
                        onClick = { followMode = !followMode }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 3) 底部控制列：也改成 SolidCard（你截圖那塊也要）
            ModernControlBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = ui.state,
                gpsReady = hasPermission && ui.lastLatLng != null,
                onPrimary = {
                    when (ui.state) {
                        RunState.IDLE -> {
                            vm.onStart()
                            if (hasPermission) {
                                vm.startLocationUpdates()
                                vm.startStepCounter()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                        RunState.RUNNING -> vm.onPause()
                        RunState.PAUSED -> {
                            vm.onResume()
                            if (hasPermission) {
                                vm.startLocationUpdates()
                                vm.startStepCounter()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    }
                },
                onStop = { vm.onStop() }
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

// =========================
// Solid 卡片（完全照你 Home 的 SolidColorCard：shadow + clip + 無 ripple）
// =========================
@Composable
private fun SolidColorCard(
    modifier: Modifier = Modifier,
    cornerDp: Dp = 24.dp,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerDp)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// =========================
// DashboardCard：改成 SolidGray（你截圖那張也要一樣）
// =========================
@Composable
private fun DashboardCard(
    modifier: Modifier,
    ui: SoloRunUiState,
    gpsText: String
) {
    SolidColorCard(
        modifier = modifier,
        cornerDp = 24.dp,
        contentPadding = 16.dp,
        backgroundColor = SolidGray
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatMillisHMS(ui.elapsedMillis),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text("Time", fontSize = 12.sp, color = TextSubtle)
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%.2f", ui.distanceKm),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("km", fontSize = 14.sp, color = TextMuted)
                    }
                    Text("Distance", fontSize = 12.sp, color = TextSubtle)
                }

                Spacer(Modifier.width(12.dp))

                Surface(
                    color = SolidBlack,
                    shape = RoundedCornerShape(999.dp),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = when (ui.state) {
                            RunState.IDLE -> "待機"
                            RunState.RUNNING -> "進行中"
                            RunState.PAUSED -> "已暫停"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Steps：${ui.steps}", fontSize = 13.sp, color = TextMuted)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// =========================
// FAB（玻璃）
// =========================
@Composable
private fun SmallRoundFab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) PrimaryBlue.copy(alpha = 0.20f) else GlassDark,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) PrimaryBlue else TextPrimary
            )
        }
    }
}

// =========================
// Solid 按鈕（照你 Home 的 SolidColorCard）
// =========================
@Composable
private fun SolidActionButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SolidBlack,
    cornerDp: Dp = 999.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerDp)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun SolidIconButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SolidBlack,
    size: Dp = 46.dp,
    cornerDp: Dp = 999.dp,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    SolidActionButton(
        modifier = modifier.size(size),
        backgroundColor = backgroundColor,
        cornerDp = cornerDp,
        contentPadding = PaddingValues(0.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { icon() }
    }
}

// =========================
// Control Bar：整張改成 SolidGray（你截圖那張也要一樣）
// =========================
@Composable
private fun ModernControlBar(
    modifier: Modifier,
    state: RunState,
    gpsReady: Boolean,
    onPrimary: () -> Unit,
    onStop: () -> Unit
) {
    SolidColorCard(
        modifier = modifier,
        cornerDp = 24.dp,
        contentPadding = 16.dp,
        backgroundColor = SolidGray
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (state) {
                        RunState.IDLE -> "準備開始跑步"
                        RunState.RUNNING -> "跑步中"
                        RunState.PAUSED -> "已暫停"
                    },
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = when (state) {
                        RunState.IDLE ->
                            if (gpsReady) "GPS 已就緒｜建議開始記錄" else "GPS 搜尋中｜建議等定位穩定再開始"
                        RunState.RUNNING -> "滑動地圖可解除跟隨"
                        RunState.PAUSED -> "按 Resume 繼續記錄"
                    },
                    fontSize = 12.sp,
                    color = TextSubtle
                )
            }

            Spacer(Modifier.weight(1f))

            val (primaryIcon, primaryText) = when (state) {
                RunState.IDLE -> Icons.Filled.PlayArrow to "Start"
                RunState.RUNNING -> Icons.Filled.Pause to "Pause"
                RunState.PAUSED -> Icons.Filled.PlayArrow to "Resume"
            }

            SolidActionButton(
                backgroundColor = SolidBlack,
                onClick = onPrimary
            ) {
                Icon(primaryIcon, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(primaryText, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            if (state != RunState.IDLE) {
                Spacer(Modifier.width(10.dp))
                SolidIconButton(
                    backgroundColor = SolidBlack,
                    onClick = onStop
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

// =========================
// Utils
// =========================
private fun formatMillisHMS(ms: Long): String {
    val totalSec = (ms / 1000L).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

private fun gpsText(has: Boolean, last: LatLng?) =
    when {
        !has -> "GPS：未授權"
        last == null -> "GPS：搜尋中"
        else -> "GPS：良好"
    }

private fun Context.hasAnyLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED