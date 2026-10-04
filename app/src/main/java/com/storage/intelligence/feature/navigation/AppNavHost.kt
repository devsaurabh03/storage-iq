package com.storage.intelligence.feature.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.storage.intelligence.R
import com.storage.intelligence.domain.model.StorageCategory
import com.storage.intelligence.feature.apps.AppsScreen
import com.storage.intelligence.feature.cleanup.CleanupCenterScreen
import com.storage.intelligence.feature.dashboard.DashboardScreen
import com.storage.intelligence.feature.downloads.DownloadsScreen
import com.storage.intelligence.feature.duplicates.DuplicatesScreen
import com.storage.intelligence.feature.largefiles.LargeFilesScreen
import com.storage.intelligence.feature.messaging.MessagingMediaScreen
import com.storage.intelligence.feature.onboarding.OnboardingScreen
import com.storage.intelligence.feature.permissions.PermissionScreen
import com.storage.intelligence.feature.scanner.ScannerScreen
import com.storage.intelligence.feature.search.SearchScreen
import com.storage.intelligence.feature.screenshots.ScreenshotsScreen
import com.storage.intelligence.feature.settings.SettingsScreen
import com.storage.intelligence.feature.similar.SimilarPhotosScreen
import com.storage.intelligence.feature.subscription.PaywallScreen
import com.storage.intelligence.feature.timeline.TimelineScreen

import androidx.compose.material.icons.filled.Speed
import com.storage.intelligence.feature.performance.ui.CpuIntelligenceScreen
import com.storage.intelligence.feature.performance.ui.DiagnosticsScreen
import com.storage.intelligence.feature.performance.ui.PerformanceDashboardScreen

import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Movie
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storage.intelligence.core.ui.theme.PrimaryCyan
import com.storage.intelligence.core.ui.theme.SurfaceOled

object Routes {
    const val ONBOARDING = "onboarding"
    const val PERMISSION = "permission"
    const val SCANNER = "scanner"
    const val DASHBOARD = "dashboard"
    const val CLEANUP = "cleanup"
    const val PERFORMANCE = "performance"
    const val RAM_ANALYSIS = "ram_analysis"
    const val CPU_INTELLIGENCE = "cpu_intelligence"
    const val DIAGNOSTICS_SLOW = "diagnostics_slow"
    const val DIAGNOSTICS_HOT = "diagnostics_hot"
    const val TIMELINE = "timeline"
    const val SETTINGS = "settings"
    const val DUPLICATES = "duplicates"
    const val SIMILAR = "similar"
    const val LARGE_FILES = "large_files"
    const val DOWNLOADS = "downloads"
    const val SCREENSHOTS = "screenshots"
    const val MESSAGING = "messaging"
    const val APPS = "apps"
    const val SEARCH = "search"
    const val PAYWALL = "paywall"
    const val OTHER_STORAGE = "other_storage"
    const val STALE_OPTIMIZE = "stale_optimize"
    const val OPTIMIZED_ARCHIVE = "optimized_archive"
    const val PHOTO_OPTIMIZATION = "photo_optimization"
    const val ME = "me"

    // AV1 Smart Video Optimizer & Performance Lab Routes
    const val VIDEO_HOME = "video_home"
    const val VIDEO_LIBRARY = "video_library"
    const val VIDEO_ACTIVITY = "video_activity"
    const val VIDEO_DETAIL = "video_detail"
    const val PERFORMANCE_LAB = "performance_lab"
    const val BENCHMARK_RESULT = "benchmark_result"
    const val CODEC_COMPARISON = "codec_comparison"
}

sealed class BottomNavItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Overview : BottomNavItem(Routes.DASHBOARD, "Overview", Icons.Default.Dashboard)
    data object Cleanup : BottomNavItem(Routes.CLEANUP, "Cleanup", Icons.Default.AutoFixHigh)
    data object Media : BottomNavItem(Routes.VIDEO_HOME, "Media", Icons.Default.Movie)
    data object Vitals : BottomNavItem(Routes.PERFORMANCE, "Vitals", Icons.Default.Speed)
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.ONBOARDING,
    hasPermission: Boolean = false,
    onRequestPermission: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Overview,
        BottomNavItem.Cleanup,
        BottomNavItem.Media,
        BottomNavItem.Vitals
    )

    val showBottomBar = currentRoute in listOf(Routes.DASHBOARD, Routes.PERFORMANCE, Routes.CLEANUP, Routes.ME, Routes.VIDEO_HOME)

    val sharedVideoViewModel: com.storage.intelligence.feature.video.presentation.VideoViewModel = hiltViewModel()

    Scaffold(
        containerColor = SurfaceOled,
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 10.dp, start = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xE60A0E16))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentRoute == item.route
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = if (isSelected) PrimaryCyan else Color.White.copy(alpha = 0.40f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.title,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) PrimaryCyan else Color.White.copy(alpha = 0.40f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        if (hasPermission) {
                            navController.navigate(Routes.SCANNER) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.PERMISSION) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    },
                    viewModel = hiltViewModel()
                )
            }

            composable(Routes.PERMISSION) {
                PermissionScreen(
                    onRequestPermission = {
                        onRequestPermission()
                        navController.navigate(Routes.SCANNER) {
                            popUpTo(Routes.PERMISSION) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SCANNER) {
                ScannerScreen(
                    viewModel = hiltViewModel(),
                    onScanComplete = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.SCANNER) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToReview = { navController.navigate(Routes.CLEANUP) },
                    onNavigateToCategory = { category ->
                        when (category) {
                            StorageCategory.VIDEOS -> navController.navigate(Routes.VIDEO_HOME)
                            StorageCategory.PHOTOS -> navController.navigate(Routes.PHOTO_OPTIMIZATION)
                            StorageCategory.DOWNLOADS -> navController.navigate(Routes.DOWNLOADS)
                            StorageCategory.SCREENSHOTS -> navController.navigate(Routes.SCREENSHOTS)
                            StorageCategory.WHATSAPP, StorageCategory.TELEGRAM -> navController.navigate(Routes.MESSAGING)
                            StorageCategory.APPS -> navController.navigate(Routes.APPS)
                            StorageCategory.OTHER -> navController.navigate(Routes.OTHER_STORAGE)
                            else -> navController.navigate(Routes.LARGE_FILES)
                        }
                    },
                    onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                    onNavigateToTimeline = { navController.navigate(Routes.TIMELINE) },
                    onNavigateToOtherStorage = { navController.navigate(Routes.OTHER_STORAGE) },
                    onNavigateToMessaging = { navController.navigate(Routes.MESSAGING) },
                    onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
                    onNavigateToPerformance = { navController.navigate(Routes.PERFORMANCE) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.PERFORMANCE) {
                PerformanceDashboardScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToCpuDetails = { navController.navigate(Routes.CPU_INTELLIGENCE) },
                    onNavigateToSlowDiagnosis = { navController.navigate(Routes.DIAGNOSTICS_SLOW) },
                    onNavigateToHotDiagnosis = { navController.navigate(Routes.DIAGNOSTICS_HOT) },
                    onNavigateToRoute = { route -> navController.navigate(route) }
                )
            }

            composable(Routes.RAM_ANALYSIS) {
                com.storage.intelligence.feature.performance.ui.RamAnalysisScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.CPU_INTELLIGENCE) {
                CpuIntelligenceScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.DIAGNOSTICS_SLOW) {
                DiagnosticsScreen(
                    mode = "slow",
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRoute = { route -> navController.navigate(route) }
                )
            }

            composable(Routes.DIAGNOSTICS_HOT) {
                DiagnosticsScreen(
                    mode = "hot",
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRoute = { route -> navController.navigate(route) }
                )
            }

            composable(Routes.CLEANUP) {
                CleanupCenterScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToDuplicates = { navController.navigate(Routes.DUPLICATES) },
                    onNavigateToSimilar = { navController.navigate(Routes.SIMILAR) },
                    onNavigateToLargeFiles = { navController.navigate(Routes.LARGE_FILES) },
                    onNavigateToDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onNavigateToScreenshots = { navController.navigate(Routes.SCREENSHOTS) },
                    onNavigateToMessaging = { navController.navigate(Routes.MESSAGING) },
                    onNavigateToStaleOptimize = { navController.navigate(Routes.STALE_OPTIMIZE) },
                    onNavigateToVideoOptimization = { navController.navigate(Routes.VIDEO_HOME) },
                    onNavigateToPhotoOptimization = { navController.navigate(Routes.PHOTO_OPTIMIZATION) }
                )
            }

            composable(Routes.PHOTO_OPTIMIZATION) {
                com.storage.intelligence.feature.photo.ui.PhotoOptimizationScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.TIMELINE) {
                TimelineScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.VIDEO_HOME) {
                val uiState by sharedVideoViewModel.uiState.collectAsState()
                val legacyVideos = uiState.videos.filter {
                    it.codecName == "H.264" || it.codecName == "MPEG-4" ||
                    it.codecName.contains("AVC", ignoreCase = true) ||
                    it.codecName.contains("264", ignoreCase = true) ||
                    it.codecName.contains("3GP", ignoreCase = true)
                }

                com.storage.intelligence.feature.video.ui.VideoHomeScreen(
                    totalVideoBytes = uiState.totalStorageBytes,
                    potentialSavingsBytes = uiState.potentialSavingsBytes,
                    totalVideoCount = uiState.videos.size,
                    optimizableCount = uiState.optimizableVideos.size,
                    alreadyOptimalCount = uiState.alreadyOptimalVideos.size,
                    fourKCount = uiState.videos.count { it.width >= 3840 || it.height >= 2160 },
                    fourKBytes = uiState.videos.filter { it.width >= 3840 || it.height >= 2160 }.sumOf { it.sizeBytes },
                    legacyCount = legacyVideos.size,
                    legacyBytes = legacyVideos.sumOf { it.sizeBytes },
                    isLoading = uiState.isLoading,
                    onFindSpaceClick = { navController.navigate(Routes.VIDEO_LIBRARY) },
                    onReviewFourKClick = { navController.navigate(Routes.VIDEO_LIBRARY) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToPhotos = { navController.navigate(Routes.PHOTO_OPTIMIZATION) }
                )
            }

            composable(Routes.VIDEO_LIBRARY) {
                val uiState by sharedVideoViewModel.uiState.collectAsState()

                com.storage.intelligence.feature.video.ui.VideoLibraryScreen(
                    videos = uiState.videos,
                    onVideoClick = { video ->
                        sharedVideoViewModel.selectVideo(video)
                        navController.navigate(Routes.VIDEO_DETAIL)
                    },
                    onPlayClick = { video ->
                        sharedVideoViewModel.selectVideo(video)
                        navController.navigate(Routes.VIDEO_DETAIL)
                    },
                    onOptimizeClick = { video ->
                        sharedVideoViewModel.selectVideo(video)
                        sharedVideoViewModel.showCompressionSheet(true)
                        navController.navigate(Routes.VIDEO_DETAIL)
                    },
                    onBatchOptimizeClick = { selectedVideos ->
                        sharedVideoViewModel.startBatchOptimization(selectedVideos)
                        navController.navigate(Routes.VIDEO_ACTIVITY)
                    },
                    onNavigateToPhotoOptimization = { navController.navigate(Routes.PHOTO_OPTIMIZATION) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.VIDEO_ACTIVITY) {
                val uiState by sharedVideoViewModel.uiState.collectAsState()

                com.storage.intelligence.feature.video.ui.VideoActivityScreen(
                    jobs = uiState.activeJobs,
                    onPauseJob = { id -> sharedVideoViewModel.pauseJob(id) },
                    onResumeJob = { id -> sharedVideoViewModel.resumeJob(id) },
                    onCancelJob = { id -> sharedVideoViewModel.cancelJob(id) },
                    onRetryJob = { id -> sharedVideoViewModel.retryJob(id) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.VIDEO_DETAIL) {
                val uiState by sharedVideoViewModel.uiState.collectAsState()
                val video = uiState.selectedVideo
                val plan = uiState.selectedVideoPlan

                if (video != null && plan != null) {
                    com.storage.intelligence.feature.video.ui.VideoDetailScreen(
                        video = video,
                        onNavigateBack = { navController.popBackStack() },
                        onStartOptimization = { sharedVideoViewModel.showCompressionSheet(true) },
                        showDebugHud = uiState.isPerformanceHudEnabled
                    )

                    if (uiState.isCompressionSheetVisible) {
                        com.storage.intelligence.feature.video.ui.VideoCompressionSheet(
                            video = video,
                            currentPlan = plan,
                            onModeSelected = { mode -> sharedVideoViewModel.updateOptimizationMode(mode) },
                            onStartOptimization = { optPlan ->
                                sharedVideoViewModel.startOptimization(optPlan)
                                navController.navigate(Routes.VIDEO_ACTIVITY)
                            },
                            onDismiss = { sharedVideoViewModel.showCompressionSheet(false) }
                        )
                    }
                } else {
                    LaunchedEffect(Unit) {
                        if (uiState.videos.isNotEmpty()) {
                            sharedVideoViewModel.selectVideo(uiState.videos.first())
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            }

            composable(Routes.PERFORMANCE_LAB) {
                val uiState by sharedVideoViewModel.uiState.collectAsState()
                val profile = uiState.deviceProfile ?: com.storage.intelligence.feature.video.codec.CodecCapabilityManager.detectCapabilities()

                com.storage.intelligence.feature.video.ui.lab.PerformanceLabScreen(
                    deviceProfile = profile,
                    onRunBenchmark = { testCase ->
                        sharedVideoViewModel.runBenchmark(testCase)
                        navController.navigate(Routes.BENCHMARK_RESULT)
                    },
                    onRunMemoryLeakTest = {
                        sharedVideoViewModel.runBenchmark("memory_leak_20x")
                        navController.navigate(Routes.BENCHMARK_RESULT)
                    },
                    onViewCodecComparison = { navController.navigate(Routes.CODEC_COMPARISON) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.BENCHMARK_RESULT) {
                val uiState by sharedVideoViewModel.uiState.collectAsState()
                val result = uiState.lastBenchmarkResult ?: com.storage.intelligence.feature.video.benchmark.CodecBenchmarkMetrics(
                    codecName = "AV1",
                    timeToFirstFrameMs = 420,
                    avgFps = 59.8f,
                    droppedFrames = 2,
                    avgCpuPercent = 12,
                    peakCpuPercent = 18,
                    startRamMb = 180,
                    peakRamMb = 241,
                    avgSeekLatencyMs = 380,
                    seekTestResults = emptyList(),
                    outputSizeBytes = 380_000_000L,
                    originalSizeBytes = 1_000_000_000L,
                    isHardwareAccelerated = true
                )

                com.storage.intelligence.feature.video.ui.lab.BenchmarkResultScreen(
                    result = result,
                    onNavigateBack = { navController.popBackStack() }
                )
            }


            composable(Routes.CODEC_COMPARISON) {
                val uiState by sharedVideoViewModel.uiState.collectAsState()

                com.storage.intelligence.feature.video.ui.lab.CodecComparisonScreen(
                    av1Result = uiState.lastBenchmarkResult,
                    hevcResult = null,
                    h264Result = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
                    onNavigateToPerformanceLab = { navController.navigate(Routes.PERFORMANCE_LAB) }
                )
            }


            composable(Routes.DUPLICATES) {
                DuplicatesScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMessaging = { navController.navigate(Routes.MESSAGING) }
                )
            }

            composable(Routes.SIMILAR) {
                SimilarPhotosScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.LARGE_FILES) {
                LargeFilesScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.DOWNLOADS) {
                DownloadsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SCREENSHOTS) {
                ScreenshotsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.MESSAGING) {
                MessagingMediaScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.APPS) {
                AppsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PAYWALL) {
                PaywallScreen(
                    viewModel = hiltViewModel(),
                    onClose = { navController.popBackStack() }
                )
            }

            composable(Routes.OTHER_STORAGE) {
                com.storage.intelligence.feature.other.OtherStorageScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.STALE_OPTIMIZE) {
                com.storage.intelligence.feature.optimization.StaleOptimizationScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToArchive = { navController.navigate(Routes.OPTIMIZED_ARCHIVE) }
                )
            }

            composable(Routes.OPTIMIZED_ARCHIVE) {
                com.storage.intelligence.feature.optimization.OptimizedArchiveScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ME) {
                com.storage.intelligence.feature.me.MeScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToActivity = { navController.navigate(Routes.VIDEO_ACTIVITY) },
                    onNavigateToPerformanceLab = { navController.navigate(Routes.PERFORMANCE_LAB) },
                    onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
        }
    }
}
