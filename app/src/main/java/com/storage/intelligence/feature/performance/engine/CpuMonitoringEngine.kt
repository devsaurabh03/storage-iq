package com.storage.intelligence.feature.performance.engine

import com.storage.intelligence.feature.performance.model.ThermalState
import com.storage.intelligence.feature.performance.provider.CpuInfoProvider
import com.storage.intelligence.feature.performance.provider.ThermalInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

enum class LoadTrend(val symbol: String, val label: String) {
    INCREASING("↗", "Increasing"),
    STABLE("→", "Stable"),
    DECREASING("↘", "Decreasing")
}

data class CpuSnapshot(
    val utilization: Float,
    val timestamp: Long,
    val thermalState: ThermalState,
    val frequency: List<Long>?,
    val loadTrend: LoadTrend,
    val history: List<Float>,
    val mainWorkload: String? = null
)

@Singleton
class CpuMonitoringEngine @Inject constructor(
    private val cpuInfoProvider: CpuInfoProvider,
    private val thermalInfoProvider: ThermalInfoProvider
) {
    private val historyBuffer = mutableListOf<Float>()

    fun startMonitoring(intervalMs: Long = 1200L): Flow<CpuSnapshot> = flow {
        while (true) {
            val cpuActivity = try {
                cpuInfoProvider.getCpuActivity()
            } catch (_: Exception) {
                null
            }
            val thermal = try {
                thermalInfoProvider.getThermalInfo(cpuActivity?.currentLoadPercent ?: 25)
            } catch (_: Exception) {
                null
            }

            val currentLoad = (cpuActivity?.currentLoadPercent ?: 20).toFloat()
            synchronized(historyBuffer) {
                historyBuffer.add(currentLoad)
                if (historyBuffer.size > 20) {
                    historyBuffer.removeAt(0)
                }
            }

            val trend = if (historyBuffer.size >= 2) {
                val last = historyBuffer.last()
                val prev = historyBuffer[historyBuffer.size - 2]
                when {
                    last - prev > 4f -> LoadTrend.INCREASING
                    prev - last > 4f -> LoadTrend.DECREASING
                    else -> LoadTrend.STABLE
                }
            } else {
                LoadTrend.STABLE
            }

            val mainWorkload = when {
                currentLoad > 65f -> "Active App / Foreground Services"
                currentLoad > 35f -> "System Services & Media Framework"
                else -> null
            }

            val snapshot = CpuSnapshot(
                utilization = currentLoad,
                timestamp = System.currentTimeMillis(),
                thermalState = thermal?.state ?: ThermalState.NORMAL,
                frequency = null,
                loadTrend = trend,
                history = synchronized(historyBuffer) { historyBuffer.toList() },
                mainWorkload = mainWorkload
            )

            emit(snapshot)
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)
}
