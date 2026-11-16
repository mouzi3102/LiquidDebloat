package com.liquiddebloat.model

import androidx.compose.ui.graphics.ImageBitmap

/** One row in the debloat list. */
data class AppEntry(
    val packageName: String,
    val label: String,
    val apkPath: String?,
    val versionName: String?,
    val isSystem: Boolean,
    val isUninstalled: Boolean,
    val icon: ImageBitmap?,
)
