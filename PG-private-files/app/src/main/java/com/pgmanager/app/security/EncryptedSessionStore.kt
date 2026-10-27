package com.pgmanager.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pgmanager.app.domain.model.UserRole

class EncryptedSessionStore(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSession(userId: String, phoneNumber: String, role: UserRole, activePgId: String?) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_PHONE, phoneNumber)
            .putString(KEY_ROLE, role.name)
            .putString(KEY_ACTIVE_PG, activePgId)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getPhoneNumber(): String? = prefs.getString(KEY_PHONE, null)

    fun getRole(): UserRole {
        val roleStr = prefs.getString(KEY_ROLE, null)
        return UserRole.fromString(roleStr)
    }

    fun getActivePgId(): String? = prefs.getString(KEY_ACTIVE_PG, null)

    fun setActivePgId(pgId: String?) {
        prefs.edit().putString(KEY_ACTIVE_PG, pgId).apply()
    }

    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(): String? = prefs.getString(KEY_FCM_TOKEN, null)

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILENAME = "pg_manager_secure_session"
        private const val KEY_USER_ID = "sec_user_id"
        private const val KEY_PHONE = "sec_phone"
        private const val KEY_ROLE = "sec_role"
        private const val KEY_ACTIVE_PG = "sec_active_pg"
        private const val KEY_TIMESTAMP = "sec_timestamp"
        private const val KEY_FCM_TOKEN = "sec_fcm_token"
    }
}
