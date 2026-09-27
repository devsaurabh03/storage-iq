package com.storage.intelligence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.storage.intelligence.core.permissions.PermissionManager
import com.storage.intelligence.core.ui.theme.StorageIQTheme
import com.storage.intelligence.feature.navigation.AppNavHost
import com.storage.intelligence.feature.navigation.Routes
import com.storage.intelligence.feature.onboarding.OnboardingViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    private val onboardingViewModel: OnboardingViewModel by viewModels()

    private val _hasPermission = androidx.compose.runtime.mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        _hasPermission.value = permissionManager.isStoragePermissionGranted()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !permissionManager.hasAllFilesAccess()) {
            permissionManager.requestAllFilesAccess(this)
        }
    }

    override fun onResume() {
        super.onResume()
        _hasPermission.value = permissionManager.isStoragePermissionGranted()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _hasPermission.value = permissionManager.isStoragePermissionGranted()

        setContent {
            StorageIQTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val hasCompletedOnboarding by onboardingViewModel.hasCompletedOnboarding.collectAsState()
                    val hasPermission by _hasPermission

                    val startDest = when {
                        !hasCompletedOnboarding -> Routes.ONBOARDING
                        !hasPermission -> Routes.PERMISSION
                        else -> Routes.SCANNER
                    }

                    AppNavHost(
                        startDestination = startDest,
                        hasPermission = hasPermission,
                        onRequestPermission = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !permissionManager.hasAllFilesAccess()) {
                                permissionLauncher.launch(permissionManager.getRequiredPermissions().toTypedArray())
                                permissionManager.requestAllFilesAccess(this@MainActivity)
                            } else {
                                permissionLauncher.launch(permissionManager.getRequiredPermissions().toTypedArray())
                            }
                        }
                    )
                }
            }
        }
    }
}
