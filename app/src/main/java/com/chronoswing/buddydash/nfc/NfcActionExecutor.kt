package com.chronoswing.buddydash.nfc

import android.net.Uri
import android.util.Log
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.data.model.PrinterStatus
import com.chronoswing.buddydash.data.model.PrinterSmartPlugState
import com.chronoswing.buddydash.data.model.SmartOutletPowerState
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.NfcActionDebounce
import com.chronoswing.buddydash.util.NfcActionKind
import com.chronoswing.buddydash.util.NfcActionOutcome
import com.chronoswing.buddydash.util.NfcDeepLink
import com.chronoswing.buddydash.util.blocksNfcPlateClear
import com.chronoswing.buddydash.util.isClearPlateAlreadyAcknowledged
import com.chronoswing.buddydash.util.isPlateKnownCleared
import com.chronoswing.buddydash.util.isPrinterSafeToPowerOff
import com.chronoswing.buddydash.util.toggleSmartPlugPower
import com.chronoswing.buddydash.util.parseNfcDeepLink
import com.chronoswing.buddydash.util.resolvePrinterByKey
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NfcActionExecutor(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
    private val homePrintersCacheRepository: HomePrintersCacheRepository,
) {
    suspend fun execute(uri: Uri): NfcActionOutcome {
        val link = parseNfcDeepLink(uri) ?: return NfcActionOutcome.InvalidLink

        val debounceKey = "${link.action.slug}:${link.printerKey.lowercase()}"
        if (!NfcActionDebounce.shouldProcess(debounceKey)) return NfcActionOutcome.Debounced

        val serverUrl = settingsRepository.serverUrl.first().trim()
        val apiKey = settingsRepository.apiKey.first().trim()
        if (serverUrl.isBlank() || apiKey.isBlank()) return NfcActionOutcome.MissingCredentials

        val printer = resolvePrinter(serverUrl, apiKey, link.printerKey)
            ?: return NfcActionOutcome.PrinterNotFound

        return when (link.action) {
            NfcActionKind.ClearPlate -> executeClearPlate(serverUrl, apiKey, printer)
            NfcActionKind.TogglePower -> executeTogglePower(serverUrl, apiKey, printer)
            NfcActionKind.Finish -> executeFinish(serverUrl, apiKey, printer)
        }
    }

    // ── Clear Plate ────────────────────────────────────────────────

    private suspend fun executeClearPlate(
        serverUrl: String,
        apiKey: String,
        printer: Printer,
    ): NfcActionOutcome {
        printer.liveStatus?.takeIf { isPlateKnownCleared(it) }?.let {
            return NfcActionOutcome.PlateAlreadyClear
        }

        val status = fetchStatusOrNull(serverUrl, apiKey, printer.id)
            ?: return NfcActionOutcome.ConnectionRequired
        if (!status.connected) return NfcActionOutcome.ConnectionRequired
        if (blocksNfcPlateClear(status)) return NfcActionOutcome.PrinterBusyPlateUnchanged
        if (isPlateKnownCleared(status)) return NfcActionOutcome.PlateAlreadyClear

        return apiClient.clearPlate(serverUrl, apiKey, printer.id).fold(
            onSuccess = { message ->
                if (isClearPlateAlreadyAcknowledged(message)) {
                    NfcActionOutcome.PlateAlreadyClear
                } else {
                    refreshCache(serverUrl, apiKey, printer)
                    NfcActionOutcome.PlateCleared(printer.name)
                }
            },
            onFailure = { error ->
                if (isClearPlateAlreadyAcknowledged(error.message.orEmpty())) {
                    NfcActionOutcome.PlateAlreadyClear
                } else {
                    logWarn("clear-plate", printer.id, error)
                    error.toConnectionOrApiFailed()
                }
            },
        )
    }

    // ── Toggle Power ───────────────────────────────────────────────

    private suspend fun executeTogglePower(
        serverUrl: String,
        apiKey: String,
        printer: Printer,
    ): NfcActionOutcome {
        val result = toggleSmartPlugPower(
            apiClient = apiClient,
            serverUrl = serverUrl,
            apiKey = apiKey,
            printer = printer,
        )
        if (result.outcome.tier == NfcActionOutcome.Tier.Success) {
            refreshCacheAfterPowerToggle(
                serverUrl = serverUrl,
                apiKey = apiKey,
                printerId = printer.id,
                updatedPlugState = result.updatedPlugState,
                updatedLiveStatus = result.updatedLiveStatus,
            )
        }
        return result.outcome
    }

    // ── Finish ─────────────────────────────────────────────────────

    private suspend fun executeFinish(
        serverUrl: String,
        apiKey: String,
        printer: Printer,
    ): NfcActionOutcome {
        val status = fetchStatusOrNull(serverUrl, apiKey, printer.id)
            ?: return NfcActionOutcome.ConnectionRequired
        if (!status.connected) return NfcActionOutcome.ConnectionRequired
        if (!isPrinterSafeToPowerOff(status)) return NfcActionOutcome.PrinterBusyFinishSkipped

        if (!isPlateKnownCleared(status)) {
            apiClient.clearPlate(serverUrl, apiKey, printer.id).onFailure { error ->
                if (!isClearPlateAlreadyAcknowledged(error.message.orEmpty())) {
                    logWarn("finish/clear-plate", printer.id, error)
                }
            }
        }

        val plugState = apiClient.fetchPrinterSmartPlugState(serverUrl, apiKey, printer.id)
            .getOrNull()
        val shouldPowerOff = plugState != null &&
            plugState.displayPowerState == SmartOutletPowerState.On

        if (shouldPowerOff) {
            apiClient.controlSmartPlug(
                serverUrl, apiKey, plugState!!.config.id, action = "off",
            ).onFailure { error ->
                logWarn("finish/power-off", printer.id, error)
            }
        }

        refreshCache(serverUrl, apiKey, printer)

        return if (shouldPowerOff) {
            NfcActionOutcome.FinishedWithPowerOff
        } else {
            NfcActionOutcome.FinishedPlateClear
        }
    }

    // ── Shared helpers ─────────────────────────────────────────────

    private suspend fun fetchStatusOrNull(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
    ): PrinterStatus? {
        return apiClient.fetchPrinterStatus(serverUrl, apiKey, printerId).getOrNull()
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

    private suspend fun refreshCacheAfterPowerToggle(
        serverUrl: String,
        apiKey: String,
        printerId: Int,
        updatedPlugState: PrinterSmartPlugState?,
        updatedLiveStatus: PrinterStatus?,
    ) {
        val snapshot = homePrintersCacheRepository.load(serverUrl)
        val basePrinters = snapshot?.printers
            ?: apiClient.fetchPrinters(serverUrl, apiKey).getOrNull()
        if (basePrinters.isNullOrEmpty()) return
        val updated = basePrinters.map { cached ->
            if (cached.id != printerId) cached
            else cached.copy(
                smartPlugState = updatedPlugState ?: cached.smartPlugState,
                liveStatus = updatedLiveStatus ?: cached.liveStatus,
            )
        }
        homePrintersCacheRepository.save(
            serverUrl = serverUrl,
            printers = updated,
            lastUpdatedAtMillis = System.currentTimeMillis(),
        )
    }

    private suspend fun refreshCache(
        serverUrl: String,
        apiKey: String,
        printer: Printer,
    ) {
        val refreshedStatus = apiClient.fetchPrinterStatus(serverUrl, apiKey, printer.id).getOrNull()
        val snapshot = homePrintersCacheRepository.load(serverUrl)
        val basePrinters = snapshot?.printers
            ?: apiClient.fetchPrinters(serverUrl, apiKey).getOrNull()
        if (basePrinters.isNullOrEmpty()) return
        val updated = basePrinters.map { cached ->
            if (cached.id != printer.id) cached
            else cached.copy(liveStatus = refreshedStatus ?: cached.liveStatus)
        }
        homePrintersCacheRepository.save(
            serverUrl = serverUrl,
            printers = updated,
            lastUpdatedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun Throwable.toConnectionOrApiFailed(): NfcActionOutcome =
        if (isConnectivityFailure()) NfcActionOutcome.ConnectionRequired
        else NfcActionOutcome.ApiFailed

    private fun Throwable?.isConnectivityFailure(): Boolean {
        val error = this ?: return false
        return error is IOException ||
            error is UnknownHostException ||
            error is SocketTimeoutException ||
            error.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
            error.message?.contains("Failed to connect", ignoreCase = true) == true ||
            error.message?.contains("timeout", ignoreCase = true) == true
    }

    private fun logWarn(action: String, printerId: Int, error: Throwable) {
        Log.w(TAG, "NFC $action failed for printerId=$printerId", error)
    }

    companion object {
        private const val TAG = "BuddyDash/NfcAction"
    }
}
