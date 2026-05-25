package com.chronoswing.buddydash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.ui.components.NfcSettingsExamplePrinter
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.validateConnectionSettings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val statusMessage: String? = null,
    val isSuccess: Boolean? = null,
    val isLoading: Boolean = false,
    val saved: Boolean = false,
    val idleGlowMultiplier: Float = 1f,
    val headerAmbientMultiplier: Float = 1f,
    val printGlowMultiplier: Float = 1f,
    val debugForcePrintGlow: Boolean = false,
    val debugShowLogoGlowBounds: Boolean = false,
    val nfcExamplePrinter: NfcSettingsExamplePrinter? = null,
    // QoL settings
    val homeCardDensity: Int = 0,
    val finishClearPlate: Boolean = true,
    val finishPowerOff: Boolean = true,
    val finishShowConfirmation: Boolean = false,
    val rememberLastDetailTab: Boolean = false,
    val keepScreenAwake: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
    private val homePrintersCacheRepository: HomePrintersCacheRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsRecoveryMessage.collect { message ->
                _uiState.update {
                    it.copy(
                        statusMessage = message,
                        isSuccess = false,
                        saved = false,
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.serverUrl.collect { url ->
                _uiState.update { it.copy(serverUrl = url) }
                refreshNfcExamplePrinter()
            }
        }
        viewModelScope.launch {
            settingsRepository.apiKey.collect { key ->
                _uiState.update { it.copy(apiKey = key) }
            }
        }
        viewModelScope.launch {
            settingsRepository.cameraToken.collect { token ->
                _uiState.update { it.copy(cameraToken = token) }
            }
        }
        viewModelScope.launch {
            combine(
                settingsRepository.homeCardDensity,
                settingsRepository.finishClearPlate,
                settingsRepository.finishPowerOff,
                settingsRepository.finishShowConfirmation,
                settingsRepository.rememberLastDetailTab,
            ) { density, clearPlate, powerOff, confirmation, rememberTab ->
                QolSnapshot(density, clearPlate, powerOff, confirmation, rememberTab)
            }.combine(settingsRepository.keepScreenAwake) { qol, keepAwake ->
                _uiState.update {
                    it.copy(
                        homeCardDensity = qol.density,
                        finishClearPlate = qol.clearPlate,
                        finishPowerOff = qol.powerOff,
                        finishShowConfirmation = qol.confirmation,
                        rememberLastDetailTab = qol.rememberTab,
                        keepScreenAwake = keepAwake,
                    )
                }
            }.collect { }
        }
        if (BuddyDashDebug.enabled) {
            viewModelScope.launch {
                combine(
                    settingsRepository.homeIdleGlowMultiplier,
                    settingsRepository.homeHeaderAmbientMultiplier,
                    settingsRepository.homePrintGlowMultiplier,
                    settingsRepository.homeDebugForcePrintGlow,
                    settingsRepository.homeDebugShowLogoGlowBounds,
                ) { idle, ambient, printGlow, forcePrintGlow, showGlowBounds ->
                    _uiState.update {
                        it.copy(
                            idleGlowMultiplier = idle,
                            headerAmbientMultiplier = ambient,
                            printGlowMultiplier = printGlow,
                            debugForcePrintGlow = forcePrintGlow,
                            debugShowLogoGlowBounds = showGlowBounds,
                        )
                    }
                }.collect { }
            }
        }
    }

    fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, saved = false) }
    }

    fun onApiKeyChange(key: String) {
        _uiState.update { it.copy(apiKey = key, saved = false) }
    }

    fun onCameraTokenChange(token: String) {
        _uiState.update { it.copy(cameraToken = token, saved = false) }
    }

    fun onIdleGlowMultiplierSelected(multiplier: Float) {
        if (!BuddyDashDebug.enabled) return
        viewModelScope.launch {
            settingsRepository.saveHomeIdleGlowMultiplier(multiplier)
            _uiState.update { it.copy(idleGlowMultiplier = multiplier) }
        }
    }

    fun onHeaderAmbientMultiplierSelected(multiplier: Float) {
        if (!BuddyDashDebug.enabled) return
        viewModelScope.launch {
            settingsRepository.saveHomeHeaderAmbientMultiplier(multiplier)
            _uiState.update { it.copy(headerAmbientMultiplier = multiplier) }
        }
    }

    fun onPrintGlowMultiplierSelected(multiplier: Float) {
        if (!BuddyDashDebug.enabled) return
        viewModelScope.launch {
            settingsRepository.saveHomePrintGlowMultiplier(multiplier)
            _uiState.update { it.copy(printGlowMultiplier = multiplier) }
        }
    }

    fun onDebugForcePrintGlowChange(enabled: Boolean) {
        if (!BuddyDashDebug.enabled) return
        viewModelScope.launch {
            settingsRepository.saveHomeDebugForcePrintGlow(enabled)
            _uiState.update { it.copy(debugForcePrintGlow = enabled) }
        }
    }

    fun onDebugShowLogoGlowBoundsChange(enabled: Boolean) {
        if (!BuddyDashDebug.enabled) return
        viewModelScope.launch {
            settingsRepository.saveHomeDebugShowLogoGlowBounds(enabled)
            _uiState.update { it.copy(debugShowLogoGlowBounds = enabled) }
        }
    }

    private data class QolSnapshot(
        val density: Int,
        val clearPlate: Boolean,
        val powerOff: Boolean,
        val confirmation: Boolean,
        val rememberTab: Boolean,
    )

    fun onHomeCardDensityChange(density: Int) {
        viewModelScope.launch {
            settingsRepository.saveHomeCardDensity(density)
        }
    }

    fun onFinishClearPlateChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveFinishClearPlate(enabled)
        }
    }

    fun onFinishPowerOffChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveFinishPowerOff(enabled)
        }
    }

    fun onFinishShowConfirmationChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveFinishShowConfirmation(enabled)
        }
    }

    fun onRememberLastDetailTabChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveRememberLastDetailTab(enabled)
        }
    }

    fun onKeepScreenAwakeChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveKeepScreenAwake(enabled)
        }
    }

    fun saveSettings() {
        val state = _uiState.value
        viewModelScope.launch {
            val result = settingsRepository.saveConnectionSettings(
                serverUrl = state.serverUrl,
                apiKey = state.apiKey,
                cameraToken = state.cameraToken,
            )
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(saved = true, statusMessage = null, isSuccess = null)
                } else {
                    it.copy(
                        saved = false,
                        isSuccess = false,
                        statusMessage = result.exceptionOrNull()?.message ?: "Could not save settings",
                    )
                }
            }
        }
    }

    fun testConnection() {
        val state = _uiState.value
        val validation = validateConnectionSettings(
            serverUrl = state.serverUrl,
            apiKey = state.apiKey,
            cameraToken = state.cameraToken,
        )
        if (validation.isFailure) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSuccess = false,
                    statusMessage = validation.exceptionOrNull()?.message ?: "Connection failed",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, statusMessage = null, isSuccess = null)
            }
            val validated = validation.getOrThrow()
            val result = apiClient.testApiConnection(validated.serverUrl, validated.apiKey)
            if (result.isSuccess) {
                cachePrintersForNfcExample(validated.serverUrl, validated.apiKey)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSuccess = result.isSuccess,
                    statusMessage = result.fold(
                        onSuccess = { message -> message },
                        onFailure = { error -> error.message ?: "Connection failed" },
                    ),
                )
            }
        }
    }

    private suspend fun refreshNfcExamplePrinter() {
        val url = _uiState.value.serverUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(nfcExamplePrinter = null) }
            return
        }
        val first = homePrintersCacheRepository.load(url)?.printers?.firstOrNull()
        _uiState.update {
            it.copy(
                nfcExamplePrinter = first?.let { printer ->
                    NfcSettingsExamplePrinter(name = printer.name, id = printer.id)
                },
            )
        }
    }

    private suspend fun cachePrintersForNfcExample(serverUrl: String, apiKey: String) {
        val printers = apiClient.fetchPrinters(serverUrl, apiKey).getOrNull() ?: return
        if (printers.isEmpty()) return
        homePrintersCacheRepository.save(
            serverUrl = serverUrl,
            printers = printers,
            lastUpdatedAtMillis = System.currentTimeMillis(),
        )
        refreshNfcExamplePrinter()
    }
}
