package com.example.mhg.Dialog

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.mhg.PlayActivity
import com.example.mhg.R
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.databinding.DialogfragmentPlayBottomSheetBinding
import com.example.mhg.databinding.PlaybuttonBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlayBottomSheetDialogFragment: BottomSheetDialogFragment() {
    lateinit var binding : DialogfragmentPlayBottomSheetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getTheme(): Int = R.style.Theme_Design_BottomSheetDialog1

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogfragmentPlayBottomSheetBinding.inflate(inflater, container, false)

        // ----- ! 버튼 연결 코드 ! -----
//        val buttonView = PlaybuttonBinding.inflate(inflater, binding.root as ViewGroup, false)
//        val params = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.MATCH_PARENT,
//            FrameLayout.LayoutParams.WRAP_CONTENT,
//        ).apply {
//            gravity = Gravity.BOTTOM
//
//        }
//        val dialog = dialog
//        binding.root.addView(buttonView.root, params)

        return binding.root
    }


    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ----- 화면 높이 설정 코드 시작 -----
        val dialog = dialog
        if (dialog != null) {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            // ----- ! 정지 높이 !-----
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val desiredHeight = (screenHeight * 0.5).toInt()
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = desiredHeight
            behavior.isHideable = true
        }


        // ----- 화면 높이 설정 코드 끝 -----

        val bundle = arguments
        val routine : HomeRVBeginnerDataClass? = bundle?.getParcelable("typeList")
        routine?.let {
            binding.tvPlayExerciseName.text = it.exerciseName
            binding.tvPlayRoutineDuration.text = it.videoTime
            binding.tvPlayExerciseDescription.text = it.exerciseDescription
            binding.tvPlayRelatedJoint.text = it.relatedJoint
            binding.tvPlayRelatedMuscle.text = it.relatedMuscle
            binding.tvPlayRelatedSymptom.text = it.relatedSymptom
        }
        val warmup : HomeRVBeginnerDataClass? = bundle?.getParcelable("warmupList")
        warmup?.let {
            binding.tvPlayExerciseName.text = it.exerciseName
            binding.tvPlayRoutineDuration.text = it.videoTime
            binding.tvPlayExerciseDescription.text = it.exerciseDescription
            binding.tvPlayRelatedJoint.text = it.relatedJoint
            binding.tvPlayRelatedMuscle.text = it.relatedMuscle
            binding.tvPlayRelatedSymptom.text = it.relatedSymptom
        }
        binding.btnPlay.setOnClickListener {
            val intent = Intent(requireContext(), PlayActivity::class.java)
            if (routine != null) {
                intent.putExtra("ExerciseData", routine)
                Log.w(TAG + "운동데이터", "${routine.relatedSymptom}")
            } else if (warmup != null){
                intent.putExtra("ExerciseData", warmup)
                Log.w(TAG + "운동데이터", "${warmup.relatedSymptom}")
            }
            startActivity(intent)

        }


    }



}