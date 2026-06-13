package com.rushi.coinmaster.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.rushi.coinmaster.R
import com.rushi.coinmaster.ui.transactions.QuickRecordActivity
import com.rushi.coinmaster.data.preferences.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class QuickExpenseWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val pendingResult = goAsync()
        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        coroutineScope.launch {
            try {
                // Fetch language code
                val languageCode = appPreferences.appLanguage.first()

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_quick_expense)

                    // Translate button text dynamically
                    val btnText = when (languageCode) {
                        "hi" -> "+ खर्च दर्ज करें"
                        "mr" -> "+ खर्च रेकॉर्ड करा"
                        else -> "+ Record Expense"
                    }
                    views.setTextViewText(R.id.tv_widget_btn_text, btnText)

                    // Create intent to launch QuickRecordActivity
                    val intent = Intent(context, QuickRecordActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_record_expense, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
