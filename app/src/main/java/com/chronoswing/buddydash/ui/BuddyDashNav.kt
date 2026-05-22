package com.chronoswing.buddydash.ui

import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chronoswing.buddydash.util.BuddyDashDebug
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chronoswing.buddydash.ui.motion.buddyDashDetailEnter
import com.chronoswing.buddydash.ui.motion.buddyDashDetailExit
import com.chronoswing.buddydash.ui.motion.buddyDashDetailPopEnter
import com.chronoswing.buddydash.ui.motion.buddyDashDetailPopExit
import com.chronoswing.buddydash.ui.motion.buddyDashSectionEnter
import com.chronoswing.buddydash.ui.motion.buddyDashSectionExit
import com.chronoswing.buddydash.ArchiveDetailViewModel
import com.chronoswing.buddydash.ArchivesViewModel
import com.chronoswing.buddydash.HomeViewModel
import com.chronoswing.buddydash.PrinterDetailViewModel
import com.chronoswing.buddydash.SettingsViewModel
import com.chronoswing.buddydash.SpoolDetailViewModel
import com.chronoswing.buddydash.SpoolsViewModel
import com.chronoswing.buddydash.util.ArchiveMaterialNavigation
import com.chronoswing.buddydash.util.ArchiveSpoolLookupFilter
import com.chronoswing.buddydash.util.parseArchiveLookupColorHexesArg
import com.chronoswing.buddydash.data.ArchivesCacheRepository
import com.chronoswing.buddydash.data.HomePrintersCacheRepository
import com.chronoswing.buddydash.data.PrinterDetailCacheRepository
import com.chronoswing.buddydash.data.SettingsRepository
import com.chronoswing.buddydash.data.SpoolDetailCacheRepository
import com.chronoswing.buddydash.data.SpoolsCacheRepository
import com.chronoswing.buddydash.network.BambuddyApiClient
import com.chronoswing.buddydash.ui.components.AppForegroundResumeEffect
import com.chronoswing.buddydash.ui.components.BuddyDashBottomNav
import com.chronoswing.buddydash.util.RefreshSource
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val HOME = "home"
    const val SPOOLS_BASE = "spools"
    const val SPOOLS =
        "spools?search={search}&archiveMatch={archiveMatch}" +
            "&lookupMaterial={lookupMaterial}&lookupMaterialKey={lookupMaterialKey}" +
            "&lookupColorLabel={lookupColorLabel}&lookupColorHexes={lookupColorHexes}"
    const val ARCHIVES_BASE = "archives"
    const val NO_PRINTER_FILTER = -1
    const val ARCHIVES = "archives?search={search}&printerId={printerId}&printerName={printerName}"
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
        return "spools?search=$encoded&archiveMatch=false&lookupMaterial=&lookupMaterialKey=" +
            "&lookupColorLabel=&lookupColorHexes="
    }

    fun spoolsArchiveLookup(filter: ArchiveSpoolLookupFilter): String {
        val enc = { value: String ->
            URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        }
        val hexArg = filter.colorHexes.joinToString(",") { it.removePrefix("#") }
        return "spools?search=&archiveMatch=true" +
            "&lookupMaterial=${enc(filter.materialLabel)}" +
            "&lookupMaterialKey=${enc(filter.materialKey)}" +
            "&lookupColorLabel=${enc(filter.colorDisplayLabel.orEmpty())}" +
            "&lookupColorHexes=${enc(hexArg)}"
    }

    fun spoolDetail(spoolId: Int): String = "spool/$spoolId"

    fun archives(
        search: String = "",
        printerId: Int = NO_PRINTER_FILTER,
        printerName: String = "",
    ): String {
        val enc = { value: String ->
            URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        }
        return "archives?search=${enc(search)}&printerId=$printerId&printerName=${enc(printerName)}"
    }

    fun routeBase(route: String?): String? = route?.substringBefore('?')

    fun isPrintersRoot(route: String?): Boolean = routeBase(route) == HOME

    fun isSpoolsRoot(route: String?): Boolean = routeBase(route) == SPOOLS_BASE

    fun isArchivesRoot(route: String?): Boolean = routeBase(route) == ARCHIVES_BASE

    /** Printer Detail, full queue, or other stack above Home within the Printers tab. */
    fun isPrinterSubScreen(route: String?): Boolean {
        val base = routeBase(route) ?: return false
        return base == PRINTER_QUEUE || base.startsWith("printer/")
    }

    /** Which bottom-nav tab is highlighted for the current destination (including detail screens). */
    fun bottomNavTab(route: String?): BottomNavTab? {
        val base = routeBase(route) ?: return null
        return when {
            base == HOME ||
                base == PRINTER_QUEUE ||
                base.startsWith("printer/") -> BottomNavTab.PRINTERS
            base == SPOOLS_BASE ||
                base.startsWith("spool/") -> BottomNavTab.SPOOLS
            base == ARCHIVES_BASE ||
                base.startsWith("archive/") -> BottomNavTab.ARCHIVES
            base == SETTINGS -> BottomNavTab.SETTINGS
            else -> null
        }
    }
}

enum class BottomNavTab {
    PRINTERS,
    SPOOLS,
    ARCHIVES,
    SETTINGS,
}

/** Temporary: verify bottom-tab destinations. Set false before release. */
private val debugLogNavDestinations: Boolean get() = com.chronoswing.buddydash.util.BuddyDashDebug.enabled
private const val TAG_NAV = "BuddyDash/Nav"

private fun bottomNavSelectedLabel(route: String?): String =
    when (Routes.bottomNavTab(route)) {
        BottomNavTab.PRINTERS -> "Printers"
        BottomNavTab.SPOOLS -> "Spools"
        BottomNavTab.ARCHIVES -> "Archives"
        BottomNavTab.SETTINGS -> "Settings"
        null -> "none"
    }

/**
 * Bottom-nav: always open a section root. Does not restore detail screens or filters.
 */
private fun NavHostController.navigateToSectionRoot(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }
}

private fun logNavState(
    event: String,
    currentRoute: String?,
    extra: String = "",
) {
    if (!debugLogNavDestinations) return
    Log.d(
        TAG_NAV,
        "$event currentRoute=$currentRoute selectedTab=${bottomNavSelectedLabel(currentRoute)} $extra",
    )
}

private fun logBottomNavTap(
    tab: BottomNavTab,
    currentRoute: String?,
    wasCurrent: Boolean,
    refreshTriggered: Boolean,
) {
    if (!BuddyDashDebug.enabled) return
    Log.d(
        TAG_NAV,
        "bottomNavTap tab=$tab wasCurrent=$wasCurrent refreshTriggered=$refreshTriggered " +
            "currentRoute=$currentRoute",
    )
}

private fun NavHostController.onBottomNavTabSelected(
    tab: BottomNavTab,
    currentRoute: String?,
    rootRoute: String,
    onReselectRefresh: () -> Unit,
) {
    val wasCurrent = Routes.bottomNavTab(currentRoute) == tab
    val refreshTriggered = wasCurrent && tab != BottomNavTab.SETTINGS
    logBottomNavTap(tab, currentRoute, wasCurrent, refreshTriggered)
    navigateToSectionRoot(rootRoute)
    if (refreshTriggered) {
        onReselectRefresh()
    }
}

@Composable
fun BuddyDashNav(
    settingsRepository: SettingsRepository,
    homePrintersCacheRepository: HomePrintersCacheRepository,
    spoolsCacheRepository: SpoolsCacheRepository,
    archivesCacheRepository: ArchivesCacheRepository,
    printerDetailCacheRepository: PrinterDetailCacheRepository,
    spoolDetailCacheRepository: SpoolDetailCacheRepository,
    apiClient: BambuddyApiClient,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var printersReselectNonce by remember { mutableIntStateOf(0) }
    var printersReturnRefreshNonce by remember { mutableIntStateOf(0) }
    var appResumeNonce by remember { mutableIntStateOf(0) }
    var previousRoute by remember { mutableStateOf<String?>(null) }
    var spoolsReselectNonce by remember { mutableIntStateOf(0) }
    var archivesReselectNonce by remember { mutableIntStateOf(0) }
    var spoolsAppResumeNonce by remember { mutableIntStateOf(0) }
    var archivesAppResumeNonce by remember { mutableIntStateOf(0) }
    var printerDetailAppResumeNonce by remember { mutableIntStateOf(0) }

    AppForegroundResumeEffect {
        appResumeNonce++
        spoolsAppResumeNonce++
        archivesAppResumeNonce++
        printerDetailAppResumeNonce++
    }

    LaunchedEffect(Unit) {
        if (debugLogNavDestinations) {
            Log.d(
                TAG_NAV,
                "bottomNavTabs=Printers(${Routes.HOME}), Spools(${Routes.SPOOLS}), " +
                    "Archives(${Routes.ARCHIVES}), Settings(${Routes.SETTINGS})",
            )
        }
    }

    LaunchedEffect(currentRoute) {
        logNavState("routeChanged", currentRoute)
        val returnedFromPrinterSubScreen =
            Routes.isPrintersRoot(currentRoute) && Routes.isPrinterSubScreen(previousRoute)
        if (returnedFromPrinterSubScreen) {
            if (BuddyDashDebug.enabled) {
                Log.d(
                    TAG_NAV,
                    "returnToPrintersRoot from=$previousRoute refreshTriggered=true " +
                        "source=${RefreshSource.RETURN_FROM_DETAIL}",
                )
            }
            printersReturnRefreshNonce++
        }
        previousRoute = currentRoute
    }

    Scaffold(
        // Tab screens own top safe area (Home header, TopAppBar, etc.). Applying top
        // insets here as well doubled status-bar padding on Home.
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
        ),
        bottomBar = {
            BuddyDashBottomNav(
                currentRoute = currentRoute,
                onPrinters = {
                    navController.onBottomNavTabSelected(
                        tab = BottomNavTab.PRINTERS,
                        currentRoute = currentRoute,
                        rootRoute = Routes.HOME,
                    ) {
                        printersReselectNonce++
                    }
                },
                onSpools = {
                    navController.onBottomNavTabSelected(
                        tab = BottomNavTab.SPOOLS,
                        currentRoute = currentRoute,
                        rootRoute = Routes.spools(),
                    ) {
                        spoolsReselectNonce++
                    }
                },
                onArchives = {
                    navController.onBottomNavTabSelected(
                        tab = BottomNavTab.ARCHIVES,
                        currentRoute = currentRoute,
                        rootRoute = Routes.archives(),
                    ) {
                        archivesReselectNonce++
                    }
                },
                onSettings = {
                    navController.onBottomNavTabSelected(
                        tab = BottomNavTab.SETTINGS,
                        currentRoute = currentRoute,
                        rootRoute = Routes.SETTINGS,
                        onReselectRefresh = {},
                    )
                },
            )
        },
    ) { innerPadding ->
        val navContext = navController.context
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { buddyDashSectionEnter(navContext) },
            exitTransition = { buddyDashSectionExit(navContext) },
            popEnterTransition = { buddyDashSectionEnter(navContext) },
            popExitTransition = { buddyDashSectionExit(navContext) },
        ) {
            composable(Routes.HOME) {
                val viewModel: HomeViewModel = viewModel(
                    factory = viewModelFactory {
                        HomeViewModel(
                            settingsRepository,
                            apiClient,
                            homePrintersCacheRepository,
                            spoolsCacheRepository,
                        )
                    },
                )
                LaunchedEffect(printersReturnRefreshNonce) {
                    if (printersReturnRefreshNonce > 0) {
                        viewModel.refreshOnReturnFromDetail(currentRoute)
                    }
                }
                LaunchedEffect(printersReselectNonce) {
                    if (printersReselectNonce > 0) {
                        viewModel.refreshFromBottomNavReselect(currentRoute)
                    }
                }
                LaunchedEffect(appResumeNonce, currentRoute) {
                    if (appResumeNonce > 0 && Routes.isPrintersRoot(currentRoute)) {
                        viewModel.refreshOnAppResume(currentRoute)
                    }
                }
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
                    navArgument("printerId") {
                        type = NavType.IntType
                        defaultValue = Routes.NO_PRINTER_FILTER
                    },
                    navArgument("printerName") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { backStackEntry ->
                val decode = { key: String ->
                    URLDecoder.decode(
                        backStackEntry.arguments?.getString(key).orEmpty(),
                        StandardCharsets.UTF_8.toString(),
                    )
                }
                val initialSearch = decode("search")
                val initialPrinterId = backStackEntry.arguments?.getInt("printerId")
                    ?: Routes.NO_PRINTER_FILTER
                val initialPrinterName = decode("printerName")
                val viewModel: ArchivesViewModel = viewModel(
                    factory = viewModelFactory {
                        ArchivesViewModel(
                            settingsRepository,
                            apiClient,
                            archivesCacheRepository,
                        )
                    },
                )
                LaunchedEffect(initialSearch, initialPrinterId, initialPrinterName) {
                    if (initialPrinterId >= 0 && initialPrinterName.isNotBlank()) {
                        viewModel.applyPrinterFilter(initialPrinterId, initialPrinterName)
                    } else {
                        viewModel.clearPrinterFilter()
                    }
                    if (initialSearch.isNotBlank()) {
                        viewModel.applyInitialSearchQuery(initialSearch)
                    } else {
                        viewModel.clearSearchQuery()
                    }
                }
                LaunchedEffect(archivesReselectNonce) {
                    if (archivesReselectNonce > 0) {
                        viewModel.refreshFromBottomNavReselect()
                    }
                }
                LaunchedEffect(archivesAppResumeNonce, currentRoute) {
                    if (archivesAppResumeNonce > 0 && Routes.isArchivesRoot(currentRoute)) {
                        viewModel.refreshOnAppResume(currentRoute)
                    }
                }
                ArchivesScreen(
                    viewModel = viewModel,
                    onArchiveClick = { archive ->
                        navController.navigate(Routes.archiveDetail(archive.id))
                    },
                    onClearPrinterFilter = {
                        navController.navigateToSectionRoot(Routes.archives())
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
                    navArgument("archiveMatch") {
                        type = NavType.StringType
                        defaultValue = "false"
                    },
                    navArgument("lookupMaterial") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("lookupMaterialKey") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("lookupColorLabel") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("lookupColorHexes") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { backStackEntry ->
                val decode = { key: String ->
                    URLDecoder.decode(
                        backStackEntry.arguments?.getString(key).orEmpty(),
                        StandardCharsets.UTF_8.toString(),
                    )
                }
                val initialSearch = decode("search")
                val archiveMatch = backStackEntry.arguments?.getString("archiveMatch") == "true"
                val initialArchiveLookup = if (archiveMatch) {
                    ArchiveSpoolLookupFilter(
                        materialLabel = decode("lookupMaterial"),
                        materialKey = decode("lookupMaterialKey"),
                        colorHexes = parseArchiveLookupColorHexesArg(decode("lookupColorHexes")),
                        colorDisplayLabel = decode("lookupColorLabel").ifBlank { null },
                    )
                } else {
                    null
                }
                val viewModel: SpoolsViewModel = viewModel(
                    factory = viewModelFactory {
                        SpoolsViewModel(
                            settingsRepository,
                            apiClient,
                            spoolsCacheRepository,
                        )
                    },
                )
                val fromArchiveMaterialLookup = archiveMatch
                LaunchedEffect(spoolsReselectNonce) {
                    if (spoolsReselectNonce > 0) {
                        viewModel.refreshFromBottomNavReselect()
                    }
                }
                LaunchedEffect(spoolsAppResumeNonce, currentRoute) {
                    if (spoolsAppResumeNonce > 0 && Routes.isSpoolsRoot(currentRoute)) {
                        viewModel.refreshOnAppResume(currentRoute)
                    }
                }
                SpoolsScreen(
                    viewModel = viewModel,
                    initialSearchQuery = initialSearch,
                    initialArchiveLookupFilter = initialArchiveLookup,
                    onBack = if (fromArchiveMaterialLookup) {
                        {
                            logNavState("tapSpoolsBack", currentRoute, "action=popToArchiveDetail")
                            navController.popBackStack()
                        }
                    } else {
                        null
                    },
                    onSpoolClick = { spoolId ->
                        navController.navigate(Routes.spoolDetail(spoolId))
                    },
                    onClearArchiveLookup = {
                        logNavState("clearArchiveLookup", currentRoute, "action=spoolsUnfilteredInStack")
                        viewModel.clearArchiveLookupFilter()
                        navController.navigate(Routes.spools()) {
                            popUpTo(backStackEntry.destination.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(
                route = Routes.SPOOL_DETAIL,
                arguments = listOf(navArgument("spoolId") { type = NavType.IntType }),
                enterTransition = { buddyDashDetailEnter(navContext) },
                exitTransition = { buddyDashDetailExit(navContext) },
                popEnterTransition = { buddyDashDetailPopEnter(navContext) },
                popExitTransition = { buddyDashDetailPopExit(navContext) },
            ) { backStackEntry ->
                val spoolId = backStackEntry.arguments?.getInt("spoolId") ?: return@composable
                val viewModel: SpoolDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        SpoolDetailViewModel(
                            settingsRepository,
                            apiClient,
                            spoolsCacheRepository,
                            spoolDetailCacheRepository,
                        )
                    },
                )
                SpoolDetailScreen(
                    spoolId = spoolId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onArchiveClick = { archiveId ->
                        navController.navigate(Routes.archiveDetail(archiveId))
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
                enterTransition = { buddyDashDetailEnter(navContext) },
                exitTransition = { buddyDashDetailExit(navContext) },
                popEnterTransition = { buddyDashDetailPopEnter(navContext) },
                popExitTransition = { buddyDashDetailPopExit(navContext) },
            ) { backStackEntry ->
                val archiveId = backStackEntry.arguments?.getInt("archiveId") ?: return@composable
                val viewModel: ArchiveDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        ArchiveDetailViewModel(
                            settingsRepository,
                            apiClient,
                            archivesCacheRepository,
                        )
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
                                logNavState(
                                    "archiveMaterialTap",
                                    currentRoute,
                                    "matchCount=1 route=SpoolDetail spoolId=${navigation.spoolId}",
                                )
                                navController.navigate(Routes.spoolDetail(navigation.spoolId))
                            }
                            is ArchiveMaterialNavigation.SpoolsFiltered -> {
                                logNavState(
                                    "archiveMaterialTap",
                                    currentRoute,
                                    "route=SpoolsFilteredPush material=${navigation.lookupFilter.materialLabel}",
                                )
                                navController.navigate(
                                    Routes.spoolsArchiveLookup(navigation.lookupFilter),
                                )
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
                enterTransition = { buddyDashDetailEnter(navContext) },
                exitTransition = { buddyDashDetailExit(navContext) },
                popEnterTransition = { buddyDashDetailPopEnter(navContext) },
                popExitTransition = { buddyDashDetailPopExit(navContext) },
            ) { backStackEntry ->
                val printerId = backStackEntry.arguments?.getInt("printerId") ?: return@composable
                val encodedName = backStackEntry.arguments?.getString("printerName").orEmpty()
                val printerName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
                val encodedModel = backStackEntry.arguments?.getString("printerModel").orEmpty()
                val printerModel = URLDecoder.decode(encodedModel, StandardCharsets.UTF_8.toString())
                    .takeIf { it.isNotBlank() }

                val viewModel: PrinterDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        PrinterDetailViewModel(
                            settingsRepository,
                            apiClient,
                            printerDetailCacheRepository,
                            homePrintersCacheRepository,
                        )
                    },
                )
                LaunchedEffect(printerDetailAppResumeNonce, currentRoute) {
                    if (printerDetailAppResumeNonce > 0 && Routes.isPrinterSubScreen(currentRoute)) {
                        viewModel.refreshOnAppResume(currentRoute)
                    }
                }
                PrinterDetailScreen(
                    printerId = printerId,
                    printerName = printerName,
                    printerModel = printerModel,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onViewFullQueue = { navController.navigate(Routes.PRINTER_QUEUE) },
                    onOpenPrinterArchives = {
                        navController.navigateToSectionRoot(
                            Routes.archives(printerId = printerId, printerName = printerName),
                        )
                    },
                    onOpenSpoolDetail = { spoolId ->
                        navController.navigate(Routes.spoolDetail(spoolId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(
                route = Routes.PRINTER_QUEUE,
                enterTransition = { buddyDashDetailEnter(navContext) },
                exitTransition = { buddyDashDetailExit(navContext) },
                popEnterTransition = { buddyDashDetailPopEnter(navContext) },
                popExitTransition = { buddyDashDetailPopExit(navContext) },
            ) {
                val parentEntry = navController.previousBackStackEntry
                if (parentEntry == null) {
                    navController.popBackStack()
                    return@composable
                }
                val viewModel: PrinterDetailViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = viewModelFactory {
                        PrinterDetailViewModel(
                            settingsRepository,
                            apiClient,
                            printerDetailCacheRepository,
                            homePrintersCacheRepository,
                        )
                    },
                )
                LaunchedEffect(printerDetailAppResumeNonce, currentRoute) {
                    if (printerDetailAppResumeNonce > 0 && Routes.isPrinterSubScreen(currentRoute)) {
                        viewModel.refreshOnAppResume(currentRoute)
                    }
                }
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
