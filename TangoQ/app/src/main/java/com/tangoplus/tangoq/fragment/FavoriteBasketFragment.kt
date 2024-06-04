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
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.listener.BasketItemTouchListener
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchExerciseJson
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.databinding.FragmentFavoriteBasketBinding
import kotlinx.coroutines.launch


class FavoriteBasketFragment : Fragment(), BasketItemTouchListener {
    lateinit var binding: FragmentFavoriteBasketBinding
    private lateinit var adapter: ExerciseRVAdapter
    val viewModel : FavoriteViewModel by activityViewModels()
    var title = ""

    companion object {
        private const val ARG_TITLE = "title"
        fun newInstance(title: String): FavoriteBasketFragment {
            val fragment = FavoriteBasketFragment()
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
        binding = FragmentFavoriteBasketBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = requireArguments().getString(ARG_TITLE).toString()
        lifecycleScope.launch {
            val responseArrayList = fetchExerciseJson(getString(R.string.IP_ADDRESS_t_exercise_description))
            Log.w(ContentValues.TAG, "jsonArr: ${responseArrayList[0]}")
            try {
                viewModel.allExercises.value = responseArrayList.toMutableList()
                val allDataList = responseArrayList.toMutableList()

                // -----! RV 필터링 시작 !-----
                val recommendlist = mutableListOf<ExerciseVO>()
//                val filterList = allDataList.filter { it.exerciseCategoryId!!.contains("2") }
//                for (element in filterList) {
//                    recommendlist.add(element)
//                    recommendlist.map { exercise ->
//                        exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseId.toString())
//                    }
//                linkAdapter(allDataList)
//                }
                linkAdapter(allDataList)
//                Log.w(ContentValues.TAG, "filterList: $filterList")
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
                                val keywords = listOf("목관절", "어깨관절", "무릎관절", "척추")
                                val filteredList = allDataList.filter { item -> keywords.any { keywords -> item.relatedJoint!!.contains(keywords) } }.toMutableList()
                                filteredList.map { exercise ->
                                    exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseId.toString())
                                }
                                Log.w(ContentValues.TAG, "topBodyList: ${filteredList[0]}")
                                linkAdapter(filteredList)
                            }
                            2 -> {
                                val keywords = listOf("목관절", "어깨관절", "팔꿉관절", "손목관절", "척추", "복부관절" )
                                val filteredList = allDataList.filter { item -> keywords.any { keywords -> item.relatedJoint!!.contains(keywords) } }.toMutableList()
                                filteredList.map { exercise ->
                                    exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseId.toString())
                                }
                                linkAdapter(filteredList)
                            }
                            3 -> {

                                val keywords = listOf("엉덩관절", "무릎관절", "발목관절")
                                val filteredList = allDataList.filter { item -> keywords.any { keywords -> item.exerciseCategoryName!!.contains(keywords) } }.toMutableList()
                                filteredList.map { exercise ->
                                    exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseId.toString())
                                }
                                linkAdapter(filteredList)
                            }
                        }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
                // -----! RV 필터링 끝 !-----

                // -----! 자동완성 검색 시작 !-----
                val exerciseNames = allDataList.map { it.exerciseName }.distinct().toMutableList()
                val adapterActv = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exerciseNames)
                binding.actvFBSearch.setAdapter(adapterActv)
                Log.v("exerciseNames", "${exerciseNames.size}")
//                binding.actvFBSearch.addTextChangedListener(object : TextWatcher {
//                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                        val inputText = s.toString()
//                        if (inputText.isNotEmpty()) {
//                            // 입력된 텍스트를 사용하여 관련된 exerciseName 필터링
//                            val filteredExerciseNames = allDataList.filter { it.relatedJoint!!.contains(inputText) }
//                                .map { it.exerciseName }
//                                .toMutableList()
//                            Log.v("filteredExerciseNames", "${filteredExerciseNames}")
//                            // 필터링된 결과로 어댑터 업데이트
//                            adapterActv.clear()
//                            adapterActv.addAll(filteredExerciseNames)
//                            adapterActv.setNotifyOnChange(true)
//                            adapterActv.notifyDataSetChanged()
//                            binding.actvFBSearch.showDropDown()
//                        }
//                    }
//
//                    override fun afterTextChanged(s: Editable?) {}
//                })

// 사용자가 항목을 선택했을 때 필터링된 결과를 리사이클러뷰에 표시
                binding.actvFBSearch.setOnItemClickListener { parent, view, position, id ->
                    val selectedItem = parent.getItemAtPosition(position) as String
                    val filterList = allDataList.filter { item ->
                        item.exerciseName == selectedItem
                    }.toMutableList()
                    val adapter = ExerciseRVAdapter(this@FavoriteBasketFragment, filterList, "basket")
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

        binding.btnPickBasketFinish.setOnClickListener {

            // -----! 어댑터의 리스트를 담아서 exerciseunit에 담기 시작 !-----
            val selectedItems = viewModel.getExerciseBasketUnit() // + 숫자 추가해놓은 곳에서 가져오기
            Log.w("기존 VM ExerciseUnit", "${selectedItems}")
            viewModel.addExercises(selectedItems)
            // -----! 어댑터의 리스트를 담아서 exerciseunit에 담기 끝 !-----

            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, FavoriteEditFragment.newInstance(title))
                commit()
            }
            viewModel.exerciseBasketUnits.value?.clear()

        }
        binding.ibtnFBBack.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, FavoriteEditFragment.newInstance(title))
                    .addToBackStack(null)


                remove(FavoriteBasketFragment()).commit()
            }

        }

    }
    private fun linkAdapter(list : MutableList<ExerciseVO>) {
        adapter = ExerciseRVAdapter(this@FavoriteBasketFragment,list,"basket")

        adapter.basketListener = this@FavoriteBasketFragment
        binding.rvFB.adapter = adapter
        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvFB.layoutManager = linearLayoutManager

    }


    override fun onBasketItemQuantityChanged(descriptionId: String, newQuantity: Int) {
        val exercise = viewModel.allExercises.value?.find { it.exerciseId.toString() == descriptionId }
        if (exercise != null) {
            viewModel.addExerciseBasketUnit(exercise, newQuantity)
        }
        viewModel.setQuantity(descriptionId, newQuantity)
        Log.w("장바구니viewmodel", "desId: ${viewModel.exerciseBasketUnits.value?.find { it.exerciseId.toString() == descriptionId }?.exerciseId}, 횟수: ${viewModel.exerciseBasketUnits.value?.find { it.exerciseId.toString() == descriptionId }?.quantity}")
    }
}