package com.tangoplus.tangoq.Fragment

import android.content.ContentValues
import android.os.Bundle
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
import com.tangoplus.tangoq.Adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.Listener.BasketItemTouchListener
import com.tangoplus.tangoq.Object.NetworkExerciseService.fetchExerciseJson
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.ExerciseVO
import com.tangoplus.tangoq.ViewModel.ExerciseViewModel
import com.tangoplus.tangoq.databinding.FragmentFavoriteBasketBinding
import kotlinx.coroutines.launch


class FavoriteBasketFragment : Fragment(), BasketItemTouchListener {
    lateinit var binding: FragmentFavoriteBasketBinding
    private lateinit var adapter: ExerciseRVAdapter
    val viewModel : ExerciseViewModel by activityViewModels()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = requireArguments().getString(ARG_TITLE).toString()
        lifecycleScope.launch {
            val responseArrayList = fetchExerciseJson(getString(R.string.IP_ADDRESS_t_Exercise_Description))
            Log.w(ContentValues.TAG, "jsonArr: ${responseArrayList[0]}")
            try {
                viewModel.allExercises.value = responseArrayList.toMutableList()
                val allDataList = responseArrayList.toMutableList()

                // -----! RV 필터링 시작 !-----
                val recommendlist = mutableListOf<ExerciseVO>()
                val filterList = allDataList.filter { it.exerciseName!!.contains("(1)") }
                for (element in filterList) {
                    recommendlist.add(element)
                    recommendlist.map { exercise ->
                        exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseDescriptionId.toString())
                    }
                    linkAdapter(recommendlist)
                }
                Log.w(ContentValues.TAG, "filterList: $filterList")
                binding.tlFB.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        when (tab?.position) {
                            0 -> {
                                recommendlist.map { exercise ->
                                    exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseDescriptionId.toString())
                                }
                                linkAdapter(recommendlist)
                            }
                            1 -> {
                                val keywords = listOf("목", "어깨", "팔꿉", "손목", "몸통", "복부" )
                                val filteredList = allDataList.filter { item -> keywords.any { keywords -> item.exerciseName!!.contains(keywords) } }.toMutableList()
                                filteredList.map { exercise ->
                                    exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseDescriptionId.toString())
                                }
                                Log.w(ContentValues.TAG, "topBodyList: ${filteredList[0]}")
                                linkAdapter(filteredList)
                            }
                            2 -> {
                                val keywords = listOf("엉덩", "무릎", "발목" )
                                val filteredList = allDataList.filter { item -> keywords.any { keywords -> item.exerciseName!!.contains(keywords) } }.toMutableList()
                                filteredList.map { exercise ->
                                    exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseDescriptionId.toString())
                                }
                                linkAdapter(filteredList)
                            }
                            3 -> {

                                val keywords = listOf("전신", "유산소", "코어", "몸통" )
                                val filteredList = allDataList.filter { item -> keywords.any { keywords -> item.exerciseName!!.contains(keywords) } }.toMutableList()
                                filteredList.map { exercise ->
                                    exercise.quantity = viewModel.getQuantityForItem(exercise.exerciseDescriptionId.toString())
                                }
                                linkAdapter(filteredList)
                            }
                        }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
                // -----! RV 필터링 끝 !-----
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            }
        }
        // -----! 자동완성 검색 시작 !-----
        val jointList = arrayOf("목", "어깨", "팔꿉", "손목", "몸통", "복부", "엉덩", "무릎", "발목", "전신", "유산소", "코어", "몸통")
        val actvAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, jointList)
        binding.actvFB.setAdapter(actvAdapter)
        // -----! 자동완성 검색 끝 !-----

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
        val exercise = viewModel.allExercises.value?.find { it.exerciseDescriptionId.toString() == descriptionId }
        if (exercise != null) {
            viewModel.addExerciseBasketUnit(exercise, newQuantity)
        }
        viewModel.setQuantity(descriptionId, newQuantity)
        Log.w("장바구니viewmodel", "desId: ${viewModel.exerciseBasketUnits.value?.find { it.exerciseDescriptionId.toString() == descriptionId }?.exerciseDescriptionId}, 횟수: ${viewModel.exerciseBasketUnits.value?.find { it.exerciseDescriptionId.toString() == descriptionId }?.quantity}")
    }
}