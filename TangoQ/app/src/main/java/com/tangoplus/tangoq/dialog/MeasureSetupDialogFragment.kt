package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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

    companion object {
        const val ARG_SETUP_CASE = "measure_setup_case"
        fun newInstance(case: Int) : MeasureSetupDialogFragment {
            val fragment = MeasureSetupDialogFragment()
            val args = Bundle()
            args.putInt(ARG_SETUP_CASE, case)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMeasureSetupDialogBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val case = arguments?.getInt(ARG_SETUP_CASE) ?: 1
        binding.clMSDAgreement1.visibility = View.VISIBLE
        binding.clMSDAgreement2.visibility = View.VISIBLE
        when (case) {
            0 -> {
                binding.ibtnMSDNameClear.setOnClickListener{ binding.etMSDName.setText("")}
                binding.tvMSDSkip.setOnClickListener { dismiss() }
                binding.ibtnMSDAgreement1.setOnClickListener {
                    val dialog = AgreementDetailDialogFragment.newInstance("agreement4")
                    dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
                }
                binding.ibtnMSDAgreement2.setOnClickListener {
                    val dialog = AgreementDetailDialogFragment.newInstance("agreement5")
                    dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
                }
                binding.clMSDAgreement1.setOnClickListener {
                    mvm.setupAgreement1.value = if (mvm.setupAgreement1.value == true) false else true
                }
                binding.clMSDAgreement2.setOnClickListener {
                    mvm.setupAgreement2.value = if (mvm.setupAgreement2.value == true) false else true
                }
                mvm.setupAgreement1.observe(viewLifecycleOwner) { agreement1 ->
                    binding.ivMSDAgreement1.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                        if (agreement1) {
                            R.drawable.icon_part_checkbox_enabled
                        } else {
                            R.drawable.icon_part_checkbox_disabled
                        }
                    ))
                }
                mvm.setupAgreement2.observe(viewLifecycleOwner) {agreement2->
                    binding.ivMSDAgreement2.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                        if (agreement2) {
                            R.drawable.icon_part_checkbox_enabled
                        } else {
                            R.drawable.icon_part_checkbox_disabled
                        }
                    ))
                }

                // 버튼 observer 세팅
                val buttonEnabled = MediatorLiveData<Boolean>().apply {
                    addSource(mvm.setupNameCondition) { nameValid ->
                        value = nameValid == true && mvm.setupNameCondition.value == true
                    }
                    addSource(mvm.setupAgreement1) { agreement1Valid ->
                        value = agreement1Valid == true && mvm.setupAgreement1.value == true
                    }
                    addSource(mvm.setupAgreement2) { agreement2Valid ->
                        value = agreement2Valid == true && mvm.setupAgreement2.value == true
                    }
                }
//        val buttonEnabled = MediatorLiveData<Boolean>().apply {
//            addSource(svm.idCondition) { idValid ->
//                value = idValid == true && svm.mobileCondition.value == true
//            }
//            addSource(svm.mobileCondition) { mobileValid ->
//                value = mobileValid == true && svm.idCondition.value == true
//            }
//        }
                buttonEnabled.observe(viewLifecycleOwner) { isEnabled ->
                    binding.btnMSDFinish.isEnabled = isEnabled
                    binding.btnMSDFinish.backgroundTintList = ColorStateList.valueOf(resources.getColor(
                        if (isEnabled) R.color.mainColor else R.color.subColor400
                    ))
                }
                binding.btnMSDFinish.text = "동의 완료"

//        val mobilePattern = "^010-\\d{4}-\\d{4}\$"
//        val mobilePatternCheck = Pattern.compile(mobilePattern)
//        binding.etMSDPhone.addTextChangedListener(object: TextWatcher {
//            private var isFormatting = false
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                if (isFormatting) return
//                isFormatting = true
//                val cleaned =s.toString().replace("-", "")
//                when {
//                    cleaned.length <= 3 -> s?.replace(0, s.length, cleaned)
//                    cleaned.length <= 7 -> s?.replace(0, s.length, "${cleaned.substring(0, 3)}-${cleaned.substring(3)}")
//                    else -> s?.replace(0, s.length, "${cleaned.substring(0, 3)}-${cleaned.substring(3, 7)}-${cleaned.substring(7)}")
//                }
//                isFormatting = false
//                svm.mobileCondition.value = mobilePatternCheck.matcher(binding.etMSDPhone.text.toString()).find()
//            }
//        })

//        binding.etMSDPhone.setText(mobile)
            }
            1 -> {
                binding.clMSDAgreement1.visibility = View.GONE
                binding.clMSDAgreement2.visibility = View.GONE
                binding.btnMSDFinish.text = "설정 완료"
            }
            else -> {

            }
        }

        // 버튼 셋엄
        val namePatternCheck = Pattern.compile(
            "^(?:" +
                    "[가-힣]{2,5}|" +  // 한글 2-5글자
                    "[a-zA-Z]{3,15}" + // 영어 3-15글자
                    ")$"
        )
        binding.etMSDName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                mvm.setupName = s.toString()
                mvm.setupNameCondition.value = namePatternCheck.matcher(binding.etMSDName.text.toString()).find()
//                Log.v("idPw", "${svm.idCondition.value}")
            }
        })

        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        val userName = userJson?.optString("user_name")
        binding.etMSDName.setText(userName)
        mvm.setupName = userName ?: ""
        binding.ibtnMSDNameClear.setOnClickListener{ binding.etMSDName.setText("")}
        binding.tvMSDSkip.setOnClickListener { dismiss() }


        binding.btnMSDFinish.setOnClickListener {
            mvm.setupName = binding.etMSDName.text.toString()
//            mvm.setupMobile = binding.etMSDPhone.text.toString()
            dismiss()
//            Log.v("mvm넣기", "mvm.Name: ${mvm.setupName}, mvm.Mobile: ${mvm.setupMobile}")
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.7f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.bckgnd_rectangle_20, null))
        dialog?.setCancelable(false)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        if (isTablet(requireContext())) {
            dialogFragmentResize(requireContext(), this@MeasureSetupDialogFragment, width = 0.7f, height = 0.4f)
        } else {
            dialogFragmentResize(requireContext(), this@MeasureSetupDialogFragment, width =  0.9f, height = 0.45f)
        }
    }
}