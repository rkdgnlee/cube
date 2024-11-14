package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ProfileRVAdapter
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.databinding.FragmentProfileEditDialogBinding
import com.tangoplus.tangoq.listener.BooleanClickListener
import com.tangoplus.tangoq.listener.ProfileUpdateListener
import com.tangoplus.tangoq.`object`.NetworkUser.fetchUserUPDATEJson
import org.json.JSONObject


class ProfileEditDialogFragment : DialogFragment(), BooleanClickListener {
    lateinit var binding : FragmentProfileEditDialogBinding
    val svm : SignInViewModel by activityViewModels()

    private val agreement3 = MutableLiveData(false)
    private val agreementMk1 = MutableLiveData(false)
    private val agreementMk2 = MutableLiveData(false)
    lateinit var userSn : String
    private var profilemenulist = mutableListOf<String>()
    private var profileUpdateListener: ProfileUpdateListener? = null

    fun setProfileUpdateListener(listener: ProfileUpdateListener) {
        this.profileUpdateListener = listener
    }
    private fun onEditComplete() {
        profileUpdateListener?.onProfileUpdated()
        dismiss()
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

        svm.snsCount = 0
        // ------! 싱글턴에서 가져오기 !------
        svm.User.value = Singleton_t_user.getInstance(requireContext()).jsonObject!!

        svm.setHeight.value = svm.User.value!!.optInt("height")
        svm.setWeight.value = svm.User.value!!.optInt("weight")
        svm.setEmail.value = svm.User.value!!.optString("email")


        userSn = svm.User.value!!.optString("sn")
        Log.v("개인정보편집", "${svm.User.value}")

        // ------! 정보 목록 recyclerView 연결 시작 !------
        profilemenulist = mutableListOf(
            "이름",
            "이메일",
            "몸무게",
            "신장",
            "성별"
        )
        setAdapter(profilemenulist)

        // ------! 정보 목록 recyclerView 연결 끝 !------
        // ------! 개인정보수정 rv 연결 끝 !------

        // ------! 이름, 전화번호 세팅 !------
//        binding.tvPEDMobile.text = userJson.optString("user_mobile")
//        binding.tvPEDName.text = userJson.optString("user_name")


//        // ------! 소셜 계정 로그인 연동 시작 !------
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

        // ------! id, pw, EmailId VM에 값 보존 시작 !------
        svm.id.value = svm.User.value!!.optString("user_id")

        when (svm.User.value!!.optInt("sms_receive")) {
            1 -> agreementMk1.value = true
            else -> agreementMk1.value = false
        }
        Log.v("userJson", svm.User.value!!.optInt("sms_receive").toString())
        when (svm.User.value!!.optInt("email_receive")) {
            1 -> agreementMk2.value = true
            else -> agreementMk2.value = false
        }
        Log.v("userJson", svm.User.value!!.optInt("email_receive").toString())
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
//            viewModel.User.value!!.put("user_id", viewModel.id.value.toString())
//            viewModel.User.value!!.put("password", viewModel.pw.value.toString())
            svm.User.value!!.put("sms_receive", if (agreementMk1.value!!) 1 else 0)
            svm.User.value!!.put("email_receive", if (agreementMk2.value!!) 1 else 0)
            svm.User.value!!.put("height", svm.setHeight.value)
            svm.User.value!!.put("weight", svm.setWeight.value)
            svm.User.value!!.put("email", svm.setEmail.value)
            Log.v("userJson>receive", "${svm.User.value!!}")
//            val userEditEmail = userJson.optString("user_email")
//            val encodedUserEmail = URLEncoder.encode(userEditEmail, "UTF-8")
            fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), svm.User.value!!.toString(), userSn) {
                Log.w(" 싱글톤객체추가", "$userSn, ${svm.User.value!!}")
                Singleton_t_user.getInstance(requireContext()).jsonObject = svm.User.value!!
//                requireActivity().runOnUiThread{
//                    uViewModel.setupProgress = 34
//                    uViewModel.setupStep = 0
//                    uViewModel.step1.value = null
//                    uViewModel.step21.value = null
//                    uViewModel.step22.value = null
//                    uViewModel.step2.value = null
//                    uViewModel.step31.value = null
//                    uViewModel.step32.value = null
//                    uViewModel.step3.value = null
//                    uViewModel.User.value = null
//                    uViewModel.User.value = null
//                }
                onEditComplete()
            }
        }
//        b
    }

    private fun setAdapter(list: MutableList<String>) {
        binding.rvPED.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val adapter = ProfileRVAdapter(this@ProfileEditDialogFragment, this@ProfileEditDialogFragment, false, "profileEdit", svm)
        adapter.userJson = svm.User.value!!
        adapter.profilemenulist = list
        binding.rvPED.adapter = adapter

        adapter.notifyDataSetChanged()

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

        val googleLoginId = jsonObject?.optString("google_login_id")
        if (!googleLoginId.isNullOrEmpty() && googleLoginId != "null") {
            google = true
        }

        val kakaoLoginId = jsonObject?.optString("kakao_login_id")
        if (!kakaoLoginId.isNullOrEmpty() && kakaoLoginId != "null") {
            kakao = true
        }

        val naverLoginId = jsonObject?.optString("naver_login_id")
        if (!naverLoginId.isNullOrEmpty() && naverLoginId != "null") {
            naver = true
        }
        return Triple(google, kakao, naver)
    }

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