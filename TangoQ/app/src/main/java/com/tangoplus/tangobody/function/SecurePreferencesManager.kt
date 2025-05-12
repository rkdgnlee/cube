package com.tangoplus.tangobody.function

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import androidx.work.WorkManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.exception.NaverIdLoginSDKNotInitializedException
import com.navercorp.nid.oauth.NidOAuthLoginState
import com.tangoplus.tangobody.IntroActivity
import com.tangoplus.tangobody.R
import com.tangoplus.tangobody.api.NetworkUser.logoutDenyRefreshJwt
import com.tangoplus.tangobody.db.Singleton_t_measure
import com.tangoplus.tangobody.db.Singleton_t_user
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.RuntimeException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import androidx.core.content.edit


object SecurePreferencesManager {
    private const val PREFERENCE_FILE = "secure_prefs"
    private lateinit var encryptedSharedPreferences: EncryptedSharedPreferences
    private const val SALT_LENGTH = 16
    private const val IV_SIZE = 12
    private const val GCM_TAG_LENGTH = 128

    fun getInstance(context: Context): EncryptedSharedPreferences {
        if (!SecurePreferencesManager::encryptedSharedPreferences.isInitialized) {
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

    class DecryptedFileCache {
        private val cache = LruCache<String, ByteArray>(32 * 1024 * 1024) // 32MB 캐시

        fun get(key: String): ByteArray? = cache.get(key)

        fun put(key: String, value: ByteArray) {
            cache.put(key, value)
        }

        fun clear() {
            cache.evictAll()
        }

        companion object {
            private var instance: DecryptedFileCache? = null

            fun getInstance(): DecryptedFileCache? {
                if (instance == null) {
                    instance = DecryptedFileCache()
                }
                return instance
            }
        }
    }

    // ------# C#의 비밀번호 암호화 복호화 코드 #------
    fun encrypt(plainText: String, secretKey: String, secretIv: String): String {
        // SHA-256 해시를 사용하여 키 생성
        val sha256 = MessageDigest.getInstance("SHA-256")
        val key = sha256.digest(secretKey.toByteArray(StandardCharsets.US_ASCII))
        val iv = secretIv.toByteArray(StandardCharsets.UTF_8)

        // AES 암호화 설정
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val aesKey = ByteArray(32)
        System.arraycopy(key, 0, aesKey, 0, 32)
        val secretKeySpec = SecretKeySpec(aesKey, "AES")
        val ivParameterSpec = IvParameterSpec(iv)

        // 암호화 실행
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
        val plainBytes = plainText.toByteArray(StandardCharsets.UTF_8)
        val cipherBytes = cipher.doFinal(plainBytes)

        // Base64 인코딩하여 반환
        return Base64.encodeToString(cipherBytes,  Base64.NO_WRAP)
    }

//    fun decrypt(cipherText: String, secretKey: String, secretIv: String): String {
//        // SHA-256 해시를 사용하여 키 생성
//        val sha256 = MessageDigest.getInstance("SHA-256")
//        val key = sha256.digest(secretKey.toByteArray(StandardCharsets.US_ASCII))
//        val iv = secretIv.toByteArray(StandardCharsets.UTF_8)
//
//        // AES 복호화 설정
//        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
//        val aesKey = ByteArray(32)
//        System.arraycopy(key, 0, aesKey, 0, 32)
//        val secretKeySpec = SecretKeySpec(aesKey, "AES")
//        val ivParameterSpec = IvParameterSpec(iv)
//
//        // 복호화 실행
//        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
//        val cipherBytes = Base64.decode(cipherText, Base64.NO_WRAP)
//        val plainBytes = cipher.doFinal(cipherBytes)
//
//        // 문자열로 변환하여 반환
//        return String(plainBytes, StandardCharsets.UTF_8)
//    }

    // ------! 저장 할 때 파일 암호화  시작 !------
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
            salt = generateSalt() // salt 만들기
            sharedPreferences.edit { putString("file_encryption_salt", salt) }
        }

        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        val keyBytes = hashWithSHA512(deviceId, salt) // deviceId와 salt를 통해서 SHA-512 해싱 -> 이 해시를 key로 해서 AES-256 Encrypted함
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptFile(inputFile: File, outputFile: File, secretKey: SecretKey) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        FileOutputStream(outputFile).use { output ->
            output.write(iv)

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
            Log.e("SecureException", "decryptedFile: ${e.message}")
            null
        }
    }

    fun getFailedUploadDir(context: Context): File {
        val dir = File(context.filesDir, "failed_upload")
        Log.v("디버그", "디렉터리 경로: ${dir.absolutePath}")
        if (!dir.exists()) {
            val created = dir.mkdirs()
            Log.v("디버그", "디렉터리 생성됨? $created")
        } else {
            Log.v("디버그", "이미 디렉터리 있음")
        }
        return dir
    }
    fun deleteDirectory(directory: File): Boolean {
        if (directory.exists()) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        deleteDirectory(file)
                    } else {
                        file.delete()
                    }
                }
            }
        }
        return directory.delete()
    }
    fun saveEncryptedFileForRetry(context: Context, originalFile: File, fileName: String, secretKey: SecretKey) {
        val targetDir = getFailedUploadDir(context)
        val encryptedFile = File(targetDir, "$fileName.enc")
        Log.v("암호화", "$originalFile is encrypted.")
        encryptFile(originalFile, encryptedFile, secretKey)
        Log.v("암호화", "암호화된 파일 위치: ${encryptedFile.absolutePath}, 존재함? ${encryptedFile.exists()}")
    }

    fun decryptFileToTempFile(context: Context, fileName: String, secretKey: SecretKey): File? {
        val encryptedFile = File(getFailedUploadDir(context), "$fileName.enc")
        if (!encryptedFile.exists()) return null

        return try {
            val decryptedBytes = encryptedFile.inputStream().use { it.readBytes() }
                .let { decryptFile(it, secretKey) }

            decryptedBytes?.let {
                val outputFile = File(context.cacheDir, fileName) // 복호화된 임시 파일
                outputFile.writeBytes(it)
                outputFile
            }
        } catch (e: Exception) {
            Log.e("DecryptFile", "Failed to decrypt and save file: ${e.message}")
            null
        }
    }
    fun deleteAllEncryptedFiles(context: Context) {
        val failedDir = getFailedUploadDir(context)
        if (failedDir.exists() && failedDir.isDirectory) {
            failedDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".enc")) {
                    file.delete()
                }
            }
        }
    }
    // ------! 저장 할 때 파일 암호화 끝 !------

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
    fun saveEncryptedJwtToken(context: Context, jwt_jo : JSONObject?) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "loginEncryptPrefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit().apply {
            putString("jwt_jo", jwt_jo.toString())
            commit()
        }
    }

    fun getEncryptedAccessJwt(context: Context): String? {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            "loginEncryptPrefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val jsonString  = sharedPreferences.getString("jwt_jo", null)
        return try {
            if (jsonString != null) {
                val jsonObject = JSONObject(jsonString)
                jsonObject.getString("access_jwt")
            } else {
                null
            }
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
            Log.e("SecureException", "getEncncryptedAccessToken: ${e.message}")
            null
        }
    }

    fun getEncryptedRefreshJwt(context: Context): String? {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            "loginEncryptPrefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val jsonString  = sharedPreferences.getString("jwt_jo", null)
        return try {
            val jsonObject = jsonString?.let { JSONObject(it) }
            jsonObject?.getString("refresh_jwt")
        } catch (e: IndexOutOfBoundsException) {
            Log.e("SecureIndex", "refresh_jwt Error: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            Log.e("SecureIllegal", "refresh_jwt Error: ${e.message}")
            null
        } catch (e: IllegalStateException) {
            Log.e("SecureIllegal", "refresh_jwt Error: ${e.message}")
            null
        } catch (e: NullPointerException) {
            Log.e("SecureNull", "refresh_jwt Error: ${e.message}")
            null
        } catch (e: java.lang.Exception) {
            Log.e("SecureException", "refresh_jwt Error: ${e.message}")
            null
        }
    }

    fun getEncryptedJwtJo(context: Context): JSONObject? {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            "loginEncryptPrefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val jsonString  = sharedPreferences.getString("jwt_jo", null)
        Log.v("저장된jwtJo", "$jsonString")
        return try {
            if (!jsonString.isNullOrEmpty() && jsonString != "null") {
                jsonString.let { JSONObject(it) }
            } else {
                null
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("SecureIndex", "jwt_jo Error: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            Log.e("SecureIllegal", "jwt_jo Error: ${e.message}")
            null
        } catch (e: IllegalStateException) {
            Log.e("SecureIllegal", "jwt_jo Error: ${e.message}")
            null
        } catch (e: NullPointerException) {
            Log.e("SecureNull", "jwt_jo Error: ${e.message}")
            null
        } catch (e: java.lang.Exception) {
            Log.e("SecureException", "jwt_jo Error: ${e.message}")
            null
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

    // ------# 손상된 토큰 초기화 #------
    fun clearEncryptedToken(context : Context) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(context.getString(R.string.SECURE_KEY_ALIAS), null).apply()
    }

    fun isValidToken(jsonObject: JSONObject): Boolean {
        return jsonObject.has("access_jwt") &&
                jsonObject.has("refresh_jwt") &&
                !jsonObject.optString("access_jwt").isNullOrEmpty() &&
                !jsonObject.optString("refresh_jwt").isNullOrEmpty()
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
            }
            saveEncryptedJwtToken(activity, null)

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