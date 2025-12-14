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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

// =========================
// 現代化配色（保留你的紫色主題，但更乾淨）
// =========================
private val PrimaryPurple = Color(0xFF4F46E5)
private val PageBg = Color(0xFFF6F1FA)

// 玻璃感：白底微透明
private val Glass = Color(0xEFFFFFFF)
private val GlassSoft = Color(0xCCFFFFFF)

// =========================
// 狀態
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
// ViewModel（定位 + 計時 + 計步）
// =========================
class SoloRunViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context = app.applicationContext
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var timerJob: Job? = null
    private var accumulatedTime: Long = 0L
    private var startTick: Long = 0L

    private var totalDistanceMeters: Float = 0f
    private var lastLocation: android.location.Location? = null

    private var baseStepCount: Int? = null
    private var stepListener: SensorEventListener? = null

    private val _uiState = MutableStateFlow(SoloRunUiState())
    val uiState: StateFlow<SoloRunUiState> = _uiState

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return

            // 精度過差就先不要收點，避免路線亂跳
            val acc = loc.accuracy
            if (acc.isNaN() || acc <= 0f || acc > 25f) return

            val prev = lastLocation
            if (prev != null) {
                val d = prev.distanceTo(loc)
                // 避免原地飄移算距離
                if (d >= 1.5f) totalDistanceMeters += d
            }

            lastLocation = loc
            val p = LatLng(loc.latitude, loc.longitude)

            _uiState.update { s ->
                s.copy(
                    distanceKm = totalDistanceMeters / 1000.0,
                    route = s.route + p,
                    lastLatLng = p
                )
            }
        }
    }

    fun onStart() {
        if (_uiState.value.state == RunState.RUNNING) return

        totalDistanceMeters = 0f
        lastLocation = null
        accumulatedTime = 0L
        baseStepCount = null

        _uiState.value = SoloRunUiState(
            distanceKm = 0.0,
            elapsedMillis = 0L,
            steps = 0,
            state = RunState.RUNNING,
            route = emptyList(),
            lastLatLng = null
        )

        startTimer()
    }

    fun onPause() {
        if (_uiState.value.state != RunState.RUNNING) return
        stopLocationUpdates()
        stopStepCounter()
        stopTimer(keepAccumulated = true)
        _uiState.update { it.copy(state = RunState.PAUSED) }
    }

    fun onResume() {
        if (_uiState.value.state != RunState.PAUSED) return
        _uiState.update { it.copy(state = RunState.RUNNING) }
        startTimer()
    }

    fun onStop() {
        stopLocationUpdates()
        stopStepCounter()
        stopTimer(keepAccumulated = false)
        _uiState.update { it.copy(state = RunState.IDLE) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        startTick = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (_uiState.value.state == RunState.RUNNING) {
                val now = System.currentTimeMillis()
                val elapsed = accumulatedTime + (now - startTick)
                _uiState.update { it.copy(elapsedMillis = elapsed) }
                delay(200L)
            }
        }
    }

    private fun stopTimer(keepAccumulated: Boolean) {
        timerJob?.cancel()
        timerJob = null
        accumulatedTime = if (keepAccumulated) _uiState.value.elapsedMillis else 0L
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (_uiState.value.state != RunState.RUNNING) return

        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(false)
            .build()

        fusedClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        fusedClient.removeLocationUpdates(locationCallback)
    }

    fun startStepCounter() {
        if (_uiState.value.state != RunState.RUNNING) return
        val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) return
        if (stepListener != null) return

        stepListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val raw = event.values.firstOrNull()?.toInt() ?: return
                val base = baseStepCount
                if (base == null) {
                    baseStepCount = raw
                    _uiState.update { it.copy(steps = 0) }
                } else {
                    val steps = max(0, raw - base)
                    _uiState.update { it.copy(steps = steps) }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopStepCounter() {
        val l = stepListener ?: return
        sensorManager.unregisterListener(l)
        stepListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        stopStepCounter()
        timerJob?.cancel()
    }
}

class SoloRunVMFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SoloRunViewModel(app) as T
}

// =========================
// 現代版 UI：地圖主視覺 + 玻璃資訊浮層 + 右下 FAB + 底部控制列
// =========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSoloScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val viewModel: SoloRunViewModel = viewModel(factory = SoloRunVMFactory(app))
    val ui by viewModel.uiState.collectAsState()

    // 權限
    val hasLocationPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val fine = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission.value = fine || coarse

        // 若正在跑步，授權後立刻開始收定位/步數
        if (hasLocationPermission.value && ui.state == RunState.RUNNING) {
            viewModel.startLocationUpdates()
            viewModel.startStepCounter()
        }
    }

    // 地圖相機
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(25.0330, 121.5654), 16f)
    }

    // 跟隨模式（更現代：用 FAB 切換）
    var followMode by remember { mutableStateOf(true) }

    // 跟隨最後位置
    LaunchedEffect(ui.lastLatLng, followMode) {
        val last = ui.lastLatLng ?: return@LaunchedEffect
        if (!followMode) return@LaunchedEffect
        cameraPositionState.position = CameraPosition.fromLatLngZoom(last, 17f)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLocationUpdates()
            viewModel.stopStepCounter()
        }
    }

    Scaffold(
        containerColor = PageBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SOLO 跑步",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBg)
                .padding(padding)
        ) {
            // 1) 地圖主視覺區
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp) // 現代感：地圖做主角
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission.value
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = false
                    )
                ) {
                    if (ui.route.size >= 2) {
                        Polyline(points = ui.route, width = 12f)
                    }
                }

                // 2) 玻璃資訊浮層（現代：資訊收斂成一張卡）
                GlassStatsOverlay(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp),
                    distanceKm = ui.distanceKm,
                    elapsedMillis = ui.elapsedMillis,
                    steps = ui.steps,
                    gpsText = gpsText(hasLocationPermission.value, ui.lastLatLng),
                    runState = ui.state
                )

                // 3) 右下 FAB（icon-only，更現代）
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 重新定位 / 回到目前位置
                    SmallRoundFab(
                        icon = Icons.Filled.MyLocation,
                        onClick = {
                            val last = ui.lastLatLng
                            if (last != null) {
                                followMode = true
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(last, 17f)
                            }
                        }
                    )

                    // 跟隨切換
                    SmallRoundFab(
                        icon = if (followMode) Icons.Filled.GpsFixed else Icons.Filled.GpsNotFixed,
                        active = followMode,
                        onClick = { followMode = !followMode }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 4) 底部控制列（現代：一條控制列，不是一顆巨大按鈕塞底）
            ModernControlBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = ui.state,
                primaryColor = PrimaryPurple,
                onPrimary = {
                    when (ui.state) {
                        RunState.IDLE -> {
                            viewModel.onStart()
                            if (hasLocationPermission.value) {
                                viewModel.startLocationUpdates()
                                viewModel.startStepCounter()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                        RunState.RUNNING -> viewModel.onPause()
                        RunState.PAUSED -> {
                            viewModel.onResume()
                            if (hasLocationPermission.value) {
                                viewModel.startLocationUpdates()
                                viewModel.startStepCounter()
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
                onStop = { viewModel.onStop() }
            )

            Spacer(Modifier.height(10.dp))

            // 底部留白（避免貼到底導航）
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =========================
// 組件：玻璃資訊卡
// =========================
@Composable
private fun GlassStatsOverlay(
    modifier: Modifier,
    distanceKm: Double,
    elapsedMillis: Long,
    steps: Int,
    gpsText: String,
    runState: RunState
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        color = Glass,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    String.format("%.2f", distanceKm),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "km",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF444444)
                )

                Spacer(Modifier.weight(1f))

                // 小狀態 chip（不是按鈕，現代提示感）
                Surface(
                    color = GlassSoft,
                    shape = RoundedCornerShape(999.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = when (runState) {
                            RunState.IDLE -> "待機"
                            RunState.RUNNING -> "進行中"
                            RunState.PAUSED -> "已暫停"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF222222)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // 一行資訊（更像運動 App：緊湊）
            Text(
                "${formatMillis(elapsedMillis)} · ${steps} steps",
                fontSize = 13.sp,
                color = Color(0xFF333333)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                gpsText,
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

// =========================
// 組件：圓形 FAB（icon-only 現代化）
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
        color = if (active) PrimaryPurple.copy(alpha = 0.18f) else Glass,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) PrimaryPurple else Color(0xFF333333)
            )
        }
    }
}

// =========================
// 組件：底部控制列（現代化）
// =========================
@Composable
private fun ModernControlBar(
    modifier: Modifier,
    state: RunState,
    primaryColor: Color,
    onPrimary: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = Glass,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左：狀態文字（小而清楚）
            Column {
                Text(
                    text = when (state) {
                        RunState.IDLE -> "Ready"
                        RunState.RUNNING -> "Running"
                        RunState.PAUSED -> "Paused"
                    },
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111111)
                )
                Text(
                    text = "GPS / Steps / Timer",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }

            Spacer(Modifier.weight(1f))

            // 右：主要操作（icon-only + 膠囊感）
            val (primaryIcon, primaryText) = when (state) {
                RunState.IDLE -> Icons.Filled.PlayArrow to "Start"
                RunState.RUNNING -> Icons.Filled.Pause to "Pause"
                RunState.PAUSED -> Icons.Filled.PlayArrow to "Resume"
            }

            Button(
                onClick = onPrimary,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(primaryIcon, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(primaryText, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.width(10.dp))

            // Stop：只在非 IDLE 顯示
            if (state != RunState.IDLE) {
                OutlinedButton(
                    onClick = onStop,
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                }
            }
        }
    }
}

// =========================
// 小工具
// =========================
private fun gpsText(hasPermission: Boolean, last: LatLng?): String {
    return when {
        !hasPermission -> "GPS：未授權（請允許位置權限）"
        last == null -> "GPS：搜尋中…"
        else -> "GPS：良好"
    }
}

private fun formatMillis(ms: Long): String {
    val totalSec = (ms / 1000L).toInt()
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%02d:%02d", m, s)
}