package com.tangoplus.tangoq.function

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.MasterKey
import androidx.work.WorkManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.exception.NaverIdLoginSDKNotInitializedException
import com.navercorp.nid.oauth.NidOAuthLoginState
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkUser.logoutDenyRefreshJwt
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.db.Singleton_t_user
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.RuntimeException
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec



object SecureDataStoreManager {
    private val Context.dataStore by preferencesDataStore(name = "secureDataStore")

    private const val PREFERENCE_FILE = "secure_prefs"
    private const val SALT_LENGTH = 16
    private const val IV_SIZE = 12
    private const val GCM_TAG_LENGTH = 128
    private val SALT_KEY = stringPreferencesKey("file_encryption_salt")
    private val PASSPHRASE_KEY = stringPreferencesKey("db_passphrase")
    private val JWT_KEY = stringPreferencesKey("encrypted_jwt_jo")
    private val SERVER_UUID_KEY = stringPreferencesKey("encrypted_server_uuid")
    private val AES_KEY = generateAESKey()

    private fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }


    object DecryptedFileCache {
        private val cache = LruCache<String, ByteArray>(32 * 1024 * 1024)

        fun get(key: String): ByteArray? = cache.get(key)
        fun put(key: String, value: ByteArray) = cache.put(key, value)
        fun clear() = cache.evictAll()
    }

    @SuppressLint("HardwareIds")
    fun generateAESKey(context: Context): SecretKey {
//        val masterKey = MasterKey.Builder(context)
//            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
//            .build()

//        val sharedPreferences = EncryptedSharedPreferences.create(
//            context,
//            "File_Encryption_Prefs",
//            masterKey,
//            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
//        )

//        var salt = sharedPreferences.getString("file_encryption_salt", null)
//        if (salt == null) {
//            salt = generateSalt() // salt 만들기
//            sharedPreferences.edit().putString("file_encryption_salt", salt).apply()
//        }
//        val deviceId = Settings.Secure.getString(
//            context.contentResolver,
//            Settings.Secure.ANDROID_ID
//        )
//        val keyBytes = hashWithSHA512(deviceId, salt) // deviceId와 salt를 통해서 SHA-512 해싱 -> 이 해시를 key로 해서 AES-256 Encrypted함
//        return SecretKeySpec(keyBytes, "AES")
        val salt = getOrCreateSalt(context)
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        val keyBytes = hashWithSHA512(deviceId, salt)
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun getOrCreateSalt(context: Context) : String {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            var salt = prefs[SALT_KEY]

            if (salt == null) {
                salt = generateSalt()
                context.dataStore.edit { it[SALT_KEY] = salt }
            }
            salt
        }
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

    fun decryptFile(encryptedData: ByteArray, secretKey: SecretKey): ByteArray? {
        return try {
            // IV 추출
            val iv = encryptedData.copyOfRange(0, IV_SIZE)
            val encryptedContent = encryptedData.copyOfRange(IV_SIZE, encryptedData.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            cipher.doFinal(encryptedContent)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("SecureIndex", "${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            Log.e("SecureIllegal", "${e.message}")
            null
        } catch (e: IllegalStateException) {
            Log.e("SecureIllegal", "${e.message}")
            null
        } catch (e: NullPointerException) {
            Log.e("SecureNull", "${e.message}")
            null
        } catch (e: java.lang.Exception) {
            Log.e("SecureException", "${e.message}")
            null
        }
    }

    fun getOrCreatePassphrase(context: Context) : ByteArray {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            var passphrase = prefs[PASSPHRASE_KEY]
            var salt = prefs[SALT_KEY]

            if (salt == null) {
                salt = generateSalt()
                context.dataStore.edit { it[SALT_KEY] = salt }
            }

            if (passphrase == null) {
                passphrase = generatePassphrase(salt, context)
                context.dataStore.edit { it[PASSPHRASE_KEY] = passphrase }
            }
            Base64.decode(passphrase, Base64.NO_WRAP)
        }
    }

    @SuppressLint("HardwareIds")
    fun generatePassphrase(salt: String, context: Context) : String {
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        val hashedKey = hashWithSHA512(deviceId, salt)
        return Base64.encodeToString(hashedKey, Base64.NO_WRAP)
    }

    fun saveEncryptedJwtToken(context: Context, jwtJo: JSONObject?) {
        val jwtString = jwtJo.toString()
        val encryptedJwt = encrypt(jwtString, AES_KEY)
        runBlocking {
            context.dataStore.edit { it[JWT_KEY] = encryptedJwt }
        }
    }

    fun getEncryptedJwtJo(context: Context): JSONObject? {
        return runBlocking {
            val encryptedJwt = context.dataStore.data.map { it[JWT_KEY] }.firstOrNull()
            encryptedJwt?.let { decrypt(it, AES_KEY) }?.let { JSONObject(it) }
        }
    }

    fun getEncryptedAccessJwt(context: Context): String? {
        return runBlocking {
            getEncryptedJwtJo(context)?.optString("access_jwt")
        }
    }

    fun getEncryptedRefreshJwt(context: Context): String? {
        return runBlocking {
            getEncryptedJwtJo(context)?.optString("refresh_jwt")
        }
    }

    /** 🔹 JWT 삭제 */
    fun clearJwt(context: Context) {
        runBlocking {
            context.dataStore.edit { it.remove(JWT_KEY) }
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

    private fun encrypt(data: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charset.forName("UTF-8")))
        val combined = iv + encryptedBytes
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(data: String, secretKey: SecretKey): String {
        val decoded = Base64.decode(data, Base64.NO_WRAP)
        val iv = decoded.copyOfRange(0, 12)
        val encryptedBytes = decoded.copyOfRange(12, decoded.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charset.forName("UTF-8"))
    }




    fun isValidToken(jsonObject: JSONObject): Boolean {
        return jsonObject.has("access_jwt") &&
                jsonObject.has("refresh_jwt") &&
                !jsonObject.optString("access_jwt").isNullOrEmpty() &&
                !jsonObject.optString("refresh_jwt").isNullOrEmpty()
    }

    fun saveServerUUID(context: Context, uuid: String) {
        val encryptedUUID = encrypt(uuid, AES_KEY)
        runBlocking {
            Log.v("uuid", "Save uuid Successes")
            context.dataStore.edit { it[SERVER_UUID_KEY] = encryptedUUID }
        }
    }

    fun getServerUUID(context: Context) : String? {
        return runBlocking {
            val encryptedJwt = context.dataStore.data.map { it[JWT_KEY] }.firstOrNull()
            encryptedJwt?.let { decrypt(it, AES_KEY) }
        }
    }
    fun logout(activity: FragmentActivity, second: Int) {
        // token 유효성 검사 해제
        activity.lifecycleScope.launch {
            logoutDenyRefreshJwt(activity.getString(R.string.API_user), activity) { code ->
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(activity, IntroActivity::class.java)
                    activity.startActivity(intent)
                    activity.finishAffinity()
                }, (second * 1000).toLong())
            }
        }
        WorkManager.getInstance(activity).cancelUniqueWork("TokenCheckWork")
        try {
            if (Firebase.auth.currentUser != null) {
                Firebase.auth.signOut()
                Log.d("로그아웃", "Firebase sign out successful")
            } else if (NaverIdLoginSDK.getState() == NidOAuthLoginState.OK) {
                if (NaverIdLoginSDK.isInitialized()) {
                    NaverIdLoginSDK.logout()
                }
                Log.d("로그아웃", "Naver sign out successful")
            } else if (AuthApiClient.instance.hasToken()) {
                UserApiClient.instance.logout { error->
                    if (error != null) {
                        Log.e("로그아웃", "KAKAO Sign out failed", error)
                    } else {
                        Log.e("로그아웃", "KAKAO Sign out successful")
                    }
                }
            } else if (getEncryptedAccessJwt(activity) != null) {
                saveEncryptedJwtToken(activity, null)
            }

            // 싱글턴에 들어갔던거 전부 비우기
            Singleton_t_user.getInstance(activity).jsonObject = null
            Singleton_t_measure.getInstance(activity).measures = null


        } catch (e: NaverIdLoginSDKNotInitializedException) {
            Log.e("NILError", "네아로 is not initialized. (${e.message})")
        } catch (e: RuntimeException) {
            Log.e("네아로오류", "ThreadException : ${e.message}")
        } catch (e: UninitializedPropertyAccessException) {
            Log.e("카카오오류", "KaKao UninitializedProperty Exception : ${e.message}")
        } catch (e: Exception) {
            Log.e("카카오오류", "KaKao UninitializedProperty Exception : ${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("logoutError", "LogoutIllegalState: ${e.message}")
        } catch (e: java.lang.IllegalArgumentException) {
            Log.e("logoutError", "LogoutIllegalArgument: ${e.message}")
        } catch (e: NullPointerException) {
            Log.e("logoutError", "LogoutNullPointer: ${e.message}")
        } catch (e: InterruptedException) {
            Log.e("logoutError", "LogoutInterrupted: ${e.message}")
        } catch (e: IndexOutOfBoundsException) {
            Log.e("logoutError", "LogoutIndexOutOfBounds: ${e.message}")
        } catch (e: Exception) {
            Log.e("logoutError", "Logout: ${e.message}")
        }
    }

}