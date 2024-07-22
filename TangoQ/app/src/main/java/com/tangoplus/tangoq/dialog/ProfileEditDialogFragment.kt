package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.SetupActivity
import com.tangoplus.tangoq.adapter.ProfileRVAdapter
import com.tangoplus.tangoq.adapter.SpinnerAdapter
import com.tangoplus.tangoq.data.SignInViewModel
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.databinding.FragmentProfileEditDialogBinding
import com.tangoplus.tangoq.listener.BooleanClickListener
import com.tangoplus.tangoq.`object`.CommonDefines
import com.tangoplus.tangoq.`object`.NetworkUser.fetchUserUPDATEJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception
import java.net.URLEncoder


class ProfileEditDialogFragment : DialogFragment(), BooleanClickListener {
    lateinit var binding : FragmentProfileEditDialogBinding
    val viewModel : SignInViewModel by activityViewModels()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var userJson : JSONObject
    private val agreement3 = MutableLiveData(false)
    private val agreementMk1 = MutableLiveData(false)
    private val agreementMk2 = MutableLiveData(false)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileEditDialogBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.AppTheme_DialogFragment)

        viewModel.snsCount = 0
        binding.ibtnPEDBack.setOnClickListener {
            dismiss()
        }

        // ------! 싱글턴에서 가져오기 !------
        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject!!

        // ------! 개인정보수정 rv 연결 시작 !------
// ------! 정보 목록 recyclerView 연결 시작 !------
        val profilemenulist = mutableListOf(
            "이름",
            "성별",
            "몸무게",
            "신장",
            "이메일"
        )
        setAdpater(profilemenulist, binding.rvPED)

        // ------! 정보 목록 recyclerView 연결 끝 !------
        // ------! 개인정보수정 rv 연결 끝 !------

        // ------! 이름, 전화번호 세팅 !------
//        binding.tvPEDMobile.text = userJson.optString("user_mobile")
//        binding.tvPEDName.text = userJson.optString("user_name")


//        // ------! 소셜 계정 로그인 연동 시작 !------
//        firebaseAuth = FirebaseAuth.getInstance()
//        // first: google second: kakao third: naver
//        val snsIntegrations = checkSNSLogin(userJson)
//
//        if (snsIntegrations.first) {
//            binding.tvGoogleInteCheck.text = "연동완료"
//            viewModel.snsCount += 1
//            Log.v("snsCount", "${viewModel.snsCount}")
//        }
//        if (snsIntegrations.second) {
//            binding.tvKakaoIntecheck.text = "연동완료"
//            viewModel.snsCount += 1
//            Log.v("snsCount", "${viewModel.snsCount}")
//        }
//        if (snsIntegrations.third) {
//            binding.tvNaverInteCheck.text = "연동완료"
//            viewModel.snsCount += 1
//            Log.v("snsCount", "${viewModel.snsCount}")
//        }
//
//        // ------! 파이어베이스 초기화 및 구글 연동 시작 !------
//        val currentUser = Firebase.auth.currentUser
//        if (currentUser == null) {
//
//            launcher = registerForActivityResult(
//                ActivityResultContracts.StartActivityForResult()
//            ) { result ->
//                Log.v(CommonDefines.TAG, "resultCode: ${result.resultCode}입니다.")
//                if (result.resultCode == AppCompatActivity.RESULT_OK) {
//                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
//                    try {
//                        task.getResult(ApiException::class.java)?.let { account ->
//                            val tokenId = account.idToken
//                            if (tokenId != null && tokenId != "") {
//                                val credential: AuthCredential =
//                                    GoogleAuthProvider.getCredential(account.idToken, null)
//                                firebaseAuth.signInWithCredential(credential)
//                                    .addOnCompleteListener {
//                                        if (firebaseAuth.currentUser != null) {
//                                            // ---- Google 토큰에서 가져오기 시작 ----
//                                            val user: FirebaseUser = firebaseAuth.currentUser!!
//                                            val jsonObj = JSONObject()
//                                            jsonObj.put("google_login_id", user.uid)
//                                            Log.v("소셜계정연동", "jsonObj: $jsonObj")
//                                            fetchUserUPDATEJson(
//                                                getString(R.string.IP_ADDRESS_t_user),
//                                                jsonObj.toString(),
//                                                userJson.getString("user_email")
//                                            ) {
//                                                requireActivity().runOnUiThread {
//                                                    binding.tvGoogleInteCheck.text = "연동완료"
//                                                    viewModel.snsCount += 1
//                                                }
//
//                                                Singleton_t_user.getInstance(requireContext()).jsonObject?.getJSONObject(
//                                                    "data"
//                                                )?.put("google_login_id", user.uid)
//                                                Log.v(
//                                                    "구글>singleton",
//                                                    "${Singleton_t_user.getInstance(requireContext()).jsonObject}"
//                                                )
//                                            }
//
//                                            // ---- Google 토큰에서 가져오기 끝 ----
//                                        }
//                                    }
//                            }
//                        } ?: throw Exception()
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
//            }
//        }
//        // ---- firebase 초기화 및 Google Login API 연동 끝 ----
//
//
//        // ---- 네이버 로그인 연동 시작 ----
//        val oauthLoginCallback = object : OAuthLoginCallback {
//            override fun onError(errorCode: Int, message: String) {
//                onFailure(errorCode, message)
//            }
//
//            override fun onFailure(httpStatus: Int, message: String) {
//                val errorCode = NaverIdLoginSDK.getLastErrorCode().code
//                val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
//                Toast.makeText(
//                    requireContext(),
//                    "errorCode: $errorCode, errorDesc: $errorDescription",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//
//            override fun onSuccess() {
//                // ---- 네이버 로그인 성공 동작 시작 ----
//                NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
//                    override fun onError(errorCode: Int, message: String) {}
//                    override fun onFailure(httpStatus: Int, message: String) {}
//
//                    override fun onSuccess(result: NidProfileResponse) {
//                        val jsonObj = JSONObject()
//                        val uid = result.profile?.id.toString()
//                        jsonObj.put("naver_login_id" , uid)
//                        fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), jsonObj.toString(), userJson.optString("user_email")) {
//                            Singleton_t_user.getInstance(requireContext()).jsonObject?.getJSONObject("data")?.put("naver_login_id", uid)
//                            requireActivity().runOnUiThread{
//                                binding.tvNaverInteCheck.text = "연동완료"
//                                viewModel.snsCount += 1
//                            }
//                        }
//
//                    }
//                })
//                // ---- 네이버 로그인 성공 동작 끝 ----
//            }
//        }
//
//        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
//            if (error != null) {
//                Log.e("kakaoTalk", "kakaoTalk 로그인 실패 $error")
//                when {
//                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
//                        Log.e("kakaoTalk", "접근이 거부 됨(동의 취소) $error")
//                    }
//                }
//            } else if (token != null) {
//                Log.e("kakaoTalk", "로그인에 성공하였습니다.")
//                UserApiClient.instance.accessTokenInfo { tokenInfo, error1 ->
//                    if (error1 != null) {
//                        Log.e(CommonDefines.TAG, "토큰 정보 보기 실패", error1)
//                    }
//                    else if (tokenInfo != null) {
//                        UserApiClient.instance.me { user, error2 ->
//                            if (error2 != null) {
//                                Log.e(CommonDefines.TAG, "사용자 정보 요청 실패", error2)
//                            }
//                            else if (user != null) {
//                                val jsonObj = JSONObject()
//                                jsonObj.put("kakao_login_id" , user.id.toString())
//                               fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), jsonObj.toString(), userJson.optString("user_email")) {
//                                   Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("data")?.put("kakao_login_id", user.id.toString())
//                                   requireActivity().runOnUiThread{
//                                       binding.tvKakaoIntecheck.text = "연동완료"
//                                       viewModel.snsCount += 1
//                                   }
//
//                               }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        binding.clGoogle.setOnClickListener {
//            when (binding.tvGoogleInteCheck.text) {
//                "미연결" -> {
//                    CoroutineScope(Dispatchers.IO).launch {
//                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                            .requestIdToken(getString(R.string.firebase_client_id))
//                            .requestEmail()
//                            .build()
//                        val googleSignInClient = GoogleSignIn.getClient(requireActivity() , gso)
//                        val signInIntent: Intent = googleSignInClient.signInIntent
//                        launcher.launch(signInIntent)
//                    }
//                }
//                "연동완료" -> {
//                    if (viewModel.snsCount > 1) {
////                        showLogoutDialog("구글")
//                        Log.v("snsCount>google", "${viewModel.snsCount}")
//                    }
//                }
//            }
//        }
//
//        binding.clKakao.setOnClickListener {
//            when (binding.tvKakaoIntecheck.text) {
//                "미연결" -> {
//                    if (UserApiClient.instance.isKakaoTalkLoginAvailable(requireContext())) {
//                        UserApiClient.instance.loginWithKakaoTalk(requireContext(), callback = callback)
//                    }  else {
//                        UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
//                    }
//                }
//                "연동완료" -> {
//                    if (viewModel.snsCount > 1) {
////                        showLogoutDialog("카카오")
//                        Log.v("snsCount>카카오", "${viewModel.snsCount}")
//                    }
//                }
//            }
//        }
//
//        binding.clNaver.setOnClickListener {
//            when (binding.tvNaverInteCheck.text) {
//                "미연결" -> {
//                    NaverIdLoginSDK.authenticate(requireContext(), oauthLoginCallback)
//                }
//                "연동완료" -> {
//                    if (viewModel.snsCount > 1) {
////                        showLogoutDialog("네이버")
//                        Log.v("snsCount>네이버", "${viewModel.snsCount}")
//                    }
//                }
//            }
//        }
        // ------! 소셜 계정 로그인 연동 끝 !------

        // ------! id, pw, EmailId VM에 값 보존 시작 !------
        viewModel.id.value = userJson.optString("user_id")
//        binding.etPEDId.addTextChangedListener(object: TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                viewModel.id.value = s.toString()
//            }
//        })
//        viewModel.id.observe(viewLifecycleOwner) {id ->
//            if (id != binding.etPEDId.text.toString()) {
//                binding.etPEDId.setText(id)
//            }
//        }
//
//        binding.etPEDPassword.addTextChangedListener(object: TextWatcher{
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                viewModel.pw.value = s.toString()
//            }
//        })
//        viewModel.pw.observe(viewLifecycleOwner) {pw ->
//            if (pw != binding.etPEDPassword.text.toString()) {
//                binding.etPEDPassword.setText(pw)
//            }
//        }
//        binding.etPEDEmailId.addTextChangedListener(object: TextWatcher{
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                viewModel.emailId.value = s.toString()
//            }
//        })
//        viewModel.emailId.observe(viewLifecycleOwner) {emailId ->
//            if (emailId != binding.etPEDEmailId.text.toString()) {
//                binding.etPEDEmailId.setText(emailId)
//            }
//        }
//        // ------! id, pw, EmailId VM에 값 보존 끝 !------
//        val userEmail = userJson.optString("user_email")
//        binding.etPEDEmailId.setText(userEmail.substring(0, userEmail.indexOf("@")))
//
//
//        when (userEmail.substringAfter("@")) {
//            "gmail.com" -> {
//                binding.spnPED.setSelection(0)
//            }
//            "naver.com" -> {
//                binding.spnPED.setSelection(1)
//            }
//            "kakao.com" -> {
//                binding.spnPED.setSelection(2)
//            }
//            else -> {
//                binding.etPEDEmail.visibility = View.GONE
//                binding.etPEDEmail.setText(userEmail.substringAfter("@"))
//                binding.spnPED.visibility = View.VISIBLE
//            }
//        }
//
//        val domainList = listOf("gmail.com", "naver.com", "kakao.com", "직접입력")
//        binding.spnPED.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, domainList)
//        binding.spnPED.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
//            override fun onItemSelected(
//                parent: AdapterView<*>?,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {
//                binding.spnPED.getItemAtPosition(position).toString()
//                if (position == 3) {
//                    binding.etPEDEmail.visibility = View.VISIBLE
//                    binding.spnPED.visibility = View.GONE
//                    binding.ivPEDSpn.setOnClickListener{
//                        binding.spnPED.performClick()
//                        binding.spnPED.visibility = View.VISIBLE
//                    }
//
//                } else {
//                    binding.etPEDEmail.visibility = View.GONE
//                    binding.etPEDEmail.setText("")
//                    binding.spnPED.visibility = View.VISIBLE
//                }
//            }
//            override fun onNothingSelected(parent: AdapterView<*>?) {}
//        }

        when (userJson.optInt("sms_receive")) {
            1 -> agreementMk1.value = true
            else -> agreementMk1.value = false
        }
        Log.v("userJson", userJson.optInt("sms_receive").toString())
        when (userJson.optInt("email_receive")) {
            1 -> agreementMk2.value = true
            else -> agreementMk2.value = false
        }
        Log.v("userJson", userJson.optInt("email_receive").toString())
        // ------! 광고성 수신 동의 시작 !------
        binding.clPEDAgreement3.setOnClickListener{
            val newValue = agreement3.value?.not() ?: false
            binding.ivPEDAgreement3.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )

            binding.ivPEDAgreementMk1.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            binding.ivPEDAgreementMk2.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            agreement3.value = newValue
            agreementMk1.value = newValue
            agreementMk2.value = newValue
        }
        binding.ibtnPEDAgreement3.setOnClickListener {
            val dialog = AgreementDetailDialogFragment.newInstance("agreement3")
            dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
        }
        binding.clPEDAgreementMk1.setOnClickListener {
            val newValue = agreementMk1.value?.not() ?: false
            binding.ivPEDAgreementMk1.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            agreementMk1.value = newValue
        }
        binding.clPEDAgreementMk2.setOnClickListener {
            val newValue = agreementMk2.value?.not() ?: false
            binding.ivPEDAgreementMk2.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            agreementMk2.value = newValue
        }
        agreement3.observe(viewLifecycleOwner) {
            updateAgreeMarketingAllState()
        }


        agreementMk1.observe(viewLifecycleOwner) {
            Log.v("광고성1", "${agreementMk1.value}")
            updateAgreeMarketingAllState()
        }

        agreementMk2.observe(viewLifecycleOwner) {
            updateAgreeMarketingAllState()
            Log.v("광고성2", "${agreementMk2.value}")

        }

        binding.btnPEDFinish.setOnClickListener {
            userJson.put("user_id", viewModel.id.value.toString())
            userJson.put("user_password", viewModel.pw.value.toString())
            userJson.put("sms_receive", agreementMk1.value)
            userJson.put("email_receive", agreementMk2.value)

//            when (binding.spnPED.selectedItemPosition) {
//                0, 1, 2 -> {
//                    viewModel.User.value?.put("user_email", "${binding.etPEDEmailId.text}@${binding.spnPED.selectedItem as String}")
//                    userJson.put("user_email", viewModel.User.value?.optString("user_email"))
//                }
//                else -> {
//                    viewModel.User.value?.put("user_email", "${binding.etPEDEmailId.text}@${binding.etPEDEmail.text}")
//                    userJson.put("user_email", viewModel.User.value?.optString("user_email"))
//                }
//            }

            val userEditEmail = userJson.optString("user_email")
            val encodedUserEmail = URLEncoder.encode(userEditEmail, "UTF-8")
//            fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), userJson.toString(), encodedUserEmail) {
//                Log.w(ContentValues.TAG +" 싱글톤객체추가", userJson.optString("user_weight").toString())
//                dismiss()
//            }
        }
        binding.btnPEDGoSetup.setOnClickListener {
            val intent = Intent(requireContext(), SetupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setAdpater(list: MutableList<String>, rv: RecyclerView ) {
        val adapter = ProfileRVAdapter(this@ProfileEditDialogFragment, this@ProfileEditDialogFragment, false, "profileEdit")
        adapter.profilemenulist = list
        adapter.userJson = userJson
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    navigateGallery()
                else
                    Toast.makeText(requireContext(), "권한 설정을 허용해 주십시오", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPermissionContextPopup() {
        AlertDialog.Builder(requireContext())
            .setTitle("권한이 필요합니다.")
            .setMessage("프로필 이미지를 바꾸기 위해서는 갤러리 접근 권한이 필요합니다.")
            .setPositiveButton("동의하기") { _, _ ->
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1000)
            }
            .setNegativeButton("취소하기") { _, _ -> }
            .create()
            .show()
    }
    private fun navigateGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 2000)

    }
    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun checkSNSLogin(jsonObject: JSONObject?) : Triple<Boolean, Boolean, Boolean> {
        var google = false
        var kakao = false
        var naver = false
//
//        val googleLoginId = jsonObject.optString("google_login_id")
//        if (googleLoginId != null && googleLoginId.isNotEmpty() && googleLoginId != "null") {
//            google = true
//        }
//
//        val kakaoLoginId = jsonObject.optString("kakao_login_id")
//        if (kakaoLoginId != null && kakaoLoginId.isNotEmpty() && kakaoLoginId != "null") {
//            kakao = true
//        }
//
//        val naverLoginId = jsonObject.optString("naver_login_id")
//        if (naverLoginId != null && naverLoginId.isNotEmpty() && naverLoginId != "null") {
//            naver = true
//        }
//
//
//        Log.v("sns", "google: $google kakao: $kakao naver : $naver")
        return Triple(google, kakao, naver)
    }

//    @SuppressLint("SetTextI18n")
//    private fun showLogoutDialog(title: String) {
//        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
//            setTitle("알림")
//            setMessage("${title}의 소셜 로그인 연동을\n해제하시겠습니까?")
//            setPositiveButton("예") { _, _ ->
//                when (title) {
//                    "구글" -> {
//                        Firebase.auth.signOut()
//                        viewModel.snsCount -= 1
//                        Log.e("로그아웃", "Firebase sign out successful")
//                        val jsonObject = JSONObject()
//                        jsonObject.put("google_login_id", "")
//                        fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), jsonObject.toString(), userJson.optString("user_email")) {
//                            requireActivity().runOnUiThread {
//                                binding.tvGoogleInteCheck.text = "미연결"
//                                Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("data")?.put("google_login_id", "")
//                            }
//                        }
//                    }
//                    "네이버" -> {
//                        NaverIdLoginSDK.logout()
//                        viewModel.snsCount -= 1
//                        Log.e("로그아웃", "Naver sign out successful")
//                        val jsonObject = JSONObject()
//                        jsonObject.put("naver_login_id", "")
//                        fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), jsonObject.toString(), userJson.optString("user_email")) {
//                            requireActivity().runOnUiThread {
//                                binding.tvNaverInteCheck.text = "미연결"
//                                Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("data")?.put("naver_login_id", "")
//                            }
//                        }
//                    }
//                    "카카오" -> {
//                        viewModel.snsCount -= 1
//                        UserApiClient.instance.unlink { error->
//                            if (error != null) {
//                                Log.e("로그아웃", "KAKAO Sign out failed", error)
//                            } else {
//                                    Log.e("로그아웃", "KAKAO Sign out successful")
//                            }
//                        }
//                        val jsonObject = JSONObject()
//                        jsonObject.put("kakao_login_id", "")
//                        fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), jsonObject.toString(), userJson.optString("user_email")) {
//                            requireActivity().runOnUiThread {
//                                binding.tvKakaoIntecheck.text = "미연결"
//                                Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("data")?.put("kakao_login_id", "")
//                            }
//                        }
//                    }
//                }
//            }
//            setNegativeButton("아니오") { _, _ ->
//            }
//            create()
//        }.show()
//    }

    private fun updateAgreeMarketingAllState() {
        val allChecked = agreementMk1.value == true && agreementMk2.value == true
        if (agreement3.value != allChecked) {
            agreement3.value = allChecked
            binding.ivPEDAgreement3.setImageResource(
                if (allChecked) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            binding.ivPEDAgreementMk1.setImageResource(
                if (allChecked) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            binding.ivPEDAgreementMk2.setImageResource(
                if (allChecked) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
        }
    }

    override fun onSwitchChanged(isChecked: Boolean) { }
}