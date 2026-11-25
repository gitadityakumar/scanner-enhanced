#!/usr/bin/env bash
set -euo pipefail

# Generates a release Android keystore and uploads signing credentials to
# GitHub Actions Secrets via `gh` without printing secrets to stdout.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_PATH="${KEYSTORE_PATH:-$REPO_ROOT/release-keystore.jks}"
KEY_ALIAS="${KEY_ALIAS:-scanner-release}"
PASS_LENGTH="${PASS_LENGTH:-32}"
DNAME="${DNAME:-CN=Scanner Enhanced, OU=Mobile, O=Scanner Enhanced, L=Unknown, ST=Unknown, C=IN}"
WRITE_PROPERTIES=true
FORCE=false

usage() {
  cat <<'EOF'
Usage: scripts/setup-signing.sh [options]

Options:
  --keystore <path>   Path to keystore file (default: ./release-keystore.jks)
  --alias <name>      Key alias name (default: scanner-release)
  --length <n>        Password length for genpass (default: 32)
  --dname <dn>        X.509 Distinguished Name for new keystore
  --no-properties     Do not write local untracked keystore.properties
  --force             Overwrite existing keystore if present
  -h, --help          Show this help message
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --keystore) KEYSTORE_PATH="$2"; shift 2 ;;
    --alias) KEY_ALIAS="$2"; shift 2 ;;
    --length) PASS_LENGTH="$2"; shift 2 ;;
    --dname) DNAME="$2"; shift 2 ;;
    --no-properties) WRITE_PROPERTIES=false; shift ;;
    --force) FORCE=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 1 ;;
  esac
done

for cmd in keytool gh base64; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Error: required command '$cmd' not found in PATH." >&2
    exit 1
  fi
done

if ! gh auth status >/dev/null 2>&1; then
  echo "Error: gh CLI is not authenticated. Run 'gh auth login' first." >&2
  exit 1
fi

# Use user's genpass function if available (e.g. from ~/.bashrc), otherwise
# fall back to the exact same /dev/urandom generator for non-interactive shells.
if ! declare -F genpass >/dev/null 2>&1; then
  if [[ -f "$HOME/.bashrc" ]]; then
    # shellcheck disable=SC1091
    source "$HOME/.bashrc" >/dev/null 2>&1 || true
  fi
fi

if ! declare -F genpass >/dev/null 2>&1; then
  genpass() {
    LC_ALL=C tr -dc 'A-Za-z0-9!@#$%^&*()-_=+[]{};:,.<>/?' < /dev/urandom | head -c "${1:-16}"
    echo
  }
fi

if [[ -f "$KEYSTORE_PATH" && "$FORCE" == "false" ]]; then
  echo "Error: Keystore already exists at '$KEYSTORE_PATH'. Use --force to overwrite." >&2
  exit 1
fi

if [[ -f "$KEYSTORE_PATH" && "$FORCE" == "true" ]]; then
  rm -f "$KEYSTORE_PATH"
fi

# Generate password into a variable without ever echoing it.
# PKCS12 keystores require identical store and key passwords for Android Gradle Plugin.
KEYSTORE_PASSWORD="$(genpass "$PASS_LENGTH")"
KEY_PASSWORD="$KEYSTORE_PASSWORD"

export KEYSTORE_PASSWORD KEY_PASSWORD

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_PATH" \
  -storetype PKCS12 \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "$DNAME" \
  -storepass:env KEYSTORE_PASSWORD \
  -keypass:env KEY_PASSWORD >/dev/null 2>&1

chmod 600 "$KEYSTORE_PATH"

if [[ "$WRITE_PROPERTIES" == "true" ]]; then
  PROPS_FILE="$REPO_ROOT/keystore.properties"
  umask 077
  {
    printf 'storeFile=%s\n' "$KEYSTORE_PATH"
    printf 'storePassword=%s\n' "$KEYSTORE_PASSWORD"
    printf 'keyAlias=%s\n' "$KEY_ALIAS"
    printf 'keyPassword=%s\n' "$KEY_PASSWORD"
  } > "$PROPS_FILE"
  chmod 600 "$PROPS_FILE"
fi

# Upload secrets to GitHub Actions via stdin so values never appear in argv/logs.
base64 < "$KEYSTORE_PATH" | tr -d '\n' | gh secret set KEYSTORE_BASE64
printf '%s' "$KEYSTORE_PASSWORD" | gh secret set KEYSTORE_PASSWORD
printf '%s' "$KEY_ALIAS" | gh secret set KEY_ALIAS
printf '%s' "$KEY_PASSWORD" | gh secret set KEY_PASSWORD

unset KEYSTORE_PASSWORD KEY_PASSWORD

echo "Done: generated keystore at '$KEYSTORE_PATH' and uploaded secrets to GitHub Actions."
