package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MediatorLiveData
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentMeasureSetupDialogBinding
import com.tangoplus.tangoq.databinding.FragmentMeasureTrendDialogBinding
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.fragment.ExtendedFunctions.dialogFragmentResize
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import java.util.regex.Pattern

class MeasureSetupDialogFragment : DialogFragment() {
    private lateinit var  binding: FragmentMeasureSetupDialogBinding
    private val mvm : MeasureViewModel by activityViewModels()
    private val svm : SignInViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMeasureSetupDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 버튼 셋엄
        binding.ibtnMSDPhoneClear.setOnClickListener{ binding.etMSDPhone.setText("")}
        binding.ibtnMSDNameClear.setOnClickListener{ binding.etMSDName.setText("")}
        binding.tvMSDSkip.setOnClickListener {
            Log.v("스킵", "go skip")
            dismiss()
        }

        // 버튼 observer 세팅
        val buttonEnabled = MediatorLiveData<Boolean>().apply {
            addSource(svm.idCondition) { idValid ->
                value = idValid == true && svm.mobileCondition.value == true
            }
            addSource(svm.mobileCondition) { mobileValid ->
                value = mobileValid == true && svm.idCondition.value == true
            }
        }
        buttonEnabled.observe(viewLifecycleOwner) { isEnabled ->
            binding.btnMSDFinish.isEnabled = isEnabled
            Log.v("isEnabled", "$isEnabled")
        }

        val mobilePattern = "^010-\\d{4}-\\d{4}\$"
        val mobilePatternCheck = Pattern.compile(mobilePattern)
        binding.etMSDPhone.addTextChangedListener(object: TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true
                val cleaned =s.toString().replace("-", "")
                when {
                    cleaned.length <= 3 -> s?.replace(0, s.length, cleaned)
                    cleaned.length <= 7 -> s?.replace(0, s.length, "${cleaned.substring(0, 3)}-${cleaned.substring(3)}")
                    else -> s?.replace(0, s.length, "${cleaned.substring(0, 3)}-${cleaned.substring(3, 7)}-${cleaned.substring(7)}")
                }
                isFormatting = false
                svm.mobileCondition.value = mobilePatternCheck.matcher(binding.etMSDPhone.text.toString()).find()
            }
        })
        val NamePatternCheck = Pattern.compile(
            "^(?:" +
                    "[가-힣]{2,5}|" +  // 한글 2-5글자
                    "[a-zA-Z]{3,15}" + // 영어 3-15글자
                    ")$"
        )
        binding.etMSDName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                svm.id.value = s.toString()
                svm.idCondition.value = NamePatternCheck.matcher(binding.etMSDName.text.toString()).find()
                Log.v("idPw", "${svm.idCondition.value}")
            }
        })

        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        val userName = userJson?.optString("user_name")
        val mobile = userJson?.optString("mobile")
        binding.etMSDName.setText(userName)
        mvm.setupName = userName ?: ""
        binding.etMSDPhone.setText(mobile)
        mvm.setupMobile = mobile ?: ""

        binding.btnMSDFinish.setOnClickListener {
            mvm.setupName = binding.etMSDName.text.toString()
            mvm.setupMobile = binding.etMSDPhone.text.toString()
            dismiss()
            Log.v("mvm넣기", "mvm.Name: ${mvm.setupName}, mvm.Mobile: ${mvm.setupMobile}")
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.7f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.bckgnd_rectangle_20))

        dialog?.setCancelable(false)
        if (isTablet(requireContext())) {
            dialogFragmentResize(requireContext(), this, width =  0.6f ,height = 0.4f)
        } else {
            dialogFragmentResize(requireContext(), this, height = 0.475f)
        }
    }
}