package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentAgreementBSDialogBinding

@Suppress("UNUSED_EXPRESSION")
class AgreementBottomSheetDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding: FragmentAgreementBSDialogBinding
    var agreeAll = MutableLiveData(false)
    val agreement1 = MutableLiveData(false)
    val agreement2 = MutableLiveData(false)
    val agreement3 = MutableLiveData(false)
    val essentialAgree = MutableLiveData(false)
    val agreementMk1 = MutableLiveData(false)
    val agreementMk2 = MutableLiveData(false)
    val marketingAgree = MutableLiveData(false)

    interface OnAgreeListener {
        fun onFinish()
    }
    private var listener: OnAgreeListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAgreementBSDialogBinding.inflate(inflater)

        return binding.root
    }
    fun setOnFinishListener(listener: OnAgreeListener) {
        this.listener = listener
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----! 개인정보 동의항목 체크 시작 !-----
        binding.clAgreementAll.setOnClickListener {
            val newValue = agreeAll.value?.not() ?: false
            binding.ivAgreementAll.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            binding.ivAgreementAll.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            binding.ivAgreement1.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            binding.ivAgreement2.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            binding.ivAgreement3.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            binding.ivAgreementMk1.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            binding.ivAgreementMk2.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )

            agreement1.value = newValue
            agreement2.value = newValue
            agreement3.value = newValue
            agreeAll.value = newValue
            agreementMk1.value = newValue
            agreementMk2.value = newValue
            marketingAgree.value = newValue
            essentialAgree.value = newValue
        }

        // ------! 나가기 !------
        binding.ibtnAgreementExit.setOnClickListener {
            dismiss()
        }


        binding.clAgreement1.setOnClickListener {
            val newValue = agreement1.value?.not() ?: false
            binding.ivAgreement1.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            agreement1.value = newValue
        }

        binding.clAgreement2.setOnClickListener {
            val newValue = agreement2.value?.not() ?: false
            binding.ivAgreement2.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            agreement2.value = newValue
        }
        // ------! 광고성 수신 동의 !------
        binding.clAgreement3.setOnClickListener {
            val newValue = agreement3.value?.not() ?: false
            binding.ivAgreement3.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )

            binding.ivAgreementMk1.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            binding.ivAgreementMk2.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            agreement3.value = newValue
            agreementMk1.value = newValue
            agreementMk2.value = newValue
        }
        agreement1.observe(viewLifecycleOwner) {
            updateAgreeAllState()
            updateEssentialAgreeState()
        }

        agreement2.observe(viewLifecycleOwner) {
            updateAgreeAllState()
            updateEssentialAgreeState()
        }

        agreement3.observe(viewLifecycleOwner) {
            updateAgreeAllState()
        }

        essentialAgree.observe(viewLifecycleOwner) {
            if (it) {
                binding.btnAgreementFinish.isEnabled = true
                binding.btnAgreementFinish.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mainColor))
                binding.btnAgreementFinish.background = resources.getDrawable(R.drawable.effect_ibtn_12dp)

                binding.btnAgreementFinish.stateListAnimator = null
            } else {
                binding.btnAgreementFinish.isEnabled = false
                binding.btnAgreementFinish.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor500))
                binding.btnAgreementFinish.background = resources.getDrawable(R.drawable.effect_ibtn_12dp)
            }
        }

        // ------! 마케팅 정보 수신 동의 !------
        binding.clAgreementMk1.setOnClickListener{
            val newValue = agreementMk1.value?.not() ?: false
            binding.ivAgreementMk1.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            agreementMk1.value = newValue
        }
        binding.clAgreementMk2.setOnClickListener{
            val newValue = agreementMk2.value?.not() ?: false
            binding.ivAgreementMk2.setImageResource(
                if (newValue) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
            agreementMk2.value = newValue
        }

        agreementMk1.observe(viewLifecycleOwner) {
            updateAgreeAllState()
            updateAgreeMarketingAllState()
        }

        agreementMk2.observe(viewLifecycleOwner) {
            updateAgreeAllState()
            updateAgreeMarketingAllState()
        }


        binding.btnAgreementFinish.setOnClickListener {
            dismiss()
            listener?.onFinish()
        }
        // -----! 개인정보 동의항목 체크 끝 !-----

        // -----! 약관 링크 버튼 시작 !-----
        binding.ibtnAgreement1.setOnClickListener {
            val dialog = AgreementDetailDialogFragment.newInstance("agreement1")
            dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
        }
        binding.ibtnAgreement2.setOnClickListener {
            val dialog = AgreementDetailDialogFragment.newInstance("agreement2")
            dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
        }
        binding.ibtnAgreement3.setOnClickListener {
            val dialog = AgreementDetailDialogFragment.newInstance("agreement3")
            dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
        }
        // -----! 약관 링크 버튼 끝 !-----

    }

    // ------! 전체 동의 시작 !------
    private fun updateAgreeAllState() {
        val allChecked = agreement1.value == true && agreement2.value == true && agreement3.value == true && agreementMk1.value == true && agreementMk2.value == true
        if (agreeAll.value != allChecked) {
            agreeAll.value = allChecked
            binding.ivAgreementAll.setImageResource(
                if (allChecked) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
        }
    }

    private fun updateEssentialAgreeState() {
        essentialAgree.value = agreement1.value == true && agreement2.value == true
    }

    private fun updateAgreeMarketingAllState() {
        val allChecked = agreementMk1.value == true && agreementMk2.value == true
        if (marketingAgree.value != allChecked) {
            marketingAgree.value = allChecked
            binding.ivAgreement3.setImageResource(
                if (allChecked) R.drawable.icon_part_checkbox_enabled else R.drawable.icon_part_checkbox_disabled
            )
        }
    }
}