package com.example.peterbondra

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.time.LocalDate

private val Context.settingsDataStore by preferencesDataStore(name = "peterbondra_settings")

enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageKey == value } ?: SYSTEM
        }
    }
}

data class BibleQuote(
    val text: String,
    val reference: String,
)

class Repository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = AppDatabase.getInstance(appContext).taskDao()

    val todoTasks = dao.observeTodoTasks()
    val doneTasks = dao.observeDoneTasks()

    val themeMode: Flow<ThemeMode> = appContext.settingsDataStore.data.map { prefs ->
        ThemeMode.fromStorage(prefs[SettingsKeys.themeMode])
    }

    val showBibleQuotes: Flow<Boolean> = appContext.settingsDataStore.data.map { prefs ->
        prefs[SettingsKeys.showBibleQuotes] ?: false
    }

    val cachedBibleQuote: Flow<BibleQuote?> = appContext.settingsDataStore.data.map { prefs ->
        prefs.toQuote()
    }

    suspend fun addTask(text: String, intensity: Int) {
        dao.insert(
            Task(
                text = text.trim(),
                intensity = intensity.coerceIn(0, 100),
            ),
        )
    }

    suspend fun markTaskDone(taskId: Long) {
        dao.markDone(taskId)
    }

    suspend fun deleteTask(taskId: Long) {
        dao.deleteById(taskId)
    }

    suspend fun getActiveTasksOnce(): List<Task> = dao.getTodoTasksOnce()

    suspend fun setThemeMode(themeMode: ThemeMode) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.themeMode] = themeMode.storageKey
        }
    }

    suspend fun setShowBibleQuotes(enabled: Boolean) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.showBibleQuotes] = enabled
        }
    }

    suspend fun isBibleQuotesEnabled(): Boolean {
        return appContext.settingsDataStore.data.first()[SettingsKeys.showBibleQuotes] ?: false
    }

    suspend fun getCachedQuoteOnce(): BibleQuote? {
        return appContext.settingsDataStore.data.first().toQuote()
    }

    suspend fun refreshQuoteIfNeeded(force: Boolean = false) {
        val quotesEnabled = isBibleQuotesEnabled()
        if (!quotesEnabled) return

        val today = LocalDate.now().toEpochDay()
        val prefs = appContext.settingsDataStore.data.first()
        val hasFreshQuote = prefs[SettingsKeys.quoteEpochDay] == today && !prefs[SettingsKeys.quoteText].isNullOrBlank()

        if (hasFreshQuote && !force) return

        val quote = fetchRandomQuoteFromApi() ?: return
        appContext.settingsDataStore.edit { mutable ->
            mutable[SettingsKeys.quoteText] = quote.text
            mutable[SettingsKeys.quoteReference] = quote.reference
            mutable[SettingsKeys.quoteEpochDay] = today
        }
    }

    private suspend fun fetchRandomQuoteFromApi(): BibleQuote? = withContext(Dispatchers.IO) {
        val reference = verseReferencePool.random()
        val encodedReference = URLEncoder.encode(reference, Charsets.UTF_8.name()).replace("+", "%20")
        val connection = (URL("https://bible-api.com/$encodedReference").openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000
            readTimeout = 6_000
            requestMethod = "GET"
            doInput = true
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) return@withContext null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val payload = JSONObject(body)
            val text = payload.optString("text").replace(Regex("\\s+"), " ").trim()
            val apiReference = payload.optString("reference").trim()
            if (text.isBlank()) return@withContext null

            BibleQuote(
                text = text,
                reference = if (apiReference.isBlank()) reference else apiReference,
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun Preferences.toQuote(): BibleQuote? {
        val text = this[SettingsKeys.quoteText]?.trim().orEmpty()
        val reference = this[SettingsKeys.quoteReference]?.trim().orEmpty()
        if (text.isBlank()) return null
        return BibleQuote(
            text = text,
            reference = if (reference.isBlank()) "Bible" else reference,
        )
    }

    private object SettingsKeys {
        val themeMode = stringPreferencesKey("theme_mode")
        val showBibleQuotes = booleanPreferencesKey("show_bible_quotes")
        val quoteText = stringPreferencesKey("quote_text")
        val quoteReference = stringPreferencesKey("quote_reference")
        val quoteEpochDay = longPreferencesKey("quote_epoch_day")
    }

    private companion object {
        val verseReferencePool = listOf(
            "Philippians 4:13",
            "Joshua 1:9",
            "Isaiah 41:10",
            "Romans 8:31",
            "Psalm 46:1",
            "2 Timothy 1:7",
            "Proverbs 3:5",
        )
    }
}

