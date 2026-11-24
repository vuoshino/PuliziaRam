package it.tiziano.puliziaram;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Pulsante nella tendina delle impostazioni rapide. */
public class CleanTileService extends TileService {

    private volatile boolean running;

    @Override
    public void onStartListening() {
        super.onStartListening();
        refresh(null);
    }

    private void refresh(String subtitle) {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel(getString(R.string.tile_label));
        tile.setState(running ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setSubtitle(subtitle != null
                ? subtitle
                : Cleaner.formatMb(Cleaner.readMemInfo().availableMb) + " MB liberi");
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (running) return;
        running = true;
        refresh(getString(R.string.cleaning));

        final Context app = getApplicationContext();
        new Thread(() -> {
            Cleaner.Result r = Cleaner.clean(app);
            running = false;
            final String subtitle = r.ok
                    ? "+" + Cleaner.formatMb(r.freedMb) + " MB"
                    : "Root assente";
            new Handler(Looper.getMainLooper()).post(() -> refresh(subtitle));
            Cleaner.toast(app, Cleaner.resultMessage(app, r));
            RamWidget.updateAll(app, null);
        }).start();
    }
}
