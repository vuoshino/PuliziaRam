package it.tiziano.puliziaram;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Widget per la home: un tocco avvia la pulizia. */
public class RamWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        updateAll(context, null);
    }

    /** Aggiorna tutti i widget. Se status è null mostra la RAM disponibile. */
    public static void updateAll(Context ctx, String status) {
        Context c = ctx.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(c);
        int[] ids = manager.getAppWidgetIds(new ComponentName(c, RamWidget.class));
        if (ids == null || ids.length == 0) return;

        String text = status != null
                ? status
                : Cleaner.formatMb(Cleaner.readMemInfo().availableMb) + " MB disponibili";

        RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.widget_ram);
        views.setTextViewText(R.id.widget_status, text);

        Intent intent = new Intent(c, CleanReceiver.class).setAction(CleanReceiver.ACTION_CLEAN);
        PendingIntent pending = PendingIntent.getBroadcast(
                c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);

        manager.updateAppWidget(ids, views);
    }
}
