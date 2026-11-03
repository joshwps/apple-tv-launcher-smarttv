#!/usr/bin/env bash
# Remove totalmente o android-tv-makeover da TV e devolve a Home ao FireLauncher.
set -euo pipefail
cd "$(dirname "$0")"

export TV_IP="${TV_IP:-192.168.1.167}"
HOME_ANTIGA="com.wolf.firelauncher/com.wolf.firelauncher.screens.launcher.LauncherActivity"

tools/tv shell "cmd package set-home-activity --user 0 $HOME_ANTIGA"
tools/tv shell "pm uninstall com.kulwant.atvhome"
tools/tv shell "rm -f /data/local/tmp/atvhome.apk"
tools/tv shell "input keyevent KEYCODE_HOME"

echo "Home agora:"
tools/tv shell "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
echo "Pacotes do launcher removido (deve estar vazio):"
tools/tv shell "pm list packages | grep kulwant" || true
echo "Removido."
