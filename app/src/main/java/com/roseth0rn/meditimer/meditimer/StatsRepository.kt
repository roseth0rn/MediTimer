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
        val json  = prefs[SESSIONS_KEY] ?: return emptyList()
        val type  = object : TypeToken<List<Session>>() {}.type
        return gson.fromJson(json, type)
    }

    suspend fun getStats(): Pair<StatsResult, List<Boolean>> {
        val sessions = getSessions()
        val today    = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)

        val weekDots = (0..6).map { i ->
            val day = weekStart.plusDays(i.toLong())
            sessions.any { LocalDate.parse(it.date) == day }
        }

        var weeklyStreak = 0
        var ws = weekStart
        for (i in 0..51) {
            val we  = ws.plusDays(6)
            val has = sessions.any {
                val d = LocalDate.parse(it.date); !d.isBefore(ws) && !d.isAfter(we)
            }
            if (has) { weeklyStreak++; ws = ws.minusWeeks(1) } else break
        }

        val monthMinutes = sessions
            .filter { LocalDate.parse(it.date).month == today.month &&
                    LocalDate.parse(it.date).year  == today.year }
            .sumOf { it.durationMinutes }

        return Pair(StatsResult(weeklyStreak, monthMinutes), weekDots)
    }

    // Minutes per day for the current week (Mon–Sun)
    suspend fun getWeeklyMinutes(): List<Int> {
        val sessions  = getSessions()
        val today     = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        return (0..6).map { i ->
            val day = weekStart.plusDays(i.toLong())
            sessions.filter { LocalDate.parse(it.date) == day }.sumOf { it.durationMinutes }
        }
    }

    // Total sessions this calendar month
    suspend fun getMonthSessions(): Int {
        val today = LocalDate.now()
        return getSessions().count {
            LocalDate.parse(it.date).month == today.month &&
                    LocalDate.parse(it.date).year  == today.year
        }
    }

    // Best weekly streak ever recorded
    suspend fun getBestStreak(): Int {
        val sessions = getSessions()
        if (sessions.isEmpty()) return 0
        val earliest = sessions.minOf { LocalDate.parse(it.date) }
        val today    = LocalDate.now()
        var best     = 0
        var current  = 0
        var ws       = earliest.minusDays(earliest.dayOfWeek.value.toLong() - 1)
        while (!ws.isAfter(today)) {
            val we  = ws.plusDays(6)
            val has = sessions.any {
                val d = LocalDate.parse(it.date); !d.isBefore(ws) && !d.isAfter(we)
            }
            if (has) { current++; best = maxOf(best, current) } else current = 0
            ws = ws.plusWeeks(1)
        }
        return best
    }
}