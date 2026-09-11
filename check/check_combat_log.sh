#!/bin/sh
# Asserts the damage accounting rules SilverLib records: overkill clamping, absorption-as-dealt,
# mitigation, accumulation, the window boundary and the participant union. No Minecraft classpath
# needed. Run from anywhere.
set -e
root=$(cd "$(dirname "$0")/.." && pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
cp "$root/check/check_combat_log/CombatShareCheck.java" "$work/"
javac -d "$work/out" "$work/CombatShareCheck.java"
java -cp "$work/out" CombatShareCheck
