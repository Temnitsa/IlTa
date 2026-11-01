#!/usr/bin/env bash
# adb wrapper with workspace-local HOME/ANDROID_USER_HOME (sandbox-safe).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="$ROOT/.tooling/android-sdk"
export HOME="$ROOT/.home"
export ANDROID_USER_HOME="$ROOT/.android"
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
mkdir -p "$HOME/.android"

exec "$SDK/platform-tools/adb" "$@"
