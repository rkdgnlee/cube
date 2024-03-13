package com.example.mhg

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.mhg.databinding.ActivitySplashBinding
import com.example.mhg.`object`.Singleton_t_user
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    private lateinit var firebaseAuth : FirebaseAuth
    private val PERMISSION_REQUEST_CODE = 5000
    // TODO 매니저님이 짜준 로직 대로, JSON, METHOD 바꿔야함
    @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_splash)

            binding = ActivitySplashBinding.inflate(layoutInflater)
            val t_userData = Singleton_t_user.getInstance(this)


            // ----- API 초기화 시작 -----
            NaverIdLoginSDK.initialize(this, getString(R.string.naver_client_id), getString(R.string.naver_client_secret), "Multi Home Gym")
            KakaoSdk.init(this, getString(R.string.kakao_client_id))
            firebaseAuth = Firebase.auth

            val googleUserExist = firebaseAuth.currentUser
            val naverTokenExist = NaverIdLoginSDK.getState()
            // ----- API 초기화 끝 ------

            // ----- 푸시 알림 시작 -----
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


        // ----- 푸시 알림 끝 -----


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

        // ---- 화면 경로 설정 시작 ----
        val Handler = Handler(Looper.getMainLooper())
        Handler.postDelayed({

            // ----- 네이버 토큰 있음 시작 -----
            if (naverTokenExist == NidOAuthLoginState.OK) {
                Log.e("네이버 로그인", "$naverTokenExist")
                val naverToken = NaverIdLoginSDK.getAccessToken()
                Log.e("네이버토큰", "$naverToken")
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
                            val responseBody = response.body?.string()
                            val jsonObj__ = responseBody?.let { JSONObject(it) }
                            val jsonObj = jsonObj__?.getJSONObject("response")
                            val naver_mobile = jsonObj?.getString("mobile")?.replaceFirst("010", "+82 10")
                            if (naver_mobile != null) {
                                fetchSELECTJson(getString(R.string.IP_ADDRESS_T_USER), naver_mobile) {
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
                            JsonObj.put("user_id", user.uid)
                            fetchSELECTJson(getString(R.string.IP_ADDRESS_T_USER), JsonObj.getString("user_id")) {
                                MainInit()
                            }
                        }
                    }
                // ----- 구글 토큰 있음 끝 -----

                // ----- 카카오 토큰 있음 시작 -----
            } else if (AuthApiClient.instance.hasToken()) {
                UserApiClient.instance.me { user, error ->
                    if (error != null) {
                        Log.e(TAG, "사용자 정보 요청 실패", error)
                    }
                    else if (user != null) {
                        Log.i(TAG, "사용자 정보 요청 성공" + "\n회원번호: ${user.id}")
                        val JsonObj = JSONObject()
                        JsonObj.put("user_id", user.id)
                        fetchSELECTJson(getString(R.string.IP_ADDRESS_T_USER), JsonObj.getString("user_id")) {
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

    private fun MainInit() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
     private fun IntroInit() {
        val intent = Intent(this, IntroActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getToken(context: Context, key:String): String? {
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "encryptedShared", masterKeyAlias, context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val storedValue = sharedPreferences.getString(key, "")
        return storedValue
    }
// ----- 알림에 대한 함수들 시작 -----
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(applicationContext, "Permission is denied", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "Permission is granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    // ----- 알림에 대한 함수들 끝 -----

    fun fetchSELECTJson(myUrl : String, user_id:String, callback: () -> Unit){
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php?user_id=$user_id")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback  {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OKHTTP3", "Failed to execute request!")
            }
            override fun onResponse(call: Call, response: Response)  {
                val responseBody = response.body?.string()
                Log.e("OKHTTP3", "Success to execute request!: $responseBody")
                val jsonObj__ = responseBody?.let { JSONObject(it) }
                val jsonObj = jsonObj__?.optJSONObject("data")
                val t_userInstance =  Singleton_t_user.getInstance(baseContext)
                t_userInstance.jsonObject = jsonObj
                Log.e("OKHTTP3>싱글톤", "${t_userInstance.jsonObject}")
                callback()
            }
        })
    }

}