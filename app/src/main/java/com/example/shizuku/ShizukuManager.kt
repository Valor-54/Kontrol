package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class ShizukuStatus(
    val isAvailable: Boolean = false,
    val isAuthorized: Boolean = false,
    val version: Int = 0,
    val uid: Int = -1,
    val message: String = "Başlatılmadı"
)

object ShizukuManager {
    private const val TAG = "ShizukuManager"
    const val REQUEST_CODE_PERMISSION = 7001

    private val _status = MutableStateFlow(ShizukuStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    @Volatile
    private var persistentShellProcess: Process? = null
    @Volatile
    private var persistentShellWriter: BufferedWriter? = null
    private val shellLock = Any()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Shizuku binder received")
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w(TAG, "Shizuku binder died")
        closePersistentShell()
        _status.value = ShizukuStatus(
            isAvailable = false,
            isAuthorized = false,
            message = "Shizuku servisi kesildi veya durduruldu"
        )
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { reqCode, grantResult ->
        if (reqCode == REQUEST_CODE_PERMISSION) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            Log.i(TAG, "Shizuku permission result: granted=$granted")
            checkStatus()
            if (granted) {
                initPersistentShell()
            }
        }
    }

    fun initialize(context: Context) {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            checkStatus()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize Shizuku listeners", e)
            _status.value = ShizukuStatus(
                isAvailable = false,
                isAuthorized = false,
                message = "Shizuku cihazda kurulu değil veya desteklenmiyor"
            )
        }
    }

    fun checkStatus() {
        try {
            val pingable = Shizuku.pingBinder()
            if (!pingable) {
                closePersistentShell()
                _status.value = ShizukuStatus(
                    isAvailable = false,
                    isAuthorized = false,
                    message = "Shizuku servisi çalışmıyor. Shizuku uygulamasını açıp başlatın."
                )
                return
            }

            val version = Shizuku.getVersion()
            val uid = Shizuku.getUid()
            val hasPerm = if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }

            _status.value = ShizukuStatus(
                isAvailable = true,
                isAuthorized = hasPerm,
                version = version,
                uid = uid,
                message = if (hasPerm) {
                    "✓ Shizuku aktif & yetkilendirildi (UID $uid)"
                } else {
                    "⚠️ Shizuku çalışıyor ancak izin verilmedi. 'Yetkilendir' butonuna basın."
                }
            )

            if (hasPerm && persistentShellWriter == null) {
                initPersistentShell()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error checking Shizuku status: ${e.message}")
            closePersistentShell()
            _status.value = ShizukuStatus(
                isAvailable = false,
                isAuthorized = false,
                message = "Shizuku bağlantı hatası: ${e.message}"
            )
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
                }
            } else {
                _status.value = _status.value.copy(
                    message = "Shizuku servisi kapalı. Lütfen önce Shizuku uygulamasını açıp çalıştırın."
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error requesting Shizuku permission", e)
        }
    }

    private val newProcessMethod by lazy {
        try {
            Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to reflect Shizuku.newProcess method", e)
            null
        }
    }

    private fun createShizukuProcess(cmd: Array<String>): Process? {
        val method = newProcessMethod ?: return null
        return try {
            method.invoke(null, cmd, null, null) as? Process
        } catch (e: Throwable) {
            Log.e(TAG, "Error invoking Shizuku.newProcess", e)
            null
        }
    }

    private fun initPersistentShell() {
        synchronized(shellLock) {
            closePersistentShell()
            try {
                val proc = createShizukuProcess(arrayOf("sh"))
                if (proc != null) {
                    persistentShellProcess = proc
                    persistentShellWriter = BufferedWriter(OutputStreamWriter(proc.outputStream))
                    Log.i(TAG, "High-performance persistent Shizuku shell started successfully")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start persistent Shizuku shell", e)
            }
        }
    }

    private fun closePersistentShell() {
        synchronized(shellLock) {
            try {
                persistentShellWriter?.close()
            } catch (_: Exception) {}
            persistentShellWriter = null

            try {
                persistentShellProcess?.destroy()
            } catch (_: Exception) {}
            persistentShellProcess = null
        }
    }

    /**
     * Ultra-fast command execution through persistent shell process (<2ms latency).
     */
    fun sendFastShellCommand(command: String): Boolean {
        if (!_status.value.isAvailable || !_status.value.isAuthorized) {
            return false
        }
        synchronized(shellLock) {
            try {
                if (persistentShellWriter == null) {
                    initPersistentShell()
                }
                val writer = persistentShellWriter ?: return false
                writer.write(command)
                writer.newLine()
                writer.flush()
                return true
            } catch (e: Throwable) {
                Log.w(TAG, "Persistent shell write failed, attempting recovery", e)
                closePersistentShell()
                try {
                    initPersistentShell()
                    persistentShellWriter?.let {
                        it.write(command)
                        it.newLine()
                        it.flush()
                        return true
                    }
                } catch (ex: Throwable) {
                    Log.e(TAG, "Shell recovery failed", ex)
                }
                return false
            }
        }
    }

    suspend fun executeCommand(command: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!_status.value.isAvailable || !_status.value.isAuthorized) {
            return@withContext Pair(false, "Shizuku çalışmıyor veya yetki verilmemiş")
        }

        try {
            val cmdArgs = arrayOf("sh", "-c", command)
            val process: Process = createShizukuProcess(cmdArgs)
                ?: return@withContext Pair(false, "Shizuku proses başlatılamadı")

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val errors = StringBuilder()
            var errLine: String?
            while (errReader.readLine().also { errLine = it } != null) {
                errors.append(errLine).append("\n")
            }

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Pair(true, output.toString().trim())
            } else {
                Pair(false, "Hata ($exitCode): ${errors.toString().trim().ifEmpty { output.toString().trim() }}")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Shizuku command execution failed: $command", e)
            Pair(false, "Çalıştırma hatası: ${e.message}")
        }
    }
}

