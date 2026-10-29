package fr.tflag.droidboost.maintenance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Packages the user disabled or removed from DroidBoost. A system update or factory reset can bring
 * them back: the debloat screen compares this registry with the actual state and offers to redo it.
 */
class DebloatRegistry(private val store: DataStore<Preferences>) {

    val packages: Flow<Set<String>> = store.data.map { it[KEY].orEmpty() }

    suspend fun add(packageName: String) = store.edit { it[KEY] = it[KEY].orEmpty() + packageName }

    suspend fun remove(packageName: String) = store.edit { it[KEY] = it[KEY].orEmpty() - packageName }

    private companion object {
        val KEY = stringSetPreferencesKey("debloated_packages")
    }
}
