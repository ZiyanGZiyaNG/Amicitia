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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.amicitia.nav.Routes
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import androidx.compose.material3.Surface


// =========================
// Theme
// =========================
private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

private val GlassDark = Color(0x2BFFFFFF)
private val TextPrimary = Color.White
private val TextMuted = Color(0xB3FFFFFF)
private val TextSubtle = Color(0x80FFFFFF)

private val SolidGray = Color(0xFF2A2A2A)
private val SolidBlack = Color(0xFF000000)
private val SolidDanger = Color(0xFF8B0000) // Finish 深紅

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
        stopTimer(keep = true)
        stopLocationUpdates()
        stopStepCounter()
        _uiState.update { it.copy(state = RunState.PAUSED) }
    }

    fun onResume() {
        _uiState.update { it.copy(state = RunState.RUNNING) }
        startTimer()
    }

    /** 取消/強制停止：回待機（不保留） */
    fun onStop() {
        stopTimer(keep = false)
        stopLocationUpdates()
        stopStepCounter()
        _uiState.value = SoloRunUiState()
    }

    /** 完成：回傳快照後回待機（保留給 UI 顯示/寫 DB） */
    fun onFinishAndReset(): SoloRunUiState {
        stopTimer(keep = false)
        stopLocationUpdates()
        stopStepCounter()
        val snap = _uiState.value
        _uiState.value = SoloRunUiState()
        return snap
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
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = SoloRunViewModel(app) as T
    })
    val ui by vm.uiState.collectAsState()

    val db = remember { FirebaseFirestore.getInstance() }
    val uid = Firebase.auth.currentUser?.uid

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

    var showFinishConfirm by remember { mutableStateOf(false) }
    var showFinishResult by remember { mutableStateOf(false) }
    var finishedSnapshot by remember { mutableStateOf<SoloRunUiState?>(null) }
    var finishDbError by remember { mutableStateOf<String?>(null) }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text("完成跑步？") },
            text = { Text("將結束本次跑步並把公里數加到跑步分數。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinishConfirm = false
                        finishDbError = null

                        val snap = vm.onFinishAndReset()
                        finishedSnapshot = snap
                        showFinishResult = true

                        val userId = uid
                        if (userId == null) {
                            finishDbError = "未登入，無法寫入分數。"
                        } else {
                            val km = snap.distanceKm
                            db.collection("users")
                                .document(userId)
                                .collection("sports")
                                .document("run")
                                .update("totalScore", FieldValue.increment(km))
                                .addOnFailureListener { e ->
                                    finishDbError = "寫入分數失敗：${e.message ?: "unknown"}"
                                }
                        }
                    }
                ) { Text("完成") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showFinishResult) {
        val snap = finishedSnapshot
        AlertDialog(
            onDismissRequest = { showFinishResult = false },
            title = { Text("本次跑步完成") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (snap == null) {
                        Text("無法取得成績。")
                    } else {
                        Text("時間：${formatMillisHMS(snap.elapsedMillis)}")
                        Text("距離：${String.format("%.2f", snap.distanceKm)} km")
                        Text("步數：${snap.steps}")
                    }
                    finishDbError?.let { Text(it) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        navController.popBackStack()
                        navController.popBackStack()
                    }
                ) { Text("關閉") }
            }
        )
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

            // 固定高度：避免你說的「卡片大小會變」
            DashboardCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                ui = ui
            )

            // 地圖永遠吃滿剩餘高度；控制列改成「覆蓋」在地圖底部，不再把 map 擠小
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f)
                    .shadow(18.dp, RoundedCornerShape(28.dp), clip = false)
                    .clip(RoundedCornerShape(28.dp))
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

                // 控制列「覆蓋」在地圖底部（不佔 Column 空間，所以 map 不會被縮）
                ModernControlBarOverlay(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(14.dp),
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
                    onStop = { vm.onStop() },
                    onFinish = { showFinishConfirm = true }
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

// =========================
// Solid 卡片（照你 Home：shadow + clip + 無 ripple）
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
// Dashboard：固定高度，不會因狀態改變而變大/變小
// =========================
@Composable
private fun DashboardCard(
    modifier: Modifier,
    ui: SoloRunUiState
) {
    SolidColorCard(
        modifier = modifier.height(108.dp),
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
    androidx.compose.material3.Surface(
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
// Solid 按鈕（照你 Home 的點擊感）
// =========================
@Composable
private fun SolidActionButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SolidBlack,
    cornerDp: Dp = 999.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
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
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    SolidActionButton(
        modifier = modifier.size(size),
        backgroundColor = backgroundColor,
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
// 控制列（覆蓋在 map 上）：不再把 map 擠小
// 只改排版，不加新功能
// =========================
@Composable
private fun ModernControlBarOverlay(
    modifier: Modifier,
    state: RunState,
    gpsReady: Boolean,
    onPrimary: () -> Unit,
    onStop: () -> Unit,
    onFinish: () -> Unit
) {
    val title = when (state) {
        RunState.IDLE -> "準備開始跑步"
        RunState.RUNNING -> "跑步中"
        RunState.PAUSED -> "已暫停"
    }
    val sub = when (state) {
        RunState.IDLE -> if (gpsReady) "GPS 已就緒｜可以開始" else "GPS 搜尋中｜建議等定位穩定再開始"
        RunState.RUNNING -> "按 Pause 暫停"
        RunState.PAUSED -> "按 Resume 繼續，或 Finish 結束"
    }

    SolidColorCard(
        modifier = modifier.fillMaxWidth(),
        cornerDp = 24.dp,
        contentPadding = 14.dp,
        backgroundColor = SolidGray
    ) {
        when (state) {
            RunState.IDLE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(sub, fontSize = 12.sp, color = TextSubtle)
                    }

                    SolidActionButton(
                        backgroundColor = SolidBlack,
                        onClick = onPrimary,
                        modifier = Modifier.height(46.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Start", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            RunState.RUNNING -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(sub, fontSize = 12.sp, color = TextSubtle)
                    }

                    SolidActionButton(
                        backgroundColor = SolidBlack,
                        onClick = onPrimary,
                        modifier = Modifier.height(46.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Pause", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    Spacer(Modifier.width(10.dp))

                    SolidIconButton(
                        backgroundColor = SolidBlack,
                        onClick = onStop,
                        size = 46.dp
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.White)
                    }
                }
            }

            RunState.PAUSED -> {
                // 這個狀態你的圖長這樣：上面文字，下面兩個大按鈕直排
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(sub, fontSize = 12.sp, color = TextSubtle)
                    Spacer(Modifier.height(12.dp))

                    SolidActionButton(
                        backgroundColor = SolidBlack,
                        onClick = onPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text("Resume", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    Spacer(Modifier.height(10.dp))

                    SolidActionButton(
                        backgroundColor = SolidDanger,
                        onClick = onFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text("Finish", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
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

private fun Context.hasAnyLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED