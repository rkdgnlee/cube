package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.transition.Visibility
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
    var index = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteBSDialogBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = arguments
        val favorite = bundle?.getParcelable<FavoriteVO>("Favorite")
        // ------! 즐겨찾기 홈에서 경로 설정 시작 !------
        if (favorite != null) {
            Log.v("favorite", "${favorite.imgThumbnails}")
            // ------! 썸네일 사진 4개 시작 !------
            val imgByteArray = bundle.getByteArray("img")
            if (imgByteArray != null) {
                // index = 0 : bytearray값 존재 ->
                // index = 1 : imgThumbnails에서 전부 glide

                index = 0
            } else {
                index = 1
            }
            when (index) {
                0 -> {
                    val imgBitmap = BitmapFactory.decodeByteArray(imgByteArray, 0, imgByteArray!!.size)
                    // ------! null 이미지와 favorite more버튼은 ivFvbsNull을 사용함(favorite는 bitmap으로 캡쳐해서 가져옴) !------
                    binding.ivFBSThumbnailNull.visibility = View.VISIBLE
                    binding.ivFBSThumbnailNull.setImageBitmap(imgBitmap)
                    setVisibility(false)
                }
                1 -> {
                    val itemCount = favorite.exercises?.size
                    binding.vFBS.visibility = View.INVISIBLE
                    Log.v("썸네일갯수", "${itemCount}")
                    when (itemCount) {
                        0 -> {
                            binding.ivFBSThumbnailNull.visibility = View.VISIBLE
                            binding.tvFBSThumbnailMore.visibility = View.INVISIBLE
                            setVisibility(false)
                        }
                        1 -> {
                            val list = listOf(binding.ivFBSThumbnail1)
                            binding.ivFBSThumbnailNull.visibility = View.GONE
                            binding.ivFBSThumbnail2.visibility = View.GONE
                            binding.llFBSThumbnailBottom.visibility = View.GONE
                            setThumbnails(favorite.imgThumbnails!!.take(1), list, listOf(true, false, false, false))
                        }
                        2 -> {
                            val list = listOf(binding.ivFBSThumbnail1, binding.ivFBSThumbnail2)
                            binding.ivFBSThumbnailNull.visibility = View.GONE
                            binding.llFBSThumbnailBottom.visibility = View.GONE
                            setThumbnails(favorite.imgThumbnails!!.take(2), list, listOf(true, true, false, false))
                        }
                        3 -> {
                            val list = listOf(binding.ivFBSThumbnail1, binding.ivFBSThumbnail2 , binding.ivFBSThumbnail3)
                            binding.ivFBSThumbnailNull.visibility = View.GONE
                            binding.ivFrBSThumbnail4.visibility = View.GONE
                            setThumbnails(favorite.imgThumbnails!!.take(3), list, listOf(true, true, true, false))
                        }
                        4 -> {
                            val list = listOf(binding.ivFBSThumbnail1, binding.ivFBSThumbnail2, binding.ivFBSThumbnail3, binding.ivFrBSThumbnail4)
                            binding.ivFBSThumbnailNull.visibility = View.GONE
                            setThumbnails(favorite.imgThumbnails!!.take(4), list, listOf(true, true, true, true))
                        }
                        else -> {
                            val list = listOf(binding.ivFBSThumbnail1, binding.ivFBSThumbnail2, binding.ivFBSThumbnail3, binding.ivFrBSThumbnail4)
                            binding.ivFBSThumbnailNull.visibility = View.GONE
                            binding.vFBS.visibility = View.VISIBLE
                            binding.tvFBSThumbnailMore.visibility = View.VISIBLE
                            setThumbnails(favorite.imgThumbnails!!.take(4), list, listOf(true, true, true, true))
                            if (itemCount != null) {
                                binding.tvFBSThumbnailMore.text = "+ ${itemCount - 4}"
                            }
                        }
                    }
                }
            } // ------! 썸네일 사진 4개 끝 !------


            binding.tvFBSPlay.text = favorite.favoriteName
            binding.llFBSPlay.setOnClickListener{
                // TODO 재생목록 만들어서 FULLSCREEN
            }
            // ---! 편집하기 !---
            binding.llFBSEdit.setOnClickListener {
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
            binding.llFBSAddExercise.setOnClickListener {
                dismiss()
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                    replace(R.id.flMain, FavoriteBasketFragment.newInstance(favorite.favoriteName.toString()))
                        .addToBackStack(null)
                        .commit()
                }
            }
            binding.llFBSChange.setOnClickListener{
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
                            favorite.favoriteSn.toString(), requireContext()
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
        binding.ibtnFBsExit.setOnClickListener { dismiss() }
    }

    // ------! 이미지 썸네일 함수 시작 !------
    private fun loadImage(context: Context, url: String, imageView: ImageView) {
        Glide.with(context)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }
    private fun setThumbnails(urls: List<String>, imageViews : List<ImageView>,visibilityFlags : List<Boolean>) {
        imageViews.forEachIndexed { index, imageView ->
            if (index < urls.size) {
                loadImage(requireContext(), urls[index], imageView)
                imageView.visibility = if (visibilityFlags[index]) View.VISIBLE else View.GONE
            } else {
                imageView.visibility = View.GONE
            }
        }
    }
    private fun setVisibility(tf : Boolean){
        if (tf) {
            binding.ivFBSThumbnail1.visibility = View.VISIBLE
            binding.ivFBSThumbnail2.visibility = View.VISIBLE
            binding.ivFBSThumbnail3.visibility = View.VISIBLE
            binding.ivFrBSThumbnail4.visibility = View.VISIBLE
        } else {
            binding.ivFBSThumbnail1.visibility = View.GONE
            binding.ivFBSThumbnail2.visibility = View.GONE
            binding.ivFBSThumbnail3.visibility = View.GONE
            binding.ivFrBSThumbnail4.visibility = View.GONE
        }
    }
}