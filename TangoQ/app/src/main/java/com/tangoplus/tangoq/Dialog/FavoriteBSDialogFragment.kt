package com.tangoplus.tangoq.Dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.ExerciseVO
import com.tangoplus.tangoq.databinding.FragmentFavoriteBSDialogBinding

class FavoriteBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding : FragmentFavoriteBSDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteBSDialogBinding.inflate(inflater)

        val bundle = arguments
        val exerciseUnit = bundle?.getParcelable<ExerciseVO>("ExerciseUnit")

        binding.tvFrBSName.text = exerciseUnit?.exerciseName
//        binding.ivFrBsThumbnail.setImageResource(R.drawable)
        binding.tvFrBSPlay.setOnClickListener {
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
        // TODO MAIN과 FAVORITE BOTTOMSHEET 를 확실하게 분리해야함.
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }
}