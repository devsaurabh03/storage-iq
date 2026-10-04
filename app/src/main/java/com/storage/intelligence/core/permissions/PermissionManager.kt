package com.storage.intelligence.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class PermissionState {
    GRANTED,
    PARTIAL,
    DENIED
}

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun checkPermissionState(): PermissionState {
        if (Build.VERSION.SDK_INT >= 34) {
            val hasSelectedAccess = ContextCompat.checkSelfPermission(
                context,
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
            ) == PackageManager.PERMISSION_GRANTED
            val hasFullImages = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val hasFullVideos = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED

            return when {
                hasFullImages && hasFullVideos -> PermissionState.GRANTED
                hasSelectedAccess || hasFullImages || hasFullVideos -> PermissionState.PARTIAL
                else -> PermissionState.DENIED
            }
        }

        val required = getRequiredPermissions()
        val grantedCount = required.count { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        return when {
            grantedCount == required.size -> PermissionState.GRANTED
            grantedCount > 0 -> PermissionState.PARTIAL
            else -> PermissionState.DENIED
        }
    }

    fun isPartialAccess(): Boolean {
        return checkPermissionState() == PermissionState.PARTIAL
    }

    fun isStoragePermissionGranted(): Boolean {
        return hasAllFilesAccess() || checkPermissionState() == PermissionState.GRANTED || checkPermissionState() == PermissionState.PARTIAL
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestAllFilesAccess(contextActivity: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val uri = android.net.Uri.parse("package:${contextActivity.packageName}")
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                contextActivity.startActivity(intent)
            } catch (e: Exception) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                contextActivity.startActivity(intent)
            }
        }
    }
}
