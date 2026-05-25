package com.chronoswing.buddydash.nfc

import android.net.Uri
import android.util.Log
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.ClearPlateActionOutcome
import com.chronoswing.buddydash.util.NfcClearPlateDebounce
import com.chronoswing.buddydash.util.blocksNfcPlateClear
import com.chronoswing.buddydash.util.parseClearPlateDeepLink
import com.chronoswing.buddydash.util.resolvePrinterByKey
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NfcClearPlateExecutor(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
    private val homePrintersCacheRepository: HomePrintersCacheRepository,
) {
    suspend fun execute(uri: Uri): ClearPlateActionOutcome {
        val link = parseClearPlateDeepLink(uri) ?: return ClearPlateActionOutcome.InvalidLink

        val debounceKey = "clear-plate:${link.printerKey.lowercase()}"
        if (!NfcClearPlateDebounce.shouldProcess(debounceKey)) {
            return ClearPlateActionOutcome.Debounced
        }

        val serverUrl = settingsRepository.serverUrl.first().trim()
        val apiKey = settingsRepository.apiKey.first().trim()
        if (serverUrl.isBlank() || apiKey.isBlank()) {
            return ClearPlateActionOutcome.MissingCredentials
        }

        val printer = resolvePrinter(serverUrl, apiKey, link.printerKey)
            ?: return ClearPlateActionOutcome.PrinterNotFound

        val statusResult = apiClient.fetchPrinterStatus(serverUrl, apiKey, printer.id)
        val status = statusResult.getOrNull()
        if (statusResult.isFailure && status == null) {
            if (statusResult.exceptionOrNull().isConnectivityFailure()) {
                return ClearPlateActionOutcome.ConnectionRequired
            }
            Log.w(TAG, "NFC clear plate status fetch failed for printerId=${printer.id}")
            return ClearPlateActionOutcome.ConnectionRequired
        }
        if (status != null && !status.connected) {
            return ClearPlateActionOutcome.ConnectionRequired
        }
        if (status != null && blocksNfcPlateClear(status)) {
            return ClearPlateActionOutcome.PrinterActive
        }

        return apiClient.clearPlate(serverUrl, apiKey, printer.id).fold(
            onSuccess = {
                refreshHomePrinterCache(serverUrl, apiKey, printer)
                ClearPlateActionOutcome.Success(printerName = printer.name)
            },
            onFailure = { error ->
                Log.w(TAG, "NFC clear plate API failed for printerId=${printer.id}", error)
                if (error.isConnectivityFailure()) {
                    ClearPlateActionOutcome.ConnectionRequired
                } else {
                    ClearPlateActionOutcome.ApiFailed
                }
            },
        )
    }

    private suspend fun resolvePrinter(
        serverUrl: String,
        apiKey: String,
        printerKey: String,
    ): Printer? {
        val networkPrinters = apiClient.fetchPrinters(serverUrl, apiKey).getOrNull()
        if (networkPrinters != null) {
            resolvePrinterByKey(networkPrinters, printerKey)?.let { return it }
        }
        val cached = homePrintersCacheRepository.load(serverUrl)?.printers.orEmpty()
        return resolvePrinterByKey(cached, printerKey)
    }

    private suspend fun refreshHomePrinterCache(
        serverUrl: String,
        apiKey: String,
        printer: Printer,
    ) {
        val refreshedStatus = apiClient.fetchPrinterStatus(serverUrl, apiKey, printer.id).getOrNull()
        val snapshot = homePrintersCacheRepository.load(serverUrl)
        val basePrinters = snapshot?.printers ?: apiClient.fetchPrinters(serverUrl, apiKey).getOrNull()
        if (basePrinters.isNullOrEmpty()) return
        val updated = basePrinters.map { cached ->
            if (cached.id != printer.id) {
                cached
            } else {
                cached.copy(liveStatus = refreshedStatus ?: cached.liveStatus)
            }
        }
        homePrintersCacheRepository.save(
            serverUrl = serverUrl,
            printers = updated,
            lastUpdatedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun Throwable?.isConnectivityFailure(): Boolean {
        val error = this ?: return false
        return error is IOException ||
            error is UnknownHostException ||
            error is SocketTimeoutException ||
            error.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
            error.message?.contains("Failed to connect", ignoreCase = true) == true ||
            error.message?.contains("timeout", ignoreCase = true) == true
    }

    companion object {
        private const val TAG = "BuddyDash/NfcClearPlate"
    }
}
