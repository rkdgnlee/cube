package com.tangoplus.tangoq.db

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


object SecurePreferencesManager {
    private const val PREFERENCE_FILE = "secure_prefs"
    private lateinit var encryptedSharedPreferences: EncryptedSharedPreferences
    private const val SALT_LENGTH = 16
    private const val IV_SIZE = 12
    private const val GCM_TAG_LENGTH = 128

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

    // ------# 파일 암호화 #------
    @SuppressLint("HardwareIds")
    fun generateAESKey(context: Context): SecretKey {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "File_Encryption_Prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var salt = sharedPreferences.getString("file_encryption_salt", null)
        if (salt == null) {
            salt = generateSalt()
            sharedPreferences.edit().putString("file_encryption_salt", salt).apply()
        }

        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        val keyBytes = hashWithSHA512(deviceId, salt)
        return SecretKeySpec(keyBytes, "AES")
    }


    fun encryptFile(inputFile: File, outputFile: File, secretKey: SecretKey) {

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        FileOutputStream(outputFile).use { output ->
            // Write IV (salt는 EncryptedSharedPreferences에 저장되므로 파일에 쓸 필요 없음)
            output.write(iv)

            // Encrypt file content
            CipherOutputStream(output, cipher).use { cipherOutStream ->
                FileInputStream(inputFile).use { input ->
                    input.copyTo(cipherOutStream)
                }
            }
        }
    }
    class DecryptedFileCache {
        private val cache = LruCache<String, ByteArray>(50 * 1024 * 1024) // 50MB 캐시

        fun get(key: String): ByteArray? = cache.get(key)

        fun put(key: String, value: ByteArray) {
            cache.put(key, value)
        }

        fun clear() {
            cache.evictAll()
        }

        companion object {
            private var instance: DecryptedFileCache? = null

            fun getInstance(): DecryptedFileCache {
                if (instance == null) {
                    instance = DecryptedFileCache()
                }
                return instance!!
            }
        }
    }
    // 메모리 기반 복호화 함수
    fun decryptFile(encryptedData: ByteArray, secretKey: SecretKey): ByteArray? {
        return try {
            // IV 추출
            val iv = encryptedData.copyOfRange(0, IV_SIZE)
            val encryptedContent = encryptedData.copyOfRange(IV_SIZE, encryptedData.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            cipher.doFinal(encryptedContent)
        } catch (e: Exception) {
            Log.e("SecureFileManager", "Decryption failed: ${e.message}")
            null
        }
    }

    // ------# DB 암호화 #------
    @SuppressLint("HardwareIds")
    fun generateSecurePassphrase(context: Context) : ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "DB_Shared_Prefs_Encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var salt = sharedPreferences.getString("db_salt", null)
        if (salt == null) {
            salt = generateSalt()
            sharedPreferences.edit().putString("db_salt", salt).apply()
        }
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return hashWithSHA512(deviceId, salt)
    }

    private fun generateSalt() : String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    private fun hashWithSHA512(input: String, salt: String) : ByteArray {
        val messageDigest = MessageDigest.getInstance("SHA-512")
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)

        val inputBytes = input.toByteArray(Charset.forName("UTF-8"))
        val combined = inputBytes + saltBytes
        val hashedBytes = messageDigest.digest(combined)

        return hashedBytes.copyOfRange(0, 32)
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
        val jsonString  = sharedPreferences.getString("jwt_token", null)
        return try {
            val jsonObject = JSONObject(jsonString)
            jsonObject.getString("jwt")
        } catch (e: Exception) {
            null // JSON 파싱 실패 시 null 반환
        }
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


    // ------# SSAID 저장 #------
    private const val SERVER_UUID_KEY = "encrypted_server_uuid"

    fun saveServerUUID(context: Context, uuid: String) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREFERENCE_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        Log.v("uuid", "Save uuid Successes")
        encryptedPrefs.edit().putString(SERVER_UUID_KEY, uuid).apply()
    }

    fun getServerUUID(context: Context): String? {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREFERENCE_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        return encryptedPrefs.getString(SERVER_UUID_KEY, null)
    }

}