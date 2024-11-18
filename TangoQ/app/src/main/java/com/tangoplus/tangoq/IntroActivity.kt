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
import com.tangoplus.tangoq.`object`.NetworkUser.storeUserInSingleton
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.databinding.ActivityIntroBinding
import com.tangoplus.tangoq.function.SecurePreferencesManager
import com.tangoplus.tangoq.function.SecurePreferencesManager.createKey
import com.tangoplus.tangoq.dialog.bottomsheet.AgreementBSDialogFragment
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.NetworkUser.getUserBySdk
import com.tangoplus.tangoq.function.SaveSingletonManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception


class IntroActivity : AppCompatActivity() {
    lateinit var binding : ActivityIntroBinding

    val sViewModel  : SignInViewModel by viewModels()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var securePref : EncryptedSharedPreferences
    private lateinit var ssm : SaveSingletonManager

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                                            Log.v("user", "${user.phoneNumber}")
                                            Log.v("user", user.uid)
                                            // ----- GOOGLE API: 전화번호 담으러 가기(signin) 시작 -----
                                            val jsonObj = JSONObject()
                                            jsonObj.put("device_sn" ,0)
                                            jsonObj.put("user_sn", 0)
                                            jsonObj.put("user_name", user.displayName.toString())
                                            jsonObj.put("email", user.email.toString())
                                            jsonObj.put("google_login_id", user.uid)
//                                            jsonObj.put("google_id_token", tokenId) // 토큰 값
                                            jsonObj.put("social_account", "google")
//                                            val encodedUserEmail = URLEncoder.encode(jsonObj.getString("user_email"), "UTF-8")
                                            Log.v("jsonObj", "$jsonObj")
                                            getUserBySdk(getString(R.string.API_user), jsonObj, this@IntroActivity) { jo ->
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
                        } ?: throw Exception()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            val intent = Intent(this@IntroActivity, MainActivity::class.java)
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
                        jsonObj.put("device_sn" ,0)
                        jsonObj.put("user_sn", 0)
                        jsonObj.put("user_name", result.profile?.name.toString())
                        jsonObj.put("gender", naverGender)
                        jsonObj.put("mobile", naverMobile)
                        jsonObj.put("email", result.profile?.email.toString())
                        jsonObj.put("birthday", result.profile?.birthYear.toString() + "-" + result.profile?.birthday.toString())
                        jsonObj.put("naver_login_id" , result.profile?.id.toString())
                        jsonObj.put("social_account", "naver")
                        jsonObj.put("device_sn", 0)
                        Log.v("jsonObj", "$jsonObj")
//                        Log.v("네이버이메일", jsonObj.getString("user_email"))
//                        val encodedUserEmail = URLEncoder.encode(jsonObj.getString("user_email"), "UTF-8")
                        getUserBySdk(getString(R.string.API_user), jsonObj, this@IntroActivity) { jo ->
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
                                jsonObj.put("device_sn" ,0)
                                jsonObj.put("user_sn", 0)
                                jsonObj.put("gender", kakaoUserGender)
                                jsonObj.put("mobile", kakaoMobile)
                                jsonObj.put("email", user.kakaoAccount?.email.toString())
                                jsonObj.put("birthday", user.kakaoAccount?.birthyear.toString() + "-" + user.kakaoAccount?.birthday?.substring(0..1) + "-" + user.kakaoAccount?.birthday?.substring(2))
                                jsonObj.put("kakao_login_id" , user.id.toString())
                                jsonObj.put("kakao_id_token", token.idToken)
                                jsonObj.put("social_account", "kakao")

                                Log.v("jsonObj", "$jsonObj")
//                                val encodedUserEmail = URLEncoder.encode(jsonObj.getString("user_email"), "UTF-8")
//                                Log.w("카카오가입>메일", encodedUserEmail)
                                getUserBySdk(getString(R.string.API_user), jsonObj, this@IntroActivity) { jo ->
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
    }

    private fun mainInit() {
        val intent = Intent(this ,MainActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun saveTokenAndIdentifyUser(jo: JSONObject, jsonObj: JSONObject, situation: Int) {
        when (situation) {
            // ------# 기존 로그인 #------
            200 -> {
                storeUserInSingleton(this@IntroActivity, jo)
                createKey(getString(R.string.SECURE_KEY_ALIAS))
                Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this).jsonObject}")
                val userUUID = Singleton_t_user.getInstance(this).jsonObject?.optString("user_uuid")!!
                val userInfoSn =  Singleton_t_user.getInstance(this).jsonObject?.optString("sn")?.toInt()!!
                ssm.getMeasures(userUUID, userInfoSn,  CoroutineScope(Dispatchers.IO)) {
                    mainInit()
                }
            }
            // ------# 최초 회원가입 #------
            201 -> {

                // ------! 광고성 수신 동의 문자 시작 !------
                val bottomSheetFragment = AgreementBSDialogFragment()
                bottomSheetFragment.isCancelable = false
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                bottomSheetFragment.setOnFinishListener(object : AgreementBSDialogFragment.OnAgreeListener {
                    override fun onFinish(agree: Boolean) {
                        if (agree) {

                            // TODO 업데이트를 한다? -> 강제로 회원가입이 된거임. 엄격하게 필수동의항목을 동의하지 않으면 회원가입X이기 때문에 -> 정보를 넣어서 t_user_info에 정보가 있는지에 대해 판단해주는 api가 있으면 수정 가능.
                            jsonObj.put("device_sn" ,0)
                            jsonObj.put("user_sn", 0)
                            jsonObj.put("sms_receive", if (sViewModel.agreementMk1.value == true) "1" else "0")
                            jsonObj.put("email_receive", if (sViewModel.agreementMk2.value == true) "1" else "0")
                            Log.v("Intro>SMS", "$jsonObj")
                            Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")


                            storeUserInSingleton(this@IntroActivity, jo)
                            createKey(getString(R.string.SECURE_KEY_ALIAS))
                            Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(this@IntroActivity).jsonObject}")
                            val userUUID = Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("user_uuid")!!
                            val userInfoSn =  Singleton_t_user.getInstance(this@IntroActivity).jsonObject?.optString("sn")?.toInt()!!
                            ssm.getMeasures(userUUID, userInfoSn,  CoroutineScope(Dispatchers.IO)) {
                                mainInit()
                            }

//                            val dialog = SetupDialogFragment()
//                            dialog.show(supportFragmentManager, "SetupDialogFragment")

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
                            this@IntroActivity.let { Toast.makeText(it, "이용약관 동의가 있어야 서비스 이용이 가능합니다", Toast.LENGTH_SHORT).show() }
                        }
                    }
                })
            }
        }
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val code = intent?.getIntExtra("SignInFinished", 0) ?: 0
        handleSignInResult(code)
    }

    private fun handleSignInResult(code: Int) {
        Log.v("SignInFinished", "$code")
        runOnUiThread {
            if (code == 201) {
                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(this@IntroActivity, "회원가입을 축하합니다 ! 로그인을 진행해주세요 !", Toast.LENGTH_SHORT).show()
                }, 500)
                Handler(Looper.getMainLooper()).postDelayed({
                    val dialog = LoginDialogFragment()
                    dialog.show(supportFragmentManager, "LoginDialogFragment")
                }, 1500)
            }  else if (code == 409) {
                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(this@IntroActivity, "잘못된 접근입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                }, 500)
                Handler(Looper.getMainLooper()).postDelayed({
                    val dialog = LoginDialogFragment()
                    dialog.show(supportFragmentManager, "LoginDialogFragment")
                }, 1200)
            } else if (code == 402){
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
    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }
}
