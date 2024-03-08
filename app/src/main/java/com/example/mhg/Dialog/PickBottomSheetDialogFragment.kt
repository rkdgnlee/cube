package com.example.mhg.Dialog

import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mhg.R
import com.example.mhg.databinding.DialogfragmentPickBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PickBottomSheetDialogFragment: BottomSheetDialogFragment() {
    lateinit var binding : DialogfragmentPickBottomSheetBinding
    var favorite = true
    override fun onStart() {
        super.onStart()

    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogfragmentPickBottomSheetBinding.inflate(inflater)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // -----! 화면 높이 설정 !-----
//        val dialog = dialog
//        if (dialog != null) {
//            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
//            // ----- ! 정지 높이 !-----
//            val displayMetrics = resources.displayMetrics
//            val screenHeight = displayMetrics.heightPixels
//            val desiredHeight = (screenHeight * 0.5).toInt()
//            // ----- ! 속성 설정 !-----
//            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
//            val behavior = BottomSheetBehavior.from(bottomSheet)
//            behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
//            behavior.peekHeight = desiredHeight
//            behavior.isHideable = true
//        }
        binding.btnPickExit.setOnClickListener {
            dialog?.dismiss()
        }

        binding.btnPickFravorite.setOnClickListener {
            val imageResource = if (favorite) R.drawable.favorite_true else R.drawable.favorite_false
            binding.btnPickFravorite.setImageResource(imageResource)
            favorite = !favorite
        }
    }
}