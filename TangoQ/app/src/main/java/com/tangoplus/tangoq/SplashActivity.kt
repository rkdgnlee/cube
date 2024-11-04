package com.tangoplus.tangoq

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
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
import com.tangoplus.tangoq.broadcastReceiver.AlarmReceiver
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.databinding.ActivitySplashBinding
import com.tangoplus.tangoq.db.DeepLinkManager
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.db.SecurePreferencesManager.decryptData
import com.tangoplus.tangoq.db.SecurePreferencesManager.getEncryptedJwtToken
import com.tangoplus.tangoq.db.SecurePreferencesManager.loadEncryptedData
import com.tangoplus.tangoq.db.SecurePreferencesManager.saveEncryptedJwtToken
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.NetworkRecommendation.createRecommendProgram
import com.tangoplus.tangoq.`object`.NetworkUser.getUserBySdk
import com.tangoplus.tangoq.`object`.NetworkUser.getUserIdentifyJson
import com.tangoplus.tangoq.`object`.NetworkUser.storeUserInSingleton
import com.tangoplus.tangoq.`object`.SaveSingletonManager
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    lateinit var binding : ActivitySplashBinding
    private lateinit var firebaseAuth : FirebaseAuth
    private val PERMISSION_REQUEST_CODE = 5000
    private lateinit var singletonMeasure : Singleton_t_measure
    private lateinit var ssm : SaveSingletonManager

    override fun onPause() {
        super.onPause()

    }

    override fun onDestroy() {
        super.onDestroy()


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        Log.v("keyhash", Utility.getKeyHash(this))
//        val md = MeasureDatabase.getDatabase(this@SplashActivity)
//        val dao = md.measureDao()
//        val info = MeasureInfo(
//            user_uuid = "",
//            user_sn =  321,
//            user_name =  "123",
//            measure_date =  LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
//            elapsed_time = "",
//            measure_seq =  0
//        )
//        CoroutineScope(Dispatchers.IO).launch {
//            dao.insertInfo(info)
//        }
        // ------! API 초기화 시작 !------
        NaverIdLoginSDK.initialize(this, getString(R.string.naver_client_id), getString(R.string.naver_client_secret), "TangoQ")
        KakaoSdk.init(this, getString(R.string.kakao_client_id))
        firebaseAuth = Firebase.auth

        val googleUserExist = firebaseAuth.currentUser
        val naverTokenExist = NaverIdLoginSDK.getState()
        ssm = SaveSingletonManager(this@SplashActivity, this)
        // ------! API 초기화 끝 !------

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
                // ------! 푸쉬 알림 끝 !------

                // ------! 인 앱 알림 시작 !------
                AlarmReceiver()
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                val calander: Calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, 13)
                } // 오후 1시에 알림이 오게끔 돼 있음.

                val alarmManager = this.getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calander.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
                // ------! 인 앱 알림 끝 !------

                val userSingleton = Singleton_t_user.getInstance(this)

                // ------# 다크모드 및 설정 불러오기  #------
                val sharedPref = this@SplashActivity.getSharedPreferences("deviceSettings", Context.MODE_PRIVATE)
                val darkMode = sharedPref.getBoolean("darkMode", false)

                AppCompatDelegate.setDefaultNightMode(
                    if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )


                val Handler = Handler(Looper.getMainLooper())
                Handler.postDelayed({

                    // ------! 네이버 토큰 있음 시작 !------
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
                                    jsonObj.put("device_sn", 0)
                                    jsonObj.put("user_sn", 0)
                                    jsonObj.put("user_name",jsonBody?.optString("name"))
                                    jsonObj.put("gender", if (jsonBody?.optString("gender") == "M") "남자" else "여자")
                                    val naverMobile = jsonBody?.optString("mobile")?.replaceFirst("010", "+8210")
                                    jsonObj.put("mobile", naverMobile)
                                    jsonObj.put("email",jsonBody?.optString("email"))
                                    jsonObj.put("birthday",jsonBody?.optString("birthyear")+"-"+jsonBody?.optString("birthday"))
                                    jsonObj.put("naver_login_id", jsonBody?.optString("id"))
                                    jsonObj.put("social_account", "naver")

                                    getUserBySdk(getString(R.string.API_user), jsonObj, this@SplashActivity) { jo ->
                                        if (jo != null) {
                                            storeUserInSingleton(this@SplashActivity, jo)
                                            Log.e("Spl네이버>싱글톤", "${Singleton_t_user.getInstance(this@SplashActivity).jsonObject}")
                                        }
                                        val userUUID = Singleton_t_user.getInstance(this@SplashActivity).jsonObject?.optString("user_uuid")!!
                                        val userInfoSn =  Singleton_t_user.getInstance(this@SplashActivity).jsonObject?.optString("sn")?.toInt()!!
                                        ssm.getMeasures(userUUID, userInfoSn, CoroutineScope(Dispatchers.IO)) {
                                            navigateDeepLink()
                                        }

                                    }
                                }
                            }
                        })
                        // ------! 네이버 토큰 있음 끝 !------

                        // ------! 구글 토큰 있음 시작 !------
                    } else if (googleUserExist != null) {
                        val user = FirebaseAuth.getInstance().currentUser
                        user!!.getIdToken(true)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val jsonObj = JSONObject()
                                    jsonObj.put("device_sn", 0)
                                    jsonObj.put("user_sn", 0)
                                    jsonObj.put("google_login_id", user.uid)
                                    jsonObj.put("user_name", user.displayName.toString())
                                    jsonObj.put("email", user.email.toString())
                                    jsonObj.put("google_login_id", user.uid)
                                    jsonObj.put("mobile", user.phoneNumber)
                                    jsonObj.put("social_account", "google")
                                    getUserBySdk(getString(R.string.API_user), jsonObj, this@SplashActivity) { jo ->
                                        if (jo != null) {
                                            storeUserInSingleton(this, jo)
                                            Log.e("Spl구글>싱글톤", "${Singleton_t_user.getInstance(this@SplashActivity).jsonObject}")
                                        }
                                        val userUUID = Singleton_t_user.getInstance(this@SplashActivity).jsonObject?.optString("user_uuid")!!
                                        val userInfoSn =  Singleton_t_user.getInstance(this@SplashActivity).jsonObject?.optString("sn")?.toInt()!!
                                        ssm.getMeasures(userUUID, userInfoSn, CoroutineScope(Dispatchers.IO)) {
                                            navigateDeepLink()
                                        }
                                    }
                                }
                            }
                        // ------! 구글 토큰 있음 끝 !------

                        // ------! 카카오 토큰 있음 시작 !------
                    } else if (AuthApiClient.instance.hasToken()) {
                        UserApiClient.instance.me { user, error ->
                            if (error != null) {
                                Log.e("kakaoError", "사용자 정보 요청 실패", error)
                            }
                            else if (user != null) {
                                Log.i(ContentValues.TAG, "사용자 정보 요청 성공" + "\n회원번호: ${user.id}")
                                val jsonObj = JSONObject()
                                val kakaoMobile = user.kakaoAccount?.phoneNumber.toString().replaceFirst("+82 10", "+8210")
                                jsonObj.put("user_name" , user.kakaoAccount?.name.toString())
                                val kakaoUserGender = if (user.kakaoAccount?.gender.toString()== "M")  "남자" else "여자"
                                jsonObj.put("device_sn", 0)
                                jsonObj.put("user_sn", 0)
                                jsonObj.put("gender", kakaoUserGender)
                                jsonObj.put("mobile", kakaoMobile)
                                jsonObj.put("email", user.kakaoAccount?.email.toString())
                                jsonObj.put("birthday", user.kakaoAccount?.birthyear.toString() + "-" + user.kakaoAccount?.birthday?.substring(0..1) + "-" + user.kakaoAccount?.birthday?.substring(2))
                                jsonObj.put("kakao_login_id" , user.id.toString())
                                jsonObj.put("social_account", "kakao")
                                getUserBySdk(getString(R.string.API_user), jsonObj, this@SplashActivity) { jo ->
                                    if (jo != null) {
                                        storeUserInSingleton(this, jo)
                                        Log.e("Spl>싱글톤", "${Singleton_t_user.getInstance(this@SplashActivity).jsonObject}")
                                    }
                                    val userUUID = Singleton_t_user.getInstance(this@SplashActivity).jsonObject?.optString("user_uuid")!!
                                    val userInfoSn =  Singleton_t_user.getInstance(this@SplashActivity).jsonObject?.optString("sn")?.toInt()!!
                                    ssm.getMeasures(userUUID, userInfoSn, CoroutineScope(Dispatchers.IO)) {
                                        navigateDeepLink()
                                    }
                                }
                            }
                        }
                    }
                    else if (getEncryptedJwtToken(this@SplashActivity) != null && loadEncryptedData(this@SplashActivity, getString(R.string.SECURE_KEY_ALIAS)) != null) {

                        // ------! 자체 로그인 !------
                        val jsonObj = decryptData(getString(R.string.SECURE_KEY_ALIAS),
                            loadEncryptedData(this@SplashActivity, getString(R.string.SECURE_KEY_ALIAS)).toString()
                        )
                        lifecycleScope.launch {
                            getUserIdentifyJson(getString(R.string.API_user), jsonObj, this@SplashActivity) { jo ->
                                if (jo != null) {
                                    storeUserInSingleton(this@SplashActivity, jo)
                                    Log.v("자체로그인>싱글톤", "${Singleton_t_user.getInstance(this@SplashActivity).jsonObject}")
                                    val userUUID = Singleton_t_user.getInstance(this@SplashActivity).jsonObject?.optString("user_uuid")!!
                                    val userInfoSn =  Singleton_t_user.getInstance(this@SplashActivity).jsonObject?.optString("sn")?.toInt()!!

                                    ssm.getMeasures(userUUID, userInfoSn, CoroutineScope(Dispatchers.IO)) {

                                        navigateDeepLink()
                                    }
                                }
                            }
                        }
                    }
                    else {
                        // 로그인 정보가 없을 경우
                        introInit()
                    }
                }, 1500)

                // ------! 카카오 토큰 있음 끝 !------
                // ------! 화면 경로 설정 끝 !------
            }
            false -> {
                Toast.makeText(this, "인터넷 연결이 필요합니다", Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, IntroActivity::class.java)
                    startActivity(intent)
                    finish()
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
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(descriptionText , name, importance)
        mChannel.description = descriptionText

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
    }

    private fun mainInit() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun introInit() {
        val intent = Intent(this@SplashActivity, IntroActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateDeepLink() {
        val data: Uri? = intent?.data
        Log.v("splash>deeplink", "data: ${data}")
        if (data != null) {
            // 딥링크 처리
            DeepLinkManager.handleDeepLink(this, data)
        } else {
            mainInit()
        }
    }
}