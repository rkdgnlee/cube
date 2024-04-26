package com.tangoplus.tangoq.Dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.databinding.FragmentSignInBottomSheetDialogBinding


class SignInBottomSheetDialogFragment: BottomSheetDialogFragment() {
    lateinit var binding: FragmentSignInBottomSheetDialogBinding
    interface onTelecomSelectedListener {
        fun onTelecomSelected(telecom:String)
    }
    private var listener: onTelecomSelectedListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignInBottomSheetDialogBinding.inflate(inflater)
        return binding.root
    }


    fun setOnCarrierSelectedListener(listener: onTelecomSelectedListener) {
        this.listener = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSignInSKT.setOnClickListener {
            listener?.onTelecomSelected("SKT")
            dismiss()
        }
        binding.tvSignInKT.setOnClickListener {
            listener?.onTelecomSelected("KT")
            dismiss()
        }
        binding.tvSignInLGU.setOnClickListener {
            listener?.onTelecomSelected("LG U+")
            dismiss()
        }
        binding.tvSignInSKTC.setOnClickListener {
            listener?.onTelecomSelected("SKT 알뜰폰")
            dismiss()
        }
        binding.tvSignInKTC.setOnClickListener {
            listener?.onTelecomSelected("KT 알뜰폰")
            dismiss()
        }
        binding.tvSignInLGUC.setOnClickListener {
            listener?.onTelecomSelected("LG U+ 알뜰폰")
            dismiss()
        }


    }


}