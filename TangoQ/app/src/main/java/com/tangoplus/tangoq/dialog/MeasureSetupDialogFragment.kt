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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MediatorLiveData
import com.tangoplus.tangoq.MeasureSkeletonActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentMeasureSetupDialogBinding
import com.tangoplus.tangoq.databinding.FragmentMeasureTrendDialogBinding
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.fragment.ExtendedFunctions.dialogFragmentResize
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FlexableDialogFragment)
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
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        val case = arguments?.getInt(ARG_SETUP_CASE) ?: 1
        binding.clMSDAgreement1.visibility = View.VISIBLE
        binding.clMSDAgreement2.visibility = View.VISIBLE
        when (case) {
            0 -> {
                binding.ibtnMSDNameClear.setOnClickListener{ binding.etMSDName.setText("")}
                binding.tvMSDSkip.setOnClickListener { dismiss() }
                binding.ibtnMSDAgreement1.setOnClickListener {
                    showAgreement4()
                }
                binding.ibtnMSDAgreement2.setOnClickListener {
                    showAgreement5()
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
                    val update = {
                        value = (mvm.setupNameCondition.value == true &&
                                mvm.setupAgreement1.value == true &&
                                mvm.setupAgreement2.value == true)
                    }
                    addSource(mvm.setupNameCondition) { update() }
                    addSource(mvm.setupAgreement1) { update() }
                    addSource(mvm.setupAgreement2) { update() }
                }

                buttonEnabled.observe(viewLifecycleOwner) { isEnabled ->
                    binding.btnMSDFinish.isEnabled = isEnabled
                    binding.btnMSDFinish.backgroundTintList = ColorStateList.valueOf(resources.getColor(
                        if (isEnabled) R.color.mainColor else R.color.subColor400
                    ))
                }
                binding.btnMSDDeny.setOnSingleClickListener {
                    dismiss()
                    (activity as MeasureSkeletonActivity).finish()
                }
                val userName = userJson?.optString("user_name")
                mvm.setupName = userName ?: ""
            }
            1 -> {
                binding.ivMSDAgreement1.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_part_checkbox_enabled))
                binding.ivMSDAgreement2.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_part_checkbox_enabled))
                binding.clMSDAgreement1.setOnSingleClickListener { showAgreement4() }
                binding.clMSDAgreement2.setOnSingleClickListener { showAgreement5() }

                binding.btnMSDDeny.visibility = View.GONE
                binding.btnMSDFinish.text = "설정 완료"

                mvm.setupNameCondition.observe(viewLifecycleOwner) { isEnabled ->
                    binding.btnMSDFinish.isEnabled = isEnabled
                    binding.btnMSDFinish.backgroundTintList = ColorStateList.valueOf(resources.getColor(
                        if (isEnabled) R.color.mainColor else R.color.subColor400
                    ))
                }
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

        binding.etMSDName.setText(mvm.setupName)

        binding.ibtnMSDNameClear.setOnClickListener{ binding.etMSDName.setText("")}
        binding.tvMSDSkip.setOnClickListener { dismiss() }


        binding.btnMSDFinish.setOnClickListener {
            mvm.setupName = binding.etMSDName.text.toString()
            dismiss()
        }
    }
    @Deprecated("Deprecated in Java")
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.bckgnd_rectangle_20, null))
    }

    private fun showAgreement4() {
        val dialog = AgreementDetailDialogFragment.newInstance("agreement4")
        dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
    }

    private fun showAgreement5() {
        val dialog = AgreementDetailDialogFragment.newInstance("agreement5")
        dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
    }
}