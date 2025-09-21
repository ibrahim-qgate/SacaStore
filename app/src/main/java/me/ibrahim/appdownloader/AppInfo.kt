package me.ibrahim.appdownloader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppInfo(
    val name: String,
    val packageName: String,
    val version: String,
    val downloadUrl: String,
    val iconUrl: String? = null,
    val description: String? = null
) : Parcelable
