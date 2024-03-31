package com.example.mhg

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.ExerciseVO
import com.example.mhg.databinding.FragmentPickBasketBinding
import com.example.mhg.`object`.NetworkExerciseService.fetchExerciseJson
import com.example.mhg.`object`.Singleton_t_user
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch


class PickBasketFragment : Fragment(), BasketItemTouchListener {
    lateinit var binding: FragmentPickBasketBinding
    lateinit var ExerciseList : List<ExerciseVO>
    val viewModel : ExerciseViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickBasketBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val t_userData = Singleton_t_user.getInstance(requireContext())
        lifecycleScope.launch {
            val responseArrayList = fetchExerciseJson(getString(R.string.IP_ADDRESS_t_Exercise_Description))
//            Log.w(ContentValues.TAG, "jsonArr: $responseArrayList")
            try {
                val allDataList = responseArrayList.toMutableList()

                // -----! RV 필터링 시작 !-----
                val recommendlist = mutableListOf<ExerciseVO>()
                val filterList = allDataList.filter { it.exerciseName!!.contains("(1)") }
                for (i in 0..filterList.size) {
                    recommendlist.add(filterList[i])
                }
                binding.tlPickBasket.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        when (tab?.position) {
                            0 -> {
                                linkAdapter(recommendlist)
                            }
                            1 -> {
                                val keywords = listOf("목", "어깨", "팔꿉", "손목", "몸통", "복부" )
                                val topBodyList = allDataList.filter { item -> keywords.any { keywords -> item.exerciseName!!.contains(keywords) } }.toMutableList()
                                linkAdapter(topBodyList)
                            }
                            2 -> {
                                val keywords = listOf("엉덩", "무릎", "발목" )
                                val topBodyList = allDataList.filter { item -> keywords.any { keywords -> item.exerciseName!!.contains(keywords) } }.toMutableList()
                                linkAdapter(topBodyList)
                            }
                            3 -> {
                                val keywords = listOf("전신", "유산소", "코어", "몸통" )
                                val topBodyList = allDataList.filter { item -> keywords.any { keywords -> item.exerciseName!!.contains(keywords) } }.toMutableList()
                                linkAdapter(topBodyList)
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
        binding.actvPickBasket.setAdapter(actvAdapter)
        // -----! 자동완성 검색 끝 !-----

        binding.btnPickBasket.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flPick, PickEditFragment())
                    .commit()
            }
        }


    }
    private fun linkAdapter(list : MutableList<ExerciseVO>) {
        val adapter = HomeVerticalRecyclerViewAdapter(list, "basket")
        adapter.verticalList = list
        adapter.basketListener = this@PickBasketFragment
        binding.rvPickBasket.adapter = adapter
        val linearLayoutManager2 =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvPickBasket.layoutManager = linearLayoutManager2
    }
    override fun onBasketItemClick(item: ExerciseVO) {
        viewModel.addExercise(item)
        Toast.makeText(requireContext(), "${item.exerciseName}, 추가됐습니다!", Toast.LENGTH_SHORT).show()
        Log.w("장바구니viewmodel", "${viewModel.exerciseUnits}")
    }
}