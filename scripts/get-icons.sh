#!/bin/sh
# Downloads the Material Symbols (Rounded) icons Tik uses, as Android vector
# drawables, into app/src/main/res/drawable.
#
# Usage (from the repo root):  sh scripts/get-icons.sh
#
# Material Symbols are by Google, under the Apache License 2.0.

set -e
base="https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android"
out="app/src/main/res/drawable"
mkdir -p "$out"

# Each line: the name in Tik, the Material Symbols name, "fill" for the filled
# style or "line" for the outlined one, and "mirror" for icons that point
# somewhere and must flip in Farsi.
icons="
today sunny line -
today_fill sunny fill -
lists stacks line -
lists_fill stacks fill -
search search line -
search_off search_off line -
settings settings line -
add add line -
arrow_up arrow_upward line -
arrow_back arrow_back line mirror
close close line -
tune tune line -
check check line -
chevron_start chevron_left line mirror
chevron_end chevron_right line mirror
expand expand_more line -
more more_horiz line -
edit edit line -
delete delete line -
undo undo line mirror
tomorrow wb_twilight line -
priority error line -
duplicate library_add line -
calendar calendar_today line -
clock schedule line -
repeat repeat line -
notes notes line mirror
checklist checklist line mirror
photo image line -
add_photo add_photo_alternate line -
drag drag_indicator line -
inbox inbox line -
verified verified fill -
warning warning fill -
info info fill -
sun_fill sunny fill -
scheduled_fill calendar_month fill -
all_fill all_inbox fill -
completed_fill check_circle fill -
inbox_fill inbox fill -
language_fill language fill -
calendar_fill calendar_month fill -
clock_fill schedule fill -
contrast_fill contrast fill -
palette_fill palette fill -
gradient_fill gradient fill -
wallpaper_fill wallpaper fill -
apps_fill apps fill -
sort_fill swap_vert fill -
chart_fill bar_chart fill -
done_all_fill done_all fill -
vibration_fill vibration fill -
volume_fill volume_up fill mirror
celebration_fill celebration fill -
notes_fill notes fill mirror
checklist_fill checklist fill mirror
event_fill event fill -
repeat_fill repeat fill -
priority_fill priority_high fill -
photo_fill image fill -
bell_fill notifications fill -
badge_fill app_badging fill -
list_list list fill mirror
list_house home fill -
list_briefcase work fill -
list_cart shopping_cart fill mirror
list_heart favorite fill -
list_star star fill -
list_book menu_book fill -
list_school school fill -
list_dumbbell fitness_center fill -
list_run directions_run fill mirror
list_food restaurant fill -
list_cup local_cafe fill -
list_airplane flight fill -
list_car directions_car fill -
list_gift redeem fill -
list_flag flag fill -
list_bolt bolt fill -
list_leaf eco fill -
list_paw pets fill -
list_gamepad sports_esports fill -
list_music music_note fill -
list_brush brush fill -
list_laptop laptop_mac fill -
list_code code fill -
list_people group fill -
list_money payments fill -
list_pills medication fill -
list_sparkles auto_awesome fill -
list_moon dark_mode fill -
list_sun sunny fill -
"

echo "$icons" | while read -r name symbol style mirror; do
  [ -z "$name" ] && continue
  if [ "$style" = "fill" ]; then file="${symbol}_fill1_24px.xml"; else file="${symbol}_24px.xml"; fi
  target="$out/ic_$name.xml"
  curl -sf --retry 3 --max-time 30 "$base/$symbol/materialsymbolsrounded/$file" -o "$target.tmp"
  # Compose tints icons itself, so the theme tint is dropped.
  if [ "$mirror" = "mirror" ] && ! grep -q autoMirrored "$target.tmp"; then
    flip='s/android:viewportHeight="960"/android:viewportHeight="960" android:autoMirrored="true"/'
  else
    flip='s/^//'
  fi
  sed -e 's/ *android:tint="[^"]*"//' -e 's/@android:color\/white/#FF000000/' -e "$flip" "$target.tmp" > "$target"
  rm "$target.tmp"
  echo "ic_$name"
done
