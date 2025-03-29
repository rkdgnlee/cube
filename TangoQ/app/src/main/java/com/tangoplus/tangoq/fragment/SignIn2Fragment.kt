package com.tangoplus.tangoq.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkUser.insertUser
import com.tangoplus.tangoq.databinding.FragmentSignIn2Binding
import com.tangoplus.tangoq.dialog.SignInDialogFragment
import com.tangoplus.tangoq.dialog.WebViewDialogFragment
import com.tangoplus.tangoq.dialog.bottomsheet.AgreementBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.SecurePreferencesManager.encrypt
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignIn2Fragment : Fragment() {
    private lateinit var binding : FragmentSignIn2Binding
    val svm : SignInViewModel by activityViewModels()
    private var parentDialog: SignInDialogFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSignIn2Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentDialog  = (requireParentFragment() as SignInDialogFragment)
        // 기본 셋팅
        binding.clName.visibility = View.GONE

        binding.btnAuthSend.setOnSingleClickListener {
            val passDialog = WebViewDialogFragment()
            passDialog.show(requireActivity().supportFragmentManager, "WebViewDialogFragment")
        }

        when (svm.isSnsSignIn) {
            true -> {
                binding.etName.setText(svm.snsJo.optString("user_name"))
                binding.etName.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                binding.etName.isEnabled = false
            }
            false -> {
                svm.passName.observe(viewLifecycleOwner) {
                    if (it != "") {
                        binding.clName.visibility = View.VISIBLE
                        binding.etName.setText(it)
                        binding.etName.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                    } else {
                        binding.etName.text = null
                        binding.etName.backgroundTintList = null
                    }
                }
                svm.passMobile.observe(viewLifecycleOwner) {
                    if (it != "") {
                        binding.etMobile.setText(it)
                        binding.etMobile.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                    } else {
                        binding.etMobile.text = null
                        binding.etMobile.backgroundTintList = null
                    }
                }
                svm.passAuthCondition.observe(viewLifecycleOwner) {
                    if (it) {
                        // 전부 check가 됐을 떄
                        showAgreementBottomSheetDialog(requireActivity())

                        storeUserValue()
                        setNextPage()
                    } else {
                        clearUserValue()
                    }
                }
                // 회원가입 버튼 state 관리
                svm.allTrueLiveData.observe(viewLifecycleOwner) {
                    if (it) {
                        parentDialog?.requireView()?.findViewById<ProgressView>(R.id.pvSignIn)?.let { it.progress = 100f }
                        parentDialog?.requireView()?.findViewById<TextView>(R.id.tvSignInGuide)?.let { it.text = "가입 버튼을 눌러주세요" }
                        binding.btnSignIn.apply {
                            isEnabled = it
                            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                        }
                    } else {
                        binding.btnSignIn.apply {
                            isEnabled = it
                            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
                        }
                    }
                }
            }
        }
    }

    private fun storeUserValue() {
        svm.User.value?.put("mobile", svm.passMobile.value)
        svm.User.value?.put("user_name", svm.passName.value)
        // 담고 옮기기
    }

    private fun setNextPage() {
        Handler(Looper.getMainLooper()).postDelayed({
            parentDialog?.setonNextPage()
        }, 600)
    }
    private fun clearUserValue() {
        svm.User.value?.put("mobile", "")
        svm.User.value?.put("user_name", "")
    }

    private fun showAgreementBottomSheetDialog(context: FragmentActivity) {
        val bottomSheetFragment = AgreementBSDialogFragment()
        bottomSheetFragment.setOnFinishListener(object : AgreementBSDialogFragment.OnAgreeListener {
            override fun onFinish(agree: Boolean) {
                if (agree) {
                    svm.User.value?.put("email", "${svm.fullEmail.value}")
                    val jsonObj = svm.User.value
                    // 암호화된 비밀번호 넣기
                    val encryptPW = encrypt(jsonObj?.optString("password_app").toString(), getString(R.string.secret_key), getString(R.string.secret_iv))
                    jsonObj?.put("password_app", encryptPW)
                    // ------! 광고성 넣기 시작 !------
                    jsonObj?.put("sms_receive", if (svm.agreementMk1.value == true) 1 else 0)
                    jsonObj?.put("email_receive", if (svm.agreementMk2.value == true) 1 else 0)
                    jsonObj?.put("device_sn" ,0)
                    jsonObj?.put("user_sn", 0)
                    // ------! 광고성 넣기 끝 !------
//                    Log.v("회원가입JSon", "$jsonObj")
                    if (jsonObj != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            insertUser(getString(R.string.API_user), jsonObj ) { status ->
                                when (status) {
                                    200, 201 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val intent = Intent(requireActivity(), IntroActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            intent.putExtra("SignInFinished", 201)

                                            parentDialog?.dismiss()
                                            startActivity(intent)
                                        }
                                    }
                                    423,  404, 403 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                                                setTitle("회원 가입 실패")
                                                setMessage("올바르지 않은 데이터가 존재합니다\n입력한 정보를 다시 확인해주세요")
                                                setPositiveButton("예") { _, _ ->

                                                }
                                                show()
                                            }
                                        }
                                    }
                                    409 -> {

                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
        val fragmentManager = context.supportFragmentManager
        bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
    }
}