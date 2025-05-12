package com.tangoplus.tangobody

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
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
import com.tangoplus.tangobody.api.DeviceService.isNetworkAvailable
import com.tangoplus.tangobody.api.NetworkUser.linkOAuthAccount
import com.tangoplus.tangobody.dialog.LoginDialogFragment
import com.tangoplus.tangobody.db.Singleton_t_user
import com.tangoplus.tangobody.viewmodel.SignInViewModel
import com.tangoplus.tangobody.databinding.ActivityIntroBinding
import com.tangoplus.tangobody.function.SecurePreferencesManager
import com.tangoplus.tangobody.function.SecurePreferencesManager.createKey
import com.tangoplus.tangobody.api.NetworkUser.oauthUser
import com.tangoplus.tangobody.api.NetworkUser.storeUserInSingleton
import com.tangoplus.tangobody.dialog.LoadingDialogFragment
import com.tangoplus.tangobody.dialog.MobileAuthDialogFragment
import com.tangoplus.tangobody.dialog.SignInDialogFragment
import com.tangoplus.tangobody.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangobody.function.SaveSingletonManager
import com.tangoplus.tangobody.function.SecurePreferencesManager.logout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception


class IntroActivity : AppCompatActivity() {
    lateinit var binding : ActivityIntroBinding

    val sViewModel : SignInViewModel by viewModels()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var securePref : EncryptedSharedPreferences
    private lateinit var ssm : SaveSingletonManager
    private var loadingDialog : LoadingDialogFragment? = null

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.clIntro)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // ------! activity 사전 설정 끝 !------

        // ------! token 저장할  securedPref init !------
        loadingDialog = LoadingDialogFragment.newInstance("회원가입전송")
        securePref = SecurePreferencesManager.getInstance(this@IntroActivity)
        ssm = SaveSingletonManager(this@IntroActivity, this)
        if (!isNetworkAvailable(this)) {
            Toast.makeText(this, "인터넷 연결 후 앱을 다시 실행해주세요", Toast.LENGTH_LONG).show()
        }

        val code = intent.getIntExtra("SignInFinished", 0)
        handleSignInResult(code)

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
//                                            val user: FirebaseUser = firebaseAuth.currentUser!!

                                            // ----- GOOGLE API: 전화번호 담으러 가기(signin) 시작 -----
                                            val jsonObj = JSONObject()
                                            jsonObj.put("device_sn" ,0)
                                            jsonObj.put("user_sn", 0)
                                            jsonObj.put("access_token", tokenId)
                                            jsonObj.put("provider", "google")
//                                            Log.v("jsonObj", "$jsonObj")
                                            sViewModel.provider = "google"
                                            sViewModel.sdkToken = tokenId

                                            if (!this@IntroActivity.isDestroyed) {
                                                loadingDialog?.show(supportFragmentManager, "LoadingDialogFragment")
                                            }
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                oauthUser(getString(R.string.API_user), jsonObj, this@IntroActivity) { responseJo ->
                                                    lifecycleScope.launch(Dispatchers.Main) {

                                                        // 로딩창 닫기
                                                        if (!this@IntroActivity.isDestroyed) {
                                                            loadingDialog?.dismiss()
                                                        }

                                                        if (responseJo != null) {
                                                            trackingUserStatus(responseJo)
                                                        } else {
                                                            enabledAllLoginBtn()
                                                            Toast.makeText(this@IntroActivity, "로그인에 실패했습니다\n관리자 문의가 필요합니다", Toast.LENGTH_LONG).show()
                                                            logout(this@IntroActivity, 0)

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                            }
                        } ?: throw Exception()
                    } catch (e: IndexOutOfBoundsException) {
                        enabledAllLoginBtn()
                        Log.e("IntroIndex", "${e.message}")
                    } catch (e: IllegalArgumentException) {
                        enabledAllLoginBtn()
                        Log.e("IntroIllegal", "${e.message}")
                    } catch (e: NullPointerException) {
                        enabledAllLoginBtn()
                        Log.e("IntroNull", "${e.message}")
                    } catch (e: Exception) {
                        enabledAllLoginBtn()
                        Log.e("IntroException", "${e.message}")
                    }
                } else {
                    enabledAllLoginBtn()
                }
            }
        } else {
            val intent = Intent(this@IntroActivity, MainActivity::class.java)
            startActivity(intent)
            ActivityCompat.finishAffinity(this@IntroActivity)
        } // ---- firebase 초기화 및 Google Login API 연동 끝 ----

        // ---- 구글 로그인 시작 ----
        binding.ibtnGoogleLogin.setOnClickListener {
            disabledSNSLogin()
            CoroutineScope(Dispatchers.IO).launch {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.firebase_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(this@IntroActivity.applicationContext, gso)
                googleSignInClient.signOut().addOnCompleteListener {
                    val signInIntent = googleSignInClient.signInIntent
                    launcher.launch(signInIntent)
                }
            }
        } // ---- 구글 로그인 끝 ----

        // ---- 네이버 로그인 연동 시작 ----
        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
                Toast.makeText(this@IntroActivity, "네이버 로그인에 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                enabledAllLoginBtn()
            }
            override fun onFailure(httpStatus: Int, message: String) {
                val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                Toast.makeText(this@IntroActivity, "로그인에 실패했습니다.\n다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                Log.e("failed Login to NAVER", "errorCode: $errorCode, errorDesc: $errorDescription")
                enabledAllLoginBtn()
            }
            override fun onSuccess() {
                // ------! 네이버 로그인 성공 동작 시작 !-----
                // 여기서 NaverIdLoginSDK.getAccessToken(),  getRefreshToken() 을 사용해서 가져올 수 있음. 내가 이걸 전달해주면 -> 회원 프로프리 조회 API 명세를 통해 서버에서 PHP의 로그인 응답결과를 받아서 사용하면 될 듯?

                NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
                    override fun onError(errorCode: Int, message: String) { enabledAllLoginBtn() }
                    override fun onFailure(httpStatus: Int, message: String) { enabledAllLoginBtn() }
                    override fun onSuccess(result: NidProfileResponse) {
//                        Log.v("네이버로그인", "${result.resultCode}, ${result.message}, ${result.profile}")
                        val naverSdkToken = "${NaverIdLoginSDK.getAccessToken()}"
                        val jsonObj = JSONObject()
                        jsonObj.put("device_sn" ,0)
                        jsonObj.put("user_sn", 0)
                        jsonObj.put("provider", "naver")
                        jsonObj.put("access_token", naverSdkToken)

                        sViewModel.apply {
                            provider = "naver"
                            sdkToken = naverSdkToken
                            fullEmail.value = result.profile?.email
                            passMobile.value = result.profile?.mobile
                        }
                        Log.v("jsonObj", "$jsonObj, ${result.profile?.mobile}")

                        if (!this@IntroActivity.isDestroyed) {
                            loadingDialog?.show(supportFragmentManager, "LoadingDialogFragment")
                        }

                        lifecycleScope.launch(Dispatchers.IO) {
                            oauthUser(getString(R.string.API_user), jsonObj, this@IntroActivity) { responseJo ->
                                lifecycleScope.launch(Dispatchers.Main) {
                                    // 로딩창 닫기
                                    if (!this@IntroActivity.isDestroyed) {
                                        loadingDialog?.dismiss()
                                    }

                                    if (responseJo != null) {
                                        Log.v("responseJo", "responseJo: $responseJo")
                                        trackingUserStatus(responseJo)
                                    } else {
                                        enabledAllLoginBtn()
                                        Toast.makeText(this@IntroActivity, "로그인에 실패했습니다\n관리자에게 문의해주세요", Toast.LENGTH_LONG).show()
                                        logout(this@IntroActivity, 0)
                                    }
                                }
                            }
                        }
                    }
                })
                // ------! 네이버 로그인 성공 동작 끝 !------
            }
        }
        binding.btnOAuthLoginImg.setOnClickListener {
            disabledSNSLogin()
            NaverIdLoginSDK.authenticate(this, oauthLoginCallback)
        }

        // ------! 카카오톡 OAuth 불러오기 !------
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("카카오톡", "카카오톡 로그인 실패 ${error.message}")
                Toast.makeText(this@IntroActivity, "카카오톡 계정와 연동이 실패했습니다", Toast.LENGTH_SHORT).show()
                enabledAllLoginBtn()
                when {
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        enabledAllLoginBtn()
                        Toast.makeText(this@IntroActivity, "카카오톡 계정의 정보 동의가 필요합니다", Toast.LENGTH_LONG).show()
                        Log.e("카카오톡", "접근이 거부 됨(동의 취소) ${error.message}")
                    }
                }
            } else if (token != null) {
                Log.v("카카오톡", "로그인에 성공하였습니다.")
                UserApiClient.instance.accessTokenInfo { tokenInfo, error1 ->
                    if (error1 != null) {
                        enabledAllLoginBtn()
                        Log.e(TAG, "토큰 정보 보기 실패", error1)
                    }
                    else if (tokenInfo != null) {
                        UserApiClient.instance.me { user, error2 ->
                            if (error2 != null) {
                                enabledAllLoginBtn()
                                Log.e(TAG, "사용자 정보 요청 실패 ${error2.message}" )
                            }
                            else if (user != null) {
                                val jsonObj = JSONObject()
                                jsonObj.put("device_sn" ,0)
                                jsonObj.put("user_sn", 0)
                                jsonObj.put("access_token", token.accessToken)
                                jsonObj.put("provider", "kakao")
//                                Log.v("jsonObj", "$jsonObj")
//                                Log.v("핸드폰 번호", "${user.kakaoAccount?.phoneNumber}")
                                sViewModel.apply {
                                    provider = "kakao"
                                    sdkToken = token.accessToken
                                    passMobile.value = user.kakaoAccount?.phoneNumber?.replace("+82 10", "010")
                                    fullEmail.value = user.kakaoAccount?.email
                                }
                                if (!this@IntroActivity.isDestroyed) {
                                    loadingDialog?.show(supportFragmentManager, "LoadingDialogFragment")
                                }

                                lifecycleScope.launch(Dispatchers.IO) {
                                    oauthUser(getString(R.string.API_user), jsonObj, this@IntroActivity) { responseJo ->
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            // 로딩창 닫기
                                            if (!this@IntroActivity.isDestroyed) {
                                                loadingDialog?.dismiss()
                                            }

                                            if (responseJo != null) {
                                                trackingUserStatus(responseJo)
                                            } else {
                                                enabledAllLoginBtn()
                                                Toast.makeText(this@IntroActivity, "로그인에 실패했습니다\n관리자에게 문의해주세요", Toast.LENGTH_LONG).show()
                                                logout(this@IntroActivity, 0)
                                            }
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
            disabledSNSLogin()
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this@IntroActivity)) {
                UserApiClient.instance.loginWithKakaoTalk(this@IntroActivity, callback = callback)
            }  else {
                UserApiClient.instance.loginWithKakaoAccount(this@IntroActivity, callback = callback)
            }
        }
        // ---- 카카오 로그인 연동 끝 ----

        binding.btnIntroLogin.setOnSingleClickListener{
            goLoginScreen()
        }

        binding.btnIntroSignIn.setOnSingleClickListener {
            val dialog = SignInDialogFragment()
            dialog.show(supportFragmentManager, "SignInDialogFragment")
        }
    }

    private fun mainInit() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun trackingUserStatus(responseJo : JSONObject) {

        val status = responseJo.optInt("status")
        when (status) {
            200, 201 -> {
                // 해당 이메일 정보가 이미 있는데 다시 누른 경우
                storeUserInSingleton(this@IntroActivity, responseJo)
                prepareLogin()
            }

            // 네이버, 카카오, 구글에서 받은 이메일로는 t_user_info에 일치하는 휴대폰이 전혀 없을 때 -> 인증창 이동
            202 -> {
                sViewModel.tempId = responseJo.optString("temp_id")
                sViewModel.fullEmail.value = responseJo.optString("email")
//                Log.v("뷰모델 토큰", "${sViewModel.tempId}, ${sViewModel.fullEmail.value}")
                val authDialog = MobileAuthDialogFragment()
                authDialog.show(supportFragmentManager, null)
            }

            409 -> {
                when (responseJo.optBoolean("linkage")) {
                    true -> {
                        sViewModel.insertToken = responseJo.optString("jwt")
                        MaterialAlertDialogBuilder( this@IntroActivity, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                            setTitle("연동 여부")
                            setMessage("현재 존재하는 계정입니다. 기존 계정과 연동하시겠습니까?")
                            setPositiveButton("예") {_, _ ->
                                val jo = JSONObject().apply {
                                    put("email", sViewModel.fullEmail.value)
                                    put("mobile", sViewModel.passMobile.value?.replace("-", ""))
                                    put("provider", sViewModel.provider)
                                    put("access_token", sViewModel.sdkToken)
                                }
                                Log.v("409Json", "$jo, ${sViewModel.insertToken} ${sViewModel.sdkToken}")
                                linkOAuthAccount(getString(R.string.API_user), sViewModel.insertToken, jo.toString(), this@IntroActivity) { responseJo ->
                                    if (responseJo != null) {
                                        navigateOAuthLink(responseJo)
                                    }
                                }
                            }
                            setNegativeButton("아니오") {_,_ ->  }
                            setCancelable(false)
                            show()
                        }

                    }
                    false -> {
                        MaterialAlertDialogBuilder(this@IntroActivity, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                            setTitle("알림")
                            setMessage("이미 회원가입 한 회원입니다. 로그인 혹은 이메일 찾기를 진행해주세요")
                            setPositiveButton("예") {_, _ ->

                            }
                            show()
                        }
                    }
                }
                sViewModel.countDownTimer?.cancel()
            }
            else -> {
            }
        }

        // 마지막에 일단 버튼 풀기
        enabledAllLoginBtn()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val code = intent.getIntExtra("SignInFinished", 0) ?: 0
        handleSignInResult(code)
    }

    private fun handleSignInResult(code: Int) {
//        Log.v("SignInFinished", "$code")
        runOnUiThread {
            when (code) {
                201 -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(this@IntroActivity, "회원가입을 축하합니다\n로그인을 진행해주세요 !", Toast.LENGTH_SHORT).show()
                    }, 500)
                    Handler(Looper.getMainLooper()).postDelayed({
                        val dialog = LoginDialogFragment()
                        dialog.show(supportFragmentManager, "LoginDialogFragment")
                    }, 1500)
                }
                409 -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(this@IntroActivity, "기존 이메일 계정으로 로그인을 진행해주세요.", Toast.LENGTH_SHORT).show()
                    }, 500)
                    Handler(Looper.getMainLooper()).postDelayed({
                        val dialog = LoginDialogFragment()
                        dialog.show(supportFragmentManager, "LoginDialogFragment")
                    }, 1500)
                }
                4091, 4092, 4093 -> {
                    val toastText = when (code) {
                        4091 -> "구글"
                        4092 -> "네이버"
                        4093 -> "카카오"
                        else -> ""
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(this@IntroActivity, "$toastText 간편 로그인을 진행해주세요.", Toast.LENGTH_SHORT).show()
                    }, 500)
                }
                402 -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(this@IntroActivity, "이미 가입된 회원입니다. 아이디/비밀번호 찾기를 진행해주세요", Toast.LENGTH_SHORT).show()
                    }, 500)
                    Handler(Looper.getMainLooper()).postDelayed({
                        val dialog = LoginDialogFragment()
                        dialog.show(supportFragmentManager, "LoginDialogFragment")
                    }, 1200)
                }
            }
        }
    }

    private fun navigateOAuthLink(responseJo : JSONObject) {
        val responseCode = responseJo.optInt("status")
        when (responseCode) {
            200, 201 -> {
                storeUserInSingleton(this@IntroActivity, responseJo)
                createKey(getString(R.string.SECURE_KEY_ALIAS))
                Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")
                val userUUID = Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("user_uuid")
                val userInfoSn =  Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("sn")?.toInt()
                if (userUUID != null && userInfoSn != null) {
                    ssm.getMeasures(userUUID, userInfoSn,  CoroutineScope(Dispatchers.IO)) {
                        val intent = Intent(this@IntroActivity, MainActivity::class.java)
                        startActivity(intent)
                        this@IntroActivity.finishAffinity()
                    }
                }
            }
            400 -> {
                Toast.makeText(this@IntroActivity, "올바르지 않은 이메일 입니다. 다시 확인해주세요", Toast.LENGTH_SHORT).show()
            }
            404 -> {
                Toast.makeText(this@IntroActivity, "존재하지 않는 사용자입니다. 정보를 다시 확인해주세요", Toast.LENGTH_SHORT).show()
            }
            500 -> {
                Toast.makeText(this@IntroActivity, "서버 오류 입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this@IntroActivity, "인증에 실패했습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disabledSNSLogin() {
        binding.ibtnGoogleLogin.isEnabled = false
        binding.ibtnKakaoLogin.isEnabled = false
        binding.btnOAuthLoginImg.isEnabled = false
        binding.btnIntroLogin.isEnabled = false
        binding.btnIntroSignIn.isEnabled = false
    }
    private fun enabledAllLoginBtn() {
        binding.ibtnGoogleLogin.isEnabled = true
        binding.ibtnKakaoLogin.isEnabled = true
        binding.btnOAuthLoginImg.isEnabled = true
        binding.btnIntroLogin.isEnabled = true
        binding.btnIntroSignIn.isEnabled = true
    }
    private fun prepareLogin() {
        createKey(getString(R.string.SECURE_KEY_ALIAS))
        Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")
        val userUUID = Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("user_uuid")
        val userInfoSn =  Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("sn")?.toInt()
        if (userUUID != null && userInfoSn != null) {
            ssm.getMeasures(userUUID, userInfoSn,  CoroutineScope(Dispatchers.IO)) {
                mainInit()
            }
        }
    }

    private fun goLoginScreen() {
        val dialog = LoginDialogFragment()
        supportFragmentManager.beginTransaction()
            .addSharedElement(binding.ivIntroLogo, "transLogo")
            .addSharedElement(binding.tvIntroComment, "transComment")
            .setReorderingAllowed(true)
            .add(dialog, "LoginDialogFragment")
            .commitAllowingStateLoss()
    }
}
