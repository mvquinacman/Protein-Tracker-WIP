package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ProteinLog
import com.example.data.ProteinRepository
import com.example.network.GeminiClient
import com.example.network.FoodAnalysisResult
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProteinViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProteinRepository
    private val sharedPrefs = application.getSharedPreferences("protein_tracker_prefs", Context.MODE_PRIVATE)

    // User goals
    val proteinGoal = MutableStateFlow(sharedPrefs.getFloat("protein_goal", 120f))
    val caloriesGoal = MutableStateFlow(sharedPrefs.getFloat("calories_goal", 2000f))
    val carbsGoal = MutableStateFlow(sharedPrefs.getFloat("carbs_goal", 250f))
    val fatsGoal = MutableStateFlow(sharedPrefs.getFloat("fats_goal", 70f))

    // Reminder setting
    val reminderEnabled = MutableStateFlow(sharedPrefs.getBoolean("reminder_enabled", true))
    val reminderHour = MutableStateFlow(sharedPrefs.getInt("reminder_hour", 20)) // 8 PM default
    val reminderMinute = MutableStateFlow(sharedPrefs.getFloat("reminder_minute", 0f).toInt())

    // All logs
    val allLogs: StateFlow<List<ProteinLog>>

    // Today's stats
    val todayProtein: StateFlow<Float>
    val todayCalories: StateFlow<Float>
    val todayCarbs: StateFlow<Float>
    val todayFats: StateFlow<Float>

    // Gemini API Scanning State
    val scanState = MutableStateFlow<ScanState>(ScanState.Idle)

    // Gamified Streak Count
    val streakCount: StateFlow<Int>

    // Favorites state
    private val _favorites = MutableStateFlow<List<FavoriteFood>>(emptyList())
    val favorites: StateFlow<List<FavoriteFood>> = _favorites.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProteinRepository(database.proteinDao())

        // Load favorites from SharedPreferences
        loadFavorites()

        // Get logs starting from 7 days ago to include the full week
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        allLogs = repository.getLogsSince(calendar.timeInMillis).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Derive today's totals
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        todayProtein = allLogs.combine(MutableStateFlow(todayStart)) { logs, start ->
            logs.filter { it.timestamp >= start }.sumOf { it.proteinGrams.toDouble() }.toFloat()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        todayCalories = allLogs.combine(MutableStateFlow(todayStart)) { logs, start ->
            logs.filter { it.timestamp >= start }.sumOf { it.calories.toDouble() }.toFloat()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        todayCarbs = allLogs.combine(MutableStateFlow(todayStart)) { logs, start ->
            logs.filter { it.timestamp >= start }.sumOf { it.carbsGrams.toDouble() }.toFloat()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        todayFats = allLogs.combine(MutableStateFlow(todayStart)) { logs, start ->
            logs.filter { it.timestamp >= start }.sumOf { it.fatsGrams.toDouble() }.toFloat()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        // Calculate dynamic streak
        streakCount = allLogs.combine(proteinGoal) { logs, goal ->
            calculateStreak(logs, goal)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        // Auto-populate mock logs on first launch so the user can visualize the app processing data immediately
        val isFirstLaunchPopulated = sharedPrefs.getBoolean("first_launch_populated", false)
        if (!isFirstLaunchPopulated) {
            injectDemoWeekLogs()
            sharedPrefs.edit().putBoolean("first_launch_populated", true).apply()
        }
    }

    // Goal persistence
    fun updateProteinGoal(value: Float) {
        proteinGoal.value = value
        sharedPrefs.edit().putFloat("protein_goal", value).apply()
        rescheduleNotification()
    }

    fun updateCaloriesGoal(value: Float) {
        caloriesGoal.value = value
        sharedPrefs.edit().putFloat("calories_goal", value).apply()
    }

    fun updateCarbsGoal(value: Float) {
        carbsGoal.value = value
        sharedPrefs.edit().putFloat("carbs_goal", value).apply()
    }

    fun updateFatsGoal(value: Float) {
        fatsGoal.value = value
        sharedPrefs.edit().putFloat("fats_goal", value).apply()
    }

    fun updateReminderSetting(enabled: Boolean, hour: Int, minute: Int) {
        reminderEnabled.value = enabled
        reminderHour.value = hour
        reminderMinute.value = minute
        sharedPrefs.edit()
            .putBoolean("reminder_enabled", enabled)
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()
        rescheduleNotification()
    }

    private fun rescheduleNotification() {
        val context = getApplication<Application>().applicationContext
        if (reminderEnabled.value) {
            NotificationHelper.scheduleDailyReminder(
                context,
                reminderHour.value,
                reminderMinute.value,
                todayProtein.value,
                proteinGoal.value
            )
        } else {
            NotificationHelper.cancelReminder(context)
        }
    }

    fun triggerInstantReminderNotification() {
        val context = getApplication<Application>().applicationContext
        val target = proteinGoal.value
        val current = todayProtein.value
        val remaining = (target - current).coerceAtLeast(0f)

        val title = "Macro Target Reminder"
        val message = if (remaining > 0) {
            "You need ${remaining.toInt()}g more protein to hit your daily goal of ${target.toInt()}g today! Fuel up!"
        } else {
            "Excellent! You hit your daily protein goal of ${target.toInt()}g!"
        }
        NotificationHelper.showNotification(context, title, message)
    }

    // Log manipulation
    fun logProtein(foodName: String, protein: Float, calories: Float, carbs: Float, fats: Float) {
        viewModelScope.launch {
            val log = ProteinLog(
                foodName = foodName,
                proteinGrams = protein,
                calories = calories,
                carbsGrams = carbs,
                fatsGrams = fats,
                timestamp = System.currentTimeMillis()
            )
            repository.insertLog(log)
            rescheduleNotification() // Update reminder with latest stats
        }
    }

    // Inject past logs for demo weekly display
    fun injectDemoWeekLogs() {
        viewModelScope.launch {
            repository.clearAll()
            val calendar = Calendar.getInstance()
            val random = java.util.Random()
            
            // Rich, highly realistic high-protein foods database for past days
            val foods = listOf(
                "Grilled Chicken Breast" to Triple(35f, 190f, Pair(0f, 3.5f)),
                "Whey Protein Shake" to Triple(25f, 120f, Pair(3f, 1.5f)),
                "Greek Yogurt Bowl" to Triple(18f, 150f, Pair(12f, 4f)),
                "Pan-Seared Salmon" to Triple(28f, 260f, Pair(0f, 15f)),
                "Ribeye Steak" to Triple(40f, 450f, Pair(0f, 32f)),
                "Scrambled Eggs (3)" to Triple(18f, 210f, Pair(2f, 15f)),
                "Tuna Salad Cup" to Triple(26f, 160f, Pair(4f, 5f)),
                "Cottage Cheese" to Triple(14f, 110f, Pair(6f, 2.5f)),
                "Beef Jerky Snack" to Triple(12f, 80f, Pair(3f, 1f)),
                "Peanut Butter Scoop" to Triple(8f, 190f, Pair(6f, 16f))
            )

            // Inject logs for last 6 days (excluding today)
            for (i in 1..6) {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                // 2-3 logs per day to hit/approach target
                val numLogs = random.nextInt(2) + 2
                for (j in 0 until numLogs) {
                    val foodIdx = random.nextInt(foods.size)
                    val (name, macros) = foods[foodIdx]
                    val p = macros.first + random.nextInt(6) - 3
                    val cal = macros.second + random.nextInt(40) - 20
                    val carbs = macros.third.first + random.nextInt(4) - 2
                    val fats = macros.third.second + random.nextInt(4) - 2
                    
                    // Set food time (e.g., morning, afternoon, evening)
                    val logCalendar = calendar.clone() as Calendar
                    logCalendar.set(Calendar.HOUR_OF_DAY, 8 + (j * 5) + random.nextInt(2))
                    logCalendar.set(Calendar.MINUTE, random.nextInt(60))
                    
                    repository.insertLog(
                        ProteinLog(
                            foodName = name,
                            proteinGrams = p.coerceAtLeast(5f),
                            calories = cal.coerceAtLeast(40f),
                            carbsGrams = carbs.coerceAtLeast(0f),
                            fatsGrams = fats.coerceAtLeast(0f),
                            timestamp = logCalendar.timeInMillis
                        )
                    )
                }
            }

            // Also add standard, perfectly realistic high-protein logs for today
            // Breakfast: Greek Yogurt Bowl
            repository.insertLog(
                ProteinLog(
                    foodName = "Greek Yogurt Bowl",
                    proteinGrams = 18f,
                    calories = 150f,
                    carbsGrams = 12f,
                    fatsGrams = 4f,
                    timestamp = System.currentTimeMillis() - (8 * 3600 * 1000) // 8 hours ago (breakfast)
                )
            )

            // Lunch: Grilled Chicken Breast
            repository.insertLog(
                ProteinLog(
                    foodName = "Grilled Chicken Breast with Brown Rice",
                    proteinGrams = 42f,
                    calories = 380f,
                    carbsGrams = 35f,
                    fatsGrams = 6f,
                    timestamp = System.currentTimeMillis() - (4 * 3600 * 1000) // 4 hours ago (lunch)
                )
            )

            // Afternoon Snack: Whey Protein Shake
            repository.insertLog(
                ProteinLog(
                    foodName = "Whey Protein Shake",
                    proteinGrams = 25f,
                    calories = 120f,
                    carbsGrams = 3f,
                    fatsGrams = 1.5f,
                    timestamp = System.currentTimeMillis() - (1 * 3600 * 1000) // 1 hour ago (snack)
                )
            )
            
            rescheduleNotification()
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteLogById(id)
            rescheduleNotification()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAll()
            rescheduleNotification()
        }
    }

    // Gemini API photo detection
    fun detectNutritionFromImage(bitmap: Bitmap) {
        scanState.value = ScanState.Loading
        viewModelScope.launch {
            try {
                val result = GeminiClient.analyzeFoodImage(bitmap)
                scanState.value = ScanState.Success(result)
            } catch (e: Exception) {
                scanState.value = ScanState.Error(e.message ?: "Failed to detect protein. Please try again.")
            }
        }
    }

    fun resetScanState() {
        scanState.value = ScanState.Idle
    }

    // Get aggregate protein daily logs for the last 7 days (including today)
    fun getWeeklySummary(): List<DailyProgress> {
        val logs = allLogs.value
        val resultList = mutableListOf<DailyProgress>()
        val calendar = Calendar.getInstance()

        // Create days Mon-Sun format of last 7 days
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayName = dateFormat.format(calendar.time)

            // Start & End of that calendar day
            val startCal = calendar.clone() as Calendar
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            val endCal = calendar.clone() as Calendar
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)

            val dayLogs = logs.filter { it.timestamp in startCal.timeInMillis..endCal.timeInMillis }
            val proteinSum = dayLogs.sumOf { it.proteinGrams.toDouble() }.toFloat()
            val caloriesSum = dayLogs.sumOf { it.calories.toDouble() }.toFloat()

            resultList.add(DailyProgress(dayName, proteinSum, caloriesSum))
        }
        return resultList
    }

    private fun calculateStreak(logs: List<ProteinLog>, goal: Float): Int {
        if (logs.isEmpty()) return 0
        var streak = 0
        var dayOffset = 0
        while (dayOffset < 365) {
            val checkCal = Calendar.getInstance()
            checkCal.add(Calendar.DAY_OF_YEAR, -dayOffset)
            checkCal.set(Calendar.HOUR_OF_DAY, 0)
            checkCal.set(Calendar.MINUTE, 0)
            checkCal.set(Calendar.SECOND, 0)
            checkCal.set(Calendar.MILLISECOND, 0)
            val startMs = checkCal.timeInMillis

            checkCal.set(Calendar.HOUR_OF_DAY, 23)
            checkCal.set(Calendar.MINUTE, 59)
            checkCal.set(Calendar.SECOND, 59)
            checkCal.set(Calendar.MILLISECOND, 999)
            val endMs = checkCal.timeInMillis

            val dayProtein = logs.filter { it.timestamp in startMs..endMs }.sumOf { it.proteinGrams.toDouble() }.toFloat()

            if (dayProtein >= goal) {
                streak++
            } else {
                if (dayOffset == 0) {
                    // Today not met yet, streak not broken yet, check yesterday
                } else {
                    break
                }
            }
            dayOffset++
        }
        return streak
    }

    private fun loadFavorites() {
        val favSet = sharedPrefs.getStringSet("favorite_foods", null)
        if (favSet == null) {
            val defaults = listOf(
                FavoriteFood("Whey Protein Shake", "Whey Protein Shake", 25f, 120f, 3f, 1.5f),
                FavoriteFood("Grilled Chicken Breast", "Grilled Chicken Breast", 35f, 190f, 0f, 3.5f),
                FavoriteFood("Greek Yogurt Bowl", "Greek Yogurt Bowl", 18f, 150f, 12f, 4f)
            )
            val serialized = defaults.map { it.serialize() }.toSet()
            sharedPrefs.edit().putStringSet("favorite_foods", serialized).apply()
            _favorites.value = defaults
        } else {
            _favorites.value = favSet.mapNotNull { FavoriteFood.deserialize(it) }.sortedBy { it.foodName }
        }
    }

    fun toggleFavorite(foodName: String, protein: Float, calories: Float, carbs: Float, fats: Float) {
        val current = _favorites.value.toMutableList()
        val existing = current.find { it.foodName.equals(foodName, ignoreCase = true) }
        if (existing != null) {
            current.remove(existing)
        } else {
            current.add(FavoriteFood(foodName, foodName, protein, calories, carbs, fats))
        }
        val serialized = current.map { it.serialize() }.toSet()
        sharedPrefs.edit().putStringSet("favorite_foods", serialized).apply()
        _favorites.value = current.sortedBy { it.foodName }
    }

    fun isFavorite(foodName: String): Boolean {
        return _favorites.value.any { it.foodName.equals(foodName, ignoreCase = true) }
    }
}

data class FavoriteFood(
    val id: String,
    val foodName: String,
    val proteinGrams: Float,
    val calories: Float,
    val carbsGrams: Float,
    val fatsGrams: Float
) {
    fun serialize(): String {
        return "$foodName|$proteinGrams|$calories|$carbsGrams|$fatsGrams"
    }

    companion object {
        fun deserialize(str: String): FavoriteFood? {
            val parts = str.split("|")
            if (parts.size < 5) return null
            return FavoriteFood(
                id = parts[0],
                foodName = parts[0],
                proteinGrams = parts[1].toFloatOrNull() ?: 0f,
                calories = parts[2].toFloatOrNull() ?: 0f,
                carbsGrams = parts[3].toFloatOrNull() ?: 0f,
                fatsGrams = parts[4].toFloatOrNull() ?: 0f
            )
        }
    }
}

data class DailyProgress(
    val dayName: String,
    val totalProtein: Float,
    val totalCalories: Float
)

sealed interface ScanState {
    object Idle : ScanState
    object Loading : ScanState
    data class Success(val result: FoodAnalysisResult) : ScanState
    data class Error(val message: String) : ScanState
}
