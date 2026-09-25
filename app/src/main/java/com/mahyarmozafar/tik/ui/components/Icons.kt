package com.mahyarmozafar.tik.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.model.AccentChoice
import com.mahyarmozafar.tik.model.AppIconChoice
import com.mahyarmozafar.tik.model.ListIcon
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.SmartList

@get:DrawableRes
val ListIcon.drawable: Int
    get() = when (this) {
        ListIcon.List -> R.drawable.ic_list_list
        ListIcon.House -> R.drawable.ic_list_house
        ListIcon.Briefcase -> R.drawable.ic_list_briefcase
        ListIcon.Cart -> R.drawable.ic_list_cart
        ListIcon.Heart -> R.drawable.ic_list_heart
        ListIcon.Star -> R.drawable.ic_list_star
        ListIcon.Book -> R.drawable.ic_list_book
        ListIcon.School -> R.drawable.ic_list_school
        ListIcon.Dumbbell -> R.drawable.ic_list_dumbbell
        ListIcon.Run -> R.drawable.ic_list_run
        ListIcon.Food -> R.drawable.ic_list_food
        ListIcon.Cup -> R.drawable.ic_list_cup
        ListIcon.Airplane -> R.drawable.ic_list_airplane
        ListIcon.Car -> R.drawable.ic_list_car
        ListIcon.Gift -> R.drawable.ic_list_gift
        ListIcon.Flag -> R.drawable.ic_list_flag
        ListIcon.Bolt -> R.drawable.ic_list_bolt
        ListIcon.Leaf -> R.drawable.ic_list_leaf
        ListIcon.Paw -> R.drawable.ic_list_paw
        ListIcon.Gamepad -> R.drawable.ic_list_gamepad
        ListIcon.Music -> R.drawable.ic_list_music
        ListIcon.Brush -> R.drawable.ic_list_brush
        ListIcon.Laptop -> R.drawable.ic_list_laptop
        ListIcon.Code -> R.drawable.ic_list_code
        ListIcon.People -> R.drawable.ic_list_people
        ListIcon.Money -> R.drawable.ic_list_money
        ListIcon.Pills -> R.drawable.ic_list_pills
        ListIcon.Sparkles -> R.drawable.ic_list_sparkles
        ListIcon.Moon -> R.drawable.ic_list_moon
        ListIcon.Sun -> R.drawable.ic_list_sun
    }

@get:DrawableRes
val SmartList.drawable: Int
    get() = when (this) {
        SmartList.Today -> R.drawable.ic_sun_fill
        SmartList.Scheduled -> R.drawable.ic_scheduled_fill
        SmartList.All -> R.drawable.ic_all_fill
        SmartList.Completed -> R.drawable.ic_completed_fill
    }

@get:StringRes
val SmartList.title: Int
    get() = when (this) {
        SmartList.Today -> R.string.today
        SmartList.Scheduled -> R.string.scheduled
        SmartList.All -> R.string.all
        SmartList.Completed -> R.string.completed
    }

@get:StringRes
val Priority.title: Int
    get() = when (this) {
        Priority.None -> R.string.priority_none
        Priority.Low -> R.string.priority_low
        Priority.Medium -> R.string.priority_medium
        Priority.High -> R.string.priority_high
    }

@get:StringRes
val AccentChoice.title: Int
    get() = when (this) {
        AccentChoice.Blue -> R.string.color_blue
        AccentChoice.Indigo -> R.string.color_indigo
        AccentChoice.Purple -> R.string.color_purple
        AccentChoice.Pink -> R.string.color_pink
        AccentChoice.Red -> R.string.color_red
        AccentChoice.Orange -> R.string.color_orange
        AccentChoice.Yellow -> R.string.color_yellow
        AccentChoice.Green -> R.string.color_green
        AccentChoice.Mint -> R.string.color_mint
        AccentChoice.Teal -> R.string.color_teal
        AccentChoice.Graphite -> R.string.color_graphite
    }

@get:StringRes
val AppIconChoice.title: Int
    get() = when (this) {
        AppIconChoice.Blue -> R.string.color_blue
        AppIconChoice.Midnight -> R.string.icon_midnight
        AppIconChoice.Light -> R.string.icon_light
        AppIconChoice.Mint -> R.string.color_mint
        AppIconChoice.Purple -> R.string.color_purple
        AppIconChoice.Sunset -> R.string.icon_sunset
    }
