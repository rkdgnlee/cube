package com.tangoplus.tangoq

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
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
import com.tangoplus.tangoq.api.NetworkUser.storeUserInSingleton
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.databinding.ActivityIntroBinding
import com.tangoplus.tangoq.function.SecurePreferencesManager
import com.tangoplus.tangoq.function.SecurePreferencesManager.createKey
import com.tangoplus.tangoq.dialog.bottomsheet.AgreementBSDialogFragment
import com.tangoplus.tangoq.api.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.api.NetworkUser.fetchUserDeleteJson
import com.tangoplus.tangoq.api.NetworkUser.fetchUserUPDATEJson
import com.tangoplus.tangoq.api.NetworkUser.identifyEmail
import com.tangoplus.tangoq.dialog.SignInDialogFragment
import com.tangoplus.tangoq.dialog.WebViewDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.function.SecurePreferencesManager.logout
import com.tangoplus.tangoq.function.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.Exception


class IntroActivity : AppCompatActivity() {
    lateinit var binding : ActivityIntroBinding

    val sViewModel : SignInViewModel by viewModels()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var securePref : EncryptedSharedPreferences
    private lateinit var ssm : SaveSingletonManager

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

        // ------# 접근 방지 #------

        // ------! token 저장할  securedPref init !------
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
                                            val user: FirebaseUser = firebaseAuth.currentUser!!

                                            // ----- GOOGLE API: 전화번호 담으러 가기(signin) 시작 -----
                                            val jsonObj = JSONObject()
                                            jsonObj.put("device_sn" ,0)
                                            jsonObj.put("user_sn", 0)
                                            jsonObj.put("providerId", user.uid)
                                            jsonObj.put("email", user.email)
                                            jsonObj.put("user_name", user.displayName)
                                            jsonObj.put("access_token", tokenId) // 토큰 값
                                            jsonObj.put("provider", "google")
//                                            val encodedUserEmail = URLEncoder.encode(jsonObj.getString("user_email"), "UTF-8")
                                            Log.v("jsonObj", "$jsonObj")
                                            sViewModel.snsJo = jsonObj
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                identifyEmail(getString(R.string.API_user), jsonObj) { responseJo ->
                                                    if (responseJo != null) {
                                                        trackingUserStatus(responseJo)
                                                    } else {
                                                        CoroutineScope(Dispatchers.Main).launch {
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
            disabledSNSLogin(0)
            CoroutineScope(Dispatchers.IO).launch {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.firebase_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(this@IntroActivity, gso)
                val signInIntent: Intent = googleSignInClient.signInIntent.apply {
                    putExtra("prompt", "select_account")
                }
                launcher.launch(signInIntent)
            }
        } // ---- 구글 로그인 끝 ----

        // ---- 네이버 로그인 연동 시작 ----
        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
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
                        val jsonObj = JSONObject()
                        jsonObj.put("device_sn" ,0)
                        jsonObj.put("user_sn", 0)
                        jsonObj.put("providerId", "${result.profile?.id}")
                        jsonObj.put("email", result.profile?.email)
                        jsonObj.put("user_name", "${result.profile?.name}")
                        jsonObj.put("mobile", "${result.profile?.mobile}")
                        jsonObj.put("provider", "naver")
                        jsonObj.put("access_token", "${NaverIdLoginSDK.getAccessToken()}")
                        sViewModel.snsJo = jsonObj
                        Log.v("jsonObj", "$jsonObj")
                        lifecycleScope.launch(Dispatchers.IO) {
                            identifyEmail(getString(R.string.API_user), jsonObj) { responseJo ->
                                if (responseJo != null) {
                                    trackingUserStatus(responseJo)
                                } else {
                                    CoroutineScope(Dispatchers.Main).launch {
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
            disabledSNSLogin(2)
            NaverIdLoginSDK.authenticate(this, oauthLoginCallback)
        }

        // ------! 카카오톡 OAuth 불러오기 !------
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("카카오톡", "카카오톡 로그인 실패 ${error.message}")
                enabledAllLoginBtn()
                when {
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        enabledAllLoginBtn()
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
                                jsonObj.put("providerId", user.uuid)
                                jsonObj.put("email", user.kakaoAccount?.email)
                                jsonObj.put("user_name", user.kakaoAccount?.name)
                                Log.v("jsonObj", "$jsonObj")
                                sViewModel.snsJo = jsonObj
//                                val encodedUserEmail = URLEncoder.encode(jsonObj.getString("user_email"), "UTF-8")
//                                Log.w("카카오가입>메일", encodedUserEmail)
                                lifecycleScope.launch(Dispatchers.IO) {
                                    identifyEmail(getString(R.string.API_user), jsonObj) { responseJo ->
                                        if (responseJo != null) {
                                            trackingUserStatus(responseJo)
                                        } else {
                                            CoroutineScope(Dispatchers.Main).launch {
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
            disabledSNSLogin(1)
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this@IntroActivity)) {
                UserApiClient.instance.loginWithKakaoTalk(this@IntroActivity, callback = callback)
            }  else {
                UserApiClient.instance.loginWithKakaoAccount(this@IntroActivity, callback = callback)
            }
        }
        // ---- 카카오 로그인 연동 끝 ----

        binding.btnIntroLogin.setOnSingleClickListener{
            val dialog = LoginDialogFragment()
            supportFragmentManager.beginTransaction()
                .addSharedElement(binding.ivIntroLogo, "transLogo")
                .addSharedElement(binding.tvIntroComment, "transComment")
                .setReorderingAllowed(true)
                .add(dialog, "LoginDialogFragment")
                .commitAllowingStateLoss()
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
        // TODO 일단 응답결과에 따른 tracking을 판단해야함
        val status = responseJo.optInt("status")
        when (status) {
            200 -> {
                // 해당 이메일 정보가 이미 있는데 소셜 로그인으로 기존에 한 경우


            }
            201 -> {
                // 해당 이메일 정보가 있긴한데, 자체 회원가입으로 진행한 계정이 있을 경우
            }
            202 -> {
                // 네이버 전용인데 네이버로 했을 때 일단 해당 naver.com이 db에 없을 경우 ( 키오스크 측정 데이터가 있든 없든 상관없음)
                val bottomSheetFragment = AgreementBSDialogFragment()
                bottomSheetFragment.isCancelable = false
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                bottomSheetFragment.setOnFinishListener(object : AgreementBSDialogFragment.OnAgreeListener {
                    override fun onFinish(agree: Boolean) {
                        if (agree) {

                            // 업데이트를 한다? -> 강제로 회원가입이 된거임. 엄격하게 필수동의항목을 동의하지 않으면 회원가입X이기 때문에 -> 정보를 넣어서 t_user_info에 정보가 있는지에 대해 판단해주는 api가 있으면 수정 가능.
//                            jsonObj.put("sms_receive", if (sViewModel.agreementMk1.value == true) "1" else "0")
//                            jsonObj.put("email_receive", if (sViewModel.agreementMk2.value == true) "1" else "0")
//                            CoroutineScope(Dispatchers.IO).launch {
//                                fetchUserUPDATEJson(this@IntroActivity, getString(R.string.API_user), jsonObj.toString(), jo.optInt("sn").toString())
//                            }
//                            Log.v("Intro>SMS", "$jsonObj")
//                            Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")

                            // 기존 입력 jo + 동의 항목인 jsonObj 통합
//                            jo.put("sms_receive", "1")
//                            jo.put("email_receive", "1")
//                            jo.put("device_sn", "1")

//                            storeUserInSingleton(this@IntroActivity, jo)
                            createKey(getString(R.string.SECURE_KEY_ALIAS))
                            Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")
                            val userUUID = Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("user_uuid")
                            val userInfoSn =  Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("sn")?.toInt()
                            if (userUUID != null && userInfoSn != null) {
                                ssm.getMeasures(userUUID, userInfoSn,  CoroutineScope(Dispatchers.IO)) {
                                    mainInit()
                                }
                            }
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
                            lifecycleScope.launch(Dispatchers.IO) {
                                fetchUserDeleteJson(this@IntroActivity, getString(R.string.API_user), "")
                                withContext(Dispatchers.Main) {
                                    this@IntroActivity.let { Toast.makeText(it, "이용약관 동의가 있어야 서비스 이용이 가능합니다", Toast.LENGTH_SHORT).show() }
                                    enabledAllLoginBtn()
                                }
                            }
                        }
                    }
                })
            }
            203 -> {

            }
            // 이메일 정보가 없어서 일단은 회원가입 창으로 이동.
            404, 405, 409, 419, 416 -> {
                val dialog = SignInDialogFragment.newInstance(true)
                dialog.show(supportFragmentManager, "SignInDialogFragment")
            }
            else -> {

            }

        }



//        when (situation) {
//            // ------# 기존 로그인 #------
//            200 -> {
//                storeUserInSingleton(this@IntroActivity, jo)
//                createKey(getString(R.string.SECURE_KEY_ALIAS))
//                Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this).jsonObject}")
//                val userUUID = Singleton_t_user.getInstance(this).jsonObject?.optString("user_uuid") ?: ""
//                val userInfoSn =  Singleton_t_user.getInstance(this).jsonObject?.optString("sn")?.toInt() ?: -1
//                ssm.getMeasures(userUUID, userInfoSn,  CoroutineScope(Dispatchers.IO)) {
//                    mainInit()
//                }
//            }
//            // ------# 최초 회원가입 #------
//            201 -> {
//                // ------! 광고성 수신 동의 문자 시작 !------
//                val bottomSheetFragment = AgreementBSDialogFragment()
//                bottomSheetFragment.isCancelable = false
//                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
//                bottomSheetFragment.setOnFinishListener(object : AgreementBSDialogFragment.OnAgreeListener {
//                    override fun onFinish(agree: Boolean) {
//                        if (agree) {
//
//                            // 업데이트를 한다? -> 강제로 회원가입이 된거임. 엄격하게 필수동의항목을 동의하지 않으면 회원가입X이기 때문에 -> 정보를 넣어서 t_user_info에 정보가 있는지에 대해 판단해주는 api가 있으면 수정 가능.
//                            jsonObj.put("sms_receive", if (sViewModel.agreementMk1.value == true) "1" else "0")
//                            jsonObj.put("email_receive", if (sViewModel.agreementMk2.value == true) "1" else "0")
//                            CoroutineScope(Dispatchers.IO).launch {
//                                fetchUserUPDATEJson(this@IntroActivity, getString(R.string.API_user), jsonObj.toString(), jo.optInt("sn").toString())
//                            }
////                            Log.v("Intro>SMS", "$jsonObj")
////                            Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")
//
//                            // 기존 입력 jo + 동의 항목인 jsonObj 통합
//                            jo.put("sms_receive", "1")
//                            jo.put("email_receive", "1")
//                            jo.put("device_sn", "1")
//
//                            storeUserInSingleton(this@IntroActivity, jo)
//                            createKey(getString(R.string.SECURE_KEY_ALIAS))
//                            Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")
//                            val userUUID = Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("user_uuid")
//                            val userInfoSn =  Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("sn")?.toInt()
//                            if (userUUID != null && userInfoSn != null) {
//                                ssm.getMeasures(userUUID, userInfoSn,  CoroutineScope(Dispatchers.IO)) {
//                                    mainInit()
//                                }
//                            }
//                        } else {
//                            // ------! 동의 하지 않음 -> 삭제 후 intro 유지 !------
//                            if (Firebase.auth.currentUser != null) {
//                                Firebase.auth.signOut()
//                                Log.d("로그아웃", "Firebase sign out successful")
//                            } else if (NaverIdLoginSDK.getState() == NidOAuthLoginState.OK) {
//                                NaverIdLoginSDK.logout()
//                                Log.d("로그아웃", "Naver sign out successful")
//                            } else if (AuthApiClient.instance.hasToken()) {
//                                UserApiClient.instance.logout { error->
//                                    if (error != null) {
//                                        Log.e("로그아웃", "KAKAO Sign out failed", error)
//                                    } else {
//                                        Log.e("로그아웃", "KAKAO Sign out successful")
//                                    }
//                                }
//                            }
//                            lifecycleScope.launch(Dispatchers.IO) {
//                                fetchUserDeleteJson(this@IntroActivity, getString(R.string.API_user), jo.optString("sn"))
//                                withContext(Dispatchers.Main) {
//                                    this@IntroActivity.let { Toast.makeText(it, "이용약관 동의가 있어야 서비스 이용이 가능합니다", Toast.LENGTH_SHORT).show() }
//                                    enabledAllLoginBtn()
//                                }
//                            }
//                        }
//                    }
//                })
//            }
//            2011 -> {
//                storeUserInSingleton(this@IntroActivity, jo)
//                val dialog = MobileAuthDialogFragment.newInstance(jo.optInt("sn"))
//                dialog.show(supportFragmentManager, "MobileAuthDialogFragment")
//                dialog.setOnFinishListener(object: MobileAuthDialogFragment.OnAuthFinishListener{
//                    override fun onFinish(agree: Boolean) {
//                        // 전화번호 인증을 반대했을 때
//                        if (!agree) {
//                            enabledAllLoginBtn()
//                            return
//                        }
//                        // ------! 광고성 수신 동의 문자 시작 !------
//                        val bottomSheetFragment = AgreementBSDialogFragment()
//                        bottomSheetFragment.isCancelable = false
//                        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
//                        bottomSheetFragment.setOnFinishListener(object : AgreementBSDialogFragment.OnAgreeListener {
//                            override fun onFinish(agree: Boolean) {
//                                if (agree) {
//
//                                    // 업데이트를 한다? -> 강제로 회원가입이 된거임. 엄격하게 필수동의항목을 동의하지 않으면 회원가입X이기 때문에 -> 정보를 넣어서 t_user_info에 정보가 있는지에 대해 판단해주는 api가 있으면 수정 가능.
//                                    jsonObj.put("sms_receive", if (sViewModel.agreementMk1.value == true) "1" else "0")
//                                    jsonObj.put("email_receive", if (sViewModel.agreementMk2.value == true) "1" else "0")
//                                    jsonObj.put("mobile", sViewModel.transformMobile)
//
//                                    CoroutineScope(Dispatchers.IO).launch {
//                                        fetchUserUPDATEJson(this@IntroActivity, getString(R.string.API_user), jsonObj.toString(), jo.optInt("sn").toString())
//                                    }
////                                    Log.v("Intro>SMS", "$jsonObj")
////                                    Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")
//
//                                    // 기존 입력 jo + 동의 항목인 jsonObj 통합
//                                    jo.put("sms_receive", "1")
//                                    jo.put("email_receive", "1")
//                                    jo.put("device_sn", "1")
////                                    Log.v("sViewModel", "${sViewModel.transformMobile}, $jo")
//                                    createKey(getString(R.string.SECURE_KEY_ALIAS))
////                                    Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")
//                                    val userUUID = Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("user_uuid")
//                                    val userInfoSn =  Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("sn")?.toInt()
//                                    if (userUUID != null && userInfoSn != null) {
//                                        ssm.getMeasures(userUUID, userInfoSn,  CoroutineScope(Dispatchers.IO)) {
//                                            mainInit()
//                                        }
//                                    }
//                                } else {
//                                    // ------! 동의 하지 않음 -> 삭제 후 intro 유지 !------
//                                    if (Firebase.auth.currentUser != null) {
//                                        Firebase.auth.signOut()
//                                        Log.d("로그아웃", "Firebase sign out successful")
//                                    } else if (NaverIdLoginSDK.getState() == NidOAuthLoginState.OK) {
//                                        NaverIdLoginSDK.logout()
//                                        Log.d("로그아웃", "Naver sign out successful")
//                                    } else if (AuthApiClient.instance.hasToken()) {
//                                        UserApiClient.instance.logout { error->
//                                            if (error != null) {
//                                                Log.e("로그아웃", "KAKAO Sign out failed", error)
//                                            } else {
//                                                Log.e("로그아웃", "KAKAO Sign out successful")
//                                            }
//                                        }
//                                    }
//                                    lifecycleScope.launch(Dispatchers.IO) {
//                                        fetchUserDeleteJson(this@IntroActivity, getString(R.string.API_user), jo.optString("sn"))
//                                        withContext(Dispatchers.Main) {
//                                            this@IntroActivity.let { Toast.makeText(it, "이용약관 동의가 있어야 서비스 이용이 가능합니다", Toast.LENGTH_SHORT).show() }
//                                            enabledAllLoginBtn()
//                                        }
//                                    }
//                                }
//                            }
//                        })
//                    }
//                })
//
//
//            }
//        }
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
                202 -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(this@IntroActivity, "수정이 완료됐습니다\n로그인을 진행해주세요 !", Toast.LENGTH_SHORT).show()
                    }, 500)
                    Handler(Looper.getMainLooper()).postDelayed({
                        val dialog = LoginDialogFragment()
                        dialog.show(supportFragmentManager, "LoginDialogFragment")
                    }, 1500)
                }
                409 -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(this@IntroActivity, "잘못된 접근입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                    }, 500)
                    Handler(Looper.getMainLooper()).postDelayed({
                        val dialog = LoginDialogFragment()
                        dialog.show(supportFragmentManager, "LoginDialogFragment")
                    }, 1200)
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

    private fun disabledSNSLogin(case : Int) {
        when (case) {
            0 -> {
                binding.ibtnGoogleLogin.isEnabled = true
                binding.ibtnKakaoLogin.isEnabled = false
                binding.btnOAuthLoginImg.isEnabled = false
                binding.btnIntroLogin.isEnabled = false
                binding.btnIntroSignIn.isEnabled = false
            }
            1 -> {
                binding.ibtnGoogleLogin.isEnabled = false
                binding.ibtnKakaoLogin.isEnabled = true
                binding.btnOAuthLoginImg.isEnabled = false
                binding.btnIntroLogin.isEnabled = false
                binding.btnIntroSignIn.isEnabled = false
            }
            2 -> {
                binding.ibtnGoogleLogin.isEnabled = false
                binding.ibtnKakaoLogin.isEnabled = false
                binding.btnOAuthLoginImg.isEnabled = true
                binding.btnIntroLogin.isEnabled = false
                binding.btnIntroSignIn.isEnabled = false
            }
        }
    }
    private fun enabledAllLoginBtn() {
        binding.ibtnGoogleLogin.isEnabled = true
        binding.ibtnKakaoLogin.isEnabled = true
        binding.btnOAuthLoginImg.isEnabled = true
        binding.btnIntroLogin.isEnabled = true
        binding.btnIntroSignIn.isEnabled = true
    }
}
