package com.chronoswing.buddydash.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chronoswing.buddydash.HomeViewModel
import com.chronoswing.buddydash.PrinterDetailViewModel
import com.chronoswing.buddydash.SettingsViewModel
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PRINTER_DETAIL = "printer/{printerId}/{printerName}"

    fun printerDetail(printerId: Int, printerName: String): String {
        val encoded = java.net.URLEncoder.encode(printerName, StandardCharsets.UTF_8.toString())
        return "printer/$printerId/$encoded"
    }
}

@Composable
fun BuddyDashNav(
    settingsRepository: SettingsRepository,
    apiClient: BambuddyApiClient,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory {
                    HomeViewModel(settingsRepository, apiClient)
                },
            )
            HomeScreen(
                viewModel = viewModel,
                onPrinterClick = { printer ->
                    navController.navigate(Routes.printerDetail(printer.id, printer.name))
                },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    SettingsViewModel(settingsRepository, apiClient)
                },
            )
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PRINTER_DETAIL,
            arguments = listOf(
                navArgument("printerId") { type = NavType.IntType },
                navArgument("printerName") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val printerId = backStackEntry.arguments?.getInt("printerId") ?: return@composable
            val encodedName = backStackEntry.arguments?.getString("printerName").orEmpty()
            val printerName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())

            val viewModel: PrinterDetailViewModel = viewModel(
                factory = viewModelFactory {
                    PrinterDetailViewModel(settingsRepository, apiClient)
                },
            )
            PrinterDetailScreen(
                printerId = printerId,
                printerName = printerName,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private inline fun <reified VM : ViewModel> viewModelFactory(
    crossinline create: () -> VM,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
