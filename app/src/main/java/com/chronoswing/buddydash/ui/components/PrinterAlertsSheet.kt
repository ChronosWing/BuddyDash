package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.R
import com.chronoswing.buddydash.data.model.MaintenanceItem
import com.chronoswing.buddydash.data.model.PrinterHmsError
import com.chronoswing.buddydash.util.HmsSeverity
import com.chronoswing.buddydash.util.MaintenanceHomeIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterAlertsSheet(
    printerName: String,
    printerModel: String?,
    hmsErrors: List<PrinterHmsError>,
    hmsAlertSeverity: HmsSeverity,
    maintenanceItems: List<MaintenanceItem>,
    maintenanceIndicator: MaintenanceHomeIndicator,
    maintenanceTotalPrintHours: Double?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        PrinterAlertsContent(
            printerName = printerName,
            printerModel = printerModel,
            hmsErrors = hmsErrors,
            hmsAlertSeverity = hmsAlertSeverity,
            maintenanceItems = maintenanceItems,
            maintenanceIndicator = maintenanceIndicator,
            maintenanceTotalPrintHours = maintenanceTotalPrintHours,
        )
    }
}

@Composable
fun PrinterAlertsContent(
    printerName: String,
    printerModel: String?,
    hmsErrors: List<PrinterHmsError>,
    hmsAlertSeverity: HmsSeverity,
    maintenanceItems: List<MaintenanceItem>,
    maintenanceIndicator: MaintenanceHomeIndicator,
    maintenanceTotalPrintHours: Double?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.printer_alerts_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = printerName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (hmsAlertSeverity != HmsSeverity.Ok) {
            Text(
                text = stringResource(R.string.printer_alerts_hms_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            HmsAlertsListContent(
                hmsErrors = hmsErrors,
                hmsAlertSeverity = hmsAlertSeverity,
                printerModel = printerModel,
            )
        }

        if (maintenanceIndicator != MaintenanceHomeIndicator.None) {
            if (hmsAlertSeverity != HmsSeverity.Ok) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                )
            }
            Text(
                text = stringResource(R.string.printer_alerts_maintenance_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            MaintenanceAlertsContent(
                printerName = printerName,
                maintenanceItems = maintenanceItems,
                maintenanceIndicator = maintenanceIndicator,
                totalPrintHours = maintenanceTotalPrintHours,
                showHeader = false,
                standaloneSheet = false,
            )
        }
    }
}
