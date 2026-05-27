package com.crossplatform.sdk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.domain.repo.FetchStatusRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpiTimerViewModel () : ViewModel() {
    companion object {
        const val TIMER_TOTAL = 300 // 5 minutes in seconds
    }

    // Counts down from 300 → 0
    private val _timeRemaining = MutableStateFlow(TIMER_TOTAL)
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private var timerJob: Job? = null

    init {
        startTimer()
    }

    fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeRemaining.value > 0) {
                delay(1_000L)
                _timeRemaining.value -= 1
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
    }


    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}