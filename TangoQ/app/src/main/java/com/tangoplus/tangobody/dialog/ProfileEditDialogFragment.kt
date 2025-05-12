package com.tangoplus.tangobody.dialog

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangobody.MyApplication
import com.tangoplus.tangobody.R
import com.tangoplus.tangobody.adapter.ProfileRVAdapter
import com.tangoplus.tangobody.viewmodel.SignInViewModel
import com.tangoplus.tangobody.db.Singleton_t_user
import com.tangoplus.tangobody.databinding.FragmentProfileEditDialogBinding
import com.tangoplus.tangobody.function.BiometricManager
import com.tangoplus.tangobody.listener.BooleanClickListener
import com.tangoplus.tangobody.listener.ProfileUpdateListener
import com.tangoplus.tangobody.api.NetworkUser.fetchUserUPDATEJson
import com.tangoplus.tangobody.fragment.ExtendedFunctions.setOnSingleClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


class ProfileEditDialogFragment : DialogFragment(), BooleanClickListener {
    lateinit var binding : FragmentProfileEditDialogBinding
    private val svm : SignInViewModel by activityViewModels()
    lateinit var userSn : String
    private var profileMenus = mutableListOf<String>()
    private var profileUpdateListener: ProfileUpdateListener? = null
    private lateinit var biometricManager : BiometricManager
    private lateinit var singletonUser : Singleton_t_user
    fun setProfileUpdateListener(listener: ProfileUpdateListener) {
        this.profileUpdateListener = listener
    }

    override fun onDestroy() {
        super.onDestroy()
        profileUpdateListener?.onProfileUpdated()
    }

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
        singletonUser = Singleton_t_user.getInstance(requireContext())
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.ibtnPEDBack.setOnSingleClickListener { dismiss() }

        // 회원탈퇴
        binding.tvWithDrawal.setOnSingleClickListener {
            val provider = Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("provider") ?: ""
            Log.v("provider", provider)
            if (provider == "null" || provider == "") {
                val dialog = InputDialogFragment.newInstance(2)
                dialog.show(requireActivity().supportFragmentManager, "InputDialogFragment")
            } else {
                Toast.makeText(requireContext(), "소셜로그인으로 로그인한 계정입니다.", Toast.LENGTH_SHORT).show()
                val withDrawalDialog = WithDrawalDialogFragment()
                withDrawalDialog.show(requireActivity().supportFragmentManager, "withDrawalDialogFragment")
            }
        }
        // ------# 초기 생체인증 init #------
        biometricManager = BiometricManager(this)
        biometricManager.authenticate(
            onSuccess = {
                userSn = svm.User.value?.optString("sn").toString()
                // ------! 정보 목록 recyclerView 연결 시작 !------
                profileMenus = mutableListOf(
                    "이름",
                    "이메일",
                    "비밀번호",
                    "전화번호",
                    "몸무게",
                    "신장",
                    "생년월일",
                    "성별"
                )
                setAdapter(profileMenus)
                // ------! 개인정보수정 rv 연결 끝 !------

                // ------! 소셜 계정 로그인 연동 시작 !------
                val snsIntegrations = checkSNSLogin(svm.User.value)

                if (snsIntegrations.first) {
                    binding.tvGoogleInteCheck.text = "계정연동"
                    binding.tvGoogleInteCheck.setTextColor(ContextCompat.getColor(requireContext(), R.color.thirdColor))
                    binding.clGoogle.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                    svm.snsCount += 1
                    Log.v("snsCount", "${svm.snsCount}")
                }
                if (snsIntegrations.second) {
                    binding.tvKakaoIntecheck.text = "계정연동"
                    binding.tvKakaoIntecheck.setTextColor(ContextCompat.getColor(requireContext(), R.color.thirdColor))
                    binding.clKakao.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                    svm.snsCount += 1
                    Log.v("snsCount", "${svm.snsCount}")
                }
                if (snsIntegrations.third) {
                    binding.tvNaverInteCheck.text = "계정연동"
                    binding.tvNaverInteCheck.setTextColor(ContextCompat.getColor(requireContext(), R.color.thirdColor))
                    binding.clNaver.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                    svm.snsCount += 1
                    Log.v("snsCount", "${svm.snsCount}")
                }

                // ------! id, pw, EmailId VM에 값 보존 & 광고성 수신 동의 시작 !------


                when (svm.User.value?.optInt("sms_receive")) {
                    1 -> svm.agreementMk1.value = true
                    else -> svm.agreementMk1.value = false
                }
                when (svm.User.value?.optInt("email_receive")) {
                    1 -> svm.agreementMk2.value = true
                    else -> svm.agreementMk2.value = false
                }
                svm.agreementAll3.observe(viewLifecycleOwner) { binding.schPEDAgreementMk3.isChecked = it }
                svm.agreementMk1.observe(viewLifecycleOwner) { binding.schPEDAgreementMk1.isChecked = it }
                svm.agreementMk2.observe(viewLifecycleOwner) { binding.schPEDAgreementMk2.isChecked = it }
                binding.schPEDAgreementMk1.setOnCheckedChangeListener { _, isChecked -> svm.agreementMk1.value = isChecked }
                binding.schPEDAgreementMk2.setOnCheckedChangeListener { _, isChecked -> svm.agreementMk2.value = isChecked }
                binding.schPEDAgreementMk1.setOnSingleClickListener {
                    if (svm.agreementMk1.value == false) {
                        createDialog(1)
                    } else {
                        setMarketingChecking(1)
                    }
                }
                binding.schPEDAgreementMk2.setOnSingleClickListener {
                    if (svm.agreementMk2.value == false) {
                        createDialog(2)
                    } else {
                        setMarketingChecking(2)
                    }
                }
                binding.schPEDAgreementMk3.setOnSingleClickListener {
                    when (svm.agreementAll3.value) {
                        true -> {
                            setMarketingChecking(3)
                        }
                        false -> {
                            createDialog(3)
                        }
                        null -> {}
                    }
                }
                binding.schPEDAgreementMk3.setOnCheckedChangeListener { _, isChecked ->
                    disabledMkUnit(isChecked)
                    svm.agreementMk1.value = isChecked
                    svm.agreementMk2.value = isChecked
                }
                // application에서 biometric success 저장
                val myApplication = requireActivity().application as MyApplication
                myApplication.setBiometricSuccess()
            },
            onError = {
                Toast.makeText(requireContext(), "인증에 실패했습니다. 다시 시도해주세요", Toast.LENGTH_LONG).show()
                dismiss()
            }
        )
    }

    private fun setAdapter(list: MutableList<String>) {
        binding.rvPED.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val adapter = ProfileRVAdapter(this@ProfileEditDialogFragment, this@ProfileEditDialogFragment, false, "profileEdit", svm)
        adapter.userJson = svm.User.value ?: JSONObject()
        adapter.profileMenus = list
        binding.rvPED.adapter = adapter
        binding.sflPED.visibility = View.GONE
        binding.sflPED.stopShimmer()
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

    }

    private fun checkSNSLogin(jsonObject: JSONObject?) : Triple<Boolean, Boolean, Boolean> {
        var google = false
        var kakao = false
        var naver = false
        val socialAccount = jsonObject?.optString("provider")
        when (socialAccount) {
            "google" -> google = true
            "naver" -> naver = true
            "kakao" -> kakao = true
        }
        return Triple(google, kakao, naver)
    }

    override fun onSwitchChanged(isChecked: Boolean) { }

    private fun disabledMkUnit(isChecked: Boolean) {
        when (isChecked) {
            true -> {
                binding.schPEDAgreementMk1.isEnabled = true
                binding.schPEDAgreementMk2.isEnabled = true
                binding.schPEDAgreementMk1.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor800)))
                binding.tvPEDMk1.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor700)))
                binding.schPEDAgreementMk2.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor800)))
                binding.tvPEDMk2.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor700)))

            }
            false -> {
                binding.schPEDAgreementMk1.isEnabled = false
                binding.schPEDAgreementMk2.isEnabled = false
                binding.schPEDAgreementMk1.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor700)))
                binding.tvPEDMk1.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor500)))
                binding.schPEDAgreementMk2.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor700)))
                binding.tvPEDMk2.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor500)))
            }
        }
    }

    // 정보 수신 동의
    private fun setMarketingChecking(case: Int) {
        val bodyJo = when (case) {
            1 -> {
                svm.User.value?.put("sms_receive", 1)
                JSONObject().apply {
                    put("sms_receive", if (svm.agreementMk1.value == true) 1 else 0)
                }
            }
            2 -> {
                svm.User.value?.put("email_receive", 1)
                JSONObject().apply {
                    put("email_receive", if (svm.agreementMk2.value == true) 1 else 0)
                }
            }
            else -> {
                svm.User.value?.put("sms_receive", 1)
                svm.User.value?.put("email_receive", 1)
                JSONObject().apply {
                    put("sms_receive", if (svm.agreementMk1.value == true) 1 else 0)
                    put("email_receive", if (svm.agreementMk2.value == true) 1 else 0)
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val isUpdateFinished = fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), bodyJo.toString(), userSn)
            when (isUpdateFinished) {
                true -> {
                    Log.w(" 싱글톤객체추가", "$userSn, ${svm.User.value}")
                    singletonUser.jsonObject = svm.User.value
                    requireActivity().runOnUiThread {
                        val dialog = AlertDialogFragment.newInstance(
                            when (case) {
                                1 -> "agreeMk1"
                                2 -> "agreeMk2"
                                else -> "agree"
                            }
                        )
                        dialog.show(requireActivity().supportFragmentManager, "AlertDialogFragment")
                    }
                }
                false -> {

                }
                null -> {
                    lifecycleScope.launch(Dispatchers.Main) {
                        svm.agreementMk1.value = false
                        svm.agreementMk2.value = false
                        svm.agreementAll3.value = false
                        Toast.makeText(requireContext(), "인터넷 연결이 필요합니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun createDialog(case: Int) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("마케팅 동의 해제")
            setMessage(
                when (case) {
                    1 -> "문자메시지 정보 수신"
                    2 -> "이메일 정보 수신"
                    else -> "마케팅 정보 수신"
                } +
                        "동의를 해제하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                val bodyJo = JSONObject()
                when (case) {
                    1 -> {
                        svm.User.value?.put("sms_receive", 0)
                        bodyJo.put("sms_receive", if (svm.agreementMk1.value == true) 1 else 0)
                    }
                    2 -> {
                        svm.User.value?.put("email_receive", 0)
                        bodyJo.put("email_receive", if (svm.agreementMk2.value == true) 1 else 0)
                    }
                    3 -> {
                        svm.User.value?.put("sms_receive", 0)
                        svm.User.value?.put("email_receive", 0)
                        bodyJo.put("sms_receive", if (svm.agreementMk1.value == true) 1 else 0)
                        bodyJo.put("email_receive", if (svm.agreementMk2.value == true) 1 else 0)
                    }
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    val isUpdateFinished = fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), bodyJo.toString(), userSn)
                    if (isUpdateFinished == true) {
                        Log.w(" 싱글톤객체추가", "$userSn, ${svm.User.value}")
                        singletonUser.jsonObject = svm.User.value
                        withContext(Dispatchers.Main) {
                            val dialog = AlertDialogFragment.newInstance(
                                when (case) {
                                    1 -> "disagreeMk1"
                                    2 -> "disagreeMk2"
                                    else -> "disagree"
                                }
                            )
                            dialog.show(requireActivity().supportFragmentManager, "AlertDialogFragment")
                        }
                    }
                }

            }
            setNegativeButton("아니오") { _, _ ->
                when (case) {
                    1 -> svm.agreementMk1.value = true
                    2 -> svm.agreementMk2.value = true
                    3 -> svm.agreementAll3.value = true
                }
            }
            setCancelable(false)
        }.show().apply {
            setCanceledOnTouchOutside(false) // 창 바깥 터치 비활성화
        }
    }
}