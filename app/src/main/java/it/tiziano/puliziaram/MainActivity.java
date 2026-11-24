package it.tiziano.puliziaram;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView ramValue;
    private TextView ramTotal;
    private ProgressBar ramBar;
    private TextView swapValue;
    private TextView lastValue;
    private TextView whenValue;
    private View cleanButton;
    private TextView cleanLabel;
    private TextView statusText;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refreshStats();
            handler.postDelayed(this, 2000);
        }
    };
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View content = findViewById(R.id.content);
        if (getResources().getConfiguration().screenWidthDp < 600) {
            ViewGroup.LayoutParams lp = content.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            content.setLayoutParams(lp);
        }

        ramValue = findViewById(R.id.ram_value);
        ramTotal = findViewById(R.id.ram_total);
        ramBar = findViewById(R.id.ram_bar);
        swapValue = findViewById(R.id.swap_value);
        lastValue = findViewById(R.id.last_value);
        whenValue = findViewById(R.id.when_value);
        cleanButton = findViewById(R.id.clean_button);
        cleanLabel = findViewById(R.id.clean_label);
        statusText = findViewById(R.id.status_text);

        SharedPreferences prefs = Cleaner.prefs(this);
        bindSwitch(R.id.opt_kill, Cleaner.KEY_KILL, true, prefs);
        bindSwitch(R.id.opt_compact, Cleaner.KEY_COMPACT, true, prefs);
        bindSwitch(R.id.opt_drop, Cleaner.KEY_DROP, false, prefs);

        cleanButton.setOnClickListener(v -> startClean());
    }

    private void bindSwitch(int id, String key, boolean def, SharedPreferences prefs) {
        Switch sw = findViewById(id);
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean(key, checked).apply());
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void refreshStats() {
        Cleaner.MemInfo m = Cleaner.readMemInfo();
        if (m.totalMb > 0) {
            ramValue.setText(Cleaner.formatMb(m.availableMb));
            ramTotal.setText("MB su " + Cleaner.formatMb(m.totalMb));
            ramBar.setProgress(m.availablePercent());
        }
        swapValue.setText(m.swapTotalMb > 0 ? Cleaner.formatMb(m.swapUsedMb()) + " MB" : "—");

        SharedPreferences prefs = Cleaner.prefs(this);
        long lastTime = prefs.getLong(Cleaner.KEY_LAST_TIME, 0);
        long lastFreed = prefs.getLong(Cleaner.KEY_LAST_FREED, 0);
        lastValue.setText(lastTime > 0 ? "+" + Cleaner.formatMb(lastFreed) + " MB" : "—");
        whenValue.setText(Cleaner.formatWhen(lastTime));
    }

    private void startClean() {
        if (busy) return;
        busy = true;
        cleanButton.setEnabled(false);
        cleanLabel.setText(R.string.cleaning);
        statusText.setVisibility(View.GONE);

        final Context app = getApplicationContext();
        new Thread(() -> {
            Cleaner.Result r = Cleaner.clean(app);
            runOnUiThread(() -> {
                busy = false;
                cleanButton.setEnabled(true);
                cleanLabel.setText(R.string.clean_button);
                statusText.setText(Cleaner.resultMessage(app, r));
                statusText.setTextColor(getColor(r.ok ? R.color.success : R.color.danger));
                statusText.setVisibility(View.VISIBLE);
                refreshStats();
                RamWidget.updateAll(app, null);
            });
        }).start();
    }
}
