package com.tangoplus.tangoq.dialog

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.fragment.FavoriteBasketFragment
import com.tangoplus.tangoq.fragment.FavoriteDetailFragment
import com.tangoplus.tangoq.fragment.FavoriteEditFragment
import com.tangoplus.tangoq.fragment.FavoriteFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.FavoriteVO
import com.tangoplus.tangoq.databinding.FragmentFavoriteBSDialogBinding
import com.tangoplus.tangoq.`object`.NetworkFavorite.deleteFavoriteItemSn


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

            val imgByteArray = bundle.getByteArray("img")
            val imgBitmap = BitmapFactory.decodeByteArray(imgByteArray, 0, imgByteArray!!.size)
            binding.ivFbsThumbnail.setImageBitmap(imgBitmap)
            Log.v("imgByteArray", "imgByteArray: ${imgByteArray}")
            binding.tvFrBSName.text = favorite.favoriteName
            binding.llFrBSPlay.setOnClickListener{
                // TODO 재생목록 만들어서 FULLSCREEN
            }
            // ---! 편집하기 !---
            binding.llFrBSEdit.setOnClickListener {
                dismiss()
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                    replace(R.id.flMain, FavoriteEditFragment.newInstance(favorite.favoriteName.toString()))
                    remove(FavoriteDetailFragment()).commit()
                }
            }
            binding.llFrBSShare.setOnClickListener{
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                // TODO 앱 게시 후 앱 링크 추가 필요
                val content = "친구가 링크를 공유했어요!\n어떤 링크인지 들어가서 확인해볼까요?"
                intent.putExtra(Intent.EXTRA_TEXT, "$content\n\nhttps://tangoplus.co.kr/")
                val chooserTitle = "친구에게 공유하기"
                startActivity(Intent.createChooser(intent, chooserTitle))
            }
            // ---! 운동 추가하기 !---
            binding.llFrBSAddExercise.setOnClickListener {
                dismiss()
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                    replace(R.id.flMain, FavoriteBasketFragment.newInstance(favorite.favoriteName.toString()))
                        .addToBackStack(null)
                        .commit()
                }
            }
            binding.llFrBSChange.setOnClickListener{
                dismiss()
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                    replace(R.id.flMain, FavoriteEditFragment.newInstance(favorite.favoriteName.toString()))
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
                            favorite.favoriteSn.toString()
                        ) {
                            requireActivity().supportFragmentManager.beginTransaction().apply {
//                                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                                replace(R.id.flMain, FavoriteFragment())
                                    .commit()
                            }
                            dismiss()
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