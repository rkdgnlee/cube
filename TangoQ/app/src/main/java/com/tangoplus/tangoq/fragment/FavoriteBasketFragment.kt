package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchExerciseJson
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.databinding.FragmentFavoriteBasketBinding
import com.tangoplus.tangoq.`object`.Singleton_t_history
import kotlinx.coroutines.launch


class FavoriteBasketFragment : Fragment() {
    lateinit var binding: FragmentFavoriteBasketBinding
    private lateinit var adapter: ExerciseRVAdapter
    val viewModel : FavoriteViewModel by activityViewModels()
    var title = ""
    var sn = 0
    private val searchHistory = "search_history"
    private lateinit var singletonInstance: Singleton_t_history

    companion object {
        private const val ARG_SN = "SN"
        fun newInstance(sn: Int): FavoriteBasketFragment {
            val fragment = FavoriteBasketFragment()
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
        binding = FragmentFavoriteBasketBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SuspiciousIndentation", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        singletonInstance = Singleton_t_history.getInstance(requireContext())
        sn = requireArguments().getInt(ARG_SN)

        lifecycleScope.launch {
            val responseArrayList = fetchExerciseJson(getString(R.string.IP_ADDRESS_t_exercise_description))
            Log.w(ContentValues.TAG, "jsonArr: ${responseArrayList[0]}")
            try {
                viewModel.allExercises.value = responseArrayList.toMutableList()
                val allDataList = responseArrayList.toMutableList()

                // -----! RV 필터링 시작 !-----
                linkAdapter(allDataList)
                binding.tlFB.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        when (tab?.position) {
                            0 -> {
//                                recommendlist.map { exercise ->
//                                    exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseId.toString())
//                                }
                                linkAdapter(allDataList)
                            }
                            1 -> {
//                                val keywords = listOf("목관절", "어깨관절", "무릎관절", "척추")
//                                val filteredList = allDataList.filter { item -> item.exerciseCategoryId == "3" }.toMutableList()
//                                filteredList.map { exercise ->
//                                    exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseId.toString())
//                                }
//                                Log.w(ContentValues.TAG, "List2: ${filteredList[0]}")
                                linkAdapter(mutableListOf())
                            }
                            2 -> {
//                                val filteredList = allDataList.filter { item -> item.exerciseCategoryId == "3" }.toMutableList()
//                                filteredList.map { exercise ->
//                                    exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseId.toString())
//                                }
//                                linkAdapter(filteredList)
                            }
                            3 -> {
//                                val filteredList = allDataList.filter { item -> item.exerciseCategoryId == "4" }.toMutableList()
//                                filteredList.map { exercise ->
//                                    exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseId.toString())
//                                }
//                                linkAdapter(filteredList)
                            }
                        }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
                // -----! RV 필터링 끝 !-----

                // -----! 자동완성 검색 시작 !-----
                binding.ibtnFBACTVClear.setOnClickListener {
                    binding.actvFBSearch.text.clear()
                    binding.tlFB.selectTab(binding.tlFB.getTabAt(0))
                    updateRecyclerView(allDataList)
                }

                // ------! actv 포커싱 시작 !------
                binding.actvFBSearch.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        loadSearchHistory()
                    }
                }



                val exerciseNames = allDataList.map { it.exerciseName }.distinct().toMutableList()
                val adapterActv = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exerciseNames)
                binding.actvFBSearch.setAdapter(adapterActv)
                Log.v("exerciseNames", "${exerciseNames.size}")
//                binding.actvFBSearch.addTextChangedListener(object : TextWatcher {
//                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                        val inputText = s.toString()
//                        if (inputText.isNotEmpty()) {
//                            // 입력된 텍스트를 사용하여 관련된 exerciseName 필터링
//                            val filteredExerciseNames = allDataList.filter { it.relatedJoint!!.contains(inputText) }
//                                .map { it.exerciseName }
//                                .toMutableList()
//                            Log.v("filteredExerciseNames", "$filteredExerciseNames")
//                            // 필터링된 결과로 어댑터 업데이트\
//
//                            requireActivity().runOnUiThread {
//                                adapterActv.clear()
//                                adapterActv.addAll(filteredExerciseNames)
//                                adapterActv.notifyDataSetChanged()
//                                binding.actvFBSearch.showDropDown()
//                            }
//                        }
//                    }
//                    override fun afterTextChanged(s: Editable?) {
//
//                    }
//                })
                // ------! enter를 눌렀을 때 해당 텍스트로 관절 검색 !------
                binding.actvFBSearch.setOnEditorActionListener{ _, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_NEXT || event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {

                        // ------! enter 클릭시 동작 시작 !------
                        binding.tlFB.selectTab(binding.tlFB.getTabAt(1))

                        val inputText = binding.actvFBSearch.text.toString()
                        if (inputText.isNotEmpty()) {
                            val filteredExercises = allDataList.filter { it.relatedJoint!!.contains(inputText) }
                                .distinct()
                                .toMutableList()
                            Log.v("filteredExercises", "${filteredExercises.size}")
                            updateRecyclerView(filteredExercises)

                        }
                        binding.actvFBSearch.dismissDropDown()
                        saveSearchQuery(inputText)
                        true
                        // ------! enter 클릭시 동작 끝 !------
                    } else {
                        false
                    }
                }

            // 사용자가 항목을 선택했을 때 필터링된 결과를 리사이클러뷰에 표시
                binding.actvFBSearch.setOnItemClickListener { parent, _, position, _ ->
                    val selectedItem = parent.getItemAtPosition(position) as String
                    val filterList = allDataList.filter { item ->
                        item.exerciseName == selectedItem
                    }.toMutableList()
                    val adapter = ExerciseRVAdapter(this@FavoriteBasketFragment, filterList, singletonInstance.viewingHistory?.toList() ?: listOf(),"basket")
                    binding.rvFB.adapter = adapter
                    val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    binding.rvFB.layoutManager = linearLayoutManager
                    adapter.notifyDataSetChanged()
                }

            // -----! 자동완성 검색 끝 !-----

            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            }
        }

        binding.btnFBFinish.setOnClickListener {

            // -----! 어댑터의 리스트를 담아서 exerciseunit에 담기 시작 !-----
            val selectedItems = viewModel.getExerciseBasketUnit() // + 숫자 추가해놓은 곳에서 가져오기
            Log.w("기존 VM ExerciseUnit", "$selectedItems")
            viewModel.addExercises(selectedItems)
            // -----! 어댑터의 리스트를 담아서 exerciseunit에 담기 끝 !-----

//            requireActivity().supportFragmentManager.beginTransaction().apply {
//                replace(R.id.flMain, FavoriteEditFragment.newInstance(sn))
//                commit()
//            }
            viewModel.exerciseBasketUnits.value?.clear()
        }

        binding.ibtnFBBack.setOnClickListener {
//            requireActivity().supportFragmentManager.beginTransaction().apply {
//                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
//                replace(R.id.flMain, FavoriteEditFragment.newInstance(sn))
//                    .addToBackStack(null)
//                remove(FavoriteBasketFragment()).commit()
//            }
        }
    }
    private fun linkAdapter(list : MutableList<ExerciseVO>) {
        adapter = ExerciseRVAdapter(this@FavoriteBasketFragment,list,listOf(),"basket")
//        adapter.basketListener = this@FavoriteBasketFragment
        binding.rvFB.adapter = adapter
        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvFB.layoutManager = linearLayoutManager

    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateRecyclerView(filteredExercises: List<ExerciseVO>) {
        // RecyclerView 어댑터에 필터링된 데이터를 설정
        val adapter = binding.rvFB.adapter as ExerciseRVAdapter
        adapter.exerciseList = filteredExercises.toMutableList()
        adapter.notifyDataSetChanged()

    }

    private fun loadSearchHistory() {
        val searchHistory = requireActivity().getSharedPreferences(searchHistory, Context.MODE_PRIVATE)
        val history = searchHistory.getStringSet(this.searchHistory, setOf())?.toMutableList()
        if (history != null) {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, history)
            binding.actvFBSearch.setAdapter(adapter)
        }
    }

    private fun saveSearchQuery(query: String) {
        val searchHistory = requireActivity().getSharedPreferences(searchHistory, Context.MODE_PRIVATE)
        val history = searchHistory.getStringSet(this.searchHistory, setOf())?.toMutableSet()
        history?.add(query)
        searchHistory.edit().putStringSet(this.searchHistory, history).apply()
    }
}