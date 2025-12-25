package com.example.tetherlite.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UsbTetherReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "UsbTetherReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive: action=$action")

        if (action == Intent.ACTION_POWER_CONNECTED || action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val autoUsb = prefs.getBoolean("auto_usb_tether", false)
            Log.d(TAG, "Auto USB preference: $autoUsb")

            if (autoUsb) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        var success = false
                        // Check multiple times up to 10 seconds
                        for (i in 1..10) { // Increased to 10 attempts
                            if (checkAndToggle(context)) {
                                success = true
                                break
                            }
                            Log.d(TAG, "Retry attempt $i in 1 second...")
                            delay(1000) // 1 second intervals
                        }
                        
                        if (!success) {
                            Log.d(TAG, "Could not detect USB connection suitable for tethering after retries.")
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun checkAndToggle(context: Context): Boolean {
        // 1. Check if we are plugged into USB via Battery Manager (Primary check)
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        
        Log.d(TAG, "Plugged state: $chargePlug (USB=$usbCharge)")

        if (usbCharge) {
            if (!TetherManager.isUsbTetheringActive(context)) {
                Log.d(TAG, "USB Connected, toggling tethering...")
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        Toast.makeText(context, "USB Detected: Enabling Tethering...", Toast.LENGTH_SHORT).show()
                        TetherManager.toggleUsbTethering(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error toggling tethering", e)
                    }
                }
                return true
            } else {
                return true 
            }
        }
        
        return false
    }
}
