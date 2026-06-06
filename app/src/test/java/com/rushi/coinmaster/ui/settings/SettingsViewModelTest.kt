package com.rushi.coinmaster.ui.settings

import com.rushi.coinmaster.data.preferences.AppPreferences
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val appPreferences: AppPreferences = mockk(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { appPreferences.appLanguage } returns flowOf("mr")
        viewModel = SettingsViewModel(appPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCurrentLanguageExposesSavedPreference() = runTest {
        val languages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.currentLanguage.collect {
                languages.add(it)
            }
        }
        testScheduler.advanceUntilIdle()
        assertEquals("mr", viewModel.currentLanguage.value)
        assertEquals("mr", languages.last())
    }

    @Test
    fun testSetLanguagePersistsSelection() = runTest {
        viewModel.setLanguage("hi")
        testScheduler.advanceUntilIdle()

        coVerify { appPreferences.setAppLanguage("hi") }
    }
}
