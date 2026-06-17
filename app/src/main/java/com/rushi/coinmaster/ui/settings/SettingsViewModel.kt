package com.rushi.coinmaster.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushi.coinmaster.data.local.database.CoinMasterDatabase
import com.rushi.coinmaster.data.preferences.AppPreferences
import com.rushi.coinmaster.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val database: CoinMasterDatabase
) : ViewModel() {

    /** The currently persisted language code (e.g. "en", "hi", "mr"). */
    val currentLanguage: StateFlow<String> = appPreferences.appLanguage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en"
        )

    /** The currently persisted theme (e.g. "light", "dark", "system"). */
    val currentTheme: StateFlow<String> = appPreferences.appTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "system"
        )

    /**
     * Persists the selected language to DataStore.
     * The caller (SettingsFragment) is responsible for applying the locale to the Activity.
     */
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            appPreferences.setAppLanguage(languageCode)
            WidgetUpdater.updateWidget(context)
        }
    }

    /**
     * Persists the selected theme to DataStore.
     */
    fun setTheme(theme: String) {
        viewModelScope.launch {
            appPreferences.setAppTheme(theme)
        }
    }

    /**
     * Clears all user data from the database, resets onboarding completion and profile details,
     * and triggers a widget update.
     */
    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
            appPreferences.clearUserData()
            WidgetUpdater.updateWidget(context)
        }
    }
}
