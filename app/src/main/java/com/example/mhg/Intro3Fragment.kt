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
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentIntro3Binding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
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
import java.lang.Exception
import kotlin.coroutines.coroutineContext

class Intro3Fragment : Fragment() {
    lateinit var binding: FragmentIntro3Binding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var launcher: ActivityResultLauncher<Intent>

    private val TAG = this.javaClass.simpleName
    private lateinit var viewModel: UserViewModel
    fun fetchJson(myUrl : String, json: String, category: String, context: Context, callback: () -> Unit){
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("$myUrl?category=$category")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback  {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OKHTTP3", "Failed to execute request!")
            }
            override fun onResponse(call: Call, response: Response)  {
                val responseBody = response.body?.string()
                Log.e("OKHTTP3", "Success to execute request!: $responseBody")
//                val jsonDataArray = JSONArray(responseBody)
//                val jsonObj = jsonDataArray.getJSONObject(0)
//                val t_userInstance = Singleton_t_user.getInstance(context)
//                t_userInstance.jsonObject = jsonObj
//                Log.e("OKHTTP3>싱글톤", "${t_userInstance.jsonObject}")
                callback()
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIntro3Binding.inflate(inflater)
        val currentUser = Firebase.auth.currentUser
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

//                                                // ----- GOOGLE API에서 DB에 넣는 공간 시작 -----
                                                val JsonObj = JSONObject()
                                                JsonObj.put("user_id", user.email.toString())
                                                JsonObj.put("user_password", user.email.toString())
                                                JsonObj.put("user_name", user.displayName.toString())
                                                JsonObj.put("user_gender", "male")
                                                JsonObj.put("user_grade", 1)
                                                JsonObj.put("user_mobile", user.phoneNumber.toString())
                                                JsonObj.put("user_email", user.email.toString())
                                                JsonObj.put("login_token", tokenId)
                                                Log.e("구글JsonObj", JsonObj.getString("user_email"))
                                                fetchJson(getString(R.string.IP_ADDRESS), JsonObj.toString(), "PUT", requireActivity().applicationContext) {
                                                    val intent = Intent(requireContext(), MainActivity::class.java)
                                                    startActivity(intent)
                                                    ActivityCompat.finishAffinity(requireActivity())
                                                }
//                                                    Singleton_t_user.getInstance(requireContext()).jsonObject = JsonObj // 질의를 통해 db에 넣음과 동시에 해당 데이터 singleton 저장
//                                                    Log.w("싱글톤_구글회원가입", "${Singleton_t_user.getInstance(requireActivity()).jsonObject}")

                                                // ----- GOOGLE API에서 DB에 넣는 공간 끝 -----

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
        // viewmodel 불러오기
        viewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)


        // ---- firebase 초기화 및 Google Login API 연동 끝 ----
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ---- firebase 초기화 및 Google Login API 연동 시작 ----

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
                // ----- GOOGLE API에서 DB에 넣는 공간 시작 -----
//                val JsonObj = JSONObject()
//                val user: FirebaseUser = firebaseAuth.currentUser!!
//                JsonObj.put("user_id", user.email.toString())
//                JsonObj.put("user_password", user.email.toString())
//                JsonObj.put("user_name", user.displayName.toString())
//                JsonObj.put("user_gender", "male")
//                JsonObj.put("user_grade", 1)
//                JsonObj.put("user_mobile", user.phoneNumber.toString())
//                JsonObj.put("user_email", user.email.toString())
//
//
//                JsonObj.put("login_token", )
//                fetchJson(getString(R.string.IP_ADDRESS), JsonObj.toString(), "PUT")

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
                        val JsonObj = JSONObject()
                        JsonObj.put("user_id" , result.profile?.id.toString())
                        JsonObj.put("user_name", result.profile?.name.toString())
                        JsonObj.put("user_gender",result.profile?.gender.toString())
                        JsonObj.put("user_mobile", result.profile?.mobile.toString())
                        JsonObj.put("user_email", result.profile?.email.toString())
                        JsonObj.put("user_birthday", result.profile?.birthYear.toString() + result.profile?.birthday.toString())
                        JsonObj.put("login_token", NaverIdLoginSDK.getAccessToken())

                        fetchJson(getString(R.string.IP_ADDRESS), JsonObj.toString(), "PUT", requireActivity().applicationContext) {
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            startActivity(intent)
                            ActivityCompat.finishAffinity(requireActivity())
                        }
                        Singleton_t_user.getInstance(requireActivity()).jsonObject = JsonObj
                        Log.w("싱글톤_네이버회원가입", "${Singleton_t_user.getInstance(requireActivity()).jsonObject}")
                        setToken(
                            requireActivity(),
                            "naverToken",
                            NaverIdLoginSDK.getAccessToken().toString()
                        )
                        Log.d("네이버토큰", "${setToken(requireActivity(), "naverToken", NaverIdLoginSDK.getAccessToken().toString())}")
                    }
                })
                val intent = Intent(requireActivity(), PersonalSetupActivity::class.java)
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
                                JsonObj.put("user_id" , user.kakaoAccount?.email.toString())
                                JsonObj.put("user_name" , user.kakaoAccount?.name.toString())
                                JsonObj.put("user_gender", user.kakaoAccount?.gender.toString())
                                JsonObj.put("user_mobile", user.kakaoAccount?.phoneNumber.toString())
                                JsonObj.put("user_email", user.kakaoAccount?.email.toString())
                                JsonObj.put("user_birthday", user.kakaoAccount?.birthyear.toString() + user.kakaoAccount?.birthday.toString())
                                JsonObj.put("login_token" , tokenInfo.id.toString())

                                fetchJson(getString(R.string.IP_ADDRESS), JsonObj.toString(), "PUT", requireActivity().applicationContext) {
                                    val intent = Intent(requireContext(), MainActivity::class.java)
                                    startActivity(intent)
                                    ActivityCompat.finishAffinity(requireActivity())
                                }
                                Singleton_t_user.getInstance(requireContext()).jsonObject = JsonObj
                                Log.w("싱글톤_카카오회원가입", "${Singleton_t_user.getInstance(requireContext()).jsonObject}")
                                TODO("싱글톤에 넣는 작업 필요")
                            }
                        }
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        }
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