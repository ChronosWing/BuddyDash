package com.chronoswing.buddydash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import com.chronoswing.buddydash.ui.motion.HomeAtmosphericFade
import com.chronoswing.buddydash.ui.motion.SecondaryScreenHeader
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.SettingsUiState
import com.chronoswing.buddydash.SettingsViewModel
import com.chronoswing.buddydash.ui.components.NfcStickersSettingsCard
import com.chronoswing.buddydash.util.BuddyDashDebug
import com.chronoswing.buddydash.util.HomeLogoGlowTuning
import com.chronoswing.buddydash.ui.layout.BuddyDashExpandedFormContainer
import com.chronoswing.buddydash.ui.layout.rememberBuddyDashExpandedGridColumnCount

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreenContent(
        uiState = uiState,
        onServerUrlChange = viewModel::onServerUrlChange,
        onApiKeyChange = viewModel::onApiKeyChange,
        onCameraTokenChange = viewModel::onCameraTokenChange,
        onSave = viewModel::saveSettings,
        onTestConnection = viewModel::testConnection,
        onIdleGlowMultiplierSelected = viewModel::onIdleGlowMultiplierSelected,
        onHeaderAmbientMultiplierSelected = viewModel::onHeaderAmbientMultiplierSelected,
        onPrintGlowMultiplierSelected = viewModel::onPrintGlowMultiplierSelected,
        onDebugForcePrintGlowChange = viewModel::onDebugForcePrintGlowChange,
        onDebugShowLogoGlowBoundsChange = viewModel::onDebugShowLogoGlowBoundsChange,
        onHomeCardDensityChange = viewModel::onHomeCardDensityChange,
        onFinishClearPlateChange = viewModel::onFinishClearPlateChange,
        onFinishPowerOffChange = viewModel::onFinishPowerOffChange,
        onFinishShowConfirmationChange = viewModel::onFinishShowConfirmationChange,
        onRememberLastDetailTabChange = viewModel::onRememberLastDetailTabChange,
        onKeepScreenAwakeChange = viewModel::onKeepScreenAwakeChange,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    onServerUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onCameraTokenChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onIdleGlowMultiplierSelected: (Float) -> Unit,
    onHeaderAmbientMultiplierSelected: (Float) -> Unit,
    onPrintGlowMultiplierSelected: (Float) -> Unit,
    onDebugForcePrintGlowChange: (Boolean) -> Unit,
    onDebugShowLogoGlowBoundsChange: (Boolean) -> Unit,
    onHomeCardDensityChange: (Int) -> Unit,
    onFinishClearPlateChange: (Boolean) -> Unit,
    onFinishPowerOffChange: (Boolean) -> Unit,
    onFinishShowConfirmationChange: (Boolean) -> Unit,
    onRememberLastDetailTabChange: (Boolean) -> Unit,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    onBack: (() -> Unit)?,
) {
    val gridColumns = rememberBuddyDashExpandedGridColumnCount()
    Scaffold(
        topBar = {
            Box {
                SecondaryScreenHeader(Modifier.matchParentSize())
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                )
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            HomeAtmosphericFade(Modifier.padding(top = innerPadding.calculateTopPadding()))
            BuddyDashExpandedFormContainer(
                gridColumns = gridColumns,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
            OutlinedTextField(
                value = uiState.serverUrl,
                onValueChange = onServerUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.server_url_label)) },
                placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedTextField(
                value = uiState.apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.api_key_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            OutlinedTextField(
                value = uiState.cameraToken,
                onValueChange = onCameraTokenChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.camera_cover_token_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_settings))
            }

            if (uiState.saved) {
                Text(
                    text = stringResource(R.string.settings_saved),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            OutlinedButton(
                onClick = onTestConnection,
                enabled = !uiState.isLoading &&
                    uiState.serverUrl.isNotBlank() &&
                    uiState.apiKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.isLoading) {
                        stringResource(R.string.testing_connection)
                    } else {
                        stringResource(R.string.test_connection)
                    },
                )
            }

            uiState.statusMessage?.let { message ->
                val color = when (uiState.isSuccess) {
                    true -> MaterialTheme.colorScheme.primary
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    text = message,
                    color = color,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            NfcStickersSettingsCard(
                examplePrinter = uiState.nfcExamplePrinter,
                modifier = Modifier.padding(top = 4.dp),
            )

            SettingsFinishWorkflowSection(
                clearPlate = uiState.finishClearPlate,
                powerOff = uiState.finishPowerOff,
                showConfirmation = uiState.finishShowConfirmation,
                onClearPlateChange = onFinishClearPlateChange,
                onPowerOffChange = onFinishPowerOffChange,
                onShowConfirmationChange = onFinishShowConfirmationChange,
            )

            SettingsCardDensitySection(
                density = uiState.homeCardDensity,
                onDensityChange = onHomeCardDensityChange,
            )

            SettingsQolSection(
                rememberTab = uiState.rememberLastDetailTab,
                keepAwake = uiState.keepScreenAwake,
                onRememberTabChange = onRememberLastDetailTabChange,
                onKeepAwakeChange = onKeepScreenAwakeChange,
            )

            if (BuddyDashDebug.enabled) {
                HomeHeaderVisualDebugSection(
                    gridColumns = gridColumns,
                    idleGlowMultiplier = uiState.idleGlowMultiplier,
                    headerAmbientMultiplier = uiState.headerAmbientMultiplier,
                    printGlowMultiplier = uiState.printGlowMultiplier,
                    debugForcePrintGlow = uiState.debugForcePrintGlow,
                    debugShowLogoGlowBounds = uiState.debugShowLogoGlowBounds,
                    onIdleGlowMultiplierSelected = onIdleGlowMultiplierSelected,
                    onHeaderAmbientMultiplierSelected = onHeaderAmbientMultiplierSelected,
                    onPrintGlowMultiplierSelected = onPrintGlowMultiplierSelected,
                    onDebugForcePrintGlowChange = onDebugForcePrintGlowChange,
                    onDebugShowLogoGlowBoundsChange = onDebugShowLogoGlowBoundsChange,
                )
            }
                }
            }
        } // Box
    }
}

@Composable
private fun HomeHeaderVisualDebugSection(
    gridColumns: Int,
    idleGlowMultiplier: Float,
    headerAmbientMultiplier: Float,
    printGlowMultiplier: Float,
    debugForcePrintGlow: Boolean,
    debugShowLogoGlowBounds: Boolean,
    onIdleGlowMultiplierSelected: (Float) -> Unit,
    onHeaderAmbientMultiplierSelected: (Float) -> Unit,
    onPrintGlowMultiplierSelected: (Float) -> Unit,
    onDebugForcePrintGlowChange: (Boolean) -> Unit,
    onDebugShowLogoGlowBoundsChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.debug_header_visual_tuning_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (gridColumns <= 1) {
            HomeHeaderVisualDebugCompactContent(
                idleGlowMultiplier = idleGlowMultiplier,
                headerAmbientMultiplier = headerAmbientMultiplier,
                printGlowMultiplier = printGlowMultiplier,
                debugForcePrintGlow = debugForcePrintGlow,
                debugShowLogoGlowBounds = debugShowLogoGlowBounds,
                onIdleGlowMultiplierSelected = onIdleGlowMultiplierSelected,
                onHeaderAmbientMultiplierSelected = onHeaderAmbientMultiplierSelected,
                onPrintGlowMultiplierSelected = onPrintGlowMultiplierSelected,
                onDebugForcePrintGlowChange = onDebugForcePrintGlowChange,
                onDebugShowLogoGlowBoundsChange = onDebugShowLogoGlowBoundsChange,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DebugMultiplierField(
                        label = stringResource(R.string.debug_idle_glow_multiplier_label),
                        selected = idleGlowMultiplier,
                        onSelected = onIdleGlowMultiplierSelected,
                    )
                    DebugMultiplierField(
                        label = stringResource(R.string.debug_header_ambient_multiplier_label),
                        selected = headerAmbientMultiplier,
                        onSelected = onHeaderAmbientMultiplierSelected,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DebugMultiplierField(
                        label = stringResource(R.string.debug_print_glow_multiplier_label),
                        selected = printGlowMultiplier,
                        onSelected = onPrintGlowMultiplierSelected,
                    )
                    DebugToggleRow(
                        label = stringResource(R.string.debug_force_print_glow_label),
                        hint = stringResource(R.string.debug_force_print_glow_hint),
                        checked = debugForcePrintGlow,
                        onCheckedChange = onDebugForcePrintGlowChange,
                    )
                    DebugToggleRow(
                        label = stringResource(R.string.debug_show_logo_glow_bounds_label),
                        hint = stringResource(R.string.debug_show_logo_glow_bounds_hint),
                        checked = debugShowLogoGlowBounds,
                        onCheckedChange = onDebugShowLogoGlowBoundsChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeaderVisualDebugCompactContent(
    idleGlowMultiplier: Float,
    headerAmbientMultiplier: Float,
    printGlowMultiplier: Float,
    debugForcePrintGlow: Boolean,
    debugShowLogoGlowBounds: Boolean,
    onIdleGlowMultiplierSelected: (Float) -> Unit,
    onHeaderAmbientMultiplierSelected: (Float) -> Unit,
    onPrintGlowMultiplierSelected: (Float) -> Unit,
    onDebugForcePrintGlowChange: (Boolean) -> Unit,
    onDebugShowLogoGlowBoundsChange: (Boolean) -> Unit,
) {
    DebugMultiplierField(
        label = stringResource(R.string.debug_idle_glow_multiplier_label),
        selected = idleGlowMultiplier,
        onSelected = onIdleGlowMultiplierSelected,
    )
    DebugMultiplierField(
        label = stringResource(R.string.debug_header_ambient_multiplier_label),
        selected = headerAmbientMultiplier,
        onSelected = onHeaderAmbientMultiplierSelected,
    )
    DebugMultiplierField(
        label = stringResource(R.string.debug_print_glow_multiplier_label),
        selected = printGlowMultiplier,
        onSelected = onPrintGlowMultiplierSelected,
    )
    DebugToggleRow(
        label = stringResource(R.string.debug_force_print_glow_label),
        hint = stringResource(R.string.debug_force_print_glow_hint),
        checked = debugForcePrintGlow,
        onCheckedChange = onDebugForcePrintGlowChange,
    )
    DebugToggleRow(
        label = stringResource(R.string.debug_show_logo_glow_bounds_label),
        hint = stringResource(R.string.debug_show_logo_glow_bounds_hint),
        checked = debugShowLogoGlowBounds,
        onCheckedChange = onDebugShowLogoGlowBoundsChange,
    )
}

@Composable
private fun DebugMultiplierField(
    label: String,
    selected: Float,
    onSelected: (Float) -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
    )
    VisualMultiplierChipRow(
        selected = selected,
        onSelected = onSelected,
    )
}

@Composable
private fun DebugToggleRow(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun VisualMultiplierChipRow(
    selected: Float,
    onSelected: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeLogoGlowTuning.multiplierPresets.forEach { preset ->
            FilterChip(
                selected = selected == preset,
                onClick = { onSelected(preset) },
                label = { Text(visualMultiplierLabel(preset)) },
            )
        }
    }
}

private fun visualMultiplierLabel(preset: Float): String = when (preset) {
    0.5f -> "0.5×"
    1f -> "1×"
    2f -> "2×"
    else -> "3×"
}

@Composable
private fun SettingsFinishWorkflowSection(
    clearPlate: Boolean,
    powerOff: Boolean,
    showConfirmation: Boolean,
    onClearPlateChange: (Boolean) -> Unit,
    onPowerOffChange: (Boolean) -> Unit,
    onShowConfirmationChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_finish_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsToggleRow(
            label = stringResource(R.string.settings_finish_clear_plate),
            checked = clearPlate,
            onCheckedChange = onClearPlateChange,
        )
        SettingsToggleRow(
            label = stringResource(R.string.settings_finish_power_off),
            checked = powerOff,
            onCheckedChange = onPowerOffChange,
        )
        SettingsToggleRow(
            label = stringResource(R.string.settings_finish_confirmation),
            checked = showConfirmation,
            onCheckedChange = onShowConfirmationChange,
        )
    }
}

@Composable
private fun SettingsCardDensitySection(
    density: Int,
    onDensityChange: (Int) -> Unit,
) {
    val modes = listOf(
        Triple(R.string.settings_view_mode_minimal, R.string.settings_view_mode_hint_minimal, 0),
        Triple(R.string.settings_view_mode_standard, R.string.settings_view_mode_hint_standard, 1),
        Triple(R.string.settings_view_mode_detailed, R.string.settings_view_mode_hint_detailed, 2),
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_view_mode_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            modes.forEach { (labelRes, _, index) ->
                FilterChip(
                    selected = density == index,
                    onClick = { onDensityChange(index) },
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }
        val hintRes = modes.firstOrNull { it.third == density }?.second
        if (hintRes != null) {
            Text(
                text = stringResource(hintRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun SettingsQolSection(
    rememberTab: Boolean,
    keepAwake: Boolean,
    onRememberTabChange: (Boolean) -> Unit,
    onKeepAwakeChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_qol_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsToggleRow(
            label = stringResource(R.string.settings_remember_tab),
            checked = rememberTab,
            onCheckedChange = onRememberTabChange,
        )
        SettingsToggleRow(
            label = stringResource(R.string.settings_keep_awake),
            checked = keepAwake,
            onCheckedChange = onKeepAwakeChange,
        )
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
