package com.mahyarmozafar.tik

import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.glance.GlanceId
import androidx.glance.action.actionParametersOf
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mahyarmozafar.tik.widget.ToggleTaskAction
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Walks through the main things people do, on the example tasks. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private fun launch(language: String = "en") {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("demo", true)
            .putExtra("lang", language)
            .putExtra("theme", "light")
            .putExtra("tab", "today")
        scenario = ActivityScenario.launch(intent)
        rule.waitUntilAtLeastOneExists(hasTestTag("todayList"), timeoutMillis = 10_000)
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun quickAddAddsATaskToToday() {
        launch()
        rule.onNodeWithTag("quickAddButton").performClick()
        rule.onNodeWithTag("quickAddField").performTextInput("Buy flowers")
        rule.onNodeWithTag("quickAddField").performImeAction()
        rule.onNodeWithTag("todayList").performScrollToNode(hasText("Buy flowers"))
        rule.onNodeWithText("Buy flowers").assertIsDisplayed()
    }

    @Test
    fun tickingATaskMarksItDone() {
        launch()
        rule.onNodeWithTag("todayList").performScrollToNode(hasTestTag("check-Call mom"))
        rule.onNodeWithTag("check-Call mom").performClick()
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("check-Call mom").assertIsOn() }.isSuccess
        }
    }

    @Test
    fun theEditorSavesChanges() {
        launch()
        rule.onNodeWithTag("todayList").performScrollToNode(hasText("Call mom"))
        rule.onNodeWithText("Call mom").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("titleField"))
        rule.onNodeWithTag("titleField").performTextReplacement("Call mom tonight")
        rule.onNodeWithTag("saveButton").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Call mom tonight"), timeoutMillis = 5_000)
    }

    @Test
    fun searchFindsTasksByTheirNotes() {
        launch()
        rule.onNodeWithTag("tab-search").performClick()
        rule.onNodeWithTag("searchField").performTextInput("designs")
        rule.waitUntilAtLeastOneExists(hasText("Team meeting"), timeoutMillis = 5_000)
    }

    @Test
    fun scheduledShowsUpcomingTasks() {
        launch()
        rule.onNodeWithTag("tab-lists").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("smart-scheduled"))
        rule.onNodeWithTag("smart-scheduled").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Team meeting"), timeoutMillis = 5_000)
    }

    /** Like the iOS LanguageSwitchTests: Settings stays open while the app changes language. */
    @Test
    fun switchingLanguageKeepsSettingsOpen() {
        launch()
        rule.onNodeWithTag("settingsButton").performClick()
        rule.onNodeWithTag("languagePicker").performClick()
        rule.onNodeWithText("فارسی").performClick()
        rule.waitUntilAtLeastOneExists(hasText("تنظیمات"), timeoutMillis = 10_000)

        rule.onNodeWithTag("languagePicker").performClick()
        rule.onNodeWithText("English").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Settings"), timeoutMillis = 10_000)
    }

    @Test
    fun theWidgetCanTickATask() {
        launch()
        val app = ApplicationProvider.getApplicationContext<TikApplication>()
        val task = runBlocking { app.store.allTasks().first { it.title == "Call mom" } }

        runBlocking {
            ToggleTaskAction().onAction(app, object : GlanceId {}, actionParametersOf(ToggleTaskAction.TaskId to task.id))
        }

        assertThat(runBlocking { app.store.task(task.id)!!.isDone }).isTrue()
    }
}
