package com.example.mhg.Dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import com.example.mhg.R
import com.example.mhg.databinding.FragmentAgreementBottomSheetDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class AgreementBottomSheetDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding : FragmentAgreementBottomSheetDialogBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAgreementBottomSheetDialogBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----! 개인정보 동의항목 체크 시작 !-----
        binding.cbAgreementAll.setOnClickListener {
            val isChecked = binding.cbAgreementAll.isChecked
            binding.cbAgreement1.isChecked = isChecked
            binding.cbAgreement2.isChecked = isChecked
            binding.cbAgreement4.isChecked = isChecked
        }


        val checkListener = CompoundButton.OnCheckedChangeListener{ _, _ ->
            if (binding.cbAgreement1.isChecked && binding.cbAgreement2.isChecked) {
                binding.btnAgreementFinish.setBackgroundColor(binding.btnAgreementFinish.resources.getColor(R.color.mainColor))
                binding.btnAgreementFinish.setTextColor(binding.btnAgreementFinish.resources.getColor(R.color.mainwhite))
                binding.btnAgreementFinish.isEnabled = true
            } else {
                binding.btnAgreementFinish.setBackgroundColor(binding.btnAgreementFinish.resources.getColor(R.color.grey600))
                binding.btnAgreementFinish.isEnabled = false
                binding.btnAgreementFinish.setTextColor(binding.btnAgreementFinish.resources.getColor(R.color.mainblack))
//                binding.cbAgreementAll.isChecked = false
            }
        }
        binding.cbAgreement1.setOnCheckedChangeListener(checkListener)
        binding.cbAgreement2.setOnCheckedChangeListener(checkListener)
        binding.btnAgreementFinish.setOnClickListener {
            dismiss()
        }
        // -----! 개인정보 동의항목 체크 시작 !-----

        // -----! 약관 링크 버튼 시작 !-----
        binding.ibtnAgreement1.setOnClickListener {
            val dialog = AgreementDialogFragment.newInstance("agreement1")
            dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
        }
        binding.ibtnAgreement2.setOnClickListener {
            val dialog = AgreementDialogFragment.newInstance("agreement2")
            dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
        }

        // -----! 약관 링크 버튼 끝 !-----
    }

}