package com.chronoswing.buddydash.ui

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chronoswing.buddydash.ArchiveDetailViewModel
import com.chronoswing.buddydash.ArchivesViewModel
import com.chronoswing.buddydash.HomeViewModel
import com.chronoswing.buddydash.PrinterDetailViewModel
import com.chronoswing.buddydash.SettingsViewModel
import com.chronoswing.buddydash.SpoolDetailViewModel
import com.chronoswing.buddydash.SpoolsViewModel
import com.chronoswing.buddydash.util.ArchiveMaterialNavigation
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.ui.components.BuddyDashBottomNav
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val HOME = "home"
    const val SPOOLS_BASE = "spools"
    const val SPOOLS = "spools?search={search}"
    const val ARCHIVES_BASE = "archives"
    const val ARCHIVES = "archives?search={search}"
    const val SPOOL_DETAIL = "spool/{spoolId}"
    const val SETTINGS = "settings"
    const val PRINTER_DETAIL = "printer/{printerId}/{printerName}/{printerModel}"
    const val PRINTER_QUEUE = "printer_queue"
    const val ARCHIVE_DETAIL = "archive/{archiveId}"

    fun printerDetail(printerId: Int, printerName: String, printerModel: String? = null): String {
        val encodedName = java.net.URLEncoder.encode(printerName, StandardCharsets.UTF_8.toString())
        val encodedModel = java.net.URLEncoder.encode(printerModel.orEmpty(), StandardCharsets.UTF_8.toString())
        return "printer/$printerId/$encodedName/$encodedModel"
    }

    fun archiveDetail(archiveId: Int): String = "archive/$archiveId"

    fun spools(search: String = ""): String {
        val encoded = URLEncoder.encode(search, StandardCharsets.UTF_8.toString())
        return "spools?search=$encoded"
    }

    fun spoolDetail(spoolId: Int): String = "spool/$spoolId"

    fun archives(search: String = ""): String {
        val encoded = URLEncoder.encode(search, StandardCharsets.UTF_8.toString())
        return "archives?search=$encoded"
    }
}

/** Temporary: verify bottom-tab destinations. Set false before release. */
private const val DEBUG_LOG_NAV_DESTINATIONS = true
private const val TAG_NAV = "BuddyDash/Nav"

private val bottomNavRoutes = setOf(
    Routes.HOME,
    Routes.SPOOLS_BASE,
    Routes.ARCHIVES_BASE,
    Routes.SETTINGS,
)

private fun isBottomNavRoute(route: String?): Boolean {
    val base = route?.substringBefore('?') ?: return false
    return base in bottomNavRoutes
}

@Composable
fun BuddyDashNav(
    settingsRepository: SettingsRepository,
    apiClient: BambuddyApiClient,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = isBottomNavRoute(currentRoute)

    LaunchedEffect(Unit) {
        if (DEBUG_LOG_NAV_DESTINATIONS) {
            Log.d(
                TAG_NAV,
                "bottomNavTabs=Printers(${Routes.HOME}), Spools(${Routes.SPOOLS}), " +
                    "Archives(${Routes.ARCHIVES}), Settings(${Routes.SETTINGS})",
            )
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BuddyDashBottomNav(
                    currentRoute = currentRoute,
                    onPrinters = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSpools = {
                        navController.navigate(Routes.spools()) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onArchives = {
                        navController.navigate(Routes.archives()) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSettings = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                val viewModel: HomeViewModel = viewModel(
                    factory = viewModelFactory {
                        HomeViewModel(settingsRepository, apiClient)
                    },
                )
                HomeScreen(
                    viewModel = viewModel,
                    onPrinterClick = { printer ->
                        navController.navigate(
                            Routes.printerDetail(printer.id, printer.name, printer.model),
                        )
                    },
                )
            }

            composable(
                route = Routes.ARCHIVES,
                arguments = listOf(
                    navArgument("search") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { backStackEntry ->
                val encodedSearch = backStackEntry.arguments?.getString("search").orEmpty()
                val initialSearch = URLDecoder.decode(encodedSearch, StandardCharsets.UTF_8.toString())
                val viewModel: ArchivesViewModel = viewModel(
                    factory = viewModelFactory {
                        ArchivesViewModel(settingsRepository, apiClient)
                    },
                )
                LaunchedEffect(initialSearch) {
                    if (initialSearch.isNotBlank()) {
                        viewModel.applyInitialSearchQuery(initialSearch)
                    }
                }
                ArchivesScreen(
                    viewModel = viewModel,
                    onArchiveClick = { archive ->
                        navController.navigate(Routes.archiveDetail(archive.id))
                    },
                )
            }

            composable(
                route = Routes.SPOOLS,
                arguments = listOf(
                    navArgument("search") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { backStackEntry ->
                val encodedSearch = backStackEntry.arguments?.getString("search").orEmpty()
                val initialSearch = URLDecoder.decode(encodedSearch, StandardCharsets.UTF_8.toString())
                val viewModel: SpoolsViewModel = viewModel(
                    factory = viewModelFactory {
                        SpoolsViewModel(settingsRepository, apiClient)
                    },
                )
                SpoolsScreen(
                    viewModel = viewModel,
                    initialSearchQuery = initialSearch,
                    onSpoolClick = { spoolId ->
                        navController.navigate(Routes.spoolDetail(spoolId))
                    },
                )
            }

            composable(
                route = Routes.SPOOL_DETAIL,
                arguments = listOf(navArgument("spoolId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val spoolId = backStackEntry.arguments?.getInt("spoolId") ?: return@composable
                val viewModel: SpoolDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        SpoolDetailViewModel(settingsRepository, apiClient)
                    },
                )
                SpoolDetailScreen(
                    spoolId = spoolId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onArchiveClick = { archive ->
                        navController.navigate(Routes.archiveDetail(archive.id))
                    },
                    onViewAllArchives = { query ->
                        navController.navigate(Routes.archives(query)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            composable(Routes.SETTINGS) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        SettingsViewModel(settingsRepository, apiClient)
                    },
                )
                SettingsScreen(viewModel = viewModel)
            }

            composable(
                route = Routes.ARCHIVE_DETAIL,
                arguments = listOf(navArgument("archiveId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val archiveId = backStackEntry.arguments?.getInt("archiveId") ?: return@composable
                val viewModel: ArchiveDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        ArchiveDetailViewModel(settingsRepository, apiClient)
                    },
                )
                ArchiveDetailScreen(
                    archiveId = archiveId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onViewQueue = { printerId, printerName, printerModel ->
                        navController.navigate(
                            Routes.printerDetail(printerId, printerName, printerModel),
                        )
                    },
                    onMaterialNavigation = { navigation ->
                        when (navigation) {
                            is ArchiveMaterialNavigation.SpoolDetail -> {
                                navController.navigate(Routes.spoolDetail(navigation.spoolId))
                            }
                            is ArchiveMaterialNavigation.SpoolsFiltered -> {
                                navController.navigate(Routes.spools(navigation.searchQuery))
                            }
                            ArchiveMaterialNavigation.None -> Unit
                        }
                    },
                )
            }

            composable(
                route = Routes.PRINTER_DETAIL,
                arguments = listOf(
                    navArgument("printerId") { type = NavType.IntType },
                    navArgument("printerName") { type = NavType.StringType },
                    navArgument("printerModel") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { backStackEntry ->
                val printerId = backStackEntry.arguments?.getInt("printerId") ?: return@composable
                val encodedName = backStackEntry.arguments?.getString("printerName").orEmpty()
                val printerName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
                val encodedModel = backStackEntry.arguments?.getString("printerModel").orEmpty()
                val printerModel = URLDecoder.decode(encodedModel, StandardCharsets.UTF_8.toString())
                    .takeIf { it.isNotBlank() }

                val viewModel: PrinterDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        PrinterDetailViewModel(settingsRepository, apiClient)
                    },
                )
                PrinterDetailScreen(
                    printerId = printerId,
                    printerName = printerName,
                    printerModel = printerModel,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onViewFullQueue = { navController.navigate(Routes.PRINTER_QUEUE) },
                )
            }

            composable(Routes.PRINTER_QUEUE) {
                val parentEntry = navController.previousBackStackEntry
                if (parentEntry == null) {
                    navController.popBackStack()
                    return@composable
                }
                val viewModel: PrinterDetailViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = viewModelFactory {
                        PrinterDetailViewModel(settingsRepository, apiClient)
                    },
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val isActivePrint = uiState.status?.rawState?.uppercase() in setOf("RUNNING", "PAUSE")
                PrinterQueueScreen(
                    printerName = uiState.printerName,
                    jobs = uiState.queueUpcoming,
                    serverUrl = uiState.serverUrl,
                    cameraToken = uiState.cameraToken,
                    showStartNextPrint = uiState.queueUpcoming.isNotEmpty() &&
                        !isActivePrint &&
                        com.chronoswing.buddydash.network.BambuddyApi.hasQueueStartEndpoint,
                    startNextQueuedPrintReadiness = uiState.startNextQueuedPrintReadiness,
                    isStartingQueuedPrint = uiState.isStartingQueuedPrint,
                    startQueuedPrintSnackbar = uiState.startQueuedPrintSnackbar,
                    onBack = { navController.popBackStack() },
                    onStartNextQueuedPrint = viewModel::startNextQueuedPrint,
                    onStartQueuedPrintSnackbarShown = viewModel::onStartQueuedPrintSnackbarShown,
                )
            }
        }
    }
}

private inline fun <reified VM : ViewModel> viewModelFactory(
    crossinline create: () -> VM,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
