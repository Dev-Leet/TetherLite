package com.example.tetherlite.core

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.provider.Settings
import android.util.Log
import java.lang.reflect.Method

object TetherManager {
    private const val TAG = "TetherManager"

    fun toggleUsbTethering(context: Context) {
        if (hasWriteSecureSettings(context)) {
            setUsbTethering(context, true) // Attempt to enable, logic might need state check to toggle properly
        } else {
            openTetherSettings(context)
        }
    }

    private fun hasWriteSecureSettings(context: Context): Boolean {
        return context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun setUsbTethering(context: Context, enable: Boolean) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            // Check current state (optional, simplified for now to just try enabling or toggle if we could read state)
            // For this implementation, we will use reflection to call setUsbTethering
            // Note: This method signature changes across Android versions.
            
            val method: Method = connectivityManager.javaClass.getDeclaredMethod("setUsbTethering", Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            val result = method.invoke(connectivityManager, enable) as Int
            Log.d(TAG, "setUsbTethering result: $result")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle USB tethering via reflection", e)
            openTetherSettings(context) // Fallback on failure
        }
    }

    private fun openTetherSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings", e)
        }
    }
}
