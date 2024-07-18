package com.tangoplus.tangoq

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.security.crypto.EncryptedSharedPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.AuthErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.NidOAuthLoginState
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import com.tangoplus.tangoq.dialog.LoginDialogFragment
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.`object`.CommonDefines.TAG
import com.tangoplus.tangoq.`object`.NetworkUser.storeUserInSingleton
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.data.BannerViewModel
import com.tangoplus.tangoq.data.SignInViewModel
import com.tangoplus.tangoq.databinding.ActivityIntroBinding
import com.tangoplus.tangoq.db.SecurePreferencesManager
import com.tangoplus.tangoq.db.SecurePreferencesManager.getServerToken
import com.tangoplus.tangoq.db.SecurePreferencesManager.saveServerToken
import com.tangoplus.tangoq.dialog.AgreementBottomSheetDialogFragment
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.NetworkUser.getUserBySdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception
import java.net.URLEncoder


class IntroActivity : AppCompatActivity() {
    lateinit var binding : ActivityIntroBinding
    val viewModel : BannerViewModel by viewModels()
    val sViewModel  : SignInViewModel by viewModels()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var launcher: ActivityResultLauncher<Intent>
//    private var bannerPosition = Int.MAX_VALUE/2
//    private var bannerHandler = HomeBannerHandler()
    private val intervalTime = 2200.toLong()
    private lateinit var securePref : EncryptedSharedPreferences

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ------! token 저장할  securedPref init !------
        securePref = SecurePreferencesManager.getInstance(this@IntroActivity)

        when (isNetworkAvailable(this)) {
            true -> {

            }
            false -> {
                binding.btnIntroLogin.isEnabled = false
                binding.btnIntroSignIn.isEnabled = false
                Toast.makeText(this, "인터넷 연결 후 앱을 다시 실행해주세요", Toast.LENGTH_LONG).show()
            }
        }


        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            firebaseAuth = FirebaseAuth.getInstance()
            launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                Log.v("google", "1, resultCode: ${result.resultCode}입니다.")
                if (result.resultCode == RESULT_OK) {
                    Log.v("google", "2, resultCode: ${result.resultCode}입니다.")
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        Log.v("google", "3, resultCode: ${result.resultCode}입니다.")
                        task.getResult(ApiException::class.java)?.let { account ->
                            val tokenId = account.idToken
                            if (tokenId != null && tokenId != "") {
                                val credential: AuthCredential = GoogleAuthProvider.getCredential(account.idToken, null)
                                firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
                                        if (firebaseAuth.currentUser != null) {

                                            // ---- Google 토큰에서 가져오기 시작 ----
                                            val user: FirebaseUser = firebaseAuth.currentUser!!
                                            Log.v("user", "${user.phoneNumber}")
                                            Log.v("user", user.providerId)
                                            // ----- GOOGLE API: 전화번호 담으러 가기(signin) 시작 -----
                                            val jsonObj = JSONObject()
                                            jsonObj.put("user_name", user.displayName.toString())
                                            jsonObj.put("user_email", user.email.toString())
                                            jsonObj.put("google_login_id", user.uid)
//                                            jsonObj.put("google_id_token", tokenId) // 토큰 값
                                            jsonObj.put("user_mobile", user.phoneNumber)
                                            jsonObj.put("social_account", "google")
//                                            val encodedUserEmail = URLEncoder.encode(jsonObj.getString("user_email"), "UTF-8")
                                            Log.v("jsonObj", "$jsonObj")
                                            getUserBySdk(getString(R.string.IP_ADDRESS_t_user), jsonObj) { jo ->
                                                if (jo != null) {
                                                    when (jo.optString("status")) {
                                                        "200" -> { saveTokenAndIdentifyUser(jo, jsonObj, 200) }
                                                        "201" -> { saveTokenAndIdentifyUser(jo, jsonObj, 201) }
                                                        else -> { Log.v("responseCodeError", "response: $jo")}
                                                    }
                                                }
                                            }
                                        // ------! GOOGLE API에서 DB에 넣는 공간 끝 !------

                                        // ------! Google 토큰에서 가져오기 끝 !------
                                        }
                                    }
                            }
                        } ?: throw Exception()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            val intent = Intent(this@IntroActivity, SignInActivity::class.java)
            startActivity(intent)
            ActivityCompat.finishAffinity(this@IntroActivity)
        } // ---- firebase 초기화 및 Google Login API 연동 끝 ----

        // ---- 구글 로그인 시작 ----
        binding.ibtnGoogleLogin.setOnClickListener {

            CoroutineScope(Dispatchers.IO).launch {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.firebase_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(this@IntroActivity, gso)
                val signInIntent: Intent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }
        } // ---- 구글 로그인 끝 ----

        // ---- 네이버 로그인 연동 시작 ----
        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
            }
            override fun onFailure(httpStatus: Int, message: String) {
                val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                Toast.makeText(this@IntroActivity, "로그인에 실패했습니다.\n다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                Log.e("failed Login to NAVER", "errorCode: $errorCode, errorDesc: $errorDescription")
            }
            override fun onSuccess() {
                // ---- 네이버 로그인 성공 동작 시작 ----
                NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
                    override fun onError(errorCode: Int, message: String) {}
                    override fun onFailure(httpStatus: Int, message: String) {}
                    override fun onSuccess(result: NidProfileResponse) {
                        val jsonObj = JSONObject()
                        val naverMobile = result.profile?.mobile.toString().replaceFirst("010", "+8210")
                        val naverGender : String = if (result.profile?.gender.toString() == "M") "남자" else "여자"
                        jsonObj.put("user_name", result.profile?.name.toString())
                        jsonObj.put("user_gender", naverGender)
                        jsonObj.put("user_mobile", naverMobile)
                        jsonObj.put("user_email", result.profile?.email.toString())
                        jsonObj.put("user_birthday", result.profile?.birthYear.toString() + "-" + result.profile?.birthday.toString())
                        jsonObj.put("naver_login_id" , result.profile?.id.toString())
                        jsonObj.put("user_age" , result.profile?.age)

                        jsonObj.put("social_account", "naver")
                        Log.v("jsonObj", "$jsonObj")
//                        Log.v("네이버이메일", jsonObj.getString("user_email"))
//                        val encodedUserEmail = URLEncoder.encode(jsonObj.getString("user_email"), "UTF-8")
                        getUserBySdk(getString(R.string.IP_ADDRESS_t_user), jsonObj) { jo ->
                            if (jo != null) {
                                when (jo.optString("status")) {
                                    "200" -> { saveTokenAndIdentifyUser(jo, jsonObj, 200) }
                                    "201" -> { saveTokenAndIdentifyUser(jo, jsonObj, 201) }
                                    else -> { Log.v("responseCodeError", "response: $jo")}
                                }
                            }
                        }
                    }
                })
                // ------! 네이버 로그인 성공 동작 끝 !------
            }
        }
        binding.btnOAuthLoginImg.setOnClickListener {
            NaverIdLoginSDK.authenticate(this, oauthLoginCallback)
        }

        // ------! 카카오톡 OAuth 불러오기 !------
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("카카오톡", "카카오톡 로그인 실패 $error")
                when {
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Log.e("카카오톡", "접근이 거부 됨(동의 취소) $error")
                    }
                }
            } else if (token != null) {
                Log.e("카카오톡", "로그인에 성공하였습니다.")
                UserApiClient.instance.accessTokenInfo { tokenInfo, error1 ->
                    if (error1 != null) {
                        Log.e(TAG, "토큰 정보 보기 실패", error1)
                    }
                    else if (tokenInfo != null) {
                        UserApiClient.instance.me { user, error2 ->
                            if (error2 != null) {
                                Log.e(TAG, "사용자 정보 요청 실패", error2)
                            }
                            else if (user != null) {
                                val jsonObj = JSONObject()
                                val kakaoMobile = user.kakaoAccount?.phoneNumber.toString().replaceFirst("+82 10", "+8210")
                                jsonObj.put("user_name" , user.kakaoAccount?.name.toString())
                                val kakaoUserGender = if (user.kakaoAccount?.gender.toString()== "M")  "남자" else "여자"
                                jsonObj.put("user_gender", kakaoUserGender)
                                jsonObj.put("user_mobile", kakaoMobile)
                                jsonObj.put("user_email", user.kakaoAccount?.email.toString())
                                jsonObj.put("user_birthday", user.kakaoAccount?.birthyear.toString() + "-" + user.kakaoAccount?.birthday?.substring(0..1) + "-" + user.kakaoAccount?.birthday?.substring(2))
                                jsonObj.put("kakao_login_id" , user.id.toString())
                                jsonObj.put("kakao_id_token", token.idToken)
                                jsonObj.put("social_account", "kakao")

                                Log.v("jsonObj", "$jsonObj")
//                                val encodedUserEmail = URLEncoder.encode(jsonObj.getString("user_email"), "UTF-8")
//                                Log.w("카카오가입>메일", encodedUserEmail)
                                getUserBySdk(getString(R.string.IP_ADDRESS_t_user), jsonObj) { jo ->
                                    if (jo != null) {
                                        when (jo.optString("status")) {
                                            "200" -> { saveTokenAndIdentifyUser(jo, jsonObj, 200) }
                                            "201" -> { saveTokenAndIdentifyUser(jo, jsonObj, 201) }
                                            else -> { Log.v("responseCodeError", "response: $jo")}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        binding.ibtnKakaoLogin.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this@IntroActivity)) {
                UserApiClient.instance.loginWithKakaoTalk(this@IntroActivity, callback = callback)
            }  else {
                UserApiClient.instance.loginWithKakaoAccount(this@IntroActivity, callback = callback)
            }
        }
        // ---- 카카오 로그인 연동 끝 ----

        binding.btnIntroLogin.setOnSingleClickListener{
            val dialog = LoginDialogFragment()
            dialog.show(this@IntroActivity.supportFragmentManager, "LoginDialogFragment")
        }

        binding.btnIntroSignIn.setOnSingleClickListener {
            val intent = Intent(this@IntroActivity, SignInActivity::class.java)
            startActivity(intent)
        }

        // -----! 배너 시작 !-----
//        viewModel.BannerList.add(ImageUrl1)
//        viewModel.BannerList.add(ImageUrl2)
//        viewModel.BannerList.add(ImageUrl3)
//        viewModel.BannerList.add(ImageUrl4)
//        viewModel.BannerList.add(ImageUrl5)
//        val bannerAdapter = BannerVPAdapter(viewModel.BannerList, "intro",this@IntroActivity)
//        bannerAdapter.notifyDataSetChanged()
//        binding.vpIntroBanner.orientation = ViewPager2.ORIENTATION_HORIZONTAL
//        binding.vpIntroBanner.adapter = bannerAdapter
//        binding.vpIntroBanner.setCurrentItem(bannerPosition, false)
//        binding.vpIntroBanner.apply {
//            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//                override fun onPageScrollStateChanged(state: Int) {
//                    super.onPageScrollStateChanged(state)
//                    when (state) {
//                        ViewPager2.SCROLL_STATE_DRAGGING -> autoScrollStop()
//                        ViewPager2.SCROLL_STATE_IDLE -> autoScrollStart(intervalTime)
//                    }
//                }
//            })
//        }

    }
//    private inner class HomeBannerHandler: Handler(Looper.getMainLooper()) {
//        override fun handleMessage(msg: Message) {
//            super.handleMessage(msg)
//            if (msg.what == 0 && viewModel.BannerList.isNotEmpty()) {
//                binding.vpIntroBanner.setCurrentItem(++bannerPosition, true)
//                // ViewPager의 현재 위치를 이미지 리스트의 크기로 나누어 현재 이미지의 인덱스를 계산합니다.
//                val currentIndex = bannerPosition % viewModel.BannerList.size // 65536  % 5
//
//                // ProgressBar의 값을 계산합니다.
//                binding.hpvIntro.progress = (currentIndex ) * 100 / (viewModel.BannerList.size -1 )
//                autoScrollStart(intervalTime)
//            }
//        }
//    }
//    private fun autoScrollStart(intervalTime: Long) {
//        bannerHandler.removeMessages(0)
//        bannerHandler.sendEmptyMessageDelayed(0, intervalTime)
//    }
//    private fun autoScrollStop() {
//        bannerHandler.removeMessages(0)
//    }
//    override fun onResume() {
//        super.onResume()
//        autoScrollStart(intervalTime)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        autoScrollStop()
//    }
// -----! 배너 끝 !-----

    private fun MainInit() {
        val intent = Intent(this ,MainActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
    private fun setupInit() {
        val intent = Intent(this, SetupActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun saveTokenAndIdentifyUser(jo: JSONObject, jsonObj: JSONObject, situation: Int) {
        when (situation) {
            // ------! 기존 로그인 !------
            200 -> {
                storeUserInSingleton(this@IntroActivity, jo)
                Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this).jsonObject}")
                MainInit()
            }
            // ------! 최초 회원가입 !------
            201 -> {
//                saveServerToken(this@IntroActivity, jo.optString("user_token"))
//                Log.v("save>Token", "${getServerToken(this@IntroActivity)}")

                // ------! 광고성 수신 동의 문자 시작 !------
                val bottomSheetFragment = AgreementBottomSheetDialogFragment()
                bottomSheetFragment.isCancelable = false
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                bottomSheetFragment.setOnFinishListener(object : AgreementBottomSheetDialogFragment.OnAgreeListener {
                    override fun onFinish(agree: Boolean) {
                        if (agree) {
                            jsonObj.put("sms_receive", if (sViewModel.agreementMk1.value == true) "1" else "0")
                            jsonObj.put("email_receive", if (sViewModel.agreementMk2.value == true) "1" else "0")
                            Log.v("Intro>SMS", "$jsonObj")

//                            insertMarketingBySn(getString(R.string.IP_ADDRESS_t_user), jsonObj, getServerToken(this@IntroActivity).toString()) { jo2 ->
//                                storeUserInSingleton(this@IntroActivity, jo)
                                Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")
                                setupInit()
//                            }
                        } else {
                            // ------! 동의 하지 않음 -> 삭제 후 intro 유지 !------
                            if (Firebase.auth.currentUser != null) {
                                Firebase.auth.signOut()
                                Log.d("로그아웃", "Firebase sign out successful")
                            } else if (NaverIdLoginSDK.getState() == NidOAuthLoginState.OK) {
                                NaverIdLoginSDK.logout()
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
                        }
                    }
                })
            }
        }
    }

    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }
}
