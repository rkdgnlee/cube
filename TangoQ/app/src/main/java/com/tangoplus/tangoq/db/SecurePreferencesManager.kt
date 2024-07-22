package com.tangoplus.tangoq.db

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


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

    // ------! 토큰 저장 !------
    fun saveEncryptedJwtToken(context: Context, token: String?) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences : SharedPreferences = EncryptedSharedPreferences.create(
            "loginEncryptPrefs", masterKeyAlias, context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val editor : SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("jwt_token", token)
        editor.apply()
    }

    // ------! 토큰 로드 !------
    fun getEncryptedJwtToken(context: Context): String? {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            "loginEncryptPrefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = sharedPreferences.getString("jwt_token", null)
        return token
    }

    // ------! 자체 로그인 암호화 저장 !------
    fun createKey(alias: String) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    fun encryptData(alias: String, jsonObj : JSONObject) : String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secretKey = keyStore.getKey(alias, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryption = cipher.doFinal(jsonObj.toString().toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encryption, Base64.DEFAULT)
    }
    fun saveEncryptedData(context: Context, key: String, encryptedData: String) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(key, encryptedData).apply()
    }

    fun decryptData(alias: String, encryptedData: String): JSONObject {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secretKey = keyStore.getKey(alias, null) as SecretKey
        val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, encryptedBytes, 0, 12)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        val decryptedBytes = cipher.doFinal(encryptedBytes, 12, encryptedBytes.size - 12)
        val decryptedString = String(decryptedBytes, Charsets.UTF_8)
        return JSONObject(decryptedString)
    }

    fun loadEncryptedData(context: Context, key: String): String? {
        val sharedPreferences = context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, null)
    }

}