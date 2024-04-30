package com.tangoplus.tangoq

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLoginState
import com.tangoplus.tangoq.BroadcastReceiver.AlarmReceiver
import com.tangoplus.tangoq.Object.NetworkUserService
import com.tangoplus.tangoq.Object.Singleton_t_user
import com.tangoplus.tangoq.databinding.ActivitySplashBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Calendar

class SplashActivity : AppCompatActivity() {
    lateinit var binding : ActivitySplashBinding
    private lateinit var firebaseAuth : FirebaseAuth
    private val PERMISSION_REQUEST_CODE = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // ----- API 초기화 시작 -----
        NaverIdLoginSDK.initialize(this, getString(R.string.naver_client_id), getString(R.string.naver_client_secret), "TangoQ")
        KakaoSdk.init(this, getString(R.string.kakao_client_id))
        firebaseAuth = Firebase.auth

        val googleUserExist = firebaseAuth.currentUser
        val naverTokenExist = NaverIdLoginSDK.getState()
        // ----- API 초기화 끝 ------

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
                            val encodedNaverMobile = URLEncoder.encode(naverMobile, "UTF-8")
                            if (naverMobile != null) {
                                fetchSELECTJson(getString(R.string.IP_ADDRESS_t_user), encodedNaverMobile, false) { jsonObject ->
                                    if (jsonObject != null) {
                                        NetworkUserService.StoreUserInSingleton(this@SplashActivity, jsonObject)
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
                            fetchSELECTJson(getString(R.string.IP_ADDRESS_t_user), JsonObj.getString("google_login_id"), true) {jsonObject ->
                                if (jsonObject != null) {
                                    NetworkUserService.StoreUserInSingleton(this, jsonObject)
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
                        Log.w("${ContentValues.TAG}, Spl카카오회원가입", JsonObj.getString("user_mobile"))


                        val encodedKakaoMobile = URLEncoder.encode(kakaoMobile, "UTF-8")

                        fetchSELECTJson(getString(R.string.IP_ADDRESS_t_user), encodedKakaoMobile, false) {jsonObject ->
                            if (jsonObject != null) {
                                NetworkUserService.StoreUserInSingleton(this, jsonObject)
                                Log.e("Spl카카오>싱글톤", "${Singleton_t_user.getInstance(this).jsonObject}")
                            }
                            MainInit()
                        }
                    }
                }
            } else {
                // 로그인 정보가 없을 경우
                IntroInit()
            }
        }, 1500)
        // ----- 카카오 토큰 있음 끝 -----
        // ---- 화면 경로 설정 끝 ----

    }
    fun fetchSELECTJson(
        myUrl: String, identifier:String, isGoogleId: Boolean,
        callback: (JSONObject?) -> Unit
    ){
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(if (isGoogleId) "${myUrl}read.php?google_login_id=$identifier" else "${myUrl}read.php?user_mobile=$identifier")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback  {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OKHTTP3", "Failed to execute request!")
            }
            override fun onResponse(call: Call, response: Response)  {
                val responseBody = response.body?.string()
                Log.e("${ContentValues.TAG}, 응답성공", "$responseBody")
                val jsonObj__ = responseBody?.let { JSONObject(it) }
                callback(jsonObj__)
            }
        })
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel("5000", name, importance)
            mChannel.description = descriptionText
            // 채널을 등록해야 알림을 받을 수 있음
            // or other notification behaviors after this.
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)

        }
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
}