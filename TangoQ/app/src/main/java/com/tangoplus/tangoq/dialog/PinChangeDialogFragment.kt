package com.tangoplus.tangoq.dialog

import android.animation.ObjectAnimator
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignBottom
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkUser.fetchUserUPDATEJson
import com.tangoplus.tangoq.databinding.FragmentPinChangeDialogBinding
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.SecurePreferencesManager.encrypt
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import `in`.aabhasjindal.otptextview.OTPListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PinChangeDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentPinChangeDialogBinding
    private val svm : SignInViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPinChangeDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.clPCD2.visibility = View.GONE
        binding.btnPCDConfirm.text = "다음으로"
        disabledBtn()
        binding.ibtnPCDPrevious.setOnSingleClickListener { dismiss() }
        binding.ibtnPCDAlert.setOnSingleClickListener { balloonCallback {  } }
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val userSn = Singleton_t_user.getInstance(requireContext()).jsonObject?.optInt("sn")
        balloonCallback {
            binding.etPCDPin.requestFocus()
            binding.etPCDPin.postDelayed({
                binding.etPCDPin.requestFocus()
                imm.showSoftInput(binding.etPCDPin, InputMethodManager.SHOW_IMPLICIT)
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            }, 250)
        }
        binding.etPCDPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 4) {
                    svm.setPin = s.toString()
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    enabledBtn()
                }
            }
        })

        binding.etPCDPinRepeat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                svm.pinCondition.value = svm.setPin == s.toString()
                if (svm.pinCondition.value == true) {
                    binding.tvPCDPinCondition.text = "번호가 일치합니다. 하단의 변경 버튼을 눌러주세요"
                    binding.tvPCDPinCondition.setTextColor(resources.getColor(R.color.mainColor, null))
                } else {
                    binding.tvPCDPinCondition.text = "일치하지 않습니다. 번호를 다시 확인해주세요"
                    binding.tvPCDPinCondition.setTextColor(resources.getColor(R.color.deleteColor, null))
                }
                if (s?.length == 4) {
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        })


        binding.btnPCDConfirm.setOnSingleClickListener {
            when (binding.btnPCDConfirm.text) {
                "다음으로" -> {
                    binding.clPCD2.visibility = View.VISIBLE
                    fadeInView(binding.clPCD2)
                    binding.etPCDPinRepeat.requestFocus()
                    binding.etPCDPinRepeat.postDelayed({
                        imm.showSoftInput(binding.etPCDPinRepeat, InputMethodManager.SHOW_IMPLICIT)
                    }, 250)


                    binding.btnPCDConfirm.text = "변경하기"
                    svm.pinCondition.observe(viewLifecycleOwner) {
                        if (it) {
                            enabledBtn()
                        } else {
                            disabledBtn()
                        }
                    }
                }
                "변경하기" -> {
                    val encryptedPin = encrypt(svm.setPin, getString(R.string.secret_key), getString(R.string.secret_iv))
                    val jo = JSONObject().apply {
                        put("password", encryptedPin)
                    }
                    Log.v("password", "$jo")
                    CoroutineScope(Dispatchers.IO).launch {
                        val isFinish = fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), jo.toString(), userSn.toString())
                        withContext(Dispatchers.Main) {
                            if (isFinish == true) {
                                Toast.makeText(requireContext(), "PIN번호 변경이 완료됐습니다", Toast.LENGTH_SHORT).show()
                                dismiss()
                            } else {
                                Toast.makeText(requireContext(), "올바르지 않은 접근입니다. 잠시후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

    }

    private fun balloonCallback(callback: () -> Unit) {
        val balloon = Balloon.Builder(requireContext())
            .setWidth(BalloonSizeSpec.WRAP)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("탱고바디 키오스크 로그인 PIN번호를 재설정하시려면\n하단에 4자리를 입력하고 변경버튼을 눌러주세요")
            .setTextColorResource(R.color.subColor800)
            .setTextSize(20f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowSize(0)
            .setMargin(6)
            .setPadding(12)
            .setCornerRadius(8f)
            .setBackgroundColorResource(R.color.white)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(viewLifecycleOwner)
            .setOnBalloonDismissListener { callback() }
            .build()
        binding.ibtnPCDAlert.showAlignBottom(balloon)
        balloon.dismissWithDelay(3000L)
        balloon.setOnBalloonClickListener { balloon.dismiss() }
    }

    private fun enabledBtn() {
        binding.btnPCDConfirm.isEnabled = true
        binding.btnPCDConfirm.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
    }
    private fun disabledBtn() {
        binding.btnPCDConfirm.isEnabled = false
        binding.btnPCDConfirm.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
    }

    fun fadeInView(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = 500 // 애니메이션 지속 시간 (ms)
            interpolator = DecelerateInterpolator()
            start()
        }
    }

}