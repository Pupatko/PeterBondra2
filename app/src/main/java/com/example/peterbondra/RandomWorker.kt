package com.example.peterbondra

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlin.math.roundToLong
import kotlin.random.Random
import java.util.concurrent.TimeUnit

class RandomNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val repository = Repository(appContext)

    override suspend fun doWork(): Result {
        NotificationHelper.ensureChannel(applicationContext)

        val activeTasks = repository.getActiveTasksOnce()
        if (activeTasks.isEmpty()) {
            clearAllSchedules(applicationContext)
            NotificationHelper.cancelAllTaskNotifications(applicationContext)
            return Result.success()
        }

        val prefs = reminderPreferences(applicationContext)
        val now = System.currentTimeMillis()
        clearStaleTaskSchedules(prefs, activeTasks.map { it.id }.toSet())

        val showQuotes = repository.isBibleQuotesEnabled()
        if (showQuotes) {
            repository.refreshQuoteIfNeeded()
        }
        val quote = if (showQuotes) repository.getCachedQuoteOnce() else null

        for (task in activeTasks) {
            val key = dueKey(task.id)
            val dueAt = prefs.getLong(key, -1L)

            if (dueAt <= 0L) {
                val firstDelay = randomDelayMs(task.intensity, firstSchedule = true)
                prefs.edit().putLong(key, now + firstDelay).apply()
                continue
            }

            if (now >= dueAt) {
                NotificationHelper.showTaskReminder(applicationContext, task, quote)
                val nextDelay = randomDelayMs(task.intensity)
                prefs.edit().putLong(key, now + nextDelay).apply()
            }
        }

        enqueueNext(applicationContext)
        return Result.success()
    }

    private fun randomDelayMs(intensity: Int, firstSchedule: Boolean = false): Long {
        val normalized = intensity.coerceIn(0, 100) / 100.0
        val baseMinutes = MAX_INTERVAL_MINUTES - ((MAX_INTERVAL_MINUTES - MIN_INTERVAL_MINUTES) * normalized)

        val minFactor = if (firstSchedule) 0.2 else 0.7
        val maxFactor = 1.0
        val randomizedMinutes = baseMinutes * Random.nextDouble(minFactor, maxFactor)
        val clampedMinutes = randomizedMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES)

        return TimeUnit.MINUTES.toMillis(clampedMinutes.roundToLong())
    }

    private fun clearStaleTaskSchedules(
        prefs: android.content.SharedPreferences,
        activeTaskIds: Set<Long>,
    ) {
        val staleKeys = prefs.all.keys.filter { key ->
            key.startsWith(KEY_PREFIX) && key.removePrefix(KEY_PREFIX).toLongOrNull() !in activeTaskIds
        }
        if (staleKeys.isEmpty()) return

        prefs.edit().apply {
            staleKeys.forEach { remove(it) }
        }.apply()
    }

    companion object {
        private const val WORK_NAME = "random_notification_loop"
        private const val PREFS_NAME = "random_notification_schedule"
        private const val KEY_PREFIX = "task_due_"

        private const val LOOP_DELAY_MINUTES = 5L
        private const val MIN_INTERVAL_MINUTES = 5.0
        private const val MAX_INTERVAL_MINUTES = 1_440.0

        fun ensureScheduled(context: Context) {
            val work = buildWorkRequest(delayMinutes = 1)
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                work,
            )
        }

        fun clearReminderDataForTask(context: Context, taskId: Long) {
            reminderPreferences(context).edit().remove(dueKey(taskId)).apply()
        }

        private fun enqueueNext(context: Context) {
            val work = buildWorkRequest(delayMinutes = LOOP_DELAY_MINUTES)
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                work,
            )
        }

        private fun buildWorkRequest(delayMinutes: Long) = OneTimeWorkRequestBuilder<RandomNotificationWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build(),
            )
            .build()

        private fun clearAllSchedules(context: Context) {
            val prefs = reminderPreferences(context)
            val keys = prefs.all.keys.filter { it.startsWith(KEY_PREFIX) }
            if (keys.isEmpty()) return

            prefs.edit().apply {
                keys.forEach { remove(it) }
            }.apply()
        }

        private fun reminderPreferences(context: Context) =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private fun dueKey(taskId: Long) = "$KEY_PREFIX$taskId"
    }
}
