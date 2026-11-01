#!/usr/bin/env bash
# Runs Gradle with the workspace-local Android SDK, Gradle home and Android home.
# Usage: scripts/gradle.sh <gradle tasks...>
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME="$ROOT/.gradle"
export ANDROID_HOME="$ROOT/.tooling/android-sdk"
export ANDROID_SDK_ROOT="$ROOT/.tooling/android-sdk"
export ANDROID_USER_HOME="$ROOT/.android"
mkdir -p "$ANDROID_USER_HOME"

cd "$ROOT"
exec "$ROOT/.tooling/gradle-8.11.1/bin/gradle" "$@"
