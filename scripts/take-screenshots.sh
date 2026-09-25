#!/bin/sh
# Takes the README screenshots on a running emulator or phone with a debug build installed.
# The status bar is set to a tidy demo state while it runs.
#
# Usage (from the repo root):  sh scripts/take-screenshots.sh [device serial]

set -e
ADB="adb${1:+ -s $1}"
APP=com.mahyarmozafar.tik
OUT=docs/screenshots
mkdir -p "$OUT"

demo() { $ADB shell am broadcast -a com.android.systemui.demo -e command "$@" > /dev/null; }
$ADB shell settings put global sysui_demo_allowed 1
demo enter
demo clock -e hhmm 0941
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile hide
demo notifications -e visible false

# shot NAME [extras for am start...]
shot() {
  name=$1
  shift
  $ADB shell am force-stop $APP
  $ADB shell am start -W -n $APP/.MainActivity --ez demo true "$@" > /dev/null
  sleep 4
  $ADB exec-out screencap -p > "$OUT/$name.png"
  echo "$name"
}

shot en-light-today --es lang en --es theme light --es tab today
shot en-light-editor --es lang en --es theme light --es screen editor
shot en-light-lists --es lang en --es theme light --es tab lists
shot en-light-settings --es lang en --es theme light --es screen settings

shot en-dark-today --es lang en --es theme dark --es tab today
shot en-dark-editor --es lang en --es theme dark --es screen editor
shot en-dark-scheduled --es lang en --es theme dark --es screen scheduled

shot fa-light-today --es lang fa --es theme light --es tab today
shot fa-light-editor --es lang fa --es theme light --es screen editor
shot fa-light-lists --es lang fa --es theme light --es tab lists
shot fa-light-settings --es lang fa --es theme light --es screen settings

# The quick add field, with a task being typed.
$ADB shell am force-stop $APP
$ADB shell am start -W -n $APP/.MainActivity --ez demo true --es lang en --es theme light --ez add true > /dev/null
sleep 3
$ADB shell input text "Pick%sup%sthe%sflowers"
sleep 1
$ADB exec-out screencap -p > "$OUT/en-light-quick-add.png"
echo en-light-quick-add

# Confetti, a moment after the last task of the day is ticked.
$ADB shell am force-stop $APP
$ADB shell am start -W -n $APP/.MainActivity --ez demo true --es lang en --es theme dark --ez celebrate true > /dev/null
sleep 2.4
$ADB exec-out screencap -p > "$OUT/all-done.png"
echo all-done

$ADB shell am force-stop $APP
demo exit
