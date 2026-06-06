package com.rushi.coinmaster.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_PREFERRED_CURRENCY = stringPreferencesKey("preferred_currency")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETE] ?: false
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETE] = complete
        }
    }

    val userName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME]
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = name
        }
    }

    val preferredCurrency: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_PREFERRED_CURRENCY] ?: "INR"
    }

    suspend fun setPreferredCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[KEY_PREFERRED_CURRENCY] = currency
        }
    }

    val appLanguage: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_APP_LANGUAGE] ?: "en"
    }

    suspend fun setAppLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = language
        }
    }
}
