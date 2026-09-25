package com.mahyarmozafar.tik.app

import android.content.Context
import android.content.res.Configuration
import com.mahyarmozafar.tik.model.AppLanguage
import java.util.Locale

/**
 * A context whose texts are in the language picked inside the app, not the phone's language.
 * Screens get this from the activity; notifications and the widget need it made by hand.
 */
fun Context.localized(language: AppLanguage): Context {
    val locale = Locale.forLanguageTag(language.tag)
    val configuration = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    return createConfigurationContext(configuration)
}
