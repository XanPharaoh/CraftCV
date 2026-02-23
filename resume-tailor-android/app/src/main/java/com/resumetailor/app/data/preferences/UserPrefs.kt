package com.resumetailor.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "resume_tailor_prefs")

class UserPrefs(private val context: Context) {

    companion object {
        private val DEVICE_ID        = stringPreferencesKey("device_id")
        private val IS_PRO           = booleanPreferencesKey("is_pro")
        private val LAST_RESUME      = stringPreferencesKey("last_resume")
        private val LAST_JOB_DESC    = stringPreferencesKey("last_job_desc")
        private val HAS_SEEN_PROFILE  = booleanPreferencesKey("has_seen_profile")
        private val PROFILE_JSON      = stringPreferencesKey("profile_json")
        private val DRAFT_RESUME_JSON = stringPreferencesKey("draft_resume_json")
    }

    val deviceId: Flow<String> = context.dataStore.data.map { it[DEVICE_ID] ?: "" }

    val isPro: Flow<Boolean> = context.dataStore.data.map { it[IS_PRO] ?: false }

    val lastResume: Flow<String> = context.dataStore.data.map { it[LAST_RESUME] ?: "" }

    val lastJobDesc: Flow<String> = context.dataStore.data.map { it[LAST_JOB_DESC] ?: "" }

    /** True once the user has completed or skipped the profile screen. */
    val hasSeenProfile: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_PROFILE] ?: false }

    suspend fun ensureDeviceId(): String {
        var id = ""
        context.dataStore.edit { prefs ->
            id = prefs[DEVICE_ID] ?: UUID.randomUUID().toString().also { prefs[DEVICE_ID] = it }
        }
        return id
    }

    suspend fun setProStatus(isPro: Boolean) {
        context.dataStore.edit { it[IS_PRO] = isPro }
    }

    suspend fun saveLastInputs(resume: String, jobDesc: String) {
        context.dataStore.edit {
            it[LAST_RESUME]   = resume
            it[LAST_JOB_DESC] = jobDesc
        }
    }

    suspend fun setHasSeenProfile(value: Boolean) {
        context.dataStore.edit { it[HAS_SEEN_PROFILE] = value }
    }

    val profileJson: Flow<String> = context.dataStore.data.map { it[PROFILE_JSON] ?: "" }

    suspend fun saveProfile(json: String) {
        context.dataStore.edit { it[PROFILE_JSON] = json }
    }

    val draftResumeJson: Flow<String> = context.dataStore.data.map { it[DRAFT_RESUME_JSON] ?: "" }

    suspend fun saveDraftResume(json: String) {
        context.dataStore.edit { it[DRAFT_RESUME_JSON] = json }
    }
}
