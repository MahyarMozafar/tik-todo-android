# Open screens are saved by their class name when Android stops the app in the background,
# and found again by that name when it comes back.
-keep class com.mahyarmozafar.tik.ui.navigation.** { *; }

# The widget's tick button names its action by class, and Glance creates it from that name.
-keep class com.mahyarmozafar.tik.widget.ToggleTaskAction { <init>(); }
