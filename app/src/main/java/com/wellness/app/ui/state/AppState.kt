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
import java.time.DayOfWeek
import java.time.LocalDate

enum class Tab(val key: String) { Home("home"), Nutrition("nutrition"), Plan("plan"), Profile("profile") }

enum class Gender(val title: String) { Male("Мужской"), Female("Женский"), Other("Не указан") }

// ── Habits ────────────────────────────────────────────────────────────────
//
// `days` is the set of ISO weekdays (1=Mon … 7=Sun) on which the habit is
// scheduled. `log` is the per-day progress count keyed by ISO-8601 date.
//
// Progress is intentionally a per-date map rather than a single counter so
// "did you actually do it today" stays truthful when the day rolls over —
// yesterday's progress doesn't bleed into today.

data class Habit(
    val id: Int,
    val name: String,
    val icon: String,
    val color: Color,
    val target: Int,
    val unit: String,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val remind: Boolean = false,
    val remindAt: String? = null,            // "HH:mm" or null
    val log: Map<String, Int> = emptyMap(),  // dateKey -> progress count
) {
    /** Progress count recorded for [dateKey]. 0 if untouched. */
    fun progressOn(dateKey: String): Int = log[dateKey] ?: 0

    /** Done if today's progress reached the target (target=1 → simple toggle). */
    fun isDoneOn(dateKey: String): Boolean = progressOn(dateKey) >= target

    /** Whether the habit is scheduled on the given ISO weekday (1..7). */
    fun isScheduledOn(isoDow: Int): Boolean = isoDow in days

    /** Compact human-readable schedule like "Ежедневно", "Будни", "Пн Ср Пт". */
    fun scheduleText(): String = scheduleTextFor(days)
}

fun scheduleTextFor(days: Set<Int>): String {
    if (days.isEmpty()) return "—"
    if (days.size == 7) return "Ежедневно"
    if (days == setOf(1, 2, 3, 4, 5)) return "Будни"
    if (days == setOf(6, 7)) return "Выходные"
    val short = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    return days.sorted().joinToString(" ") { short[it - 1] }
}

// ── Other entities ────────────────────────────────────────────────────────

data class WaterEntry(val ml: Int, val time: String, val label: String, val icon: String)

data class Meal(val name: String, val title: String, val icon: String, val color: Color, val kcal: Int?, val description: String?)

enum class TaskStatus { Done, Live, Upcoming }

// `startMinutes` / `endMinutes` are minutes from midnight. `completions`
// records the dateKeys on which the user marked the task done — the live
// status is otherwise derived from the current wall-clock minute.

data class TaskItem(
    val id: Int,
    val name: String,
    val icon: String = "alarm-bold-duotone",
    val color: Color = WellnessColors.Purple,
    val startMinutes: Int,
    val endMinutes: Int,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val remind: Boolean = false,
    val remindMinutesBefore: Int = 10,
    val completions: Set<String> = emptySet(),
) {
    val startTime: String get() = "%02d:%02d".format(startMinutes / 60, startMinutes % 60)
    val endTime: String get() = "%02d:%02d".format(endMinutes / 60, endMinutes % 60)
    val durationLabel: String get() = formatDurationLabel((endMinutes - startMinutes).coerceAtLeast(0))

    /** Whether the task is scheduled on the given ISO weekday (1..7). */
    fun isScheduledOn(isoDow: Int): Boolean = isoDow in days

    fun isCompletedOn(dateKey: String): Boolean = dateKey in completions

    fun statusAt(nowMinutes: Int, dateKey: String): TaskStatus {
        if (isCompletedOn(dateKey)) return TaskStatus.Done
        return when {
            nowMinutes >= endMinutes -> TaskStatus.Done
            nowMinutes in startMinutes until endMinutes -> TaskStatus.Live
            else -> TaskStatus.Upcoming
        }
    }

    fun statusTextAt(nowMinutes: Int, dateKey: String): String {
        if (isCompletedOn(dateKey)) return "Выполнено"
        return when (statusAt(nowMinutes, dateKey)) {
            TaskStatus.Done -> "Выполнено"
            TaskStatus.Live -> "Идёт сейчас · до конца ${formatRelative(endMinutes - nowMinutes)}"
            TaskStatus.Upcoming -> {
                val left = startMinutes - nowMinutes
                if (left <= 0) "Запланировано" else "Через ${formatRelative(left)}"
            }
        }
    }
}

private fun formatDurationLabel(total: Int): String = when {
    total <= 0 -> "—"
    total < 60 -> "${total}м"
    total % 60 == 0 -> "${total / 60}ч"
    else -> "${total / 60}ч ${total % 60}м"
}

private fun formatRelative(total: Int): String = when {
    total <= 0 -> "сейчас"
    total < 60 -> "${total} мин"
    total % 60 == 0 -> "${total / 60}ч"
    else -> "${total / 60}ч ${total % 60}м"
}

data class SleepDay(val label: String, val height: Float, val highlighted: Boolean = false)

// ── Date helpers ──────────────────────────────────────────────────────────

object Dates {
    /** Today's date as "yyyy-MM-dd". */
    fun todayKey(): String = LocalDate.now().toString()

    /** ISO weekday for today: 1=Mon … 7=Sun. */
    fun todayDow(): Int = LocalDate.now().dayOfWeek.value

    fun dowFor(date: LocalDate): Int = date.dayOfWeek.value

    fun dayOfWeekFromIso(iso: Int): DayOfWeek = DayOfWeek.of(iso)
}

// ── AppState ──────────────────────────────────────────────────────────────

class AppState(private val bindingStore: TelegramBindingStore? = null) {
    var themeMode by mutableStateOf(ThemeMode.Dark)
    var accent by mutableStateOf<Color>(WellnessColors.Purple)

    // Telegram binding — restored from disk on construction.
    var telegramUser by mutableStateOf(bindingStore?.load())
        private set

    fun bindTelegram(user: TelegramAuth.TelegramUser) {
        telegramUser = user
        bindingStore?.save(user)
        val tgName = user.displayName.trim()
        if (tgName.isNotEmpty()) {
            userName = tgName
        }
    }

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
        Tab.Home, Tab.Nutrition, Tab.Plan, Tab.Profile,
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

    // ── Habits (real, day-scheduled, with daily progress log) ─────────────
    //
    // Default seed gives the user something visible on first launch so they
    // can poke at the system before creating anything. Days are ISO (1=Mon).
    val habits: SnapshotStateList<Habit> = mutableStateListOf(
        Habit(1, "Вода", "bottle-bold-duotone", WellnessColors.Water, 8, "стаканов"),
        Habit(2, "Чтение", "book-bookmark-bold-duotone", WellnessColors.Purple, 20, "страниц"),
        Habit(3, "Медитация", "meditation-round-bold-duotone", WellnessColors.Orange, 10, "мин",
            days = setOf(1, 3, 5)),
        Habit(4, "Зарядка", "dumbbell-large-bold-duotone", WellnessColors.Cal, 1, ""),
    )

    /** Habits scheduled for today (ISO-DOW filtered). Recomputed on every
     *  read inside @Composable — reads from `habits` snapshot list, so any
     *  mutation triggers recomposition. */
    fun habitsToday(): List<Habit> {
        val dow = Dates.todayDow()
        return habits.filter { it.isScheduledOn(dow) }
    }

    /** Increment today's progress for a habit. For target<=1 toggles between
     *  0 and 1; for larger targets it bumps by 1 and wraps to 0 once the
     *  target is reached (matches the home-screen ring filling up). */
    fun tapHabit(habitId: Int) {
        val idx = habits.indexOfFirst { it.id == habitId }
        if (idx < 0) return
        val h = habits[idx]
        val k = Dates.todayKey()
        val cur = h.progressOn(k)
        val next = when {
            h.target <= 1 -> if (cur >= 1) 0 else 1
            cur >= h.target -> 0
            else -> cur + 1
        }
        habits[idx] = h.copy(log = h.log + (k to next))
    }

    fun addHabit(h: Habit): Habit {
        val id = (habits.maxOfOrNull { it.id } ?: 0) + 1
        val withId = h.copy(id = id)
        habits.add(withId)
        return withId
    }

    fun deleteHabit(habitId: Int) {
        val idx = habits.indexOfFirst { it.id == habitId }
        if (idx >= 0) habits.removeAt(idx)
    }

    // ── Schedule (tasks) ──────────────────────────────────────────────────
    val tasks: SnapshotStateList<TaskItem> = mutableStateListOf(
        TaskItem(1, "Утренняя пробежка", "running-bold-duotone", WellnessColors.Mint,
            startMinutes = 7 * 60, endMinutes = 7 * 60 + 30,
            days = setOf(1, 2, 3, 4, 5), remind = true, remindMinutesBefore = 10),
        TaskItem(2, "Завтрак", "sun-2-bold-duotone", WellnessColors.Orange,
            startMinutes = 8 * 60, endMinutes = 8 * 60 + 20),
        TaskItem(3, "Тренировка · Ноги", "dumbbell-bold-duotone", WellnessColors.Cal,
            startMinutes = 16 * 60 + 30, endMinutes = 17 * 60 + 15,
            days = setOf(1, 3, 5), remind = true, remindMinutesBefore = 10),
        TaskItem(4, "Ужин и отдых", "cup-hot-bold-duotone", WellnessColors.Carb,
            startMinutes = 18 * 60 + 30, endMinutes = 19 * 60 + 30),
        TaskItem(5, "Чтение перед сном", "book-bold-duotone", WellnessColors.Purple,
            startMinutes = 21 * 60, endMinutes = 21 * 60 + 20, remind = true, remindMinutesBefore = 5),
    )

    /** Tasks scheduled for today. */
    fun tasksToday(): List<TaskItem> {
        val dow = Dates.todayDow()
        return tasks.filter { it.isScheduledOn(dow) }
    }

    /** Toggle task completion for today. */
    fun toggleTaskDone(taskId: Int) {
        val idx = tasks.indexOfFirst { it.id == taskId }
        if (idx < 0) return
        val t = tasks[idx]
        val k = Dates.todayKey()
        val next = if (k in t.completions) t.completions - k else t.completions + k
        tasks[idx] = t.copy(completions = next)
    }

    fun addTask(t: TaskItem): TaskItem {
        val id = (tasks.maxOfOrNull { it.id } ?: 0) + 1
        val withId = t.copy(id = id)
        tasks.add(withId)
        return withId
    }

    fun deleteTask(taskId: Int) {
        val idx = tasks.indexOfFirst { it.id == taskId }
        if (idx >= 0) tasks.removeAt(idx)
    }

    // Weight
    //
    // `weightStart` is the user's weight when they first set the current
    // goal — the burn-down progress on the profile screen is `(start -
    // current) / (start - goal)`. We seed it slightly higher than the
    // initial `weight` so the bar lands at a non-zero, non-100 % value
    // on a fresh install (matches the prototype's example) and so the
    // user can see the bar respond as soon as they log a weigh-in.
    var weight by mutableStateOf(78.4f)
    var weightGoal by mutableStateOf(75.0f)
    var weightStart by mutableStateOf(80.0f)

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
        val today = habitsToday()
        val habitsP = if (today.isEmpty()) 0f else
            today.count { it.isDoneOn(Dates.todayKey()) }.toFloat() / today.size
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
