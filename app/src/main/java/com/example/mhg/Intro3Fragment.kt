package com.example.mhg

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.mhg.VO.UserVO
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentIntro3Binding
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception

class Intro3Fragment : Fragment() {
    lateinit var binding: FragmentIntro3Binding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var launcher: ActivityResultLauncher<Intent>


    private val TAG = this.javaClass.simpleName
    private lateinit var viewModel: UserViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIntro3Binding.inflate(inflater)

        // viewmodel 불러오기
        viewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        // ---- firebase 초기화 및 Google Login API 연동 시작 ----
        firebaseAuth = Firebase.auth

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            firebaseAuth = FirebaseAuth.getInstance()
            launcher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(), ActivityResultCallback { result ->
                    Log.d(TAG, "resultCode: ${result.resultCode}입니다.")
                    Log.d(TAG, "$result")
                    if (result.resultCode == RESULT_OK) {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        try {
                            task.getResult(ApiException::class.java)?.let { account ->
                                val tokenId = account.idToken
                                Log.d("토큰 있나요?", "예. $tokenId")
                                if (tokenId != null && tokenId != "") {
                                    val credential: AuthCredential =
                                        GoogleAuthProvider.getCredential(account.idToken, null)
                                    firebaseAuth.signInWithCredential(credential)
                                        .addOnCompleteListener {
                                            if (firebaseAuth.currentUser != null) {
                                                Log.d("로그인", "${firebaseAuth.currentUser}")
                                                // ---- Google 토큰에서 가져오기 시작 ----
                                                val user: FirebaseUser = firebaseAuth.currentUser!!
//                                            setToken(requireContext(), "google", firebaseAuth.currentUser.toString())
                                                val JSonObj = JSONObject()
                                                JSonObj.put("user_name", user.displayName.toString())
                                                JSonObj.put("user_email", user.email.toString())
                                                JSonObj.put("mobile", user.phoneNumber.toString())
                                                JSonObj.put("login_token", tokenId)
                                                
                                                fetchJson(R.string.IP_ADDRESS.toString(), JSonObj.toString(), "PUT")

                                                val DataInstance = UserVO(
                                                    name = user.displayName.toString(),
                                                    email = user.email.toString(),
                                                    phoneNumber = user.phoneNumber.toString(),
                                                )
                                                viewModel.User.value = DataInstance

                                                Log.d("구글로그인", "이름: ${viewModel.User.value?.name}, 이메일: ${viewModel.User.value?.email}, 핸드폰번호: ${viewModel.User.value?.phoneNumber}")
                                                val googleSignInToken = account.idToken ?: ""
                                                if (googleSignInToken != "") {
                                                    Log.e(TAG, "googleSignInToken : $googleSignInToken")
                                                } else {
                                                    Log.e(TAG, "googleSignInToken이 null")
                                                }
                                                // ---- Google 토큰에서 가져오기 끝 ----
                                            }
                                        }
                                }
                                val intent = Intent(requireContext(), MainActivity::class.java)
                                startActivity(intent)
                                ActivityCompat.finishAffinity(requireActivity())
                            } ?: throw Exception()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        } else {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            ActivityCompat.finishAffinity(requireActivity())
        }
        // ---- firebase 초기화 및 Google Login API 연동 끝 ----
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ---- 구글 로그인 시작 ----
        binding.ibtnGoogleLogin.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.firebase_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
                val signInIntent: Intent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }
        }
        // ---- 구글 로그인 끝 ----


        // ---- 앱 내 자체 회원가입/로그인 시작 ----
        binding.btnSignin.setOnClickListener {
            val intent = Intent(requireContext(), PersonalSetupActivity::class.java)
            startActivity(intent)

        }
        // ---- 네이버 로그인 연동 시작 ----
        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
            }

            override fun onFailure(httpStatus: Int, message: String) {
                val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                Toast.makeText(
                    requireContext(),
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
                        val JSonObj = JSONObject()
                        JSonObj.put("user_name", result.profile?.name.toString())
                        JSonObj.put("user_email", result.profile?.email.toString())
                        JSonObj.put("mobile", result.profile?.mobile.toString())
                        JSonObj.put("birthday", result.profile?.birthday.toString())
                        JSonObj.put("login_token", NaverIdLoginSDK.getAccessToken())

                        fetchJson(R.string.IP_ADDRESS.toString(), JSonObj.toString(), "PUT")
                        val DataInstance = UserVO(
                            name = result.profile?.name.toString(),
                            email = result.profile?.email.toString(),
                            birth = result.profile?.birthday.toString(),
                            phoneNumber = result.profile?.mobile.toString()
                        )
                        viewModel.User.value = DataInstance
                        Log.d(
                            "네이버",
                            "이름: ${viewModel.User.value?.name}, 이메일: ${viewModel.User.value?.email}, 생년월일: ${viewModel.User.value?.birth} 핸드폰번호: ${viewModel.User.value?.phoneNumber}\""
                        )
                        setToken(
                            requireContext(),
                            "naverToken",
                            NaverIdLoginSDK.getAccessToken().toString()
                        )
                        Log.d(
                            "네이버토큰",
                            "${
                                setToken(
                                    requireContext(),
                                    "naverToken",
                                    NaverIdLoginSDK.getAccessToken().toString()
                                )
                            }"
                        )
                    }
                })
                val intent = Intent(requireContext(), PersonalSetupActivity::class.java)
                startActivity(intent)
                // ---- 네이버 로그인 성공 동작 끝 ----

            }
        }
        binding.buttonOAuthLoginImg.setOAuthLogin(oauthLoginCallback = oauthLoginCallback)
        // ---- 네이버 로그인 연동 끝  ----

        // ---- 카카오톡 OAuth 불러오기 ----
        val callback: (OAuthToken?, Throwable?) -> Unit = {token, error ->
            if (error != null) {
                Log.e("카카오톡", "카카오톡 로그인 실패 $error")
                when {
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Log.e("카카오톡", "접근이 거부 됨(동의 취소) $error")
                    }
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Log.e("카카오톡", "접근이 거부 됨(동의 취소) $error")
                    }
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Log.e("카카오톡", "접근이 거부 됨(동의 취소) $error")
                    }
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Log.e("카카오톡", "접근이 거부 됨(동의 취소) $error")
                    }
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Log.e("카카오톡", "접근이 거부 됨(동의 취소) $error")
                    }
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Log.e("카카오톡", "접근이 거부 됨(동의 취소) $error")
                    }
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Log.e("카카오톡", "접근이 거부 됨(동의 취소) $error")
                    }
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Log.e("카카오톡", "접근이 거부 됨(동의 취소) $error")
                    }

                }
            }
            else if (token != null) {
                Log.e("카카오톡", "로그인에 성공하였습니다.")
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
            }
        }

        // ---- 카카오톡 로그인 성공 끝 ----
        binding.ibtnKakaoLogin.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(requireContext())) {
                UserApiClient.instance.loginWithKakaoTalk(requireContext(), callback = callback)
            }  else {
                UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
            }
        }
        // ---- 카카오 로그인 연동 끝 ----
    }

    private fun setToken(context: Context, key: String, value: String) {
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "encryptedShared", masterKeyAlias, context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit().putString(key, value).apply()
    }
}