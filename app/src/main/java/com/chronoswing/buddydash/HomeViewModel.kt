package com.chronoswing.buddydash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.model.Printer
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.HomeLoadTiming
import com.chronoswing.buddydash.util.RefreshGuard
import com.chronoswing.buddydash.util.toUserNetworkMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG_HOME_VM = "BuddyDash/HomeVM"

data class HomeUiState(
    val printers: List<Printer> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    /** True while per-printer status/queue enrichment runs after the base list is shown. */
    val isEnriching: Boolean = false,
    val error: String? = null,
    val refreshError: String? = null,
    val hasCompletedLoad: Boolean = false,
    val serverUrl: String = "",
    val apiKey: String = "",
    val cameraToken: String = "",
    val hasCredentials: Boolean = false,
    val lastUpdatedAtMillis: Long? = null,
)

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val apiClient: BambuddyApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var listFetchJob: Job? = null
    private var enrichJob: Job? = null
    private val manualRefreshGuard = RefreshGuard()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.serverUrl,
                settingsRepository.apiKey,
                settingsRepository.cameraToken,
            ) { url, key, cameraToken ->
                Triple(url, key, cameraToken)
            }.collect { (url, key, cameraToken) ->
                val hasCredentials = url.isNotBlank() && key.isNotBlank()
                val hadCredentials = _uiState.value.hasCredentials
                _uiState.update {
                    it.copy(
                        serverUrl = url,
                        apiKey = key,
                        cameraToken = cameraToken,
                        hasCredentials = hasCredentials,
                    )
                }
                if (hasCredentials) {
                    HomeLoadTiming.log("settings loaded")
                    if (!hadCredentials || !_uiState.value.hasCompletedLoad) {
                        loadPrinters(showLoading = _uiState.value.printers.isEmpty())
                    }
                }
            }
        }
    }

    fun onRefreshErrorShown() {
        _uiState.update { it.copy(refreshError = null) }
    }

    fun loadPrinters(
        showLoading: Boolean = false,
        fromPull: Boolean = false,
        fromUser: Boolean = false,
    ) {
        val state = _uiState.value
        if (!state.hasCredentials) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isEnriching = false,
                    error = null,
                    refreshError = null,
                )
            }
            return
        }
        if (fromUser && !fromPull && manualRefreshGuard.shouldSkipManualRefresh()) return
        if (fromUser && (listFetchJob?.isActive == true || enrichJob?.isActive == true)) return

        val hadPrinters = state.printers.isNotEmpty()
        if (BuddyDashDebug.enabled) {
            Log.d(
                TAG_HOME_VM,
                "loadPrinters showLoading=$showLoading fromPull=$fromPull hadPrinters=$hadPrinters",
            )
        }

        listFetchJob?.cancel()
        enrichJob?.cancel()
        listFetchJob = viewModelScope.launch {
            try {
                if (fromPull || hadPrinters) {
                    _uiState.update { it.copy(isRefreshing = true, refreshError = null) }
                } else if (showLoading) {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }

                val credentials = _uiState.value
                HomeLoadTiming.log("printer list request started")
                ensureActive()
                val listResult = apiClient.fetchPrinters(
                    credentials.serverUrl,
                    credentials.apiKey,
                )
                ensureActive()

                listResult.fold(
                    onSuccess = { basePrinters ->
                        HomeLoadTiming.log(
                            "printer list response (count=${basePrinters.size})",
                        )
                        _uiState.update {
                            it.copy(
                                printers = basePrinters,
                                isLoading = false,
                                hasCompletedLoad = true,
                                error = null,
                                refreshError = null,
                                isEnriching = basePrinters.isNotEmpty(),
                                isRefreshing = basePrinters.isNotEmpty() || it.isRefreshing,
                            )
                        }
                        HomeLoadTiming.log("first printer cards available in state")

                        if (basePrinters.isEmpty()) {
                            _uiState.update {
                                it.copy(isRefreshing = false, isEnriching = false)
                            }
                            return@launch
                        }

                        enrichJob = viewModelScope.launch {
                            enrichPrinters(credentials.serverUrl, credentials.apiKey, basePrinters)
                        }
                    },
                    onFailure = { error ->
                        val message = error.toUserNetworkMessage("Failed to load printers")
                        _uiState.update { current ->
                            if (current.printers.isNotEmpty()) {
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    isEnriching = false,
                                    refreshError = message,
                                    hasCompletedLoad = true,
                                )
                            } else {
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    isEnriching = false,
                                    error = message,
                                    hasCompletedLoad = true,
                                )
                            }
                        }
                    },
                )
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    private suspend fun enrichPrinters(
        serverUrl: String,
        apiKey: String,
        basePrinters: List<Printer>,
    ) {
        try {
            currentCoroutineContext().ensureActive()
            val result = apiClient.enrichPrintersForHome(serverUrl, apiKey, basePrinters)
            currentCoroutineContext().ensureActive()
            result.fold(
                onSuccess = { enriched ->
                    _uiState.update {
                        it.copy(
                            printers = enriched,
                            isEnriching = false,
                            isRefreshing = false,
                            lastUpdatedAtMillis = System.currentTimeMillis(),
                        )
                    }
                    HomeLoadTiming.log("secondary data applied to printer cards")
                },
                onFailure = { error ->
                    if (BuddyDashDebug.enabled) {
                        Log.w(TAG_HOME_VM, "enrichPrinters failed", error)
                    }
                    _uiState.update {
                        it.copy(
                            isEnriching = false,
                            isRefreshing = false,
                            refreshError = error.toUserNetworkMessage(
                                "Could not refresh printer status",
                            ),
                        )
                    }
                },
            )
        } catch (e: CancellationException) {
            throw e
        }
    }

    override fun onCleared() {
        listFetchJob?.cancel()
        enrichJob?.cancel()
        super.onCleared()
    }
}
