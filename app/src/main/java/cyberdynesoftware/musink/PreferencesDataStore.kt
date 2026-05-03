package cyberdynesoftware.musink

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val HOME_DIRECTORY = stringPreferencesKey("home_directory")

class PreferencesDataStore(val context: Context) {
    fun homeFlow(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[HOME_DIRECTORY]
    }

    suspend fun saveHome(home: String) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[HOME_DIRECTORY] = home
            }
        }
    }
}