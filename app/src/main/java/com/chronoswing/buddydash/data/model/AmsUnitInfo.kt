package com.chronoswing.buddydash.data.model

import com.chronoswing.buddydash.util.isAmsLiteModule

data class AmsUnitInfo(
    val amsId: Int,
    val label: String,
    /** Bambuddy AMSUnit `module_type`, e.g. AMS / AMS_LITE. */
    val moduleType: String? = null,
    val tempC: Double?,
    val humidityPercent: Int?,
) {
    val isAmsLite: Boolean
        get() = isAmsLiteModule(moduleType)
}
