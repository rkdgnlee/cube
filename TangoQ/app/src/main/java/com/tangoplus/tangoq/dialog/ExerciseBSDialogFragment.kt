package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.FavoriteVO
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.databinding.FragmentExerciseBSDialogBinding


class ExerciseBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding : FragmentExerciseBSDialogBinding
    val viewModel: FavoriteViewModel by activityViewModels()
    var sn = 0

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
        val exerciseUnit = bundle?.getParcelable<ExerciseVO>("exerciseUnit")


        binding.tvEBSName.text = exerciseUnit?.exerciseName
        binding.tvEBSSymptom.text = exerciseUnit?.relatedSymptom
        Glide.with(requireContext())
            .load("${exerciseUnit?.imageFilePathReal}")
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(180)
            .into(binding.ivEBsThumbnail)

        // ------! 세트 계산 시작 !------
        val count = MutableLiveData(1)

        binding.ibtnEBSMinus.setOnClickListener {
            if (count.value!! > 1) count.value = count.value!! - 1
            if (exerciseUnit != null) {
                viewModel.selectedFavorite.value?.exercises?.remove(exerciseUnit)
            }
        }
        binding.ibtnEBSPlus.setOnClickListener {
            if (count.value!! < 10) count.value = count.value!! + 1
            if (exerciseUnit != null) {
                viewModel.selectedFavorite.value?.exercises?.add(exerciseUnit)
            }
        }
        count.observe(viewLifecycleOwner){
            binding.tvEBSCount.text = it.toString()
            Log.v("count", "${count.value}")
        }
        // ------! 세트 계산 끝 !-------

        binding.llEBsPlay.setOnClickListener {
            dismiss()
            val dialogFragment = PlayThumbnailDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("ExerciseUnit", exerciseUnit)
                }
            }
            dialogFragment.show(requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
//            requireActivity().supportFragmentManager.beginTransaction().apply {
//                replace(R.id.flMain, DialogFragment)
//                commit()
//            }
        }
        binding.ibtnEBsExit.setOnClickListener {
            dismiss()
        }

        // ------! 공유하기 시작 !------



    }
}