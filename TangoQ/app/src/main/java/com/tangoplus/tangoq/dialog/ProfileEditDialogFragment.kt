package com.tangoplus.tangoq.dialog

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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ProfileRVAdapter
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.databinding.FragmentProfileEditDialogBinding
import com.tangoplus.tangoq.function.BiometricManager
import com.tangoplus.tangoq.listener.BooleanClickListener
import com.tangoplus.tangoq.listener.ProfileUpdateListener
import com.tangoplus.tangoq.api.NetworkUser.fetchUserUPDATEJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject


class ProfileEditDialogFragment : DialogFragment(), BooleanClickListener {
    lateinit var binding : FragmentProfileEditDialogBinding
    private val svm : SignInViewModel by activityViewModels()
    lateinit var userSn : String
    private var profilemenulist = mutableListOf<String>()
    private var profileUpdateListener: ProfileUpdateListener? = null
    private lateinit var biometricManager : BiometricManager

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

        // ------# 초기 생체인증 init #------
        biometricManager = BiometricManager(this)
        biometricManager.authenticate(
            onSuccess = {
                binding.sflPED.startShimmer()
                svm.snsCount = 0
                // ------! 싱글턴에서 가져오기 !------
                svm.User.value = Singleton_t_user.getInstance(requireContext()).jsonObject
                svm.setHeight.value = svm.User.value?.optInt("height")
                svm.setWeight.value = svm.User.value?.optInt("weight")
                svm.setEmail.value = svm.User.value?.optString("email")
                svm.setBirthday.value = svm.User.value?.optInt("birthday").toString()
                svm.setMobile.value = svm.User.value?.optString("mobile").toString()
                userSn = svm.User.value?.optString("sn").toString()
                Log.v("개인정보편집", "${svm.User.value}")

                // ------! 정보 목록 recyclerView 연결 시작 !------
                profilemenulist = mutableListOf(
                    "이름",
                    "이메일",
                    "비밀번호",
                    "전화번호",
                    "몸무게",
                    "신장",
                    "생년월일",
                    "성별"
                )
                setAdapter(profilemenulist)

                // ------! 정보 목록 recyclerView 연결 끝 !------
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
                svm.id.value = svm.User.value?.optString("user_id")

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
                binding.schPEDAgreementMk1.setOnClickListener { createDialog(1) }
                binding.schPEDAgreementMk2.setOnClickListener { createDialog(2) }
                binding.schPEDAgreementMk3.setOnClickListener {
                    when (svm.agreementAll3.value) {
                        true -> {
                            svm.User.value?.put("sms_receive", 1)
                            svm.User.value?.put("email_receive", 1)

                            val bodyJo = JSONObject().apply {
                                put("sms_receive", if (svm.agreementMk1.value == true) 1 else 0)
                                put("email_receive", if (svm.agreementMk2.value == true) 1 else 0)
                            }
                            CoroutineScope(Dispatchers.IO).launch {
                                val isUpdateFinished = fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), bodyJo.toString(), userSn)
                                if (isUpdateFinished == true) {
                                    Log.w(" 싱글톤객체추가", "$userSn, ${svm.User.value}")
                                    Singleton_t_user.getInstance(requireContext()).jsonObject = svm.User.value
                                    requireActivity().runOnUiThread {
                                        val dialog = AlertDialogFragment.newInstance("agree")
                                        dialog.show(requireActivity().supportFragmentManager, "AlertDialogFragment")
                                    }
                                }
                            }
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
//                binding.btnPEDFinish.setOnClickListener {
////            viewModel.User.value?.put("user_id", viewModel.id.value.toString())
////            viewModel.User.value?.put("password", viewModel.pw.value.toString())
//                    svm.User.value?.put("sms_receive", if (svm.agreementMk1.value == true) 1 else 0)
//                    svm.User.value?.put("email_receive", if (svm.agreementMk2.value == true) 1 else 0)
//                    svm.User.value?.put("height", svm.setHeight.value)
//                    svm.User.value?.put("weight", svm.setWeight.value)
//                    svm.User.value?.put("email", svm.setEmail.value)
//                    Log.v("userJson>receive", "${svm.User.value}")
////            val userEditEmail = userJson.optString("user_email")
////            val encodedUserEmail = URLEncoder.encode(userEditEmail, "UTF-8")
//                    fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), svm.User.value?.toString().toString(), userSn) {
//                        Log.w(" 싱글톤객체추가", "$userSn, ${svm.User.value}")
//                        Singleton_t_user.getInstance(requireContext()).jsonObject = svm.User.value
////                requireActivity().runOnUiThread{
////                    uViewModel.setupProgress = 34
////                    uViewModel.setupStep = 0
////                    uViewModel.step1.value = null
////                    uViewModel.step21.value = null
////                    uViewModel.step22.value = null
////                    uViewModel.step2.value = null
////                    uViewModel.step31.value = null
////                    uViewModel.step32.value = null
////                    uViewModel.step3.value = null
////                    uViewModel.User.value = null
////                    uViewModel.User.value = null
////                }
//                        onEditComplete()
//                    }
//                }
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
        adapter.profileMenuList = list
        binding.rvPED.adapter = adapter
        binding.sflPED.visibility = View.GONE
        binding.sflPED.stopShimmer()
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//        setAdapter(profilemenulist, binding.rvPED)
    }

    private fun checkSNSLogin(jsonObject: JSONObject?) : Triple<Boolean, Boolean, Boolean> {
        var google = false
        var kakao = false
        var naver = false
        val socialAccount = jsonObject?.optString("social_account")
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

    private fun createDialog(case: Int) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("마케팅 동의 해제")
            setMessage("마케팅 이용 동의를 해제하시겠습니까?")
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
                CoroutineScope(Dispatchers.IO).launch {
                    val isUpdateFinished = fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), bodyJo.toString(), userSn)
                    if (isUpdateFinished == true) {
                        Log.w(" 싱글톤객체추가", "$userSn, ${svm.User.value}")
                        Singleton_t_user.getInstance(requireContext()).jsonObject = svm.User.value
                        requireActivity().runOnUiThread {
                            val dialog = AlertDialogFragment.newInstance("disagree")
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