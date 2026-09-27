package com.storage.intelligence.feature.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class AppStorageItem(
    val packageName: String,
    val appName: String,
    val appSizeBytes: Long,
    val dataSizeBytes: Long,
    val cacheSizeBytes: Long
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    fun openAppStorageSettings(packageName: String? = null) {
        val intent = if (packageName != null) {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }
}
