package com.example.peterbondra

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application.applicationContext)

    val todoTasks: StateFlow<List<Task>> = repository.todoTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val doneTasks: StateFlow<List<Task>> = repository.doneTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val themeMode: StateFlow<ThemeMode> = repository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM,
    )

    val showBibleQuotes: StateFlow<Boolean> = repository.showBibleQuotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    val bibleQuote: StateFlow<BibleQuote?> = repository.cachedBibleQuote.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    init {
        NotificationHelper.ensureChannel(getApplication())
        RandomNotificationWorker.ensureScheduled(getApplication())

        viewModelScope.launch {
            repository.refreshQuoteIfNeeded()
        }
    }

    fun onAppForeground() {
        RandomNotificationWorker.ensureScheduled(getApplication())
        viewModelScope.launch {
            repository.refreshQuoteIfNeeded()
        }
    }

    fun addTask(text: String, intensity: Int) {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return

        viewModelScope.launch {
            repository.addTask(normalizedText, intensity)
            RandomNotificationWorker.ensureScheduled(getApplication())
        }
    }

    fun markDone(task: Task) {
        viewModelScope.launch {
            repository.markTaskDone(task.id)
            RandomNotificationWorker.clearReminderDataForTask(getApplication(), task.id)
            NotificationHelper.cancelTaskNotification(getApplication(), task.id)
        }
    }

    fun deleteDoneTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
            RandomNotificationWorker.clearReminderDataForTask(getApplication(), task.id)
            NotificationHelper.cancelTaskNotification(getApplication(), task.id)
        }
    }

    fun markTodo(task: Task) {
        viewModelScope.launch {
            repository.markTaskTodo(task.id)
            RandomNotificationWorker.ensureScheduled(getApplication())
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setBibleQuotesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowBibleQuotes(enabled)
            if (enabled) {
                repository.refreshQuoteIfNeeded(force = true)
            }
        }
    }

    fun refreshQuote() {
        viewModelScope.launch {
            repository.refreshQuoteIfNeeded(force = true)
        }
    }
}
