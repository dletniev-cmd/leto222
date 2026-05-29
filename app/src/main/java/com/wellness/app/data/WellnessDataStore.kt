package com.wellness.app.data

import android.content.Context
import com.wellness.app.ui.state.SleepEntry
import com.wellness.app.ui.state.WeightEntry

/**
 * SharedPreferences-backed persistence for the user's REAL logged data —
 * weigh-ins and sleep records — plus the weight goal scalars. Mirrors the
 * lightweight approach of [com.wellness.app.telegram.TelegramBindingStore].
 *
 * Logs are encoded as flat strings (record separator ';', field separator
 * '|') to avoid pulling in a JSON/serialization dependency. The date keys
 * are ISO "yyyy-MM-dd" so they never contain the separators.
 */
class WellnessDataStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("wellness.data", Context.MODE_PRIVATE)

    // ── Weight scalars ────────────────────────────────────────────────
    fun loadWeight(default: Float): Float = prefs.getFloat(KEY_WEIGHT, default)
    fun loadWeightGoal(default: Float): Float = prefs.getFloat(KEY_WEIGHT_GOAL, default)
    fun loadWeightStart(default: Float): Float = prefs.getFloat(KEY_WEIGHT_START, default)

    fun saveWeightScalars(weight: Float, goal: Float, start: Float) {
        prefs.edit()
            .putFloat(KEY_WEIGHT, weight)
            .putFloat(KEY_WEIGHT_GOAL, goal)
            .putFloat(KEY_WEIGHT_START, start)
            .apply()
    }

    fun loadSleepGoal(default: Int): Int = prefs.getInt(KEY_SLEEP_GOAL, default)

    // ── Weight log ────────────────────────────────────────────────────
    fun loadWeightLog(): List<WeightEntry> {
        val raw = prefs.getString(KEY_WEIGHT_LOG, null) ?: return emptyList()
        return raw.split(RECORD_SEP).mapNotNull { token ->
            if (token.isBlank()) return@mapNotNull null
            val p = token.split(FIELD_SEP)
            if (p.size != 2) return@mapNotNull null
            val kg = p[1].toFloatOrNull() ?: return@mapNotNull null
            WeightEntry(p[0], kg)
        }
    }

    fun saveWeightLog(entries: List<WeightEntry>) {
        val raw = entries.joinToString(RECORD_SEP) { "${it.dateKey}$FIELD_SEP${it.kg}" }
        prefs.edit().putString(KEY_WEIGHT_LOG, raw).apply()
    }

    // ── Sleep log ─────────────────────────────────────────────────────
    fun loadSleepLog(): List<SleepEntry> {
        val raw = prefs.getString(KEY_SLEEP_LOG, null) ?: return emptyList()
        return raw.split(RECORD_SEP).mapNotNull { token ->
            if (token.isBlank()) return@mapNotNull null
            val p = token.split(FIELD_SEP)
            if (p.size != 4) return@mapNotNull null
            val from = p[1].toIntOrNull() ?: return@mapNotNull null
            val to = p[2].toIntOrNull() ?: return@mapNotNull null
            val q = p[3].toIntOrNull() ?: return@mapNotNull null
            SleepEntry(p[0], from, to, q)
        }
    }

    fun saveSleepLog(entries: List<SleepEntry>) {
        val raw = entries.joinToString(RECORD_SEP) {
            "${it.dateKey}$FIELD_SEP${it.fromMinutes}$FIELD_SEP${it.toMinutes}$FIELD_SEP${it.quality}"
        }
        prefs.edit().putString(KEY_SLEEP_LOG, raw).apply()
    }

    private companion object {
        const val RECORD_SEP = ";"
        const val FIELD_SEP = "|"
        const val KEY_WEIGHT = "weight"
        const val KEY_WEIGHT_GOAL = "weight_goal"
        const val KEY_WEIGHT_START = "weight_start"
        const val KEY_SLEEP_GOAL = "sleep_goal"
        const val KEY_WEIGHT_LOG = "weight_log"
        const val KEY_SLEEP_LOG = "sleep_log"
    }
}
