#!/usr/bin/env bash
# Assina e builda a release do Simple Browser usando a keystore local.
# Uso:
#   ./sign-release.sh            # roda assembleGenericRelease
#   ./sign-release.sh --dry-run  # só valida o ambiente (sem build)
#   ./sign-release.sh <task>     # roda outra task (ex.: assembleGenericDebug)
set -euo pipefail

KEYSTORE_DIR="${KEYSTORE_DIR:-$HOME/simple-browser-keystores}"
SECRETS="$KEYSTORE_DIR/secrets.txt"

if [ ! -f "$SECRETS" ]; then
  echo "ERRO: $SECRETS não encontrado." >&2
  exit 1
fi

# Carrega senhas/alias sem vazar para o log
source "$SECRETS"

export KEYSTORE_PATH="${KEYSTORE_PATH:-$KEYSTORE_DIR/ge.jks}"
export KEYSTORE_PASSWORD="$STORE_PASS"
export KEY_ALIAS="${KEY_ALIAS:-$ALIAS}"
export KEY_PASSWORD="$STORE_PASS"

if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "ERRO: keystore não encontrada em $KEYSTORE_PATH" >&2
  exit 1
fi

if [ "${1:-}" = "--dry-run" ]; then
  echo "Ambiente OK:"
  echo "  KEYSTORE_PATH=$KEYSTORE_PATH"
  echo "  KEY_ALIAS=$KEY_ALIAS"
  echo "  KEYSTORE_PASSWORD=*** (set)"
  echo "  KEY_PASSWORD=*** (set)"
  exit 0
fi

TASK="${1:-assembleGenericRelease}"
echo "Rodando ./gradlew $TASK (assinado)..."
exec ./gradlew "$TASK"
