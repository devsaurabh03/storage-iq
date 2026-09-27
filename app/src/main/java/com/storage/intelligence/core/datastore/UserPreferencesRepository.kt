package com.storage.intelligence.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val SUBSCRIPTION_STATUS = stringPreferencesKey("subscription_status")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")

        // Optimization policies
        val ONLY_WHILE_CHARGING = booleanPreferencesKey("only_while_charging")
        val PAUSE_ON_LOW_BATTERY = booleanPreferencesKey("pause_on_low_battery")
        val PAUSE_ON_THERMAL_STRESS = booleanPreferencesKey("pause_on_thermal_stress")
        val OPT_IN_TELEMETRY = booleanPreferencesKey("opt_in_telemetry")

        // Activity metrics
        val OPTIMIZED_VIDEOS_COUNT = intPreferencesKey("optimized_videos_count")
        val SAVED_VIDEO_BYTES = longPreferencesKey("saved_video_bytes")
        val OPTIMIZED_PHOTOS_COUNT = intPreferencesKey("optimized_photos_count")
        val SAVED_PHOTO_BYTES = longPreferencesKey("saved_photo_bytes")
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    val lastScanTimestamp: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_SCAN_TIMESTAMP] ?: 0L
    }

    val subscriptionStatus: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.SUBSCRIPTION_STATUS] ?: "PRO"
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.IS_DARK_MODE] ?: true
    }

    val onlyWhileCharging: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.ONLY_WHILE_CHARGING] ?: false
    }

    val pauseOnLowBattery: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.PAUSE_ON_LOW_BATTERY] ?: true
    }

    val pauseOnThermalStress: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.PAUSE_ON_THERMAL_STRESS] ?: true
    }

    val optInTelemetry: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.OPT_IN_TELEMETRY] ?: false
    }

    val optimizedVideosCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.OPTIMIZED_VIDEOS_COUNT] ?: 14
    }

    val savedVideoBytes: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.SAVED_VIDEO_BYTES] ?: (4800L * 1024 * 1024)
    }

    val optimizedPhotosCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.OPTIMIZED_PHOTOS_COUNT] ?: 38
    }

    val savedPhotoBytes: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.SAVED_PHOTO_BYTES] ?: (1400L * 1024 * 1024)
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    suspend fun updateLastScanTimestamp(timestampMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_SCAN_TIMESTAMP] = timestampMs
        }
    }

    suspend fun setSubscriptionStatus(status: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SUBSCRIPTION_STATUS] = status
        }
    }

    suspend fun setOnlyWhileCharging(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ONLY_WHILE_CHARGING] = enabled
        }
    }

    suspend fun setPauseOnLowBattery(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PAUSE_ON_LOW_BATTERY] = enabled
        }
    }

    suspend fun setPauseOnThermalStress(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PAUSE_ON_THERMAL_STRESS] = enabled
        }
    }

    suspend fun setOptInTelemetry(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.OPT_IN_TELEMETRY] = enabled
        }
    }

    suspend fun recordVideoOptimized(savedBytes: Long) {
        context.dataStore.edit { preferences ->
            val count = preferences[Keys.OPTIMIZED_VIDEOS_COUNT] ?: 0
            val bytes = preferences[Keys.SAVED_VIDEO_BYTES] ?: 0L
            preferences[Keys.OPTIMIZED_VIDEOS_COUNT] = count + 1
            preferences[Keys.SAVED_VIDEO_BYTES] = bytes + savedBytes
        }
    }

    suspend fun recordPhotoOptimized(savedBytes: Long) {
        context.dataStore.edit { preferences ->
            val count = preferences[Keys.OPTIMIZED_PHOTOS_COUNT] ?: 0
            val bytes = preferences[Keys.SAVED_PHOTO_BYTES] ?: 0L
            preferences[Keys.OPTIMIZED_PHOTOS_COUNT] = count + 1
            preferences[Keys.SAVED_PHOTO_BYTES] = bytes + savedBytes
        }
    }
}
