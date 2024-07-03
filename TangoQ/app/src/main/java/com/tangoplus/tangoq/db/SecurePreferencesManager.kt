package com.tangoplus.tangoq.db

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey


object SecurePreferencesManager {
    private const val PREFERENCE_FILE = "secure_prefs"
    private lateinit var encryptedSharedPreferences: EncryptedSharedPreferences

    fun getInstance(context: Context): EncryptedSharedPreferences {
        if (!::encryptedSharedPreferences.isInitialized) {
            val masterKeyAlias = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFERENCE_FILE,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        }
        return encryptedSharedPreferences
    }

    // 서버 토큰 저장
    fun saveServerToken(context: Context, token: String?) {
        val prefs = getInstance(context)
        prefs.edit().putString("server_token", token).apply()
    }

    // 서버 토큰 불러오기
    fun getServerToken(context: Context): String? {
        val prefs = getInstance(context)
        return prefs.getString("server_token", null)
    }
}