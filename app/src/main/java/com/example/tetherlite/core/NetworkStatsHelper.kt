package com.example.tetherlite.core

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.RemoteException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper to retrieve network statistics.
 * Refactored to accept dispatcher for better testability.
 */
class NetworkStatsHelper(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

    suspend fun getUsbTetheringUsage(): Pair<Long, Long> = withContext(dispatcher) {
        var rxBytes = 0L
        var txBytes = 0L

        try {
            // Identify cellular usage which is the most common target for tethering monitoring requests 
            // in a simple utility app, though strictly 'tethering' traffic might be counted differently 
            // depending on carrier config.
            val bucket = NetworkStats.Bucket()
            val queryUsage = networkStatsManager.querySummaryForDevice(
                NetworkCapabilities.TRANSPORT_CELLULAR,
                null, // subscriberId
                0, // start time
                System.currentTimeMillis()
            )
            
            rxBytes = queryUsage.rxBytes
            txBytes = queryUsage.txBytes
            
        } catch (e: RemoteException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
             e.printStackTrace()
        }

        return@withContext Pair(rxBytes, txBytes) 
    }
}
