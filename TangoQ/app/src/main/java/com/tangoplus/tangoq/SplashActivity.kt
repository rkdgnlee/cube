package com.tangoplus.tangoq

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLoginState
import com.tangoplus.tangoq.broadcastReceiver.AlarmReceiver
import com.tangoplus.tangoq.`object`.NetworkUser
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.databinding.ActivitySplashBinding
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.NetworkUser.getUserSELECTJson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Calendar

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    lateinit var binding : ActivitySplashBinding
    private lateinit var firebaseAuth : FirebaseAuth
    private val PERMISSION_REQUEST_CODE = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.v("keyhash", Utility.getKeyHash(this))

        // ----- API 초기화 시작 -----
        NaverIdLoginSDK.initialize(this, getString(R.string.naver_client_id), getString(R.string.naver_client_secret), "TangoQ")
        KakaoSdk.init(this, getString(R.string.kakao_client_id))
        firebaseAuth = Firebase.auth

        val googleUserExist = firebaseAuth.currentUser
        val naverTokenExist = NaverIdLoginSDK.getState()
        // ----- API 초기화 끝 ------

        // ------! 인터넷 연결 확인 !------
        when (isNetworkAvailable(this)) {
            true -> {
                // ------! 푸쉬 알림 시작 !-----
                permissionCheck()
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("TAG", "FETCHING FCM registration token failed", task.exception)
                        return@OnCompleteListener
                    }
                    val token = task.result.toString()
                    Log.e("메시지토큰", "fcm token :: $token")
                })
                createNotificationChannel()

                // ------! 푸쉬 알림 끝 !-----

                // ----- 인 앱 알림 시작 -----
                AlarmReceiver()
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                val calander: Calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, 17)
                }
                val alarmManager = this.getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calander.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
                // ----- 인 앱 알림 끝 -----

                val t_userData = Singleton_t_user.getInstance(this)

                // -----! 다크모드 및 설정 불러오기 시작 !-----
                val sharedPref = this@SplashActivity.getSharedPreferences("deviceSettings", Context.MODE_PRIVATE)
                val darkMode = sharedPref.getBoolean("darkMode", false)


                AppCompatDelegate.setDefaultNightMode(
                    if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
                // -----! 다크모드 및 설정 불러오기 끝 !-----



                val Handler = Handler(Looper.getMainLooper())
                Handler.postDelayed({
                    // ----- 네이버 토큰 있음 시작 -----
                    if (naverTokenExist == NidOAuthLoginState.OK) {
                        Log.e("네이버 로그인", "$naverTokenExist")
                        val naverToken = NaverIdLoginSDK.getAccessToken()
                        val url = "https://openapi.naver.com/v1/nid/me"
                        val request = Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Bearer $naverToken")
                            .build()
                        val client = OkHttpClient()
                        client.newCall(request).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) { }
                            override fun onResponse(call: Call, response: Response) {
                                if (response.isSuccessful) {
                                    val jsonBody = response.body?.string()?.let { JSONObject(it) }?.getJSONObject("response")
                                    val jsonObj = JSONObject()
                                    jsonObj.put("user_name",jsonBody?.optString("name"))
                                    jsonObj.put("user_email",jsonBody?.optString("email"))
                                    jsonObj.put("user_birthday",jsonBody?.optString("birthyear")+"-"+jsonBody?.optString("birthday"))
                                    jsonObj.put("user_gender", if (jsonBody?.optString("gender") == "M") "남자" else "여자")
                                    val naverMobile = jsonBody?.optString("mobile")?.replaceFirst("010", "+8210")
                                    jsonObj.put("naver_login_id", jsonBody?.optString("id"))
                                    jsonObj.put("user_mobile", naverMobile)
                                    // -----! 전화번호 변환 !-----
                                    val encodedNaverEmail = URLEncoder.encode(jsonObj.getString("user_email"), "UTF-8")
                                    if (naverMobile != null) {
                                        getUserSELECTJson(getString(R.string.IP_ADDRESS_t_user), encodedNaverEmail) { jsonObject ->
                                            if (jsonObject != null) {
                                                NetworkUser.StoreUserInSingleton(this@SplashActivity, jsonObject)
                                                Log.e("Spl네이버>싱글톤", "${Singleton_t_user.getInstance(this@SplashActivity).jsonObject}")
                                            }
                                            MainInit()
                                        }
                                    }
                                }
                            }
                        })
                        // ----- 네이버 토큰 있음 끝 -----

                        // ----- 구글 토큰 있음 시작 -----
                    } else if (googleUserExist != null) {
                        val user = FirebaseAuth.getInstance().currentUser
                        user!!.getIdToken(true)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val JsonObj = JSONObject()
                                    JsonObj.put("google_login_id", user.uid)
                                    getUserSELECTJson(getString(R.string.IP_ADDRESS_t_user), JsonObj.getString("google_login_id")) {jsonObject ->
                                        if (jsonObject != null) {
                                            NetworkUser.StoreUserInSingleton(this, jsonObject)
                                            Log.e("Spl구글>싱글톤", "${Singleton_t_user.getInstance(this@SplashActivity).jsonObject}")
                                        }
                                        MainInit()
                                    }
                                }
                            }
                        // ----- 구글 토큰 있음 끝 -----

                        // ----- 카카오 토큰 있음 시작 -----
                    } else if (AuthApiClient.instance.hasToken()) {
                        UserApiClient.instance.me { user, error ->
                            if (error != null) {
                                Log.e(ContentValues.TAG, "사용자 정보 요청 실패", error)
                            }
                            else if (user != null) {
                                Log.i(ContentValues.TAG, "사용자 정보 요청 성공" + "\n회원번호: ${user.id}")
                                val JsonObj = JSONObject()
                                val kakaoMobile = user.kakaoAccount?.phoneNumber.toString().replaceFirst("+82 10", "+8210")
                                JsonObj.put("user_name" , user.kakaoAccount?.name.toString())
                                val kakaoUserGender = if (user.kakaoAccount?.gender.toString()== "M") {
                                    "남자"
                                } else {
                                    "여자"
                                }
                                JsonObj.put("user_gender", kakaoUserGender)
                                JsonObj.put("user_mobile", kakaoMobile)
                                JsonObj.put("user_email", user.kakaoAccount?.email.toString())
                                JsonObj.put("user_birthday", user.kakaoAccount?.birthyear.toString() + "-" + user.kakaoAccount?.birthday?.substring(0..1) + "-" + user.kakaoAccount?.birthday?.substring(2))
                                JsonObj.put("kakao_login_id" , user.id.toString())
                                Log.w("Spl카카오회원가입", JsonObj.getString("user_email"))


                                val encodedKakaoEmail = URLEncoder.encode(JsonObj.getString("user_email"), "UTF-8")

                                getUserSELECTJson(getString(R.string.IP_ADDRESS_t_user), encodedKakaoEmail) {jsonObject ->
                                    if (jsonObject != null) {
                                        NetworkUser.StoreUserInSingleton(this, jsonObject)
                                        Log.e("Spl카카오>싱글톤", "${Singleton_t_user.getInstance(this).jsonObject}")
                                    }
                                    MainInit()
                                }
                            }
                        }
                    }
                    else if (getEncryptedJwtToken(this@SplashActivity) != null) {
                        val token = getEncryptedJwtToken(this@SplashActivity)
                        //TODO 자체 로그인 토큰 존재 확인해야함 ! -> token으로 값을 다 받아올건지. token으로 select하는 api가 필요함.
                    }
                    else {
                        // 로그인 정보가 없을 경우
                        IntroInit()
                    }
                }, 1500)



                // ----- 카카오 토큰 있음 끝 -----
                // ---- 화면 경로 설정 끝 ----
            }
            false -> {
                Toast.makeText(this, "인터넷 연결이 필요합니다", Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, IntroActivity::class.java)
                    startActivity(intent)
                }, 4000
                )
            }
        }
    }

    private fun permissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    private fun createNotificationChannel() {
        // Create the NotificationChannel.
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel("5000", name, importance)
        mChannel.description = descriptionText
        // 채널을 등록해야 알림을 받을 수 있음
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)

    }
    private fun MainInit() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun IntroInit() {
        val intent = Intent(this@SplashActivity, IntroActivity::class.java)
        startActivity(intent)
        finish()
    }

    // 토큰 저장
    private fun saveEncryptedJwtToken(context: Context, token: String, expirationTime: Long) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences : SharedPreferences = EncryptedSharedPreferences.create(
            "loginEncryptPrefs", masterKeyAlias, context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val editor : SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("jwt_token", token)
        editor.putLong("token_expiration_time", expirationTime)
        editor.apply()
    }

    // 토큰 로드
    private fun getEncryptedJwtToken(context: Context): String? {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            "loginEncryptPrefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = sharedPreferences.getString("jwt_token", null)
        val expirationTime = sharedPreferences.getLong("token_expiration_time", 0)
        if (token != null && System.currentTimeMillis() > expirationTime) {
            Log.v("JWT", "Token has expired")
            return null
        }
        return token
    }

    // 토큰 갱신
    private fun refreshJwtToken(context: Context): String? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://yourserver.com/api/token/refresh")
            .header("Authorization", "Bearer " + getEncryptedJwtToken(context)) // 만료된 토큰을 보내 새 토큰을 요청
            .build()

        client.newCall(request).execute().use { response ->
            return if (response.isSuccessful) {
                val newToken = response.body?.string()
                val newExpirationTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24시간 유효기간
                if (newToken != null) {
                    saveEncryptedJwtToken(context, newToken, newExpirationTime)
                }
                newToken
            } else {
                null
            }
        }
    }
}