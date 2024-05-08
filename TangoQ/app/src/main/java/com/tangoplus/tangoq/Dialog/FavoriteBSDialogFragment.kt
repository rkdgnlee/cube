package com.tangoplus.tangoq.Dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.Fragment.FavoriteBasketFragment
import com.tangoplus.tangoq.Fragment.FavoriteDetailFragment
import com.tangoplus.tangoq.Fragment.FavoriteEditFragment
import com.tangoplus.tangoq.Fragment.FavoriteFragment
import com.tangoplus.tangoq.Object.NetworkExerciseService.deleteFavoriteItemSn
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.ExerciseVO
import com.tangoplus.tangoq.ViewModel.FavoriteItemVO
import com.tangoplus.tangoq.ViewModel.FavoriteVO
import com.tangoplus.tangoq.databinding.FragmentFavoriteBSDialogBinding


class FavoriteBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding : FragmentFavoriteBSDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = arguments
        val favorite = bundle?.getParcelable<FavoriteVO>("Favorite")
        // ------! 즐겨찾기 홈에서 경로 설정 시작 !------
        if (favorite != null) {

            // ------! 썸네일 사진 4개 시작 !------

            if (favorite.imgThumbnailList != null) {
                // TODO 썸네일 URL GLIDE로 뿌리기.
            }


            binding.tvFrBSName.text = favorite.name
            binding.llFrBSPlay.setOnClickListener{
                // TODO 재생목록 만들어서 FULLSCREEN
            }
            // ---! 편집하기 !---
            binding.llFrBSEdit.setOnClickListener {
                dismiss()
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                    replace(R.id.flMain, FavoriteEditFragment.newInstance(favorite.name.toString()))
                    remove(FavoriteDetailFragment()).commit()
                }
            }
            // ---! 운동 추가하기 !---
            binding.llFrBSAddExercise.setOnClickListener {
                dismiss()
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                    replace(R.id.flMain, FavoriteBasketFragment.newInstance(favorite.name.toString()))
                        .addToBackStack(null)
                        .commit()
                }
            }
            binding.llFrBSChange.setOnClickListener{
                dismiss()
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                    replace(R.id.flMain, FavoriteEditFragment.newInstance(favorite.name.toString()))
                    remove(FavoriteDetailFragment()).commit()
                }
            }
            // ---! 즐겨찾기 삭제 !---
            binding.llFrBSDelete.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setTitle("⚠️ 알림")
                    setMessage("플레이리스트에서 삭제하시겠습니까?")
                    setPositiveButton("확인") { dialog, _ ->
                        deleteFavoriteItemSn(getString(R.string.IP_ADDRESS_t_favorite),
                            favorite.sn.toString()
                        ) {
                            requireActivity().supportFragmentManager.beginTransaction().apply {
                                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                                replace(R.id.flMain, FavoriteFragment())
                                    .commit()
                            }
                        }
                    }
                    setNegativeButton("취소") { dialog, _ -> }
                    create()
                }.show()
            }

        } // ------! 즐겨찾기 홈에서 경로 설정 끝 !------
        binding.ibtnFrBsExit.setOnClickListener { dismiss() }


    }


}