package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.dialog.FavoriteBSDialogFragment
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ACTVAdapter
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.data.FavoriteVO
import com.tangoplus.tangoq.databinding.FragmentFavoriteDetailBinding
import com.tangoplus.tangoq.`object`.NetworkFavorite.fetchFavoriteItemJsonBySn
import com.tangoplus.tangoq.`object`.NetworkFavorite.jsonToFavoriteItemVO
import com.tangoplus.tangoq.`object`.Singleton_t_history
import kotlinx.coroutines.launch
import org.json.JSONObject


class FavoriteDetailFragment : Fragment(){
    lateinit var binding : FragmentFavoriteDetailBinding
    val viewModel : FavoriteViewModel by activityViewModels()
    var sn = 0
    var title = ""
    lateinit var currentItem : FavoriteVO
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var singletonInstance: Singleton_t_history
    private lateinit var behavior: BottomSheetBehavior<ConstraintLayout>

//    var popupWindow : PopupWindow?= null
    companion object {
        private const val ARG_SN = "SN"
        fun newInstance(sn: Int): FavoriteDetailFragment {
            val fragment = FavoriteDetailFragment()
            val args = Bundle()
            args.putInt(ARG_SN, sn)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        singletonInstance = Singleton_t_history.getInstance(requireContext())
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) { }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteDetailBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.nsvFV.isNestedScrollingEnabled = false
//        binding.rvFV.isNestedScrollingEnabled = false
//        binding.rvFV.overScrollMode = View.OVER_SCROLL_NEVER

        sn = requireArguments().getInt(ARG_SN)
        Log.v("currentItem", "favoriteSn: ${sn}")
//        currentItem = viewModel.favoriteList.value?.find { it.favoriteSn == sn }!!
//        viewModel.favoriteItem.value = currentItem
//        Log.v("currentItem", "${viewModel.favoriteItem.value!!.favoriteTotalTime}")
//        Log.v("currentItem", "${viewModel.favoriteItem.value!!.favoriteTotalCount}")

//        setFVDetail(viewModel.favoriteItem.value!!.favoriteSn)
        /** ============================= 흐름 =============================
         *  1. favoriteList에서 sn을 받아서 옴
         *  2. sn으로 조회한 즐겨찾기 1개의 이름, 설명, 운동 목록 가져옴 (favoritelist에서)
         *  3. 가져와서 뿌리기 (viewModel.pickitem
         *
         *
         *
         * */
        // ------! behavior 조작 시작 !------
        val isTablet = resources.configuration.screenWidthDp >= 600
        behavior = BottomSheetBehavior.from(binding.clFD)
        val screenHeight = resources.displayMetrics.heightPixels
        val topSpaceHeight = resources.getDimensionPixelSize(R.dimen.top_space_height)
        val peekHeight = screenHeight - topSpaceHeight
        behavior.apply {
            this.peekHeight = peekHeight
            isFitToContents = false
            expandedOffset = 0
            state = BottomSheetBehavior.STATE_COLLAPSED
            skipCollapsed = false
            halfExpandedRatio = if (isTablet) {
                0.65f
            } else {
                0.99f
            }
        }
        // ------! behavior 조작 끝 !------

        // -----! 운동 favoriteList, 제목 가져오기 시작 !-----
        lifecycleScope.launch {
            binding.sflFV.startShimmer()

            currentItem = fetchFavoriteItemJsonBySn(getString(R.string.IP_ADDRESS_t_favorite), sn.toString())
//            // ------! 즐겨찾기 넣어서 가져오기 시작 !------
//            currentItem = viewModel.favoriteList.value?.find { it.favoriteSn == sn }!!
            binding.actFVDetail.setText(currentItem.favoriteName)
            setFVDetail(viewModel.favoriteItem.value!!.favoriteSn)
//
            if (currentItem.exercises!!.isEmpty()) {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE

            } else {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE
            }

            val favoriteList = mutableListOf<Pair<Int, String>>()
            viewModel.favoriteList.observe(viewLifecycleOwner) { array ->
                favoriteList.clear()
                for (i in 0 until array.size) {
                    favoriteList.add(Pair(array[i].favoriteSn, array[i].favoriteName.toString()))
                }
            }

            val adapter = ACTVAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line , favoriteList)
            binding.actFVDetail.setAdapter(adapter)
//            binding.actFVDetail.addTextChangedListener(object: TextWatcher {
//                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {  }
//                override fun afterTextChanged(s: Editable?) {
//                    title = s.toString()
//                    currentItem = viewModel.favoriteList.value?.find { it.favoriteSn == sn }!!
////                    favoriteSn = viewModel.favoriteList.value?.find { it.favoriteSn == currentItem.favoriteSn }?.favoriteSn.toString() // Pair로 특정 favoriteList하나의 sn을 가져오기
//                    Log.v("현재Sn", "현재 sn: ${currentItem.favoriteSn}")
//                    setFVDetail(sn)
//                }
//            })
            binding.actFVDetail.setOnItemClickListener { parent, view, position, id ->
                val selectedPair = parent.adapter.getItem(position) as Pair<Int, String>
                val selectedSn = selectedPair.first
                val selectedName = selectedPair.second

                Log.v("선택된 항목", "sn : $selectedSn, Name: $selectedName")
                currentItem = viewModel.favoriteList.value?.find { it.favoriteSn == selectedSn }!!
                setFVDetail(selectedSn)

            }
        }
        // ----- 운동 favoriteList, 제목 가져오기 끝 -----

        binding.ibtnFDBack.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, FavoriteFragment())
                commit()
            }
            it.isClickable = true
        }
        binding.ibtnFDMore.setOnClickListener {
            // ------! img bytearray로 만들기가 안됨.


            val bsFragment = FavoriteBSDialogFragment()
            val bundle = Bundle()
            Log.v("currentItem상태", "${currentItem.exercises?.size}")

            bundle.putParcelable("Favorite", currentItem)
            bsFragment.arguments = bundle
            val fragmentManager = requireActivity().supportFragmentManager
            bsFragment.show(fragmentManager, bsFragment.tag)
        }


        // -----! 편집 버튼 시작 !-----
        binding.btnFVEdit.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
//                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, FavoriteEditFragment.newInstance(sn))


                remove(FavoriteDetailFragment()).commit()
            }
            requireContext()
        } // -----! 편집 버튼 끝 !-----


        // ------! 플롯팅액션버튼 시작 !------
        binding.fabtnFDPlay.setOnClickListener {
            if (currentItem.exercises?.isNotEmpty() == true) {
                    val urls = storeFavoriteUrl(viewModel)
//                    Log.w("url in resourceList", "$resourceList")
                    val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
                    intent.putStringArrayListExtra("urls", ArrayList(urls))
                    intent.putExtra("total_time", currentItem.favoriteTotalTime)
                    startForResult.launch(intent)
                } else {
                    val snackbar = Snackbar.make(requireView(), "운동을 추가해주세요 ! ", Snackbar.LENGTH_SHORT)
                    snackbar.setAction("확인") { snackbar.dismiss() }
                snackbar.setTextColor(Color.WHITE)
                    snackbar.setActionTextColor(Color.WHITE)
                    snackbar.show()
                }
        } // ------! 플롯팅액션버튼 끝 !------

    }
    @SuppressLint("NotifyDataSetChanged")
    private fun setFVDetail(sn: Int){
        lifecycleScope.launch {
            binding.sflFV.startShimmer()
            Log.v("현재 Sn", "$sn")
//            snData = fetchFavoriteItemJsonBySn(getString(R.string.IP_ADDRESS_t_favorite), sn.toString())
//            currentItem = jsonToFavoriteItemVO(snData)

//            viewModel.favoriteList.value = viewModel.favoriteList.value
            if (currentItem.exercises!!.isEmpty()) {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE
            } else {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE
            }

            // ------! 썸네일 4개 시작 !------
            when (currentItem.imgThumbnails?.size) {
                0 -> {
                    binding.ivFDThumbnailNull.visibility = View.VISIBLE
//                    holder.tvFvThumbnailMore.visibility = View.INVISIBLE
                    setThumbnailGone()
//                    holder.tvFvTime.text = "0"
                }
                1 -> {
                    val list = listOf(binding.ivFDThumbnail1)
                    binding.ivFDThumbnailNull.visibility = View.GONE
                    binding.ivFDThumbnail2.visibility = View.GONE
                    binding.llFDThumbnailBottom.visibility = View.GONE
                    setThumbnails(currentItem.imgThumbnails!!.take(1), list , listOf(true, false, false, false))
                }
                2 -> {
                    val list = listOf(binding.ivFDThumbnail1, binding.ivFDThumbnail2)
                    binding.ivFDThumbnailNull.visibility = View.GONE
                    binding.llFDThumbnailBottom.visibility = View.GONE
                    setThumbnails(currentItem.imgThumbnails!!.take(2), list , listOf(true, true, false, false))
                }
                3 -> {
                    val list = listOf(binding.ivFDThumbnail1, binding.ivFDThumbnail2, binding.ivFDThumbnail3)
                    binding.ivFDThumbnailNull.visibility = View.GONE
                    binding.ivFDThumbnail4.visibility = View.GONE
                    setThumbnails(currentItem.imgThumbnails!!.take(3), list , listOf(true, true, true, false))

                }
                4 -> {
                    val list = listOf(binding.ivFDThumbnail1, binding.ivFDThumbnail2, binding.ivFDThumbnail3, binding.ivFDThumbnail4)
                    binding.ivFDThumbnailNull.visibility = View.GONE
                    setThumbnails(currentItem.imgThumbnails!!.take(4), list , listOf(true, true, true, true))
                }
                else -> { // ------! 5개 이상일 때 !------
                    val list = listOf(binding.ivFDThumbnail1, binding.ivFDThumbnail2, binding.ivFDThumbnail3, binding.ivFDThumbnail4)
                    binding.ivFDThumbnailNull.visibility = View.GONE
                    setThumbnails(currentItem.imgThumbnails!!.take(4), list , listOf(true, true, true, true))
                }
            }
            // ------! 썸네일 4개 끝 !------

            // ------! 갯수 + 시간 시작 !------
            binding.tvFDTime.text = (if (currentItem.favoriteTotalTime?.toInt()!! <= 60) {
                "${currentItem.favoriteTotalTime?.toInt()}초"
            } else {
                "${currentItem.favoriteTotalTime?.toInt()!! / 60}분 ${currentItem.favoriteTotalTime?.toInt()!! % 60}초"
            })
            binding.tvFDCount.text = "${currentItem.exercises?.size} 개"
//            Log.v("SetcurrentItem", "${currentItem.exercises}")
            // ------! 갯수 + 시간 끝 !------

            val rvAdapter = ExerciseRVAdapter(this@FavoriteDetailFragment, currentItem.exercises!!, singletonInstance.viewingHistory?.toList() ?: listOf(),"main")
            rvAdapter.exerciseList = currentItem.exercises!!
            val linearLayoutManager2 =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvFV.layoutManager = linearLayoutManager2
            binding.rvFV.adapter = rvAdapter

            var totalTime = 0
            for (i in 0 until currentItem.exercises!!.size) {
                val exercises = currentItem.exercises!![i]
//                Log.w("운동각 시간" ,"${exercises.videoDuration!!.toInt()}")
                totalTime += exercises.videoDuration!!.toInt()
            }
            Log.w("총 시간", "$totalTime")
        }
    }

    private fun storeFavoriteUrl(viewModel : FavoriteViewModel) : MutableList<String> {
        val urls = mutableListOf<String>()
//        val title = requireArguments().getString(ARG_SN).toString()
        val currentItem = viewModel.favoriteList.value?.find { it.favoriteSn == sn }
        Log.w("PreviousStoreURL", "$currentItem")
        for (i in 0 until currentItem!!.exercises!!.size) {
            val exercises = currentItem.exercises!![i]
            urls.add(exercises.videoFilepath.toString())
            Log.w("Finish?storeUrl", "$urls")
        }
        return  urls
    }

    private fun setThumbnails( urls: List<String>, imageViews: List<ImageView>, visibilityFlags: List<Boolean>) {
        imageViews.forEachIndexed{ index, imageView ->
            if (index < urls.size) {
                Glide.with(requireContext())
                    .load(urls[index])
                    .override(300)
                    .into(imageView)
                imageView.visibility = if (visibilityFlags[index]) View.VISIBLE else View.GONE
            } else {
                imageView.visibility = View.GONE
            }
        }
    }
    fun setThumbnailGone() {
        binding.ivFDThumbnail1.visibility = View.GONE
        binding.ivFDThumbnail2.visibility = View.GONE
        binding.ivFDThumbnail3.visibility = View.GONE
        binding.ivFDThumbnail4.visibility = View.GONE
    }
//    fun getBitMapFromView(view: View) : Bitmap {
//        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        view.draw(canvas)
//        return bitmap
//    }

}