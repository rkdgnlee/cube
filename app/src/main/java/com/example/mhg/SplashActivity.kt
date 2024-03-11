package com.example.mhg

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.common.KakaoSdk
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLoginState
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    private lateinit var firebaseAuth : FirebaseAuth
//    private lateinit var viewModel: UserViewModel
    private val PERMISSION_REQUEST_CODE = 5000

    @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_splash)

            binding = ActivitySplashBinding.inflate(layoutInflater)
            val t_userData = Singleton_t_user.getInstance(this)
            // ----/ server연결 /---
        // update 문 실행
//            val jsonObj = JSONObject()
//            jsonObj.put("user_sn", )
//            Log.e("JSON", "$jsonObj")
//            fetchJson(IP_ADDRESS, jsonObj.toString(), "PUT")

//         select 문 실행해보기
//            val jsonObj = JSONObject()
//            jsonObj.put("login_token", "")
//            fetchJson(IP_ADDRESS, jsonObj.toString(), "POST")

        // delete 문 실행해보기
//            val jsonObj3 = JSONObject()
//            jsonObj3.put("user_sn", 2)
//            fetchJson(IP_ADDRESS, jsonObj3.toString(), "DELETE")

            // ----- API 초기화 시작 -----
            NaverIdLoginSDK.initialize(this, "m5zFu7piZAAWFf4M4v0j", "ICzYhMzvrT", "Multi Home Gym")
            KakaoSdk.init(this, "3b8fabecdc76c31056605852a34ea729")
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
                val naverToken = getToken(this, "naverToken")
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
                            val JsonObj = JSONObject()
                            val idToken = NaverIdLoginSDK.getAccessToken().toString()
                            JsonObj.put("login_token", idToken)
                            fetchSELECTJson(getString(R.string.IP_ADDRESS), JsonObj.toString(), "1", applicationContext, idToken){
                                MainInit()
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
                            val idToken: String? = task.result.token
                            val JsonObj = JSONObject()
                            JsonObj.put("login_token", "$idToken")
                            if (idToken != null) {
                                fetchSELECTJson(getString(R.string.IP_ADDRESS), JsonObj.toString(), "1", applicationContext, idToken){
                                    MainInit()
                                }
                            }
                        }
                    }
                // ----- 구글 토큰 있음 끝 -----

                // ----- 카카오 토큰 있음 시작 -----
            } else if (AuthApiClient.instance.hasToken()) {
                val kakaoToken = getToken(this, "kakaoToken")
                val JsonObj = JSONObject()
                JsonObj.put("login_token", "$kakaoToken")
                if (kakaoToken != null) {
                    fetchSELECTJson(getString(R.string.IP_ADDRESS), JsonObj.toString(),"1", applicationContext, kakaoToken){
                        MainInit()
                    }
                }

            } else {
                IntroInit()
            } // 로그인 정보가 없을 경우
        }, 1500)


        // ----- 카카오 토큰 있음 시작 -----
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

    fun fetchSELECTJson(myUrl : String, json: String, category: String, context: Context, token: String, callback: () -> Unit){
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("$myUrl?category=$category&login_token=$token")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback  {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OKHTTP3", "Failed to execute request!")
            }
            override fun onResponse(call: Call, response: Response)  {
                val responseBody = response.body?.string()
                Log.e("OKHTTP3/SELECT", "Success to execute request!: $responseBody")
//                val jsonObj__ = responseBody?.let { JSONObject(it) }
//                val jsonDataArray = jsonObj__?.getJSONArray("aaData")
//                val jsonObj = jsonDataArray?.getJSONObject(0)
//                val t_userInstance = context.let { Singleton_t_user.getInstance(it) }
//                t_userInstance.jsonObject = jsonObj
//                Log.e("OKHTTP3>싱글톤", "${t_userInstance.jsonObject}")
                callback()
            }
        })
    }


}