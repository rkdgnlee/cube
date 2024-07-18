package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.BundleCompat.getParcelableArrayList
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.callback.ItemTouchCallback
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.FavoriteVO
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.databinding.FragmentFavoriteEditBinding
import com.tangoplus.tangoq.`object`.NetworkFavorite.updateFavoriteItemJson
import com.tangoplus.tangoq.`object`.Singleton_t_history
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList


class FavoriteEditFragment : Fragment() {
    lateinit var binding: FragmentFavoriteEditBinding
    val viewModel: FavoriteViewModel by activityViewModels()
    var sn = 0
    var title = ""
    lateinit var currentItem : FavoriteVO
    private lateinit var singletonInstance: Singleton_t_history
    private lateinit var behavior: BottomSheetBehavior<ConstraintLayout>

    companion object {
        private const val ARG_SN = "SN"
        fun newInstance(sn : Int): FavoriteEditFragment {
            val fragment = FavoriteEditFragment()
            val args = Bundle()
            args.putInt(ARG_SN, sn)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteEditBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // -----! 데이터 선언 !-----
//        val appClass = requireContext().applicationContext as AppClass

        singletonInstance = Singleton_t_history.getInstance(requireContext())
        val t_userData = Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("login_data")
        sn = requireArguments().getInt(ARG_SN)


        currentItem = viewModel.favoriteList.value?.find { it.favoriteSn == sn }!!
        binding.etFEName.setText(currentItem.favoriteName)

        // ------! behavior 조작 시작 !------
        val isTablet = resources.configuration.screenWidthDp >= 600
        behavior = BottomSheetBehavior.from(binding.clFE)
        val screenHeight = resources.displayMetrics.heightPixels
        val topSpaceHeight = resources.getDimensionPixelSize(R.dimen.top_space_height)
        val peekHeight = screenHeight - topSpaceHeight
        behavior.apply {
            this.peekHeight = peekHeight
            isFitToContents = false
            expandedOffset = 0
            state = BottomSheetBehavior.STATE_COLLAPSED
            skipCollapsed = false
            // 기기 유형에 따라 halfExpandedRatio 설정
            halfExpandedRatio = if (isTablet) {
                0.65f
            } else {
                0.99f
            }
        }
        // ------! behavior 조작 끝 !------

        // -----! EditText 셋팅 시작 !-----
        val currentPickItem = viewModel.favoriteList.value?.find { it.favoriteSn == sn }
        if (viewModel.favoriteEditItem.value == null || viewModel.favoriteEditItem.value!!.length() == 0) { // 초기 화면 일 때,
            if (currentPickItem != null) {
                binding.etFEName.setText(currentPickItem.favoriteName.toString())
            }
        } else {
            binding.etFEName.setText(viewModel.favoriteEditItem.value!!.optString("favorite_name"))

        }
// -----! EditText 셋팅 끝 !-----
//        val currentPickItem = appClass.pickItems.value?.get(appClass.pickList.value!!.indexOf(title))
        if (currentPickItem?.exercises != null) {
            val adapter = ExerciseRVAdapter(this@FavoriteEditFragment, currentPickItem.exercises!!, singletonInstance.viewingHistory!!.toList(),"add")
            binding.rvFE.adapter = adapter
            viewModel.exerciseUnits.value = currentPickItem.exercises
        }
        // -----! EditText 등 제목 VM 연동 시작 !-----
        binding.etFEName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.favoriteEditItem.value?.put("favorite_name", s.toString())
                viewModel.favoriteEditItem.value?.let { Log.w("편집이름", it.optString("favorite_name")) }
            }
        })

        // -----! EditText 등 제목 VM 연동 끝 !-----

        viewModel.exerciseUnits.observe(viewLifecycleOwner) { basketUnits ->
            val adapter = ExerciseRVAdapter(this@FavoriteEditFragment, basketUnits, singletonInstance.viewingHistory?.toList() ?: listOf(), "edit")
            binding.rvFE.adapter = adapter
            val linearLayoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvFE.layoutManager = linearLayoutManager

            // -----! item에 swipe 및 dragNdrop 연결 !-----
            val callback = ItemTouchCallback(adapter).apply {
//                setClamp(260f)
            }
            val touchHelper = ItemTouchHelper(callback)
            touchHelper.attachToRecyclerView(binding.rvFE)
            binding.rvFE.adapter = adapter
            adapter.startDrag(object : ExerciseRVAdapter.OnStartDragListener {
                override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
                    touchHelper.startDrag(viewHolder)
                }
            })
            adapter.notifyDataSetChanged()
            // -----! swipe, drag 연동 !-----
//            if (viewModel.exerciseUnits.value?.size != 0 ) {
//                binding.ivPickEditNull.visibility = View.GONE
//            } else {
//                binding.ivPickEditNull.visibility = View.VISIBLE
//            }
        }

        binding.nsvFE.isNestedScrollingEnabled = true
        binding.rvFE.isNestedScrollingEnabled = false
        binding.rvFE.overScrollMode = View.OVER_SCROLL_NEVER

        // -----! 장바구니로 가기 !-----
        binding.btnFEGoBasket.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, ExerciseFragment.newInstance(sn))
                    .addToBackStack(null)
                    .commit()
            }
            it.isClickable = true
        }

        binding.ibtnFEBack.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, FavoriteDetailFragment.newInstance(sn))
                remove(FavoriteDetailFragment())
                commit()
            }
            viewModel.exerciseUnits.value?.clear()
            it.isClickable = true
        }
        // -----! 즐겨찾기 하나 만들기 시작 !-----
        binding.fabtnFEFinish.setOnClickListener {
            updateFavorite()
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, FavoriteDetailFragment.newInstance(sn))
                remove(FavoriteDetailFragment())
                commit()
            }
        } // -----! 즐겨찾기 하나 만들기 끝 !-----
    }


    @SuppressLint("SuspiciousIndentation")
    private fun updateFavorite() {

//        val appClass = requireContext().applicationContext as AppClass
        val index = viewModel.favoriteList.value?.indexOfFirst { it.favoriteSn == sn } // 즐겨찾기이름이 title인 인덱스를 가져온다.

        val currentEditItem = viewModel.favoriteList.value!![index!!]

        currentEditItem.favoriteName = binding.etFEName.text.toString()
        currentEditItem.exercises = viewModel.exerciseUnits.value?.toMutableList()
        Log.w("favoriteSave", "시리얼넘버: ${currentEditItem.favoriteSn}, 운동들: ${currentEditItem.exercises}")

        viewModel.favoriteList.value?.set(index, currentEditItem)


        val favoriteitem = viewModel.favoriteList.value?.get(index)
        favoriteitem?.favoriteName = currentEditItem.favoriteName.toString()


            if (favoriteitem != null) {
            viewModel.favoriteList.value?.set(index, favoriteitem)

        }
        title = currentEditItem.favoriteName.toString()


        // ------! 운동 리스트, 썸네일 중복없이 담기 시작 !------
        val descriptionIdList = mutableListOf<Int>()
        val imgThumbnails = mutableSetOf<String>()
        for (i in 0 until (viewModel.exerciseUnits.value?.size ?: Log.w(ContentValues.TAG, "unvalid Data"))) {
            viewModel.exerciseUnits.value?.get(i)?.exerciseId?.let {
                descriptionIdList.add(
                    it.toInt())
            }
            // 업데이트 할 운동 목록에서 이미지 썸네일 받아오기
            imgThumbnails.add(viewModel.exerciseUnits.value?.get(i)?.imageFilePathReal.toString())
        }
        Log.v("DscIDList", "${descriptionIdList}")
        Log.v("imgThumbnails", "${imgThumbnails}")
        // ------! 운동 리스트, 썸네일 중복없이 담기 끝 !------
        viewModel.favoriteItem.value = currentEditItem
        // -----! json으로 변환 후 update 시작 !-----
        val JsonObj = JSONObject()
        JsonObj.put("favorite_name", currentEditItem.favoriteName)
        JsonObj.put("exercise_ids", JSONArray(descriptionIdList))

        Log.w("JsonExerciseIdList","${JsonObj.get("exercise_ids")}")
        Log.w("updateJsonInBody", "$JsonObj")
        updateFavoriteItemJson(getString(R.string.IP_ADDRESS_t_favorite), currentEditItem.favoriteSn.toString(), JsonObj.toString(), requireContext()) {
            viewModel.favoriteList.value!![index].imgThumbnails = imgThumbnails.toMutableList()
        }
        viewModel.exerciseUnits.value?.clear()

        // -----! json으로 변환 후 update 끝 !-----
    }
}