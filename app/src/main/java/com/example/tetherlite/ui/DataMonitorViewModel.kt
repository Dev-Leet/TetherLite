package com.example.tetherlite.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tetherlite.core.NetworkStatsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DataUsage(
    val uploadBytes: Long = 0,
    val downloadBytes: Long = 0
)

class DataMonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val helpers = NetworkStatsHelper(application)
    
    private val _dataUsage = MutableStateFlow(DataUsage())
    val dataUsage: StateFlow<DataUsage> = _dataUsage.asStateFlow()

    private var initialRx = 0L
    private var initialTx = 0L
    private var isInitialized = false

    fun startMonitoring() {
        viewModelScope.launch {
            while (isActive) {
                val usage = helpers.getUsbTetheringUsage()
                
                if (!isInitialized) {
                    initialRx = usage.first
                    initialTx = usage.second
                    isInitialized = true
                }
                
                val currentRx = usage.first
                val currentTx = usage.second

                // Calculate session usage (deltas)
                val sessionRx = if (currentRx >= initialRx) currentRx - initialRx else 0L
                val sessionTx = if (currentTx >= initialTx) currentTx - initialTx else 0L

                _dataUsage.emit(DataUsage(sessionTx, sessionRx))
                
                delay(1000)
            }
        }
    }
}
