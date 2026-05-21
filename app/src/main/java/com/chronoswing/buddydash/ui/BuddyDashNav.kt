package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.chronoswing.buddydash.HomeViewModel
import com.chronoswing.buddydash.PrinterDetailViewModel
import com.chronoswing.buddydash.SettingsViewModel
import com.chronoswing.buddydash.SpoolsViewModel
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.ui.components.BuddyDashBottomNav
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object Routes {
    const val HOME = "home"
    const val SPOOLS = "spools"
    const val SETTINGS = "settings"
    const val PRINTER_DETAIL = "printer/{printerId}/{printerName}/{printerModel}"
    const val PRINTER_QUEUE = "printer_queue"

    fun printerDetail(printerId: Int, printerName: String, printerModel: String? = null): String {
        val encodedName = java.net.URLEncoder.encode(printerName, StandardCharsets.UTF_8.toString())
        val encodedModel = java.net.URLEncoder.encode(printerModel.orEmpty(), StandardCharsets.UTF_8.toString())
        return "printer/$printerId/$encodedName/$encodedModel"
    }
}

private val bottomNavRoutes = setOf(Routes.HOME, Routes.SPOOLS, Routes.SETTINGS)

@Composable
fun BuddyDashNav(
    settingsRepository: SettingsRepository,
    apiClient: BambuddyApiClient,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

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
                        navController.navigate(Routes.SPOOLS) {
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

            composable(Routes.SPOOLS) {
                val viewModel: SpoolsViewModel = viewModel(
                    factory = viewModelFactory {
                        SpoolsViewModel(settingsRepository, apiClient)
                    },
                )
                SpoolsScreen(viewModel = viewModel)
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
                PrinterQueueScreen(
                    printerName = uiState.printerName,
                    jobs = uiState.queueUpcoming,
                    serverUrl = uiState.serverUrl,
                    cameraToken = uiState.cameraToken,
                    onBack = { navController.popBackStack() },
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
