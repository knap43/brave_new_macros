package com.knap43.macros.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Macros and the activity log, kept as two small JSON files in app storage.
 * Both the UI and the background receiver share this one instance per process.
 */
class MacroStore private constructor(context: Context) {
    private val macrosFile = File(context.filesDir, "macros.json")
    private val logFile = File(context.filesDir, "log.json")
    private val mutex = Mutex()

    private val _macros = MutableStateFlow(read(macrosFile) { json.decodeFromString<List<Macro>>(it) })
    val macros: StateFlow<List<Macro>> = _macros.asStateFlow()

    private val _log = MutableStateFlow(read(logFile) { json.decodeFromString<List<LogEntry>>(it) })
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    fun get(id: String): Macro? = _macros.value.firstOrNull { it.id == id }

    suspend fun save(macro: Macro) = mutate { list ->
        if (list.any { it.id == macro.id }) list.map { if (it.id == macro.id) macro else it } else list + macro
    }

    suspend fun delete(id: String) = mutate { list -> list.filterNot { it.id == id } }

    suspend fun setEnabled(id: String, enabled: Boolean) = mutate { list ->
        list.map { if (it.id == id) it.copy(enabled = enabled) else it }
    }

    suspend fun markRun(id: String, time: Long) = mutate { list ->
        list.map { if (it.id == id) it.copy(lastRun = time) else it }
    }

    suspend fun log(macro: String, message: String, ok: Boolean = true) = mutex.withLock {
        _log.update { (listOf(LogEntry(System.currentTimeMillis(), macro, message, ok)) + it).take(LOG_LIMIT) }
        write(logFile, json.encodeToString(_log.value))
    }

    suspend fun clearLog() = mutex.withLock {
        _log.value = emptyList()
        write(logFile, "[]")
    }

    private suspend fun mutate(change: (List<Macro>) -> List<Macro>) = mutex.withLock {
        _macros.update(change)
        write(macrosFile, json.encodeToString(_macros.value))
    }

    /** Write beside the real file, then rename over it, so a crash never leaves half a file. */
    private suspend fun write(file: File, text: String) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(text)
        tmp.renameTo(file)
    }

    private fun <T> read(file: File, decode: (String) -> List<T>): List<T> =
        runCatching { if (file.exists()) decode(file.readText()) else emptyList() }.getOrDefault(emptyList())

    companion object {
        private const val LOG_LIMIT = 200

        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminator = "type"
        }

        @Volatile private var instance: MacroStore? = null

        fun get(context: Context): MacroStore =
            instance ?: synchronized(this) {
                instance ?: MacroStore(context.applicationContext).also { instance = it }
            }
    }
}
