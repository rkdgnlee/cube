package com.example.mhg.Dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mhg.databinding.DialogfragmentPersonalSetup0BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class PersonalSetup0BottomSheetDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding : DialogfragmentPersonalSetup0BottomSheetBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogfragmentPersonalSetup0BottomSheetBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

}