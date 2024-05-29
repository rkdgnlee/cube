package com.tangoplus.tangoq

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.AuthErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import com.tangoplus.tangoq.adapter.BannerVPAdapter
import com.tangoplus.tangoq.dialog.LoginDialogFragment
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.`object`.CommonDefines.TAG
import com.tangoplus.tangoq.`object`.NetworkUserService.StoreUserInSingleton
import com.tangoplus.tangoq.`object`.NetworkUserService.fetchUserINSERTJson
import com.tangoplus.tangoq.`object`.NetworkUserService.fetchUserUPDATEJson
import com.tangoplus.tangoq.`object`.NetworkUserService.getUserSELECTJson
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.data.BannerViewModel
import com.tangoplus.tangoq.data.SignInViewModel
import com.tangoplus.tangoq.databinding.ActivityIntroBinding
import com.tangoplus.tangoq.dialog.GoogleSignInDialogFragment
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
    private var bannerPosition = Int.MAX_VALUE/2
    private var bannerHandler = HomeBannerHandler()
    private val intervalTime = 2200.toLong()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)



        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            firebaseAuth = FirebaseAuth.getInstance()
            launcher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(), ActivityResultCallback { result ->
                    Log.v(TAG, "resultCode: ${result.resultCode}입니다.")
                    if (result.resultCode == RESULT_OK) {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        try {
                            task.getResult(ApiException::class.java)?.let { account ->
                                val tokenId = account.idToken
                                if (tokenId != null && tokenId != "") {
                                    val credential: AuthCredential =
                                        GoogleAuthProvider.getCredential(account.idToken, null)
                                    firebaseAuth.signInWithCredential(credential)
                                        .addOnCompleteListener {
                                            if (firebaseAuth.currentUser != null) {
                                                Log.d("로그인", "${firebaseAuth.currentUser}")
                                                // ---- Google 토큰에서 가져오기 시작 ----
                                                val user: FirebaseUser = firebaseAuth.currentUser!!

                                                // ----- GOOGLE API: 전화번호 담으러 가기(signin) 시작 -----
                                                sViewModel.googleJson.put("user_name", user.displayName.toString())
                                                sViewModel.googleJson.put("user_email", user.email.toString())
                                                sViewModel.googleJson.put("google_login_id", user.uid)
                                                Log.e("구글JsonObj", sViewModel.googleJson.optString("google_login_id"))
                                                val dialog = GoogleSignInDialogFragment()
                                                dialog.isCancelable = false
                                                dialog.show(supportFragmentManager, "GoogleSignInDialogFragment")
                                                // ----- GOOGLE API에서 DB에 넣는 공간 끝 -----

                                                val googleSignInToken = account.idToken ?: ""
                                                if (googleSignInToken != "") {
                                                    Log.e(TAG, "googleSignInToken : $googleSignInToken")
                                                } else {
                                                    Log.e(TAG, "googleSignInToken =  null")
                                                }
                                                // ---- Google 토큰에서 가져오기 끝 ----
                                            }
                                        }
                                }
                            } ?: throw Exception()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
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
                Toast.makeText(
                    this@IntroActivity,
                    "errorCode: $errorCode, errorDesc: $errorDescription",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onSuccess() {
                // ---- 네이버 로그인 성공 동작 시작 ----
                NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
                    override fun onError(errorCode: Int, message: String) {}
                    override fun onFailure(httpStatus: Int, message: String) {}

                    override fun onSuccess(result: NidProfileResponse) {
                        val JsonObj = JSONObject()
                        val naverMobile = result.profile?.mobile.toString().replaceFirst("010", "+8210")
                        val naverGender : String
                        naverGender = if (result.profile?.gender.toString() == "M") {
                            "남자"
                        } else {
                            "여자"
                        }
                        JsonObj.put("user_name", result.profile?.name.toString())
                        JsonObj.put("user_gender", naverGender)
                        JsonObj.put("user_mobile", naverMobile)
                        JsonObj.put("user_email", result.profile?.email.toString())
                        JsonObj.put("user_birthday", result.profile?.birthYear.toString() + "-" + result.profile?.birthday.toString())
                        JsonObj.put("naver_login_id" , result.profile?.id.toString())

                        Log.i("네이버핸드폰번호", JsonObj.getString("user_mobile"))
                        val encodedUserMobile = URLEncoder.encode(naverMobile, "UTF-8")
                        getUserSELECTJson(getString(R.string.IP_ADDRESS_t_user), encodedUserMobile) { jsonObj ->
                            if (jsonObj?.getInt("status") == 404) {
                                fetchUserINSERTJson(getString(R.string.IP_ADDRESS_t_user), JsonObj.toString()) {
                                    StoreUserInSingleton(this@IntroActivity, JsonObj)
                                    Log.e("네이버>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")
                                    setupInit() // 최초 회원가입
                                }
                            } else {
                                fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), JsonObj.toString(), encodedUserMobile) {
                                    if (jsonObj != null) {
                                        StoreUserInSingleton(this@IntroActivity, jsonObj)
                                    }
                                    Log.e("네이버>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")
                                    MainInit()
                                }
                            }
                        }
                    }
                })
                // ---- 네이버 로그인 성공 동작 끝 ----
            }
        }
        binding.buttonOAuthLoginImg.setOnClickListener {
            NaverIdLoginSDK.authenticate(this, oauthLoginCallback)
        }
// ---- 카카오톡 OAuth 불러오기 ----
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
                UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
                    if (error != null) {
                        Log.e(TAG, "토큰 정보 보기 실패", error)
                    }
                    else if (tokenInfo != null) {
                        UserApiClient.instance.me { user, error ->
                            if (error != null) {
                                Log.e(TAG, "사용자 정보 요청 실패", error)
                            }
                            else if (user != null) {
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

                                val encodedUserMobile = URLEncoder.encode(kakaoMobile, "UTF-8")
                                Log.w("$TAG, 카카오회원가입", JsonObj.getString("user_mobile"))
                                getUserSELECTJson(getString(R.string.IP_ADDRESS_t_user), encodedUserMobile) { jsonObj ->
                                    if (jsonObj?.getInt("status") == 404) {
                                        fetchUserINSERTJson(getString(R.string.IP_ADDRESS_t_user), JsonObj.toString()) {
                                            StoreUserInSingleton(this, JsonObj)
                                            Log.e("카카오>싱글톤", "${Singleton_t_user.getInstance(this).jsonObject}")
                                            setupInit() //TODO 최초 회원가입
                                        }
                                    } else {
                                        fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), JsonObj.toString(), encodedUserMobile) {
                                            if (jsonObj != null) {
                                                StoreUserInSingleton(this, jsonObj)
                                            }
                                            Log.e("카카오>싱글톤", "${Singleton_t_user.getInstance(this).jsonObject}")
                                            MainInit()
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

        val ImageUrl1 = "https://images.unsplash.com/photo-1572196459043-5c39f99a7555?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        val ImageUrl2 = "https://images.unsplash.com/photo-1605558162119-2de4d9ff8130?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        val ImageUrl3 = "https://images.unsplash.com/photo-1533422902779-aff35862e462?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        val ImageUrl4 = "https://images.unsplash.com/photo-1587387119725-9d6bac0f22fb?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        val ImageUrl5 = "https://images.unsplash.com/photo-1598449356475-b9f71db7d847?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        viewModel.BannerList.add(ImageUrl1)
        viewModel.BannerList.add(ImageUrl2)
        viewModel.BannerList.add(ImageUrl3)
        viewModel.BannerList.add(ImageUrl4)
        viewModel.BannerList.add(ImageUrl5)

        val bannerAdapter = BannerVPAdapter(viewModel.BannerList, "intro",this@IntroActivity)
        bannerAdapter.notifyDataSetChanged()
        binding.vpIntroBanner.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.vpIntroBanner.adapter = bannerAdapter
        binding.vpIntroBanner.setCurrentItem(bannerPosition, false)
        binding.vpIntroBanner.apply {
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    when (state) {
                        ViewPager2.SCROLL_STATE_DRAGGING -> autoScrollStop()
                        ViewPager2.SCROLL_STATE_IDLE -> autoScrollStart(intervalTime)
                    }
                }
            })
        }

    }
    private fun MainInit() {
        val intent = Intent(this ,MainActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
    private inner class HomeBannerHandler: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 0 && viewModel.BannerList.isNotEmpty()) {
                binding.vpIntroBanner.setCurrentItem(++bannerPosition, true)
                // ViewPager의 현재 위치를 이미지 리스트의 크기로 나누어 현재 이미지의 인덱스를 계산합니다.
                val currentIndex = bannerPosition % viewModel.BannerList.size // 65536  % 5

                // ProgressBar의 값을 계산합니다.
                binding.hpvIntro.progress = (currentIndex ) * 100 / (viewModel.BannerList.size -1 )
                autoScrollStart(intervalTime)
            }
        }
    }

    private fun autoScrollStart(intervalTime: Long) {
        bannerHandler.removeMessages(0)
        bannerHandler.sendEmptyMessageDelayed(0, intervalTime)

    }
    private fun autoScrollStop() {
        bannerHandler.removeMessages(0)
    } // -----! 배너 끝 !-----

    private fun setupInit() {
        val intent = Intent(this, SetupActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }

    override fun onResume() {
        super.onResume()
        autoScrollStart(intervalTime)
    }

    override fun onPause() {
        super.onPause()
        autoScrollStop()
    }
}