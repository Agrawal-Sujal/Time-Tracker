package com.sunflower.timetracker.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.timetracker.data.repository.TimeTrackerRepository
import com.sunflower.timetracker.domain.model.ActiveSessionState
import com.sunflower.timetracker.domain.model.Tag
import com.sunflower.timetracker.service.TimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TimeTrackerRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val tags: StateFlow<List<Tag>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeState = MutableStateFlow(ActiveSessionState(null, null, 0L))
    val activeState: StateFlow<ActiveSessionState> = _activeState.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    init {
        observeActiveSession()
        tickElapsed()
    }

    private fun observeActiveSession() {
        viewModelScope.launch {
            combine(repository.getActiveSession(), tags) { session, tagList ->
                val tag = tagList.find { it.id == session?.tagId }
                ActiveSessionState(session, tag, 0L)
            }.collect { state ->
                _activeState.value = state
                val session = state.session
                if (session != null && !session.isPaused) {
                    _durationMs.value =
                        session.durationMs + (System.currentTimeMillis() - session.latestStartTime)
                } else if (session != null) {
                    _durationMs.value = session.durationMs
                } else {
                    _durationMs.value = 0L
                }
            }
        }
    }

    private fun tickElapsed() {
        viewModelScope.launch {
            while (isActive) {
                val session = _activeState.value.session
                if (session != null && !session.isPaused) {
                    _durationMs.value =
                        session.durationMs + (System.currentTimeMillis() - session.latestStartTime)
                }
                delay(500L)
            }
        }
    }

    fun startTimer(tagId: Long) {
        viewModelScope.launch {
            repository.startSession(tagId)
            startService()
        }
    }

    fun pauseTimer() {
        viewModelScope.launch {
            repository.pauseActiveSession()
            // Keep service running (shows paused state)
            context.startService(Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_PAUSE
            })
        }
    }

    fun resumeTimer() {
        viewModelScope.launch {
            repository.resumeActiveSession()
            context.startService(Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_RESUME
            })
        }
    }

    fun stopTimer() {
        viewModelScope.launch {
            repository.stopActiveSession()
            context.stopService(Intent(context, TimerService::class.java))
        }
    }

    private fun startService() {
        val intent = Intent(context, TimerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    // Tag CRUD
    fun addTag(name: String, colorHex: String) {
        viewModelScope.launch { repository.insertTag(Tag(name = name.trim(), colorHex = colorHex)) }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch { repository.deleteTag(tag) }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch { repository.updateTag(tag) }
    }
}
