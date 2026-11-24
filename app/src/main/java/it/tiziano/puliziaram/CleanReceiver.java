package it.tiziano.puliziaram;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Riceve il tocco sul widget ed esegue la pulizia in background. */
public class CleanReceiver extends BroadcastReceiver {

    public static final String ACTION_CLEAN = "it.tiziano.puliziaram.CLEAN";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_CLEAN.equals(intent.getAction())) return;

        final Context app = context.getApplicationContext();
        final PendingResult pending = goAsync();
        RamWidget.updateAll(app, app.getString(R.string.cleaning));

        new Thread(() -> {
            try {
                Cleaner.Result r = Cleaner.clean(app);
                String status = r.ok
                        ? "+" + Cleaner.formatMb(r.freedMb) + " MB · " + Cleaner.formatMb(r.availableMb) + " liberi"
                        : "Root non disponibile";
                RamWidget.updateAll(app, status);
                Cleaner.toast(app, Cleaner.resultMessage(app, r));
            } finally {
                pending.finish();
            }
        }).start();
    }
}
