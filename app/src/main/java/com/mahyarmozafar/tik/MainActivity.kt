package com.mahyarmozafar.tik

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mahyarmozafar.tik.app.DemoData
import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.ui.LocalChangeLanguage
import com.mahyarmozafar.tik.ui.TikRoot
import com.mahyarmozafar.tik.ui.components.LocalSounds
import com.mahyarmozafar.tik.ui.theme.TikTheme
import com.mahyarmozafar.tik.ui.theme.isDarkTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val app get() = application as TikApplication

    /** Goes up when a reminder or the widget asks for Today. */
    private var openTodaySignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false

        if (savedInstanceState == null) {
            if (BuildConfig.DEBUG) DemoData.applyLaunchExtras(intent, app)
            handle(intent)
        }
        syncLanguage()

        setContent {
            val settings by app.model.settings.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent
            val dark = isDarkTheme(current.theme)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            CompositionLocalProvider(
                LocalSounds provides app.sounds,
                LocalChangeLanguage provides ::changeLanguage,
            ) {
                TikTheme(current) {
                    TikRoot(app.model, openTodaySignal)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    /** Reminders open Today; the widget opens Today, or Today with the quick add field (tik://new). */
    private fun handle(intent: Intent?) {
        intent ?: return
        val host = intent.data?.takeIf { it.scheme == "tik" }?.host
        if (intent.action == ACTION_TODAY || host == "today" || host == "new") openTodaySignal++
        if (host == "new") app.model.pendingQuickAdd.value = true
    }

    /**
     * The language lives in two places: Tik's settings (for the widget and reminders) and Android's
     * per-app language (for the screens). Tik keeps them the same; if the language was changed in
     * Android's own settings, Tik follows it.
     */
    private fun syncLanguage() {
        lifecycleScope.launch {
            val chosen = app.model.currentSettings().language
            val locales = AppCompatDelegate.getApplicationLocales()
            val applied = locales[0]?.language?.let(AppLanguage::fromTag)
            when {
                applied == null -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(chosen.tag))
                applied != chosen -> app.model.updateSettings { it.copy(language = applied) }
            }
        }
    }

    private fun changeLanguage(language: AppLanguage) {
        lifecycleScope.launch {
            app.model.updateSettings { it.copy(language = language) }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }
    }

    companion object {
        const val ACTION_TODAY = "com.mahyarmozafar.tik.TODAY"
    }
}
