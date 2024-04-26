package com.example.mhg

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ExerciseVO
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.PickItemVO
import com.example.mhg.databinding.FragmentPickEditBinding
import com.example.mhg.`object`.NetworkExerciseService.updateFavoriteItemJson
import com.example.mhg.`object`.Singleton_t_user
import org.json.JSONArray
import org.json.JSONObject


class PickEditFragment : Fragment() {
    lateinit var binding: FragmentPickEditBinding
    val viewModel: ExerciseViewModel by activityViewModels()

    var title = ""
    companion object {
        private const val ARG_TITLE = "title"
        fun newInstance(title: String): PickEditFragment {
            val fragment = PickEditFragment()
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
        binding = FragmentPickEditBinding.inflate(inflater)
        return binding.root
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----! 데이터 선언 !-----
//        val appClass = requireContext().applicationContext as AppClass
        val t_userData = Singleton_t_user.getInstance(requireContext())
        title = requireArguments().getString(ARG_TITLE).toString()
        binding.etPickEditName.setText(title)

        // -----! EditText 셋팅 시작 !-----
        val currentPickItem = viewModel.favoriteItems.value?.find { it.favoriteName == title }
        if (viewModel.favoriteEditItem.value == null || viewModel.favoriteEditItem.value!!.length() == 0) { // 초기 화면 일 때,
            if (currentPickItem != null) {
                binding.etPickEditName.setText(currentPickItem.favoriteName.toString())
                binding.etPickEditExplainTitle.setText(currentPickItem.favoriteExplainTitle.toString())
                binding.etPickEditExplain.setText(currentPickItem.favoriteExplain.toString())
            }
        } else {
            binding.etPickEditName.setText(viewModel.favoriteEditItem.value!!.optString("favorite_name"))
            binding.etPickEditExplainTitle.setText(viewModel.favoriteEditItem.value!!.optString("favorite_explain_title"))
            binding.etPickEditExplain.setText(viewModel.favoriteEditItem.value!!.optString("favorite_explain"))
        }
// -----! EditText 셋팅 끝 !-----

//        val currentPickItem = appClass.pickItems.value?.get(appClass.pickList.value!!.indexOf(title))
        if (currentPickItem?.exercises != null) {
            val adapter = HomeVerticalRecyclerViewAdapter(currentPickItem.exercises!!, "add")
            binding.rvPickEdit.adapter = adapter
            viewModel.exerciseUnits.value = currentPickItem.exercises
        }
        // -----! EditText 등 제목 VM 연동 시작 !-----
        binding.etPickEditName.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.favoriteEditItem.value?.put("favorite_name", s.toString())
                viewModel.favoriteEditItem.value?.let { Log.w("편집이름", it.optString("favorite_name")) }
            }
        })
        binding.etPickEditExplainTitle.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.favoriteEditItem.value?.put("favorite_explain_title", s.toString())
                viewModel.favoriteEditItem.value?.let { Log.w("편집설명title", it.optString("favorite_explain_title")) }
            }
        })

        binding.etPickEditExplain.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.favoriteEditItem.value?.put("favorite_explain", s.toString())
                viewModel.favoriteEditItem.value?.let { Log.w("편집설명", it.optString("favorite_explain")) }
            }
        })

        // -----! EditText 등 제목 VM 연동 끝 !-----

        viewModel.exerciseUnits.observe(viewLifecycleOwner) { basketUnits ->
            val adapter = HomeVerticalRecyclerViewAdapter(basketUnits, "add")
            adapter.verticalList = basketUnits
            val linearLayoutManager2 =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvPickEdit.layoutManager = linearLayoutManager2

            // -----! item에 swipe 및 dragNdrop 연결 !-----
            val callback = ItemTouchCallback(adapter).apply {
//                setClamp(260f)
//                removePreviousClamp(binding.rvPickadd)
            }
            val touchHelper = ItemTouchHelper(callback)
            touchHelper.attachToRecyclerView(binding.rvPickEdit)
            binding.rvPickEdit.adapter = adapter
            adapter.startDrag(object : HomeVerticalRecyclerViewAdapter.OnStartDragListener {
                override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
                    touchHelper.startDrag(viewHolder)
                }
            })
            adapter.notifyDataSetChanged()
            // -----! swipe, drag 연동 !-----
            if (viewModel.exerciseUnits.value?.size != 0 ) {
                binding.ivPickEditNull.visibility = View.GONE
            } else {
                binding.ivPickEditNull.visibility = View.VISIBLE
            }
        }

        binding.nsvPickEdit.isNestedScrollingEnabled = true
        binding.rvPickEdit.isNestedScrollingEnabled = false
        binding.rvPickEdit.overScrollMode = View.OVER_SCROLL_NEVER

        // -----! 장바구니로 가기 !-----
        binding.tvPickEditGoBasket.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, PickBasketFragment.newInstance(title))
                    .addToBackStack(null)
                    .commit()
            }
            it.isClickable = true
        }

        binding.tvPickEditBack.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, PickDetailFragment.newInstance(title))
                commit()
            }
            viewModel.exerciseUnits.value?.clear()
            it.isClickable = true
        }
        // -----! 즐겨찾기 하나 만들기 시작 !-----
        binding.btnPickEditFinish.setOnClickListener {
            updatePickEdit()
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, PickDetailFragment.newInstance(title))

                remove(PickEditFragment()).commit()
            }
        } // -----! 즐겨찾기 하나 만들기 끝 !-----
    }
    private fun updatePickEdit() {

        // -----! 데이터 갈무리 후 appClass 수정 시작 !-----
//        val appClass = requireContext().applicationContext as AppClass
        val index = viewModel.favoriteItems.value?.indexOfFirst { it.favoriteName == title }

        val pickItem = PickItemVO(
            favoriteSn = index?.let { viewModel.favoriteItems.value!![it].favoriteSn }!!,
            favoriteName = binding.etPickEditName.text.toString(),
            favoriteExplainTitle = binding.etPickEditExplainTitle.text.toString(),
            favoriteExplain = binding.etPickEditExplain.text.toString(),
            exercises = viewModel.exerciseUnits.value?.toMutableList(),
        )
        Log.w("PickItemSave", "시리얼넘버: ${pickItem.favoriteSn}, 운동들: ${pickItem.exercises}")

        viewModel.favoriteItems.value?.set(index, pickItem)
        viewModel.favoriteList.value?.set(index, Pair(pickItem.favoriteSn.toInt(), pickItem.favoriteName.toString()))
        title = pickItem.favoriteName.toString()

        // -----! 데이터 갈무리 후 appClass 수정 끝 !-----

        val descriptionIdList = mutableListOf<Int>()
        for (i in 0 until (viewModel.exerciseUnits.value?.size ?: Log.w(TAG, "unvalid Data"))) {
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
        JsonObj.put("favorite_description_title", pickItem.favoriteExplainTitle)
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