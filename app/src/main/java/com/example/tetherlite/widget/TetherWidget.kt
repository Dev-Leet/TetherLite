package com.example.tetherlite.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.tetherlite.R
import com.example.tetherlite.core.TetherManager

class TetherWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.example.tetherlite.widget.TOGGLE_TETHER"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            TetherManager.toggleUsbTethering(context)
            // Ideally we would check the new state and update the widget UI here or register a receiver for connectivity changes
            // For now, let's just refresh the widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, TetherWidget::class.java))
            onUpdate(context, appWidgetManager, ids)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val intent = Intent(context, TetherWidget::class.java).apply {
            action = ACTION_TOGGLE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val views = RemoteViews(context.packageName, R.layout.widget_tether)
        views.setOnClickPendingIntent(R.id.widget_icon, pendingIntent)
        
        // In a real app we'd change the icon color based on state here.
        // views.setImageViewResource(R.id.widget_icon, if (isActive) ... else ...)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
