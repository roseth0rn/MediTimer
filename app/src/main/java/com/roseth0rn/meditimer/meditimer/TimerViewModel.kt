package com.roseth0rn.meditimer

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class TimerState { IDLE, RUNNING, FINISHED }

class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = StatsRepository(app)

    val timerState:       StateFlow<TimerState>                    get() = _timerState
    val secondsRemaining: StateFlow<Int>                           get() = _secondsRemaining
    val selectedMinutes:  StateFlow<Int>                           get() = _selectedMinutes
    val stats:            StateFlow<Pair<StatsResult, List<Boolean>>> get() = _stats
    val weeklyMinutes:    StateFlow<List<Int>>                     get() = _weeklyMinutes
    val monthSessions:    StateFlow<Int>                           get() = _monthSessions
    val bestStreak:       StateFlow<Int>                           get() = _bestStreak

    private val _timerState       = MutableStateFlow(TimerState.IDLE)
    private val _secondsRemaining = MutableStateFlow(0)
    private val _selectedMinutes  = MutableStateFlow(10)
    private val _stats            = MutableStateFlow(Pair(StatsResult(0, 0), List(7) { false }))
    private val _weeklyMinutes    = MutableStateFlow(List(7) { 0 })
    private val _monthSessions    = MutableStateFlow(0)
    private val _bestStreak       = MutableStateFlow(0)

    private var timerJob: Job? = null

    init { refreshStats() }

    fun setMinutes(min: Int) { _selectedMinutes.value = min.coerceIn(1, 120) }

    fun startTimer() {
        val total = _selectedMinutes.value * 60
        _secondsRemaining.value = total
        _timerState.value = TimerState.RUNNING
        timerJob = viewModelScope.launch {
            while (_secondsRemaining.value > 0) {
                delay(1000)
                _secondsRemaining.value--
            }
            onTimerComplete()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.IDLE
    }

    fun resetToIdle() { _timerState.value = TimerState.IDLE }

    private fun onTimerComplete() {
        _timerState.value = TimerState.FINISHED
        playChime()
        vibrate()
        viewModelScope.launch {
            repo.saveSession(_selectedMinutes.value)
            refreshStats()
        }
    }

    private fun refreshStats() {
        viewModelScope.launch {
            _stats.value        = repo.getStats()
            _weeklyMinutes.value = repo.getWeeklyMinutes()
            _monthSessions.value = repo.getMonthSessions()
            _bestStreak.value   = repo.getBestStreak()
        }
    }

    private fun playChime() {
        try {
            val mp = MediaPlayer.create(getApplication(), R.raw.chime)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun vibrate() {
        val v = getApplication<Application>()
            .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 600), -1))
    }
}