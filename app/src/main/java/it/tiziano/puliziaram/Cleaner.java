package it.tiziano.puliziaram;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Logica di pulizia condivisa da app, widget e riquadro delle impostazioni rapide. */
public final class Cleaner {

    public static final String PREFS = "pulizia";
    public static final String KEY_KILL = "opt_kill";
    public static final String KEY_COMPACT = "opt_compact";
    public static final String KEY_DROP = "opt_drop";
    public static final String KEY_LAST_FREED = "last_freed";
    public static final String KEY_LAST_TIME = "last_time";

    private static final Object LOCK = new Object();

    private Cleaner() {
    }

    /** Valori di /proc/meminfo in MB. */
    public static final class MemInfo {
        public long totalMb;
        public long availableMb;
        public long swapTotalMb;
        public long swapFreeMb;

        public long swapUsedMb() {
            return Math.max(0, swapTotalMb - swapFreeMb);
        }

        public int availablePercent() {
            if (totalMb <= 0) return 0;
            return (int) Math.min(100, Math.round(availableMb * 100.0 / totalMb));
        }
    }

    /** Esito di una pulizia. */
    public static final class Result {
        public final boolean ok;
        public final long freedMb;
        public final long availableMb;

        Result(boolean ok, long freedMb, long availableMb) {
            this.ok = ok;
            this.freedMb = freedMb;
            this.availableMb = availableMb;
        }
    }

    public static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static MemInfo readMemInfo() {
        MemInfo m = new MemInfo();
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 2) continue;
                long kb;
                try {
                    kb = Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    continue;
                }
                long mb = kb / 1024;
                switch (parts[0]) {
                    case "MemTotal:":
                        m.totalMb = mb;
                        break;
                    case "MemAvailable:":
                        m.availableMb = mb;
                        break;
                    case "SwapTotal:":
                        m.swapTotalMb = mb;
                        break;
                    case "SwapFree:":
                        m.swapFreeMb = mb;
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException ignored) {
            // valori a zero: l'interfaccia mostrerà il trattino
        }
        return m;
    }

    /** Esegue la pulizia con il root. Va chiamato fuori dal thread principale. */
    public static Result clean(Context ctx) {
        synchronized (LOCK) {
            SharedPreferences p = prefs(ctx);
            boolean kill = p.getBoolean(KEY_KILL, true);
            boolean compact = p.getBoolean(KEY_COMPACT, true);
            boolean drop = p.getBoolean(KEY_DROP, false);

            long before = readMemInfo().availableMb;

            StringBuilder s = new StringBuilder();
            s.append("[ \"$(id -u)\" = \"0\" ] || exit 99\n");
            if (kill) s.append("am kill-all >/dev/null 2>&1\n");
            s.append("sync\n");
            if (drop) s.append("echo 3 > /proc/sys/vm/drop_caches 2>/dev/null\n");
            if (compact) s.append("echo 1 > /proc/sys/vm/compact_memory 2>/dev/null\n");
            s.append("exit 0\n");

            int code = runAsRoot(s.toString());
            if (code != 0) {
                return new Result(false, 0, before);
            }

            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            long after = readMemInfo().availableMb;
            long freed = Math.max(0, after - before);
            p.edit()
                    .putLong(KEY_LAST_FREED, freed)
                    .putLong(KEY_LAST_TIME, System.currentTimeMillis())
                    .apply();
            return new Result(true, freed, after);
        }
    }

    private static int runAsRoot(String script) {
        Process proc = null;
        try {
            proc = new ProcessBuilder("su", "-c", script).redirectErrorStream(true).start();
            try (InputStream in = proc.getInputStream()) {
                byte[] buf = new byte[1024];
                while (in.read(buf) != -1) {
                    // svuota l'output per non bloccare il processo
                }
            }
            if (!proc.waitFor(20, TimeUnit.SECONDS)) {
                proc.destroy();
                return -1;
            }
            return proc.exitValue();
        } catch (IOException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        } finally {
            if (proc != null) proc.destroy();
        }
    }

    public static String formatMb(long mb) {
        return String.format(Locale.ITALY, "%,d", mb);
    }

    public static String formatWhen(long time) {
        if (time <= 0) return "—";
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(time);
        boolean today = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR);
        String pattern = today ? "HH:mm" : "dd/MM HH:mm";
        return new SimpleDateFormat(pattern, Locale.ITALY).format(new Date(time));
    }

    public static String resultMessage(Context ctx, Result r) {
        if (!r.ok) return ctx.getString(R.string.no_root);
        if (r.freedMb <= 10) {
            return "Memoria già in ordine · " + formatMb(r.availableMb) + " MB disponibili";
        }
        return "Liberati " + formatMb(r.freedMb) + " MB · " + formatMb(r.availableMb) + " MB disponibili";
    }

    public static void toast(Context ctx, String msg) {
        final Context app = ctx.getApplicationContext();
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(app, msg, Toast.LENGTH_LONG).show());
    }
}
