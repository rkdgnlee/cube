package com.tangoplus.tangoq

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.common.KakaoSdk
import com.navercorp.nid.NaverIdLoginSDK
import com.tangoplus.tangoq.broadcastReceiver.AlarmReceiver
import com.tangoplus.tangoq.databinding.ActivitySplashBinding
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.api.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.api.NetworkUser.storeUserInSingleton
import com.tangoplus.tangoq.api.NetworkUser.trySelfLogin
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.function.DeepLinkManager
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedJwtJo
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedRefreshJwt
import com.tangoplus.tangoq.function.SecurePreferencesManager.isValidToken
import com.tangoplus.tangoq.function.SecurePreferencesManager.logout
import com.tangoplus.tangoq.function.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    lateinit var binding : ActivitySplashBinding
    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var ssm : SaveSingletonManager
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        // 일정 시간이 지나도 응답이 없으면 IntroActivity로 이동
        introInit()
    }

//    private var integrityTokenProvider: StandardIntegrityTokenProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SoundManager.init(this@SplashActivity)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//        // ------! integrity API 시작 !------
//        val standardIntegrityManager = IntegrityManagerFactory.createStandard(applicationContext)
//        val cloudProjectNumber = 196772683133
//        standardIntegrityManager.prepareIntegrityToken(
//            PrepareIntegrityTokenRequest.builder()
//                .setCloudProjectNumber(cloudProjectNumber)
//                .build()
//        )
//            .addOnSuccessListener { tokenProvider ->
//                integrityTokenProvider = tokenProvider
//                val requestHash = getString(R.string.integrityHashKey)
//                val integrityTokenResponse: Task<StandardIntegrityToken> =
//                    integrityTokenProvider!!.request(
//                        StandardIntegrityTokenRequest.builder()
//                            .setRequestHash(requestHash)
//                            .build()
//                    )
//                integrityTokenResponse
//                    .addOnSuccessListener { response ->
////                        response.showDialog(this@SplashActivity, 1)
//                        Log.v("integrityToken", response.token())
//                        playIntegrityVerify(getString(R.string.API_integrity), response.token()) { payload ->
//                            verifyIntegrityResult(payload)
//                        }
//                    }
//                    .addOnFailureListener { exception ->
//                        Log.e("TokenException", "${exception.message}")
//                    }
//            }
//            .addOnFailureListener { exception -> Log.e("integrityError", "$exception") }
//        // ------! integrity API 끝 !------

        // ------! API 초기화 시작 !------
        NaverIdLoginSDK.initialize(this, getString(R.string.naver_client_id), getString(R.string.naver_client_secret), "TangoQ")
        KakaoSdk.init(this, getString(R.string.kakao_client_id))
        firebaseAuth = Firebase.auth

        ssm = SaveSingletonManager(this@SplashActivity, this)
        // ------! API 초기화 끝 !------

        // ------! 인터넷 연결 확인 !------
        when (isNetworkAvailable(this)) {
            true -> {
                // ------! 푸쉬 알림 시작 !-----
                AlarmReceiver()
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("firebaseMessaging", "FETCHING FCM registration token failed : ${task.exception?.message}")
                        return@OnCompleteListener
                    }
                    val token = task.result.toString()
                    Log.e("메시지토큰", "fcm token :: $token")
                })
                createNotificationChannel()
                // ------! 푸쉬 알림 끝 !------

                // ------! 인 앱 알림 시작 !------

                cacheDir.deleteRecursively()
                // ------# 다크모드 및 설정 불러오기  #------
                val sharedPref = this@SplashActivity.getSharedPreferences("deviceSettings", Context.MODE_PRIVATE)
                val darkMode = sharedPref.getBoolean("darkMode", false)

                AppCompatDelegate.setDefaultNightMode(
                    if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
                val encryptedJwtJo = getEncryptedJwtJo(this@SplashActivity)
                Log.v("encryptedJwtJo", "${encryptedJwtJo?.length()}, ${encryptedJwtJo?.let {
                    isValidToken(
                        it
                    )
                }}")
                if (encryptedJwtJo != null && isValidToken(encryptedJwtJo)) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        // TODO trySelfLogin에서 버전에 대해서 알려주고 여기서 로그인 전 해당 데이터를 토대로 앱이 업데이트가 필수적이라면 앱스토어로 경로 이동해줘야함.
                        trySelfLogin(getString(R.string.API_user), this@SplashActivity, getEncryptedRefreshJwt(this@SplashActivity)) { jo ->
                            if (jo != null) {
                                storeUserInSingleton(this@SplashActivity, jo)
    //                                Log.v("Spl>selfLogin", "자체 자동 로그인 성공: ${jo} ${Singleton_t_user.getInstance(this@SplashActivity).jsonObject}")
                                val userUUID = Singleton_t_user.getInstance(this@SplashActivity).jsonObject?.optString("user_uuid") ?: ""
                                val userInfoSn =  Singleton_t_user.getInstance(this@SplashActivity).jsonObject?.optString("sn")?.toInt() ?: 0
                                ssm.getMeasures(userUUID, userInfoSn, CoroutineScope(Dispatchers.IO)) {
                                    navigateDeepLink()
                                }
                            } else {
                                Log.v("invalidRefresh", "logout invalidRefreshToken.")
                                logout(this@SplashActivity, 0)
                            }
                        }
                    }
                } // 로그인 정보가 없을 경우
                else {
                    Log.v("invalidRefresh", "intro Init")
                    logout(this@SplashActivity, 0)
                }

                timeoutHandler.postDelayed(timeoutRunnable, 30000)
                // ------! 카카오 토큰 있음 끝 !------
                // ------! 화면 경로 설정 끝 !------
            }
            // ------# 인터넷 연결이 없을 때 #------
            false -> {
                Toast.makeText(this, "인터넷 연결이 필요합니다", Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, IntroActivity::class.java)
                    startActivity(intent)
                    finish()
                    logout(this@SplashActivity, 0)
                }, 4000
                )
            }
        }
    }
    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(name, descriptionText , importance)
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
        Log.v("splash>deeplink", "data: $data")
        if (data != null) {
            // 딥링크 처리
            DeepLinkManager.handleDeepLink(this, data)
        } else {
            mainInit()
        }
    }

//    private fun verifyIntegrityResult(jo: JSONObject) : Boolean {
//        val requestDetails = jo.getJSONObject("requestDetails")
//
//        val requestPackageName = requestDetails.getString("requestPackageName")
//        val requestHash = requestDetails.getString("requestHash")
//        val timestampMillis = requestDetails.getLong("timestampMillis")
//
//        val requestIntegrity = jo.getJSONObject("appIntegrity")
//        val appRecognitionVerdict = requestIntegrity.optString("appRecognitionVerdict")
//
//        val deviceIntegrity = jo.getJSONObject("deviceIntegrity")
//        val deviceRecognitionVerdict = if (deviceIntegrity.has("deviceRecognitionVerdict")) {
//            deviceIntegrity.getJSONArray("deviceRecognitionVerdict").toString()
//        } else {
//            ""
//        }
//        val accountDetails = jo.getJSONObject("accountDetails")
//        val appLicensingVerdict = accountDetails.getString("appLicensingVerdict")
//
//        val environmentDetails = jo.getJSONObject("environmentDetails")
//        val appAccessRiskVerdict = environmentDetails.getJSONObject("appAccessRiskVerdict")
//        // 패키지나 hash키가 다를 때
//        if (!requestPackageName.equals("com.tangoplus.tangoq") || !requestHash.equals(getString(R.string.integrityHashKey))) {
//            return false
//        }
//
//        // 앱 무결성 에 대한 값들 찾기
//        if (!appRecognitionVerdict.equals("PLAY_RECOGNIZSED")) {
//            return false
//        }
//
//        // 기기 무결성
//        if (!deviceRecognitionVerdict.contains("MEETS_DEVICE_INTEGRITY")) {
//            return false
//        }
//
//        if (!appLicensingVerdict.equals("LICENSED")) {
//            return false
//        }
//
//        if (appAccessRiskVerdict.has("appsDetected")) {
//            val appsDetected = appAccessRiskVerdict.getJSONArray("appsDetected").toString()
//            if (!appsDetected.contains("CAPTURING") && !appsDetected.contains("CONTROLLING")) {
//                return false
//            }
//        }
//        return true
//    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler.removeCallbacks(timeoutRunnable)
    }
}