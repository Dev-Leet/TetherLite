package com.example.tetherlite

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tetherlite.core.TetherManager
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (shouldShowSettings()) {
            setupSettingsUI()
        } else {
            // Check if Single-Tap Tethering is enabled
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val singleTapEnabled = prefs.getBoolean(KEY_SINGLE_TAP_TETHER, true) // Default true for original behavior

            if (singleTapEnabled) {
                markLaunchTime()
                TetherManager.toggleUsbTethering(this)
                finish()
            } else {
                setupSettingsUI()
            }
        }
    }
    
    companion object {
        private const val PREFS_NAME = "AppPrefs"
        private const val KEY_LAST_LAUNCH = "last_launch_time"
        private const val KEY_SINGLE_TAP_TETHER = "single_tap_tether"
        private const val KEY_AUTO_USB_TETHER = "auto_usb_tether"
        private const val DOUBLE_TAP_DELTA = 2000 // 2s window
    }

    private fun markLaunchTime() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_LAUNCH, System.currentTimeMillis())
            .apply()
    }

    private fun shouldShowSettings(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastLaunchTime = prefs.getLong(KEY_LAST_LAUNCH, 0)
        val currentTime = System.currentTimeMillis()
        
        val isDoubleTap = (currentTime - lastLaunchTime) < DOUBLE_TAP_DELTA
        return isDoubleTap
    }

    private fun setupSettingsUI() {
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    
    var isTetheringActive by remember { mutableStateOf(false) }
    var singleTapEnabled by remember { mutableStateOf(prefs.getBoolean("single_tap_tether", true)) }
    var autoUsbEnabled by remember { mutableStateOf(prefs.getBoolean("auto_usb_tether", false)) }

    // Poll for tethering status
    LaunchedEffect(Unit) {
        while (true) {
            isTetheringActive = TetherManager.isUsbTetheringActive(context)
            delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() 
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "TetherLite", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isTetheringActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isTetheringActive) "USB Tethering is ON" else "USB Tethering is OFF",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle Button
        Button(
            onClick = { TetherManager.toggleUsbTethering(context) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(text = if (isTetheringActive) "Turn OFF Tethering" else "Turn ON Tethering", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Single Tap Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Single-Tap Toggle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Toggle tethering automatically when app is launched via single tap.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = singleTapEnabled,
                onCheckedChange = { 
                    singleTapEnabled = it
                    prefs.edit().putBoolean("single_tap_tether", it).apply()
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Auto USB Enable Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Plug-in Auto Enable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Automatically turn on USB tethering when plugged into a computer.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = autoUsbEnabled,
                onCheckedChange = { 
                    autoUsbEnabled = it
                    prefs.edit().putBoolean("auto_usb_tether", it).apply()
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Note: Double tap always opens this screen.", style = MaterialTheme.typography.labelSmall)
    }
}
