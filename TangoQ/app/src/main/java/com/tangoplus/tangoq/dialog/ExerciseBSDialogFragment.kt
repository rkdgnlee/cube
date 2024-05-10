package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.databinding.FragmentExerciseBSDialogBinding


class ExerciseBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding : FragmentExerciseBSDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExerciseBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = arguments
        val exerciseUnit = bundle?.getParcelable<ExerciseVO>("ExerciseUnit")

        binding.tvEcBSName.text = exerciseUnit?.exerciseName
//        binding.ivFrBsThumbnail.setImageResource(R.drawable)
        binding.llEcBSPlay.setOnClickListener {
            dismiss()
            val DialogFragment = PlayThumbnailDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("ExerciseUnit", exerciseUnit)
                }
            }
            DialogFragment.show(requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
//            requireActivity().supportFragmentManager.beginTransaction().apply {
//                replace(R.id.flMain, DialogFragment)
//                commit()
//            }
        }
        binding.ibtnEcBsExit.setOnClickListener {
            dismiss()
        }

    }
}