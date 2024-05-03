package com.tangoplus.tangoq.Fragment

import android.content.ContentValues
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.Adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.Callback.ItemTouchCallback
import com.tangoplus.tangoq.Object.NetworkExerciseService.updateFavoriteItemJson
import com.tangoplus.tangoq.Object.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.ExerciseViewModel
import com.tangoplus.tangoq.ViewModel.FavoriteItemVO
import com.tangoplus.tangoq.ViewModel.FavoriteVO
import com.tangoplus.tangoq.databinding.FragmentFavoriteEditBinding
import org.json.JSONArray
import org.json.JSONObject


class FavoriteEditFragment : Fragment() {
    lateinit var binding: FragmentFavoriteEditBinding
    val viewModel: ExerciseViewModel by activityViewModels()
    var title = ""
    companion object {
        private const val ARG_TITLE = "title"
        fun newInstance(title: String): FavoriteEditFragment {
            val fragment = FavoriteEditFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
// -----! 데이터 선언 !-----
//        val appClass = requireContext().applicationContext as AppClass
        val t_userData = Singleton_t_user.getInstance(requireContext())
        title = requireArguments().getString(ARG_TITLE).toString()
        binding.etFEName.setText(title)

        // -----! EditText 셋팅 시작 !-----
        val currentPickItem = viewModel.favoriteItems.value?.find { it.favoriteName == title }
        if (viewModel.favoriteEditItem.value == null || viewModel.favoriteEditItem.value!!.length() == 0) { // 초기 화면 일 때,
            if (currentPickItem != null) {
                binding.etFEName.setText(currentPickItem.favoriteName.toString())
                binding.etFEExplain.setText(currentPickItem.favoriteExplain.toString())
            }
        } else {
            binding.etFEName.setText(viewModel.favoriteEditItem.value!!.optString("favorite_name"))
            binding.etFEExplain.setText(viewModel.favoriteEditItem.value!!.optString("favorite_explain"))
        }
// -----! EditText 셋팅 끝 !-----
//        val currentPickItem = appClass.pickItems.value?.get(appClass.pickList.value!!.indexOf(title))
        if (currentPickItem?.exercises != null) {
            val adapter = ExerciseRVAdapter(this@FavoriteEditFragment, currentPickItem.exercises!!, "add")
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
        binding.etFEExplain.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.favoriteEditItem.value?.put("favorite_explain", s.toString())
                viewModel.favoriteEditItem.value?.let { Log.w("편집설명", it.optString("favorite_explain")) }
            }
        })

        // -----! EditText 등 제목 VM 연동 끝 !-----

        viewModel.exerciseUnits.observe(viewLifecycleOwner) { basketUnits ->
            val adapter = ExerciseRVAdapter(this@FavoriteEditFragment, basketUnits, "edit")
            binding.rvFE.adapter = adapter
            val linearLayoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvFE.layoutManager = linearLayoutManager

            // -----! item에 swipe 및 dragNdrop 연결 !-----
            val callback = ItemTouchCallback(adapter).apply {
//                setClamp(260f)
//                removePreviousClamp(binding.rvPickadd)
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
        binding.btnFEGoBakset.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, FavoriteBasketFragment.newInstance(title))
                    .addToBackStack(null)
                    .commit()
            }
            it.isClickable = true
        }

        binding.ibtnFEBack.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, FavoriteDetailFragment.newInstance(title))
                commit()
            }
            viewModel.exerciseUnits.value?.clear()
            it.isClickable = true
        }
        // -----! 즐겨찾기 하나 만들기 시작 !-----
        binding.btnFEFinish.setOnClickListener {
            updatePickEdit()
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, FavoriteDetailFragment.newInstance(title))

                remove(FavoriteDetailFragment()).commit()
            }
        } // -----! 즐겨찾기 하나 만들기 끝 !-----
    }


    private fun updatePickEdit() {

//        val appClass = requireContext().applicationContext as AppClass
        val index = viewModel.favoriteItems.value?.indexOfFirst { it.favoriteName == title } // 즐겨찾기이름이 title인 인덱스를 가져온다.

        val pickItem = FavoriteItemVO(
            favoriteSn = index?.let { viewModel.favoriteItems.value!![it].favoriteSn }!!,
            favoriteName = binding.etFEName.text.toString(),
            favoriteExplain = binding.etFEExplain.text.toString(),
            exercises = viewModel.exerciseUnits.value?.toMutableList(),
        )
        Log.w("PickItemSave", "시리얼넘버: ${pickItem.favoriteSn}, 운동들: ${pickItem.exercises}")

        viewModel.favoriteItems.value?.set(index, pickItem)
        val favoriteitem = viewModel.favoriteList.value?.get(index)
        favoriteitem?.name = pickItem.favoriteName.toString()


        if (favoriteitem != null) {
            viewModel.favoriteList.value?.set(index, favoriteitem)
        }
        title = pickItem.favoriteName.toString()



        val descriptionIdList = mutableListOf<Int>()
        for (i in 0 until (viewModel.exerciseUnits.value?.size ?: Log.w(ContentValues.TAG, "unvalid Data"))) {
            viewModel.exerciseUnits.value?.get(i)?.exerciseDescriptionId?.let {
                descriptionIdList.add(
                    it)
            }
        }
        Log.w("DscIDList", "${descriptionIdList}")
        // -----! json으로 변환 후 update 시작 !-----
        val JsonObj = JSONObject()
        JsonObj.put("favorite_name", pickItem.favoriteName)
        JsonObj.put("exercise_description_ids", JSONArray(descriptionIdList))
        JsonObj.put("favorite_description", pickItem.favoriteExplain)
        Log.w("JsonExerciseIdList","${JsonObj.get("exercise_description_ids")}")
        Log.w("updateJsonInBody", "$JsonObj")
        updateFavoriteItemJson(getString(R.string.IP_ADDRESS_t_favorite), pickItem.favoriteSn.toString(), JsonObj.toString()) {
            // TODO 반환된 값으로 뭐 해도 되고 안해도 됨.
        }
        viewModel.exerciseUnits.value?.clear()
        // -----! json으로 변환 후 update 끝 !-----
    }

}