package com.roseth0rn.meditimer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material.*

class MainActivity : ComponentActivity() {
    private val vm: TimerViewModel by viewModels()
    private var isAmbient by mutableStateOf(false)

    private val ambientObserver = AmbientLifecycleObserver(this,
        object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                isAmbient = true
            }
            override fun onExitAmbient() {
                isAmbient = false
            }
            override fun onUpdateAmbient() {}
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { MediTimerApp(vm, isAmbient) }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(ambientObserver)
    }
}

@Composable
fun MediTimerApp(vm: TimerViewModel, isAmbient: Boolean) {
    val state   by vm.timerState.collectAsState()
    val seconds by vm.secondsRemaining.collectAsState()
    val minutes by vm.selectedMinutes.collectAsState()
    val (stats, weekDots) = vm.stats.collectAsState().value

    val glowAnim = rememberInfiniteTransition(label = "glow")
    val glowPhase by glowAnim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowPhase"
    )

    MaterialTheme {
        // In ambient mode during a session show the dim timer
        if (isAmbient && state == TimerState.RUNNING) {
            AmbientScreen(seconds)
        } else {
            when (state) {
                TimerState.IDLE     -> SetupScreen(minutes, stats, weekDots, glowPhase, vm)
                TimerState.RUNNING  -> RunningScreen(seconds, vm, glowPhase)
                TimerState.FINISHED -> FinishedScreen(vm, glowPhase)
            }
        }
    }
}

// ── Ambient Screen ───────────────────────────────────────────
@Composable
fun AmbientScreen(seconds: Int) {
    val min = String.format("%02d", seconds / 60)
    val sec = String.format("%02d", seconds % 60)
    Box(
        Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Minimal outline clock — burns fewer pixels
            Text("$min:$sec",
                color = Color(0xFF6A3A80),
                fontSize = 36.sp,
                fontWeight = FontWeight.Thin,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text("MEDITIMER",
                color = Color(0xFF3A1A50),
                fontSize = 9.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

// ── Buddha + Glow Canvas ─────────────────────────────────────
@Composable
fun BuddhaCanvas(glowPhase: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f * 0.92f
        val r  = size.minDimension / 2f

        val glowR  = r * (0.55f + 0.12f * glowPhase)
        val gAlpha = 0.18f + 0.22f * glowPhase
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFB07FD4).copy(alpha = gAlpha * 1.4f),
                    Color(0xFF7B3FA0).copy(alpha = gAlpha * 0.6f),
                    Color.Transparent
                ),
                center = Offset(cx, cy * 0.88f),
                radius = glowR * 1.5f
            ),
            radius = glowR * 1.5f,
            center = Offset(cx, cy * 0.88f)
        )

        val ringAlpha = 0.15f + 0.25f * glowPhase
        drawCircle(color = Color(0xFFC39BD3).copy(alpha = ringAlpha),
            radius = r * 0.50f, center = Offset(cx, cy * 0.88f), style = Stroke(1.5f))
        drawCircle(color = Color(0xFF9B59B6).copy(alpha = ringAlpha * 0.6f),
            radius = r * 0.57f, center = Offset(cx, cy * 0.88f), style = Stroke(1f))
        drawCircle(color = Color(0xFF7B3FA0).copy(alpha = ringAlpha * 0.3f),
            radius = r * 0.64f, center = Offset(cx, cy * 0.88f), style = Stroke(0.5f))

        val s  = r / 110f
        val bc = Color(0xFFC9B3D9).copy(alpha = 0.18f)

        fun oe(x: Float, y: Float, rx: Float, ry: Float, rot: Float = 0f) {
            rotate(rot, Offset(cx + x * s, cy * 0.88f + y * s)) {
                drawOval(color = bc,
                    topLeft = Offset(cx + (x - rx) * s, cy * 0.88f + (y - ry) * s),
                    size = Size(rx * 2 * s, ry * 2 * s))
            }
        }

        oe(0f, -49f, 5f, 4f)
        oe(0f, -44f, 8f, 7f)
        oe(0f, -26f, 18f, 20f)
        drawRect(color = bc,
            topLeft = Offset(cx - 6 * s, cy * 0.88f - 8f * s),
            size = Size(12 * s, 8 * s))
        oe(0f, 30f, 38f, 32f)
        oe(-32f, 12f, 14f, 10f, -15f)
        oe( 32f, 12f, 14f, 10f,  15f)
        drawPath(path = Path().apply {
            moveTo(cx - 38f * s, cy * 0.88f + 20f * s)
            quadraticTo(cx - 50f * s, cy * 0.88f + 40f * s,
                cx - 36f * s, cy * 0.88f + 50f * s)
            quadraticTo(cx - 28f * s, cy * 0.88f + 54f * s,
                cx - 20f * s, cy * 0.88f + 50f * s)
        }, color = bc)
        drawPath(path = Path().apply {
            moveTo(cx + 38f * s, cy * 0.88f + 20f * s)
            quadraticTo(cx + 50f * s, cy * 0.88f + 40f * s,
                cx + 36f * s, cy * 0.88f + 50f * s)
            quadraticTo(cx + 28f * s, cy * 0.88f + 54f * s,
                cx + 20f * s, cy * 0.88f + 50f * s)
        }, color = bc)
        oe(0f, 52f, 24f, 10f)
        oe(-34f, 64f, 22f, 12f)
        oe( 34f, 64f, 22f, 12f)
        oe(0f,   77f, 42f, 10f)
        oe(0f,   84f, 18f,  8f)
        oe(-24f, 82f, 14f,  7f, -20f)
        oe( 24f, 82f, 14f,  7f,  20f)
        oe(-42f, 77f, 12f,  6f, -35f)
        oe( 42f, 77f, 12f,  6f,  35f)

        val eyeColor = Color(0xFF7B3FA0).copy(alpha = 0.35f)
        drawArc(color = eyeColor, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(cx - 10f * s, cy * 0.88f - 23f * s),
            size = Size(8f * s, 4f * s), style = Stroke(1.2f))
        drawArc(color = eyeColor, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(cx + 2f * s, cy * 0.88f - 23f * s),
            size = Size(8f * s, 4f * s), style = Stroke(1.2f))
        drawArc(color = eyeColor, startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(cx - 5f * s, cy * 0.88f - 16f * s),
            size = Size(10f * s, 5f * s), style = Stroke(1f))
        drawCircle(
            color = Color(0xFFD4A8E8).copy(alpha = 0.3f + 0.3f * glowPhase),
            radius = 1.5f * s, center = Offset(cx, cy * 0.88f - 28f * s)
        )
    }
}

// ── Week Dots ────────────────────────────────────────────────
@Composable
fun WeekDots(sessionDays: List<Boolean>) {
    val labels = listOf("M","T","W","T","F","S","S")
    val today  = java.time.LocalDate.now().dayOfWeek.value - 1
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("THIS WEEK", color = Color(0xFF9B59B6), fontSize = 8.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEachIndexed { i, d ->
                val isToday = i == today
                val done    = sessionDays[i]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isToday) {
                            Canvas(modifier = Modifier.size(17.dp)) {
                                drawCircle(
                                    color = if (done) Color(0xFFD4A8E8) else Color(0xFF888888),
                                    style = Stroke(width = 1.5f)
                                )
                            }
                        }
                        Box(modifier = Modifier
                            .size(13.dp)
                            .background(
                                color = when {
                                    done    -> Color(0xFF7B3FA0)
                                    isToday -> Color(0xFF3A2A4A)
                                    else    -> Color(0xFF2A2A3A)
                                },
                                shape = CircleShape
                            )
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(d,
                        color = when {
                            done    -> Color(0xFFC39BD3)
                            isToday -> Color(0xFF888888)
                            else    -> Color(0xFF444444)
                        },
                        fontSize = 7.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Screen 1: Setup ──────────────────────────────────────────
@Composable
fun SetupScreen(
    minutes: Int, stats: StatsResult,
    weekDots: List<Boolean>, glowPhase: Float,
    vm: TimerViewModel
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        BuddhaCanvas(glowPhase = glowPhase)
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 14.dp, bottom = 16.dp)
        ) {
            item {
                Text("MEDITIMER", color = Color(0xFFC39BD3),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { vm.setMinutes(minutes - 1) },
                        modifier = Modifier.size(32.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2A1A3A))) {
                        Text("−", fontSize = 18.sp, color = Color(0xFFC39BD3))
                    }
                    Text("$minutes min", color = Color.White, fontSize = 20.sp,
                        fontWeight = FontWeight.Light, textAlign = TextAlign.Center,
                        modifier = Modifier.width(68.dp))
                    Button(onClick = { vm.setMinutes(minutes + 1) },
                        modifier = Modifier.size(32.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2A1A3A))) {
                        Text("+", fontSize = 18.sp, color = Color(0xFFC39BD3))
                    }
                }
            }
            item {
                Button(onClick = { vm.startTimer() },
                    modifier = Modifier.width(90.dp).height(34.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF7B3FA0))) {
                    Text("Begin", color = Color.White, fontSize = 13.sp,
                        fontWeight = FontWeight.Medium)
                }
            }
            item { WeekDots(sessionDays = weekDots) }
            item {
                Text("${stats.weeklyStreak} week streak · ${stats.monthMinutes}m this month",
                    color = Color(0xFF7B3FA0), fontSize = 9.sp,
                    modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

// ── Screen 2: Running ────────────────────────────────────────
@Composable
fun RunningScreen(seconds: Int, vm: TimerViewModel, glowPhase: Float) {
    val min = String.format("%02d", seconds / 60)
    val sec = String.format("%02d", seconds % 60)
    val progress by remember(seconds) {
        derivedStateOf { seconds.toFloat() / (vm.selectedMinutes.value * 60f) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        BuddhaCanvas(glowPhase = glowPhase)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 5f
            val inset  = stroke / 2f
            val arcColor = Color(
                red   = 0.55f + 0.2f  * glowPhase,
                green = 0.25f,
                blue  = 0.63f + 0.15f * glowPhase,
                alpha = 1f
            )
            drawArc(color = Color(0xFF1A0A2A),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke))
            drawArc(color = arcColor,
                startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        Column(modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text("$min:$sec", color = Color.White, fontSize = 40.sp,
                fontWeight = FontWeight.Thin, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.cancelTimer() },
                modifier = Modifier.width(100.dp).height(30.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2A0A1A))) {
                Text("Cancel", color = Color(0xFFFF6B6B), fontSize = 12.sp)
            }
        }
    }
}

// ── Screen 3: Finished ───────────────────────────────────────
@Composable
fun FinishedScreen(vm: TimerViewModel, glowPhase: Float) {
    val bellAnim = rememberInfiniteTransition(label = "bell")
    val bellRot by bellAnim.animateFloat(
        initialValue = -15f, targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bellRot"
    )
    var ringing by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        ringing = false
    }

    Box(Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center) {
        BuddhaCanvas(glowPhase = glowPhase)
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🔔", fontSize = 34.sp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = if (ringing) bellRot else 0f
                })
            Text("Session\ncomplete", color = Color(0xFFC39BD3),
                fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
            Button(onClick = { vm.resetToIdle() },
                modifier = Modifier.width(100.dp).height(32.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF7B3FA0))) {
                Text("Done", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}