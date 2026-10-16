package com.knap43.macros.engine

import android.content.Context
import android.content.pm.PackageManager
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

/** Shell commands with ADB-level rights, borrowed from Shizuku. */
object Shell {
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val PERMISSION_REQUEST = 4301

    enum class Status { NOT_INSTALLED, NOT_RUNNING, NO_PERMISSION, READY }

    /** Bumped whenever Shizuku comes, goes, or answers a permission request; the UI re-reads [status]. */
    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes.asStateFlow()

    fun init() {
        Shizuku.addBinderReceivedListenerSticky { _changes.value++ }
        Shizuku.addBinderDeadListener { _changes.value++ }
        Shizuku.addRequestPermissionResultListener { _, _ -> _changes.value++ }
    }

    fun status(context: Context): Status = when {
        alive() && granted() -> Status.READY
        alive() -> Status.NO_PERMISSION
        installed(context) -> Status.NOT_RUNNING
        else -> Status.NOT_INSTALLED
    }

    fun requestPermission() {
        if (alive() && !granted()) Shizuku.requestPermission(PERMISSION_REQUEST)
    }

    /**
     * In a process the system just woke for a broadcast, Shizuku hands over its binder a moment
     * after start-up, so wait briefly for it rather than giving up at once.
     */
    suspend fun awaitReady(timeoutMs: Long = 3_000): Boolean {
        if (!alive()) {
            var listener: Shizuku.OnBinderReceivedListener? = null
            try {
                withTimeoutOrNull(timeoutMs) {
                    suspendCancellableCoroutine { cont ->
                        listener = Shizuku.OnBinderReceivedListener { if (cont.isActive) cont.resume(Unit) }
                        Shizuku.addBinderReceivedListenerSticky(listener!!)
                    }
                }
            } finally {
                listener?.let { Shizuku.removeBinderReceivedListener(it) }
            }
        }
        return alive() && granted()
    }

    data class Result(val code: Int, val error: String)

    /**
     * Runs a command as the shell user. Shizuku has deprecated (and in places hidden) newProcess in
     * favour of a bound UserService; for one-line commands like these it is still the simplest path,
     * so it's reached by reflection and kept from R8 in proguard-rules.pro.
     */
    suspend fun run(vararg command: String): Result = withContext(Dispatchers.IO) {
        runCatching {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            ).apply { isAccessible = true }
            val process = method.invoke(null, arrayOf(*command), null, null) as Process
            val error = process.errorStream.bufferedReader().readText().trim()
            process.inputStream.close()
            Result(process.waitFor(), error)
        }.getOrElse { Result(-1, it.cause?.message ?: it.message ?: it.javaClass.simpleName) }
    }

    private fun alive() = runCatching { Shizuku.pingBinder() && !Shizuku.isPreV11() }.getOrDefault(false)

    private fun granted() =
        runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)

    private fun installed(context: Context) =
        runCatching { context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0); true }.getOrDefault(false)
}
