package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.adapter.ExerciseCategoryRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.adapter.SpinnerAdapter
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.`object`.NetworkExerciseService.fetchExerciseJson
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.databinding.FragmentExerciseBinding
import kotlinx.coroutines.launch


class ExerciseFragment : Fragment(), OnCategoryClickListener {
    lateinit var binding : FragmentExerciseBinding
    var verticalDataList = mutableListOf<ExerciseVO>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExerciseBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sflEc.startShimmer()
        binding.nsvEc.isNestedScrollingEnabled = false
        binding.rvEcAll.isNestedScrollingEnabled = false
        binding.rvEcAll.overScrollMode = 0
        binding.ibtnEcACTVClear.setOnClickListener {
            binding.actvEcSearch.text.clear()
        }
        // -----! 카테고리  시작 !-----
        val CategoryList = arrayOf("목", "어깨", "팔꿉", "손목", "몸통", "복부", "엉덩", "무릎", "발목", "전신", "유산소", "코어", "몸통")
        val adapter2 = ExerciseCategoryRVAdapter(CategoryList,  this@ExerciseFragment )
        adapter2.category = CategoryList
        binding.rvEcCategory.adapter = adapter2
        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvEcCategory.layoutManager = linearLayoutManager2
        // -----! 카테고리 끝 !-----



        lifecycleScope.launch {
            val responseArrayList = fetchExerciseJson(getString(R.string.IP_ADDRESS_t_Exercise_Description))

            // ------! 자동완성 시작 !------
            val keywordList = mutableListOf<String>()
            for (i in 0 until responseArrayList.size) {
                keywordList.add(responseArrayList[i].exerciseName.toString())
            }
            val adapterActv = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, keywordList)
            binding.actvEcSearch.setAdapter(adapterActv)

            binding.actvEcSearch.setOnItemClickListener { parent, view, position, id ->
                val selectedItem = parent.getItemAtPosition(position) as String
                val filterList = verticalDataList.filter { item ->
                    item.exerciseName == selectedItem
                }.toMutableList()
                val adapter = ExerciseRVAdapter(this@ExerciseFragment, filterList, "main")
                binding.rvEcAll.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvEcAll.layoutManager = linearLayoutManager
                adapter.notifyDataSetChanged()
            }

            // ------! 자동완성 끝 !------

            try { // ------! rv vertical 시작 !------
                binding.sflEc.stopShimmer()
                binding.sflEc.visibility= View.GONE
                verticalDataList = responseArrayList.toMutableList()
                val adapter = ExerciseRVAdapter(this@ExerciseFragment, verticalDataList, "main")
                adapter.exerciseList = verticalDataList
                binding.rvEcAll.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvEcAll.layoutManager = linearLayoutManager

                // ------! rv vertical 끝 !------
                val filterList = arrayListOf<String>()
                filterList.add("필터")
                filterList.add("최신순")
                filterList.add("인기순")
                filterList.add("추천순")
                binding.spnEcFilter.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, filterList)
                binding.spnEcFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        when (position) {
                            0 -> {}
                            1 -> {
                                verticalDataList.sortBy { it.exerciseDescription }
                                adapter.notifyDataSetChanged()
                            }
                            2 -> {
                                verticalDataList.sortByDescending { it.videoTime }
                                adapter.notifyDataSetChanged()
                            }
                            3 -> {
                                verticalDataList.sortBy { it.exerciseIntensity }
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }


            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCategoryClick(category: String) {

        val filterList = verticalDataList.filter { item ->
            item.exerciseName!!.contains(category)
        }.toMutableList()
        val adapter = ExerciseRVAdapter(this@ExerciseFragment, filterList, "main")
        binding.rvEcAll.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvEcAll.layoutManager = linearLayoutManager
        adapter.notifyDataSetChanged()
    }

}