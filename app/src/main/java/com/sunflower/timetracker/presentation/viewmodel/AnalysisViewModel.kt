package com.sunflower.timetracker.presentation.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.timetracker.data.repository.TimeTrackerRepository
import com.sunflower.timetracker.domain.model.Tag
import com.sunflower.timetracker.domain.model.TagStats
import com.sunflower.timetracker.domain.model.TimeSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AnalysisPeriod { DAY, WEEK }
enum class SortMode { BY_DURATION, BY_NAME }

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val repository: TimeTrackerRepository
) : ViewModel() {

    private val _period = MutableStateFlow(AnalysisPeriod.DAY)
    val period: StateFlow<AnalysisPeriod> = _period.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.BY_DURATION)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    // null = no tag selected (list view), non-null = tag detail view
    private val _selectedTag = MutableStateFlow<Tag?>(null)
    val selectedTag: StateFlow<Tag?> = _selectedTag.asStateFlow()

    val stats: StateFlow<List<TagStats>> = combine(_period, _sortMode) { period, sort ->
        val rawFlow = when (period) {
            AnalysisPeriod.DAY -> repository.getDayStats()
            AnalysisPeriod.WEEK -> repository.getWeekStats()
        }
        rawFlow.map { list ->
            when (sort) {
                SortMode.BY_DURATION -> list.sortedByDescending { it.totalDurationMs }
                SortMode.BY_NAME -> list.sortedBy { it.tag.name }
            }
        }
    }.flatMapLatest { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All sessions for the selected tag (all-time, not period-filtered)
    val tagSessions: StateFlow<List<TimeSession>> = _selectedTag
        .flatMapLatest { tag ->
            if (tag == null) flowOf(emptyList())
            else repository.getSessionsByTagId(tag.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPeriod(p: AnalysisPeriod) {
        _period.value = p
    }

    fun setSortMode(s: SortMode) {
        _sortMode.value = s
    }

    fun selectTag(tag: Tag?) {
        _selectedTag.value = tag
    }

    // Session CRUD
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch { repository.deleteSession(sessionId) }
    }

    fun updateSession(session: TimeSession) {
        viewModelScope.launch { repository.updateSession(session) }
    }

    fun addManualSession(tagId: Long, startTime: Long, endTime: Long) {
        viewModelScope.launch {
            repository.insertManualSession(
                TimeSession(
                    tagId = tagId,
                    startTime = startTime,
                    endTime = endTime,
                    durationMs = endTime - startTime
                )
            )
        }
    }
}
