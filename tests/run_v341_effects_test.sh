#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
work=$(mktemp -d); trap 'rm -rf "$work"' EXIT
src=app/src/main/java/com/abhi/ganasuno
# Compile the production normalizer without unrelated catalogue helpers.
{ printf 'package com.abhi.ganasuno.data\nimport kotlin.math.*\n'; sed -n '/^class RmsNormalizer/,$p' "$src/data/Batch14.kt"; } > "$work/RmsNormalizer.kt"
processor=${EFFECTS_PROCESSOR_SOURCE:-$src/player/EffectsProcessor.kt}
kotlinc "$processor" "$src/player/"{StereoDsp,TenBandDsp,SoftwareReverb,TransitionMath}.kt "$work/RmsNormalizer.kt" tests/v341_stubs/*.kt tests/V341EffectsProcessorTest.kt -nowarn -include-runtime -d "$work/test.jar"
java -jar "$work/test.jar"
