package com.wellness.app.ui.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.wellness.app.telegram.TelegramAuth
import com.wellness.app.telegram.TelegramBindingStore
import com.wellness.app.ui.theme.ThemeMode
import com.wellness.app.ui.theme.WellnessColors

enum class Tab(val key: String) { Home("home"), Nutrition("nutrition"), Plan("plan"), Trackers("trackers"), Profile("profile") }

enum class Gender(val title: String) { Male("Мужской"), Female("Женский"), Other("Не указан") }

data class Habit(
    val id: Int,
    val name: String,
    val icon: String,
    val color: Color,
    val target: Int,
    var progress: Int,
    val unit: String,
    val schedule: String,
    val done: Boolean = false,
)

data class WaterEntry(val ml: Int, val time: String, val label: String, val icon: String)

data class Meal(val name: String, val title: String, val icon: String, val color: Color, val kcal: Int?, val description: String?)

data class TaskItem(
    val id: Int,
    val name: String,
    val time: String,
    val duration: String,
    val status: TaskStatus,
    val statusText: String,
)

enum class TaskStatus { Done, Live, Upcoming }

data class SleepDay(val label: String, val height: Float, val highlighted: Boolean = false)

class AppState(private val bindingStore: TelegramBindingStore? = null) {
    var themeMode by mutableStateOf(ThemeMode.Dark)
    var accent by mutableStateOf<Color>(WellnessColors.Purple)

    // Telegram binding — restored from disk on construction. The bindings
    // screen reads this for its "bound vs not bound" branch, and writes
    // through `bindTelegram` / `unbindTelegram` so the change is durable
    // across process restarts. `null` means "not bound".
    var telegramUser by mutableStateOf(bindingStore?.load())
        private set

    fun bindTelegram(user: TelegramAuth.TelegramUser) {
        telegramUser = user
        bindingStore?.save(user)
        // Sync the canonical display name into `userName` so the profile
        // editor seeds with the same value that the profile header shows
        // (otherwise the editor opens with the stale "Иван" placeholder
        // while the header proudly displays the TG first/last name).
        val tgName = user.displayName.trim()
        if (tgName.isNotEmpty()) {
            userName = tgName
        }
    }

    /**
     * Patch the bound user's profile photo URL once the asynchronous
     * `getUserProfilePhotos` -> `getFile` round trip resolves. No-op if
     * the user has been unbound in the meantime (avoids resurrecting a
     * cleared binding from a stale coroutine).
     */
    fun updateTelegramPhoto(photoUrl: String?) {
        val current = telegramUser ?: return
        val patched = current.copy(photoUrl = photoUrl)
        telegramUser = patched
        bindingStore?.save(patched)
    }

    fun unbindTelegram() {
        telegramUser = null
        bindingStore?.clear()
    }

    // Navbar configuration — order of items and which tab opens by default.
    val navbarOrder: SnapshotStateList<Tab> = mutableStateListOf(
        Tab.Home, Tab.Nutrition, Tab.Plan, Tab.Trackers, Tab.Profile,
    )
    var defaultTab by mutableStateOf(Tab.Home)
    var currentTab by mutableStateOf(defaultTab)

    // Water
    var waterMl by mutableStateOf(1750)
    var waterTarget by mutableStateOf(2500)
    val waterHistory: SnapshotStateList<WaterEntry> = mutableStateListOf(
        WaterEntry(350, "12:40", "Стакан воды", "cup-paper-bold-duotone"),
        WaterEntry(500, "10:15", "Бутылка", "bottle-bold-duotone"),
        WaterEntry(250, "08:02", "Утро", "waterdrop-outline"),
    )

    // Nutrition / calories
    var kcal by mutableStateOf(1420)
    var kcalTarget by mutableStateOf(2300)
    var protein by mutableStateOf(72)
    val proteinTarget = 130
    var fat by mutableStateOf(42)
    val fatTarget = 70
    var carb by mutableStateOf(160)
    val carbTarget = 280

    val meals: SnapshotStateList<Meal> = mutableStateListOf(
        Meal("breakfast", "Завтрак", "sun-outline", WellnessColors.Orange, 420, "Овсянка с ягодами"),
        Meal("lunch", "Обед", "plate-bold-duotone", WellnessColors.Cal, 620, "Курица с рисом"),
        Meal("dinner", "Ужин", "moon-outline", WellnessColors.Purple, null, "Не добавлено"),
        Meal("snack", "Перекусы", "donut-bitten-outline", WellnessColors.Pink, 380, "Яблоко, орехи"),
    )

    // Habits
    val habits: SnapshotStateList<Habit> = mutableStateListOf(
        Habit(1, "Вода", "bottle-bold-duotone", WellnessColors.Water, 8, 7, "стаканов", "Ежедневно"),
        Habit(2, "Чтение", "book-bookmark-bold-duotone", WellnessColors.Purple, 20, 20, "страниц", "Ежедневно"),
        Habit(3, "Медитация", "meditation-round-bold-duotone", WellnessColors.Orange, 10, 0, "мин", "Пн Ср Пт"),
        Habit(4, "Зарядка", "dumbbell-large-bold-duotone", WellnessColors.Cal, 1, 0, "", "Ежедневно"),
    )

    // Habit list on home screen (today)
    val todayHabits: SnapshotStateList<Habit> = mutableStateListOf(
        Habit(1, "Выпить 8 стаканов воды", "bottle-bold-duotone", WellnessColors.Water, 8, 7, "", "", done = true),
        Habit(2, "Прочитать 20 страниц", "book-bookmark-bold-duotone", WellnessColors.Purple, 20, 20, "", "", done = true),
        Habit(3, "Медитация 10 минут", "meditation-round-bold-duotone", WellnessColors.Orange, 10, 0, "", "", done = false),
        Habit(4, "Зарядка", "dumbbell-large-bold-duotone", WellnessColors.Cal, 1, 0, "", "", done = false),
    )

    // Schedule
    val tasks: SnapshotStateList<TaskItem> = mutableStateListOf(
        TaskItem(1, "Утренняя пробежка", "07:00", "30м", TaskStatus.Done, "Выполнено"),
        TaskItem(2, "Завтрак", "08:00", "20м", TaskStatus.Done, "Выполнено"),
        TaskItem(3, "Тренировка · Ноги", "16:30", "45м", TaskStatus.Live, "Идёт сейчас · до конца 23 мин"),
        TaskItem(4, "Ужин и отдых", "18:30", "1ч", TaskStatus.Upcoming, "Через 1ч 35м"),
        TaskItem(5, "Чтение перед сном", "21:00", "20м", TaskStatus.Upcoming, "Через 4ч 05м"),
    )

    // Weight
    var weight by mutableStateOf(78.4f)
    var weightGoal by mutableStateOf(75.0f)

    // Sleep last 7 days (relative height 0..1)
    val sleep: List<SleepDay> = listOf(
        SleepDay("Пн", 0.55f),
        SleepDay("Вт", 0.75f),
        SleepDay("Ср", 0.60f),
        SleepDay("Чт", 0.80f),
        SleepDay("Пт", 0.70f),
        SleepDay("Сб", 0.90f),
        SleepDay("Вс", 0.74f, highlighted = true),
    )

    // Profile
    var userName by mutableStateOf("Иван")
    var age by mutableStateOf(22)
    var gender by mutableStateOf(Gender.Male)

    // Notifications (very lightweight UI-only flags for the screen)
    var notifyMorning by mutableStateOf(true)
    var notifyHabits by mutableStateOf(true)
    var notifyWater by mutableStateOf(false)

    // Legacy aliases retained for callers that still read the immutable goal.
    val waterGoal: Int get() = waterTarget
    val kcalGoal: Int get() = kcalTarget

    fun overallProgress(): Float {
        val w = waterMl.toFloat() / waterTarget
        val k = kcal.toFloat() / kcalTarget
        val habitsP = todayHabits.count { it.done }.toFloat() / todayHabits.size
        return ((w + k + habitsP) / 3f).coerceIn(0f, 1f)
    }
}

val LocalAppState = compositionLocalOf<AppState> { error("AppState not provided") }

@Composable
fun rememberAppState(): AppState {
    val context = LocalContext.current.applicationContext
    return remember {
        AppState(bindingStore = TelegramBindingStore(context))
    }
}
