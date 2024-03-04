package com.example.mhg.Dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.mhg.PlayActivity
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.databinding.DialogfragmentPlayBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlayBottomSheetDialogFragment: BottomSheetDialogFragment() {
    lateinit var binding : DialogfragmentPlayBottomSheetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogfragmentPlayBottomSheetBinding.inflate(inflater, container, false)
        val params = binding.root.layoutParams as? CoordinatorLayout.LayoutParams
        val behavior = params?.behavior
        if (behavior is BottomSheetBehavior) {

//            behavior.expandedOffset = 0
//            behavior.setPeekHeight(450)

//            behavior.halfExpandedRatio = (resources.displayMetrics.heightPixels / 2).toFloat()

            behavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    behavior.saveFlags = BottomSheetBehavior.SAVE_ALL
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (slideOffset >= 0.3) {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            })
        }


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