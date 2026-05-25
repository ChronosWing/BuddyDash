package com.chronoswing.buddydash.util

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import androidx.core.content.FileProvider
import com.chronoswing.buddydash.BuildConfig
import com.chronoswing.buddydash.data.SettingsRepository
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a redacted diagnostics report safe to share publicly.
 * Never includes API keys, tokens, full server URLs, or other secrets.
 */
object DiagnosticsExport {

    suspend fun generate(
        context: Context,
        settingsRepository: SettingsRepository,
    ): String {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())

        val serverUrl = try { settingsRepository.serverUrl.first() } catch (_: Exception) { "" }
        val apiKey = try { settingsRepository.apiKey.first() } catch (_: Exception) { "" }
        val cameraToken = try { settingsRepository.cameraToken.first() } catch (_: Exception) { "" }
        val density = try { settingsRepository.homeCardDensity.first() } catch (_: Exception) { 0 }
        val clearPlate = try { settingsRepository.finishClearPlate.first() } catch (_: Exception) { true }
        val powerOff = try { settingsRepository.finishPowerOff.first() } catch (_: Exception) { true }
        val confirmation = try { settingsRepository.finishShowConfirmation.first() } catch (_: Exception) { false }
        val rememberTab = try { settingsRepository.rememberLastDetailTab.first() } catch (_: Exception) { false }
        val keepAwake = try { settingsRepository.keepScreenAwake.first() } catch (_: Exception) { false }

        val viewMode = HomeCardViewMode.fromIndex(density)
        val nfcAdapter = runCatching { NfcAdapter.getDefaultAdapter(context) }.getOrNull()

        return buildString {
            appendLine("BuddyDash Diagnostics Report")
            appendLine("Generated: $now")
            appendLine()

            section("App") {
                field("Version", BuildConfig.VERSION_NAME)
                field("Build code", BuildConfig.VERSION_CODE.toString())
                field("Build type", BuildConfig.BUILD_TYPE)
                field("Package", BuildConfig.APPLICATION_ID)
            }

            section("Device") {
                field("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                field("Model", "${Build.MANUFACTURER} ${Build.MODEL}")
                field("Device", Build.DEVICE)
            }

            section("Connection") {
                field("Server configured", if (serverUrl.isNotBlank()) "yes" else "no")
                field("Server", if (serverUrl.isNotBlank()) "<local-server>" else "(not set)")
                field("API key", if (apiKey.isNotBlank()) "<redacted>" else "(not set)")
                field("Camera token", if (cameraToken.isNotBlank()) "<redacted>" else "(not set)")
                field(
                    "Encrypted storage",
                    if (settingsRepository.encryptedStore.isAvailable) "active" else "unavailable",
                )
                field(
                    "Migration complete",
                    settingsRepository.encryptedStore.hasMigrated().toString(),
                )
            }

            section("Settings") {
                field("Home card view", viewMode.name)
                field("Finish: clear plate", clearPlate.toString())
                field("Finish: power off", powerOff.toString())
                field("Finish: confirmation", confirmation.toString())
                field("Remember detail tab", rememberTab.toString())
                field("Keep screen awake", keepAwake.toString())
            }

            section("Hardware") {
                field("NFC supported", (nfcAdapter != null).toString())
                field("NFC enabled", (nfcAdapter?.isEnabled == true).toString())
            }

            appendLine("--- End of diagnostics ---")
        }
    }

    fun shareReport(context: Context, report: String): Boolean {
        return try {
            val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val fileName = "BuddyDash-diagnostics-$dateStamp.txt"
            val cacheDir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
            val file = File(cacheDir, fileName).apply { writeText(report) }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.diagnostics",
                file,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BuddyDash Diagnostics $dateStamp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, null))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun StringBuilder.section(name: String, block: StringBuilder.() -> Unit) {
        appendLine("[$name]")
        block()
        appendLine()
    }

    private fun StringBuilder.field(key: String, value: String) {
        appendLine("  $key: $value")
    }
}
