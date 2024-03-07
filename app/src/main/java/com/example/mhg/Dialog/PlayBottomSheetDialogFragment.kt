package com.example.mhg.Dialog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.mhg.PlayActivity
import com.example.mhg.R
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.databinding.DialogfragmentPlayBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlayBottomSheetDialogFragment: BottomSheetDialogFragment() {
    lateinit var binding : DialogfragmentPlayBottomSheetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getTheme(): Int = R.style.Theme_Design_BottomSheetDialog1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogfragmentPlayBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = arguments
        val routine : HomeRVBeginnerDataClass? = bundle?.getParcelable("routineList")
        routine?.let {
            binding.tvPlayRoutineName.text = it.name
            binding.tvPlayRoutineDuration.text = it.duration.toString()
            binding.tvPlayRoutineExplanation.text = it.explanation
        }
        val warmup : HomeRVBeginnerDataClass? = bundle?.getParcelable("warmupList")
        warmup?.let {
            binding.tvPlayRoutineName.text = it.name
            binding.tvPlayRoutineName.text = it.name
            binding.tvPlayRoutineDuration.text = it.duration.toString()
            binding.tvPlayRoutineExplanation.text = it.explanation
        }
        binding.btnPlay.setOnClickListener {
            val intent = Intent(requireContext(), PlayActivity::class.java)
            startActivity(intent)

        }


    }



}