package com.mardous.booming.core.appwidgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.mardous.booming.core.appwidgets.config.WidgetConfigStore
import com.mardous.booming.core.appwidgets.widget.CardWidget
import com.mardous.booming.core.appwidgets.widget.CircleWidget
import com.mardous.booming.core.appwidgets.widget.CookieWidget
import com.mardous.booming.core.appwidgets.widget.GridWidget
import com.mardous.booming.core.appwidgets.widget.LibraryWidget

/** Refresh ony any change so Widgets dont go stale */
abstract class BoomingWidgetReceiver : GlanceAppWidgetReceiver() {

    final override val glanceAppWidget get() = widgetFor(this)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // The data itself is fetched inside the Glance session
        WidgetUpdater.notifyWidgetsChanged(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetConfigStore.clear(context, appWidgetIds)
    }
}

class CardWidgetReceiver : BoomingWidgetReceiver()

class CircleWidgetReceiver : BoomingWidgetReceiver()

class CookieWidgetReceiver : BoomingWidgetReceiver()

class GridWidgetReceiver : BoomingWidgetReceiver()

class LibraryWidgetReceiver : BoomingWidgetReceiver()

internal val widgetsByReceiver: Map<String, BoomingWidget> = mapOf(
    CardWidgetReceiver::class.java.name to CardWidget(),
    CircleWidgetReceiver::class.java.name to CircleWidget(),
    CookieWidgetReceiver::class.java.name to CookieWidget(),
    GridWidgetReceiver::class.java.name to GridWidget(),
    LibraryWidgetReceiver::class.java.name to LibraryWidget()
)

internal fun widgetFor(receiver: BoomingWidgetReceiver): GlanceAppWidget =
    widgetsByReceiver.getValue(receiver.javaClass.name)
