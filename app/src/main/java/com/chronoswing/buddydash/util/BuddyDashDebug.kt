package com.chronoswing.buddydash.util

import com.chronoswing.buddydash.BuildConfig

/** Central gate for verbose development logging (never logs API keys or tokens). */
object BuddyDashDebug {
    val enabled: Boolean = BuildConfig.DEBUG
}
