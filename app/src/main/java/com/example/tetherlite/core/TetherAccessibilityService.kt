package com.example.tetherlite.core

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TetherAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isToggling = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "TetherLite Service Connected", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isToggling) return
        
        event?.source?.let { rootNode ->
            findAndClickTetherSwitch(rootNode)
        }
    }

    override fun onInterrupt() {
        // Required method
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_TOGGLE) {
            isToggling = true
            serviceScope.launch {
                // Timeout to stop looking for the switch
                delay(5000) 
                isToggling = false
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun findAndClickTetherSwitch(rootNode: AccessibilityNodeInfo) {
        // Search for "USB tethering" text
        val nodes = rootNode.findAccessibilityNodeInfosByText("USB tethering")
        
        for (node in nodes) {
            // The node itself might be the switch, or its parent
            if (tryClickNode(node)) return
            
            // Check parent (often the list item is clickable)
            if (node.parent != null && tryClickNode(node.parent)) return
            
            // Check sibling (often the switch is a separate view in the same ViewGroup)
            val parent = node.parent
            if (parent != null) {
                for (i in 0 until parent.childCount) {
                    val sibling = parent.getChild(i)
                    if (sibling != null && tryClickNode(sibling)) return
                }
            }
        }
    }
    
    private fun tryClickNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isCheckable) {
            // Found a switch!
            if (!node.isChecked) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                isToggling = false // Done
                Toast.makeText(this, "USB Tethering Toggled", Toast.LENGTH_SHORT).show()
                performGlobalAction(GLOBAL_ACTION_BACK) // Go back to app
                return true
            } else {
                // Already on
                isToggling = false
                performGlobalAction(GLOBAL_ACTION_BACK)
                return true
            }
        }
        // Sometimes the row is clickable but not checkable, triggering the switch
        // But "USB tethering" text is usually on the label. 
        // We look specifically for the switch widget (Switch/CheckBox class) or checkable property.
        
        return false
    }

    companion object {
        const val ACTION_START_TOGGLE = "com.example.tetherlite.START_TOGGLE"
    }
}
