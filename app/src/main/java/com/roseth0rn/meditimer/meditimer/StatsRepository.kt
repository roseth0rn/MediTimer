package com.roseth0rn.meditimer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "medi_stats")

data class Session(val date: String, val durationMinutes: Int)
data class StatsResult(val weeklyStreak: Int, val monthMinutes: Int)

class StatsRepository(private val context: Context) {
    private val gson = Gson()
    private val SESSIONS_KEY = stringPreferencesKey("sessions")

    suspend fun saveSession(durationMinutes: Int) {
        val sessions = getSessions().toMutableList()
        sessions.add(Session(LocalDate.now().toString(), durationMinutes))
        context.dataStore.edit { it[SESSIONS_KEY] = gson.toJson(sessions) }
    }

    suspend fun getSessions(): List<Session> {
        val prefs = context.dataStore.data.first()
        val json = prefs[SESSIONS_KEY] ?: return emptyList()
        val type = object : TypeToken<List<Session>>() {}.type
        return gson.fromJson(json, type)
    }

    suspend fun getStats(): Pair<StatsResult, List<Boolean>> {
        val sessions = getSessions()
        val today = LocalDate.now()

        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val weekDots = (0..6).map { i ->
            val day = weekStart.plusDays(i.toLong())
            sessions.any { LocalDate.parse(it.date) == day }
        }

        var weeklyStreak = 0
        var ws = weekStart
        for (i in 0..51) {
            val we = ws.plusDays(6)
            val has = sessions.any {
                val d = LocalDate.parse(it.date)
                !d.isBefore(ws) && !d.isAfter(we)
            }
            if (has) { weeklyStreak++; ws = ws.minusWeeks(1) } else break
        }

        val monthMinutes = sessions
            .filter {
                LocalDate.parse(it.date).month == today.month &&
                        LocalDate.parse(it.date).year == today.year
            }
            .sumOf { it.durationMinutes }

        return Pair(StatsResult(weeklyStreak, monthMinutes), weekDots)
    }
}