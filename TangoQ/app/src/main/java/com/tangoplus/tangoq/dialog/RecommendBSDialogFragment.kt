package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.FragmentRecommendBSDialogBinding

class RecommendBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding: FragmentRecommendBSDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecommendBSDialogBinding.inflate(inflater)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = arguments
        val program = bundle?.getParcelable<ProgramVO>("Program")

        binding.ibtnRcBSExit.setOnClickListener { dismiss() }
        binding.tvRcBSName.text = program?.programName.toString()
//        Glide.with(binding.ivRcBSThumbnail.context)
//            .load(program?.programImageUrl)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .into(binding.ivRcBSThumbnail)

        // ------! bs에서 경로 설정 시작 !------
//        else if (favoriteItem != null) {
//            binding.tvFrBSName.text = favoriteItem.favoriteName
////            binding.ivFrBsThumbnail.
//            binding.llFrBSPlay.setOnClickListener{
//                // TODO 재생목록 만들어서 FULLSCREEN
//            }
//
//            // ---! 편집하기 !---
//            binding.llFrBSEdit.setOnClickListener {
//                dismiss()
//                requireActivity().supportFragmentManager.beginTransaction().apply {
//                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
//                    replace(R.id.flMain, FavoriteEditFragment.newInstance(favoriteItem.favoriteName.toString()))
//                    remove(FavoriteDetailFragment()).commit()
//                }
//            }
//            // ---! 운동 추가하기 !---
//            binding.llFrBSAddExercise.setOnClickListener {
//                dismiss()
//                requireActivity().supportFragmentManager.beginTransaction().apply {
//                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
//                    replace(R.id.flMain, FavoriteBasketFragment.newInstance(favoriteItem.favoriteName.toString()))
//                        .addToBackStack(null)
//                        .commit()
//                }
//            }
//            // ---! 즐겨찾기 삭제 !---
//            binding.llFrBSDelete.setOnClickListener {
//                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
//                    setTitle("⚠️ 알림")
//                    setMessage("플레이리스트에서 삭제하시겠습니까?")
//                    setPositiveButton("확인") { dialog, _ ->
//                        NetworkExerciseService.deleteFavoriteItemSn(
//                            getString(R.string.IP_ADDRESS_t_favorite),
//                            favoriteItem.favoriteSn.toString()
//                        ) {
//                            requireActivity().supportFragmentManager.beginTransaction().apply {
//                                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
//                                replace(R.id.flMain, FavoriteFragment())
//                                    .commit()
//                            }
//                        }
//                    }
//                    setNegativeButton("취소") { dialog, _ -> }
//                    create()
//                }.show()
//            }
//        }

    }
}