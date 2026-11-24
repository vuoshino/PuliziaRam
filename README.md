# Pulizia RAM

App per Xiaomi Pad 5 con root (BakaSU / KernelSU) che libera la memoria con un tocco.

- Schermata con RAM disponibile, swap e risultato dell'ultima pulizia
- Widget per la home
- Pulsante nelle impostazioni rapide (tendina)

## Installazione

1. Scarica `PuliziaRAM.apk` dall'ultima release.
2. Installalo sul tablet.
3. In BakaSU → Accesso root, attiva **Pulizia RAM**.

## Cosa fa la pulizia

- Chiude le app in cache con `am kill-all` (non tocca app in uso o che riproducono audio)
- Compatta la memoria
- Facoltativo: svuota la cache dei file (sconsigliato)

L'APK viene compilato automaticamente da GitHub Actions a ogni modifica.
