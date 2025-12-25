package com.example.tetherlite.core

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import java.lang.reflect.Method
import java.net.NetworkInterface

object TetherManager {
    private const val TAG = "TetherManager"

    fun isUsbTetheringActive(context: Context): Boolean {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                // Common USB tethering interface names: rndis0, usb0, ncm0
                if (intf.isUp && (intf.name.startsWith("rndis") || 
                                  intf.name.startsWith("usb") || 
                                  intf.name.startsWith("ncm"))) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking tethering status", e)
        }
        return false
    }

    fun toggleUsbTethering(context: Context) {
        // 1. Try Root (Best if available)
        if (executeRootCommand("cmd connectivity start-tethering usb")) {
             Toast.makeText(context, "USB Tethering Enabled (Root)", Toast.LENGTH_SHORT).show()
             return
        }

        // 2. Try Reflection (Works on old Android, or some specific ROMs)
        if (hasWriteSecureSettings(context)) {
            if (tryReflectionToggle(context)) return
        }

        // 3. Fallback: Accessibility Service
        if (isAccessibilityServiceEnabled(context, TetherAccessibilityService::class.java)) {
            // Service is enabled, automate the UI
            Toast.makeText(context, "Automating via Accessibility...", Toast.LENGTH_SHORT).show()
            
            val serviceIntent = Intent(context, TetherAccessibilityService::class.java)
            serviceIntent.action = TetherAccessibilityService.ACTION_START_TOGGLE
            context.startService(serviceIntent)
            
            openTetherSettings(context)
        } else {
            // Service not enabled
            Toast.makeText(context, "Please enable Accessibility Service for auto-toggle", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    private fun hasWriteSecureSettings(context: Context): Boolean {
        return context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun tryReflectionToggle(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Method 1: setUsbTethering
        try {
            val method = connectivityManager.javaClass.getDeclaredMethod("setUsbTethering", Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            val result = method.invoke(connectivityManager, true) as Int
            if (result == 0) return true
        } catch (e: Exception) {}

        // Method 2: startTethering
        try {
            val cmClass = Class.forName("android.net.ConnectivityManager")
            val method = findStartTetheringMethod(cmClass)
            if (method != null) {
                method.isAccessible = true
                val args = arrayOfNulls<Any>(method.parameterTypes.size)
                args[0] = 1 // USB
                args[1] = false 
                method.invoke(connectivityManager, *args)
                return true
            }
        } catch (e: Exception) {}
        
        return false
    }

    private fun findStartTetheringMethod(clazz: Class<*>): Method? {
        for (method in clazz.declaredMethods) {
            if (method.name == "startTethering") {
                val params = method.parameterTypes
                if (params.size >= 2 && 
                    params[0] == Int::class.javaPrimitiveType && 
                    params[1] == Boolean::class.javaPrimitiveType) {
                    return method
                }
            }
        }
        return null
    }

    private fun executeRootCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
        val expectedComponentName = ComponentName(context, serviceClass)
        val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName)
                return true
        }
        return false
    }

    private fun openTetherSettings(context: Context) {
        try {
            val intent = Intent()
            intent.action = "android.settings.TETHER_SETTINGS" 
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val wirelessIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                wirelessIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(wirelessIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings", e)
        }
    }
}
