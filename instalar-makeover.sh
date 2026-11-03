#!/usr/bin/env bash
# Instala o launcher android-tv-makeover na TV Aiwa. Rode dentro da pasta do repositório.
set -euo pipefail
cd "$(dirname "$0")"

export TV_IP="${TV_IP:-192.168.1.167}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"

die() { echo "ERRO: $*" >&2; exit 1; }

[ -x "$JAVA_HOME/bin/java" ] || die "JDK 17 não encontrado em $JAVA_HOME (sudo apt install openjdk-17-jdk)"
"$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "17' || die "O JAVA_HOME precisa ser JDK 17"
[ -d "$ANDROID_HOME/platforms/android-34" ] || die "Falta o SDK platform 34 em $ANDROID_HOME"
[ -d "$ANDROID_HOME/build-tools/34.0.0" ] || die "Falta o build-tools 34.0.0 em $ANDROID_HOME"

[ -d .venv ] || ./setup.sh
echo "$TV_IP" > .tv-ip

echo "Testando conexão com a TV em $TV_IP (aceite 'Permitir depuração USB?' na TV, se aparecer)..."
tools/tv shell "getprop ro.product.model" || die "Sem conexão com a TV. Ela está ligada e com a depuração de rede ativa?"

echo "Fazendo backup do estado atual da TV..."
./backup.sh

echo "Home antes da instalação:"
tools/tv shell "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"

./build_and_install.sh

sleep 3
echo "Home depois da instalação:"
tools/tv shell "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
echo "Pronto. Aperte Home no controle da TV."
