package com.example.tetherlite.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.tetherlite.R
import com.example.tetherlite.TetherPromptActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UsbTetherReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "UsbTetherReceiver"
        private const val CHANNEL_ID = "tether_prompt_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive: action=$action")

        if (action == Intent.ACTION_POWER_CONNECTED || action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val autoUsb = prefs.getBoolean("auto_usb_tether", false)

            if (autoUsb) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        var detected = false
                        for (i in 1..10) {
                            if (checkForUsbConnection(context)) {
                                detected = true
                                break
                            }
                            delay(1000)
                        }
                        
                        if (detected) {
                             CoroutineScope(Dispatchers.Main).launch {
                                 // Try launching activity first
                                 try {
                                     launchPrompt(context)
                                 } catch (e: Exception) {
                                     Log.e(TAG, "Failed to launch activity from background", e)
                                     // Fallback to Notification
                                     showNotificationPrompt(context)
                                 }
                             }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } else if (action == "com.example.tetherlite.ACTION_ENABLE_TETHER") {
            // Action from Notification
            TetherManager.toggleUsbTethering(context)
            // Cancel notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    private fun checkForUsbConnection(context: Context): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        
        if (usbCharge) {
            if (!TetherManager.isUsbTetheringActive(context)) {
                return true
            }
        }
        return false
    }

    private fun launchPrompt(context: Context) {
        Log.d(TAG, "Launching Prompt Activity...")
        val intent = Intent(context, TetherPromptActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(intent)
    }

    private fun showNotificationPrompt(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tethering Prompts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val enableIntent = Intent(context, UsbTetherReceiver::class.java).apply {
            action = "com.example.tetherlite.ACTION_ENABLE_TETHER"
        }
        val enablePendingIntent = PendingIntent.getBroadcast(
            context, 0, enableIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Ensure you have a valid icon
            .setContentTitle("USB Connected")
            .setContentText("Tap to enable USB Tethering")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Turn On", enablePendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
