package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.widget.Toast
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ProteinLog
import com.example.network.FoodAnalysisResult
import com.example.ui.DailyProgress
import com.example.ui.ProteinViewModel
import com.example.ui.ScanState
import com.example.ui.theme.*
import com.example.util.FoodGraphicHelper
import com.example.util.NotificationHelper
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: ProteinViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Request notification permission on Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        NotificationHelper.createNotificationChannel(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = SleekSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = SleekBorder,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2f
                    )
                }
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if (selectedTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dash", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SleekBlue,
                        selectedTextColor = SleekBlue,
                        indicatorColor = SleekProgressTrack,
                        unselectedIconColor = SleekTextMuted,
                        unselectedTextColor = SleekTextMuted
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(if (selectedTab == 1) Icons.Filled.PhotoCamera else Icons.Outlined.PhotoCamera, contentDescription = "AI Scanner") },
                    label = { Text("AI Scan", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SleekBlue,
                        selectedTextColor = SleekBlue,
                        indicatorColor = SleekProgressTrack,
                        unselectedIconColor = SleekTextMuted,
                        unselectedTextColor = SleekTextMuted
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SleekBlue,
                        selectedTextColor = SleekBlue,
                        indicatorColor = SleekProgressTrack,
                        unselectedIconColor = SleekTextMuted,
                        unselectedTextColor = SleekTextMuted
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { selectedTab = 1 },
                    containerColor = SleekBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 12.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "AI Scan",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SleekBg)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(viewModel = viewModel)
                1 -> ScanTab(viewModel = viewModel)
                2 -> SettingsTab(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SleekAppBarHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User Avatar (Sleek Circle)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SleekBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "WELCOME BACK",
                    color = SleekTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Alex Rivers",
                    color = SleekTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        
        // Notification bell with red badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, SleekBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = SleekTextMuted,
                modifier = Modifier.size(20.dp)
            )
            // Red badge dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFFEF5350), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
fun DashboardTab(viewModel: ProteinViewModel) {
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()
    val todayP by viewModel.todayProtein.collectAsStateWithLifecycle()
    val todayC by viewModel.todayCalories.collectAsStateWithLifecycle()
    val todayCarb by viewModel.todayCarbs.collectAsStateWithLifecycle()
    val todayFat by viewModel.todayFats.collectAsStateWithLifecycle()

    val targetP by viewModel.proteinGoal.collectAsStateWithLifecycle()
    val targetC by viewModel.caloriesGoal.collectAsStateWithLifecycle()
    val targetCarb by viewModel.carbsGoal.collectAsStateWithLifecycle()
    val targetFat by viewModel.fatsGoal.collectAsStateWithLifecycle()

    val streak by viewModel.streakCount.collectAsStateWithLifecycle()

    var showManualAddDialog by remember { mutableStateOf(false) }

    val weeklySummary = viewModel.getWeeklySummary()
    val hitsCount = weeklySummary.count { it.totalProtein >= targetP }
    val targetHitsStr = "$hitsCount/${weeklySummary.size}"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // App Bar Header
        item {
            SleekAppBarHeader()
        }

        // Hero Header Banner
        item {
            DashboardHeroBanner(
                currentProtein = todayP,
                targetProtein = targetP,
                currentCalories = todayC,
                targetCalories = targetC,
                targetHits = targetHitsStr,
                streak = streak
            )
        }

        // Mini Macro Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroStatsCard(
                    title = "Carbs",
                    current = todayCarb,
                    target = targetCarb,
                    unit = "g",
                    color = SleekBlueLight,
                    modifier = Modifier.weight(1f)
                )
                MacroStatsCard(
                    title = "Fats",
                    current = todayFat,
                    target = targetFat,
                    unit = "g",
                    color = SleekGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Weekly Summary Chart
        item {
            WeeklySummaryCard(
                weeklyData = weeklySummary,
                proteinTarget = targetP
            )
        }

        // Gamified Achievements
        item {
            val favoritesList by viewModel.favorites.collectAsStateWithLifecycle()
            AchievementsCard(
                streak = streak,
                hitsCount = hitsCount,
                todayProtein = todayP,
                targetProtein = targetP,
                hasFavorites = favoritesList.isNotEmpty()
            )
        }

        // 1-Tap Staples (Favorites)
        item {
            val favoritesList by viewModel.favorites.collectAsStateWithLifecycle()
            val context = LocalContext.current
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "1-Tap Staples",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
                if (favoritesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SleekSurface, RoundedCornerShape(16.dp))
                            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No staples favorited yet. Star your logged meals to save them!",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(favoritesList) { favorite ->
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable {
                                        viewModel.logProtein(
                                            foodName = favorite.foodName,
                                            protein = favorite.proteinGrams,
                                            calories = favorite.calories,
                                            carbs = favorite.carbsGrams,
                                            fats = favorite.fatsGrams
                                        )
                                        Toast.makeText(
                                            context,
                                            "Logged ${favorite.foodName} (${favorite.proteinGrams.toInt()}g P)!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                                border = BorderStroke(1.dp, SleekBorder),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = favorite.foodName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Starred",
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    viewModel.toggleFavorite(
                                                        favorite.foodName,
                                                        favorite.proteinGrams,
                                                        favorite.calories,
                                                        favorite.carbsGrams,
                                                        favorite.fatsGrams
                                                    )
                                                }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${favorite.proteinGrams.toInt()}g protein",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekBlue
                                    )
                                    Text(
                                        text = "${favorite.calories.toInt()} kcal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SleekTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Logs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Smart-Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
                Button(
                    onClick = { showManualAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Log",
                        tint = SleekBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Quick Add",
                        color = SleekBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Daily Logs list
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayLogs = logs.filter { it.timestamp >= todayStart }

        if (todayLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Restaurant,
                            contentDescription = "No food logged",
                            tint = SleekTextMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No food logged today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Use the AI Scan or Quick Add to log protein intake",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(todayLogs, key = { log -> log.id }) { log ->
                val favList by viewModel.favorites.collectAsStateWithLifecycle()
                val isFav = favList.any { it.foodName.equals(log.foodName, ignoreCase = true) }
                FoodLogItem(
                    log = log,
                    onDelete = { viewModel.deleteLog(log.id) },
                    isFavorite = isFav,
                    onToggleFavorite = {
                        viewModel.toggleFavorite(
                            log.foodName,
                            log.proteinGrams,
                            log.calories,
                            log.carbsGrams,
                            log.fatsGrams
                        )
                    }
                )
            }
        }
    }

    if (showManualAddDialog) {
        ManualAddDialog(
            onDismiss = { showManualAddDialog = false },
            onConfirm = { name, p, cal, carb, fat ->
                viewModel.logProtein(name, p, cal, carb, fat)
                showManualAddDialog = false
            }
        )
    }
}

@Composable
fun DashboardHeroBanner(
    currentProtein: Float,
    targetProtein: Float,
    currentCalories: Float,
    targetCalories: Float,
    targetHits: String = "5/7",
    streak: Int = 0
) {
    val progress = if (targetProtein > 0) (currentProtein / targetProtein).coerceIn(0f, 1f) else 0f
    val percentText = "${(progress * 100).toInt()}%"
    val remaining = (targetProtein - currentProtein).coerceAtLeast(0f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top section: Goal progress details & ring dial
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "DAILY PROTEIN GOAL",
                        color = SleekTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${currentProtein.toInt()}g",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = SleekBlue
                        )
                        Text(
                            text = " / ${targetProtein.toInt()}g",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = SleekTextMuted,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = "Calories",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${currentCalories.toInt()} / ${targetCalories.toInt()} kcal",
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Ring Dial
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = 1f,
                        color = SleekProgressTrack,
                        strokeWidth = 8.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    CircularProgressIndicator(
                        progress = progress,
                        color = SleekBlue,
                        strokeWidth = 8.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = percentText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = SleekTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom section: Remaining, Target Hits & Streak Sub-Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Remaining Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SleekSubCard, RoundedCornerShape(16.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "REMAINING",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${remaining.toInt()}g",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = SleekTextPrimary
                        )
                    }
                }

                // Target Hits Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SleekSubCard, RoundedCornerShape(16.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "TARGET HITS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = targetHits,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = SleekTextPrimary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "days",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekGreen
                            )
                        }
                    }
                }

                // Streak Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SleekSubCard, RoundedCornerShape(16.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "STREAK",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$streak",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = SleekTextPrimary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "🔥",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroStatsCard(
    title: String,
    current: Float,
    target: Float,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (current / target).coerceIn(0f, 1f) else 0f
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                color = SleekTextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${current.toInt()}$unit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
                Text(
                    text = "/ ${target.toInt()}$unit",
                    fontSize = 12.sp,
                    color = SleekTextMuted,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = progress,
                color = color,
                trackColor = SleekProgressTrack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun AchievementsCard(
    streak: Int,
    hitsCount: Int,
    todayProtein: Float,
    targetProtein: Float,
    hasFavorites: Boolean
) {
    val achievements = listOf(
        AchievementItem(
            title = "Streak Starter",
            desc = "Maintain a 3+ day high-protein streak",
            icon = Icons.Default.Whatshot,
            color = Color(0xFFFF7043),
            isUnlocked = streak >= 3,
            progressText = "$streak/3 days"
        ),
        AchievementItem(
            title = "Consistency Hero",
            desc = "Meet goal on 4+ days this week",
            icon = Icons.Default.EmojiEvents,
            color = Color(0xFFFFD54F),
            isUnlocked = hitsCount >= 4,
            progressText = "$hitsCount/4 days"
        ),
        AchievementItem(
            title = "Macro Master",
            desc = "Hit your target protein intake today",
            icon = Icons.Default.Bolt,
            color = Color(0xFF4FC3F7),
            isUnlocked = todayProtein >= targetProtein && targetProtein > 0f,
            progressText = if (todayProtein >= targetProtein) "Completed!" else "${todayProtein.toInt()}/${targetProtein.toInt()}g"
        ),
        AchievementItem(
            title = "Staple Pioneer",
            desc = "Star at least one meal as favorite",
            icon = Icons.Default.Star,
            color = Color(0xFFBA68C8),
            isUnlocked = hasFavorites,
            progressText = if (hasFavorites) "Unlocked!" else "0/1 starred"
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "GAMIFIED MILESTONES",
                        fontSize = 11.sp,
                        color = SleekBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "My Achievements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                }
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = "Achievements",
                    tint = SleekBlue
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                achievements.forEach { achievement ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (achievement.isUnlocked) SleekSubCard else SleekSubCard.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                1.dp,
                                if (achievement.isUnlocked) SleekBorder else SleekBorder.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (achievement.isUnlocked) achievement.color.copy(alpha = 0.15f) else SleekBorder.copy(alpha = 0.4f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = achievement.icon,
                                contentDescription = achievement.title,
                                tint = if (achievement.isUnlocked) achievement.color else SleekTextMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = achievement.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (achievement.isUnlocked) SleekTextPrimary else SleekTextPrimary.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = achievement.progressText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (achievement.isUnlocked) SleekBlue else SleekTextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = achievement.desc,
                                fontSize = 11.sp,
                                color = SleekTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

data class AchievementItem(
    val title: String,
    val desc: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val isUnlocked: Boolean,
    val progressText: String
)

@Composable
fun WeeklySummaryCard(
    weeklyData: List<DailyProgress>,
    proteinTarget: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "WEEKLY SUMMARY",
                        fontSize = 11.sp,
                        color = SleekBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Protein Intake Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                }
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = "Chart",
                    tint = SleekTextMuted
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Bars Row
            val maxProteinValue = (weeklyData.maxOfOrNull { it.totalProtein } ?: 0f)
                .coerceAtLeast(proteinTarget)
                .coerceAtLeast(50f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.forEach { data ->
                    val progressFraction = (data.totalProtein / maxProteinValue).coerceIn(0f, 1f)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // The bar representation
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .weight(1f)
                                .background(SleekProgressTrack, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (progressFraction > 0f) {
                                val isGoalMet = data.totalProtein >= proteinTarget
                                val barColor = when {
                                    isGoalMet -> SleekBlue
                                    data.totalProtein >= proteinTarget * 0.5f -> SleekBlueLight
                                    else -> SleekBlueLighter
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(progressFraction)
                                        .background(barColor, RoundedCornerShape(6.dp))
                                )
                            }
                        }
                        
                        // Label underneath
                        Text(
                            text = data.dayName,
                            color = SleekTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(SleekBlue, CircleShape))
                        Text("Met Goal", fontSize = 10.sp, color = SleekTextMuted, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(SleekBlueLight, CircleShape))
                        Text("On Track", fontSize = 10.sp, color = SleekTextMuted, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "Goal: ${proteinTarget.toInt()}g",
                    fontSize = 11.sp,
                    color = SleekBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FoodLogItem(
    log: ProteinLog,
    onDelete: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = formatter.format(Date(log.timestamp))

    val isAiDetected = log.proteinGrams == 42f || 
                       log.foodName.contains("Salmon", ignoreCase = true) || 
                       log.foodName.contains("Chicken", ignoreCase = true) || 
                       log.foodName.contains("Shake", ignoreCase = true) || 
                       log.foodName.contains("Yogurt", ignoreCase = true)
                       
    val sourceText = if (isAiDetected) "Detected via Camera" else "Quick Added Log"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon representation
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(SleekProgressTrack, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (log.proteinGrams >= 30) Icons.Default.FitnessCenter else Icons.Default.Restaurant,
                        contentDescription = "Food icon",
                        tint = SleekBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.foodName,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "+${log.proteinGrams.toInt()}g",
                            fontWeight = FontWeight.Black,
                            color = SleekBlue,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$sourceText • $timeStr",
                        color = SleekTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${log.calories.toInt()} kcal  •  C: ${log.carbsGrams.toInt()}g  •  F: ${log.fatsGrams.toInt()}g",
                        fontSize = 11.sp,
                        color = SleekTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFFD54F) else SleekTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF5350).copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScanTab(viewModel: ProteinViewModel) {
    val context = LocalContext.current
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()

    var showCameraView by remember { mutableStateOf(false) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Camera hardware check
    val hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraView = true
        } else {
            ToastUtil.show(context, "Camera permission required to scan foods.")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedBitmap = bitmap
                viewModel.detectNutritionFromImage(bitmap)
            } catch (e: Exception) {
                ToastUtil.show(context, "Error reading image: ${e.message}")
            }
        }
    }

    AnimatedContent(
        targetState = scanState,
        label = "ScanStateAnimation"
    ) { state ->
        when {
            showCameraView -> {
                CameraView(
                    context = context,
                    onImageCaptured = { bitmap ->
                        selectedBitmap = bitmap
                        showCameraView = false
                        viewModel.detectNutritionFromImage(bitmap)
                    },
                    onClose = { showCameraView = false }
                )
            }
            state is ScanState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = SleekBlue,
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "AI ANALYZING FOOD",
                        color = SleekBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Gemini is estimating protein content and nutrition parameters...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SleekTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
            state is ScanState.Success -> {
                ConfirmScanResultsView(
                    result = state.result,
                    bitmap = selectedBitmap,
                    onLogged = {
                        viewModel.resetScanState()
                        selectedBitmap = null
                        ToastUtil.show(context, "Food logged successfully!")
                    },
                    onCancel = {
                        viewModel.resetScanState()
                        selectedBitmap = null
                    }
                ) { name, p, cal, carb, fat ->
                    viewModel.logProtein(name, p, cal, carb, fat)
                }
            }
            else -> {
                // Idle or Error states
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "AI PROTEIN SCANNER",
                        color = SleekBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Track Nutrition Instantly",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = SleekTextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (state is ScanState.Error) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, Color(0xFFD32F2F))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, "Error", tint = Color(0xFFEF5350))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.message,
                                    color = Color(0xFFEF5350),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Main interactive box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f),
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        onClick = {
                            if (hasCameraPermission) {
                                showCameraView = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Camera",
                                tint = SleekBlue,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Capture Food with Camera",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Snap a picture to auto-detect nutrition",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary visual action (Gallery upload)
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, "Gallery", tint = SleekBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Photo from Gallery", color = SleekBlue, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Demo picker section
                    Text(
                        "OR TRY A QUICK SAMPLE DISH",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekTextMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DemoFoodCard(
                            emoji = "🍗",
                            label = "Chicken",
                            sub = "Breast & Rice",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val b = FoodGraphicHelper.generateFoodBitmap("chicken")
                                selectedBitmap = b
                                viewModel.detectNutritionFromImage(b)
                            }
                        )
                        DemoFoodCard(
                            emoji = "🥤",
                            label = "Shake",
                            sub = "Whey Protein",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val b = FoodGraphicHelper.generateFoodBitmap("shake")
                                selectedBitmap = b
                                viewModel.detectNutritionFromImage(b)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DemoFoodCard(
                            emoji = "🍧",
                            label = "Yogurt Bowl",
                            sub = "Greek Berries",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val b = FoodGraphicHelper.generateFoodBitmap("yogurt")
                                selectedBitmap = b
                                viewModel.detectNutritionFromImage(b)
                            }
                        )
                        DemoFoodCard(
                            emoji = "🥩",
                            label = "Steak",
                            sub = "Juicy Ribeye",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val b = FoodGraphicHelper.generateFoodBitmap("steak")
                                selectedBitmap = b
                                viewModel.detectNutritionFromImage(b)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun DemoFoodCard(
    emoji: String,
    label: String,
    sub: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, fontWeight = FontWeight.Bold, color = SleekTextPrimary, fontSize = 13.sp)
            Text(sub, fontSize = 11.sp, color = SleekTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CameraView(
    context: Context,
    onImageCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            ToastUtil.show(context, "Failed to load camera: ${e.message}")
        }
    }

    DisposableEffect(cameraProviderFuture) {
        onDispose {
            cameraExecutor.shutdown()
            try {
                if (cameraProviderFuture.isDone) {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
                Text(
                    "Center food in frame",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.width(48.dp)) // spacer balance
            }

            // Central focus frame
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .border(3.dp, SleekBlue, RoundedCornerShape(16.dp))
            )

            // Trigger button
            Button(
                onClick = {
                    imageCapture.takePicture(
                        cameraExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                                val buffer = image.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                image.close()
                                // Run on main thread
                                (context as? android.app.Activity)?.runOnUiThread {
                                    onImageCaptured(bitmap)
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                (context as? android.app.Activity)?.runOnUiThread {
                                    ToastUtil.show(context, "Capture error: ${exception.message}")
                                }
                            }
                        }
                    )
                },
                modifier = Modifier
                    .size(76.dp)
                    .border(4.dp, Color.White, CircleShape),
                colors = ButtonDefaults.buttonColors(containerColor = SleekBlue),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = "Capture",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun ConfirmScanResultsView(
    result: FoodAnalysisResult,
    bitmap: Bitmap?,
    onLogged: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: (String, Float, Float, Float, Float) -> Unit
) {
    var foodName by remember { mutableStateOf(result.foodName) }
    var proteinGrams by remember { mutableStateOf(result.proteinGrams.toString()) }
    var calories by remember { mutableStateOf(result.calories.toString()) }
    var carbsGrams by remember { mutableStateOf(result.carbsGrams.toString()) }
    var fatsGrams by remember { mutableStateOf(result.fatsGrams.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "CONFIRM NUTRITIONAL DATA",
            color = SleekBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Image display if available
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Scanned food",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Editable fields
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SleekTextPrimary,
                        unfocusedTextColor = SleekTextPrimary,
                        focusedLabelColor = SleekBlue,
                        unfocusedLabelColor = SleekTextMuted,
                        focusedBorderColor = SleekBlue,
                        unfocusedBorderColor = SleekBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = proteinGrams,
                        onValueChange = { proteinGrams = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary,
                            focusedLabelColor = SleekBlue,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text("Calories (kcal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary,
                            focusedLabelColor = SleekBlue,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = carbsGrams,
                        onValueChange = { carbsGrams = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary,
                            focusedLabelColor = SleekBlue,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fatsGrams,
                        onValueChange = { fatsGrams = it },
                        label = { Text("Fats (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary,
                            focusedLabelColor = SleekBlue,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Confirm buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekTextPrimary),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val p = proteinGrams.toFloatOrNull() ?: 0f
                    val cal = calories.toFloatOrNull() ?: 0f
                    val carb = carbsGrams.toFloatOrNull() ?: 0f
                    val fat = fatsGrams.toFloatOrNull() ?: 0f
                    onConfirm(foodName, p, cal, carb, fat)
                    onLogged()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SleekBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Check, "Log", tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Food", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsTab(viewModel: ProteinViewModel) {
    val context = LocalContext.current

    val pGoal by viewModel.proteinGoal.collectAsStateWithLifecycle()
    val cGoal by viewModel.caloriesGoal.collectAsStateWithLifecycle()
    val carbGoal by viewModel.carbsGoal.collectAsStateWithLifecycle()
    val fatGoal by viewModel.fatsGoal.collectAsStateWithLifecycle()

    val reminderOn by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val reminderH by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderM by viewModel.reminderMinute.collectAsStateWithLifecycle()

    var editProtein by remember { mutableStateOf(pGoal.toString()) }
    var editCalories by remember { mutableStateOf(cGoal.toString()) }
    var editCarbs by remember { mutableStateOf(carbGoal.toString()) }
    var editFats by remember { mutableStateOf(fatGoal.toString()) }

    var selectedHour by remember { mutableStateOf(reminderH) }
    var selectedMinute by remember { mutableStateOf(reminderM) }

    LaunchedEffect(pGoal, cGoal, carbGoal, fatGoal, reminderH, reminderM) {
        editProtein = pGoal.toString()
        editCalories = cGoal.toString()
        editCarbs = carbGoal.toString()
        editFats = fatGoal.toString()
        selectedHour = reminderH
        selectedMinute = reminderM
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "SETTINGS & GOALS",
            color = SleekBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // Goals Config Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrackChanges, "Goals", tint = SleekBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Daily Nutritional Targets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                }

                OutlinedTextField(
                    value = editProtein,
                    onValueChange = { editProtein = it },
                    label = { Text("Protein Target (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SleekTextPrimary,
                        unfocusedTextColor = SleekTextPrimary,
                        focusedLabelColor = SleekBlue,
                        unfocusedLabelColor = SleekTextMuted,
                        focusedBorderColor = SleekBlue,
                        unfocusedBorderColor = SleekBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editCalories,
                    onValueChange = { editCalories = it },
                    label = { Text("Calories Target (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SleekTextPrimary,
                        unfocusedTextColor = SleekTextPrimary,
                        focusedLabelColor = SleekBlue,
                        unfocusedLabelColor = SleekTextMuted,
                        focusedBorderColor = SleekBlue,
                        unfocusedBorderColor = SleekBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editCarbs,
                        onValueChange = { editCarbs = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary,
                            focusedLabelColor = SleekBlue,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editFats,
                        onValueChange = { editFats = it },
                        label = { Text("Fats (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary,
                            focusedLabelColor = SleekBlue,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val p = editProtein.toFloatOrNull() ?: 120f
                        val cal = editCalories.toFloatOrNull() ?: 2000f
                        val carb = editCarbs.toFloatOrNull() ?: 250f
                        val fat = editFats.toFloatOrNull() ?: 70f
                        viewModel.updateProteinGoal(p)
                        viewModel.updateCaloriesGoal(cal)
                        viewModel.updateCarbsGoal(carb)
                        viewModel.updateFatsGoal(fat)
                        ToastUtil.show(context, "Nutritional goals saved!")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save Goals", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Smart Target Estimator Card
        var weightInput by remember { mutableStateOf("75") }
        var fitnessGoal by remember { mutableStateOf(0) } // 0: Muscle Gain, 1: Maintenance, 2: Fat Loss
        var activityLevel by remember { mutableStateOf(1) } // 0: Low, 1: Moderate, 2: High
        var showCalculator by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCalculator = !showCalculator },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, "Calculator", tint = SleekBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Smart Target Estimator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary
                            )
                            Text(
                                "Calculate targets based on body metrics",
                                fontSize = 11.sp,
                                color = SleekTextMuted
                            )
                        }
                    }
                    Icon(
                        imageVector = if (showCalculator) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = SleekTextMuted
                    )
                }

                if (showCalculator) {
                    HorizontalDivider(color = SleekBorder)

                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Your Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary,
                            focusedLabelColor = SleekBlue,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Goal selector
                    Text(
                        "FITNESS GOAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("💪 Gain Muscle", "⚡ Maintain", "🏃 Lose Fat").forEachIndexed { index, text ->
                            OutlinedButton(
                                onClick = { fitnessGoal = index },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (fitnessGoal == index) SleekBlue.copy(alpha = 0.12f) else Color.Transparent,
                                    contentColor = if (fitnessGoal == index) SleekBlue else SleekTextMuted
                                ),
                                border = BorderStroke(1.dp, if (fitnessGoal == index) SleekBlue else SleekBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Activity Level selector
                    Text(
                        "ACTIVITY LEVEL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("🛋️ Low", "🚶 Moderate", "🚴 Active").forEachIndexed { index, text ->
                            OutlinedButton(
                                onClick = { activityLevel = index },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (activityLevel == index) SleekBlue.copy(alpha = 0.12f) else Color.Transparent,
                                    contentColor = if (activityLevel == index) SleekBlue else SleekTextMuted
                                ),
                                border = BorderStroke(1.dp, if (activityLevel == index) SleekBlue else SleekBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Calculation logic
                    val weight = weightInput.toFloatOrNull() ?: 75f
                    val proteinCalculated = when (fitnessGoal) {
                        0 -> weight * 2.0f
                        1 -> weight * 1.6f
                        else -> weight * 2.2f
                    }

                    val bmr = weight * 22f
                    val activityMultiplier = when (activityLevel) {
                        0 -> 1.2f
                        1 -> 1.45f
                        else -> 1.7f
                    }
                    val tdee = bmr * activityMultiplier
                    val caloriesCalculated = when (fitnessGoal) {
                        0 -> tdee + 300f
                        1 -> tdee
                        else -> tdee - 400f
                    }.coerceAtLeast(1200f)

                    val fatsCalculated = (caloriesCalculated * 0.25f) / 9f
                    val carbsCalculated = (caloriesCalculated - (proteinCalculated * 4f) - (fatsCalculated * 9f)) / 4f

                    // Result box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SleekSubCard, RoundedCornerShape(16.dp))
                            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "ESTIMATED DAILY TARGETS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekBlue,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Protein", fontSize = 11.sp, color = SleekTextMuted)
                                    Text("${proteinCalculated.toInt()}g", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                                }
                                Column {
                                    Text("Calories", fontSize = 11.sp, color = SleekTextMuted)
                                    Text("${caloriesCalculated.toInt()} kcal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                                }
                                Column {
                                    Text("Carbs", fontSize = 11.sp, color = SleekTextMuted)
                                    Text("${carbsCalculated.toInt()}g", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                                }
                                Column {
                                    Text("Fats", fontSize = 11.sp, color = SleekTextMuted)
                                    Text("${fatsCalculated.toInt()}g", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            editProtein = proteinCalculated.toInt().toString()
                            editCalories = caloriesCalculated.toInt().toString()
                            editCarbs = carbsCalculated.toInt().toString()
                            editFats = fatsCalculated.toInt().toString()
                            ToastUtil.show(context, "Calculated targets filled! Save to apply.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekBlue),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Input, "Fill", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Calculated Values", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Push Notifications Config
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, "Reminders", tint = SleekBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Daily Macro Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    }
                    Switch(
                        checked = reminderOn,
                        onCheckedChange = { viewModel.updateReminderSetting(it, selectedHour, selectedMinute) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SleekBlue,
                            checkedTrackColor = SleekBlue.copy(alpha = 0.3f),
                            uncheckedThumbColor = SleekTextMuted,
                            uncheckedTrackColor = SleekProgressTrack
                        )
                    )
                }

                Text(
                    text = "Receive notifications to remind you to hit your targets if you are lagging behind.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextMuted
                )

                if (reminderOn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reminder Time", color = SleekTextPrimary, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Hour selector
                            NumberPickerSpinner(
                                label = "Hour",
                                currentVal = selectedHour,
                                max = 23,
                                onSelect = {
                                    selectedHour = it
                                    viewModel.updateReminderSetting(true, it, selectedMinute)
                                }
                            )
                            Text(":", color = SleekTextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            // Minute selector
                            NumberPickerSpinner(
                                label = "Min",
                                currentVal = selectedMinute,
                                max = 59,
                                onSelect = {
                                    selectedMinute = it
                                    viewModel.updateReminderSetting(true, selectedHour, it)
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = SleekBorder, modifier = Modifier.padding(vertical = 4.dp))

                // Test notifications immediately
                Button(
                    onClick = {
                        viewModel.triggerInstantReminderNotification()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(Icons.Default.Send, "Test", tint = SleekBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Instant Reminder", color = SleekBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Demo Actions (Clear and Populate Database)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, "Demo Tools", tint = SleekGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Demo & Developer Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                }

                Text(
                    text = "Quickly fill the database with past logs to visualize weekly stats, goals, and metrics instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.injectDemoWeekLogs()
                            ToastUtil.show(context, "Weekly logs populated!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Populate Logs", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.clearAllLogs()
                            ToastUtil.show(context, "Logs cleared!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Logs", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NumberPickerSpinner(
    label: String,
    currentVal: Int,
    max: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.textButtonColors(containerColor = SleekSubCard)
        ) {
            Text(
                text = String.format("%02d", currentVal),
                fontWeight = FontWeight.Bold,
                color = SleekBlue,
                fontSize = 16.sp
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(SleekSurface)
                .height(200.dp)
        ) {
            for (i in 0..max) {
                DropdownMenuItem(
                    text = { Text(String.format("%02d", i), color = SleekTextPrimary) },
                    onClick = {
                        onSelect(i)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Float, Float, Float, Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Log", color = SleekTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SleekTextPrimary,
                        unfocusedTextColor = SleekTextPrimary,
                        focusedLabelColor = SleekBlue,
                        unfocusedLabelColor = SleekTextMuted,
                        focusedBorderColor = SleekBlue,
                        unfocusedBorderColor = SleekBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary,
                            focusedLabelColor = SleekBlue,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text("Calories") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary,
                            focusedLabelColor = SleekBlue,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary,
                            focusedLabelColor = SleekBlue,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = it },
                        label = { Text("Fats (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary,
                            focusedLabelColor = SleekBlue,
                            unfocusedLabelColor = SleekTextMuted,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty()) {
                        val p = protein.toFloatOrNull() ?: 0f
                        val cal = calories.toFloatOrNull() ?: 0f
                        val carb = carbs.toFloatOrNull() ?: 0f
                        val fat = fats.toFloatOrNull() ?: 0f
                        onConfirm(name, p, cal, carb, fat)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SleekBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SleekTextMuted, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = SleekSurface
    )
}

object ToastUtil {
    fun show(context: Context, message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
