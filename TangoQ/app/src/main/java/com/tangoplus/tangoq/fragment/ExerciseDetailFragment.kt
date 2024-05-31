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
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseCategoryRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseJointTypeRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.adapter.SpinnerAdapter
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.databinding.FragmentExerciseDetailBinding
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.`object`.NetworkExerciseService
import com.tangoplus.tangoq.`object`.NetworkExerciseService.fetchCategoryAndSearch
import com.tangoplus.tangoq.`object`.NetworkExerciseService.fetchExerciseJsonByType
import kotlinx.coroutines.launch


class ExerciseDetailFragment : Fragment(), OnCategoryClickListener {
    lateinit var binding : FragmentExerciseDetailBinding
    var filteredDataList = mutableListOf<ExerciseVO>()
    val viewModel : ExerciseViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExerciseDetailBinding.inflate(inflater)
        (activity as? MainActivity)?.setTopLayoutFull(requireActivity().findViewById(R.id.flMain), requireActivity().findViewById(R.id.clMain))
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), onBackPressedCallback)

        return binding.root
    }
    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_CATEGORY_NAME = "cagetory_name"
        private const val ARG_SERACH_ID = "search_id"
        private const val ARG_SERACH_NAME = "search_name"

        fun newInstance(category: Pair<Int, String>, type : Pair<Int, String>): ExerciseDetailFragment {
            val fragment = ExerciseDetailFragment()
            val args = Bundle()
            args.putInt(ARG_CATEGORY_ID, category.first)
            args.putString(ARG_CATEGORY_NAME, category.second)
            args.putInt(ARG_SERACH_ID, type.first)
            args.putString(ARG_SERACH_NAME, type.second)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------! 뒤로가기 시작 !------

        // ------! 선택 카테고리 & 타입 가져오기 시작 !------

        val categoryId = arguments?.getInt(ARG_CATEGORY_ID)
        val categoryName = arguments?.getString(ARG_CATEGORY_NAME)
        val searchId = arguments?.getInt(ARG_SERACH_ID)
        val searchName = arguments?.getString(ARG_SERACH_NAME)
        // ------! 선택 카테고리 & 타입 가져오기  !------

        binding.sflED.startShimmer()
        binding.nsvED.isNestedScrollingEnabled = false
        binding.rvEDAll.isNestedScrollingEnabled = false
        binding.rvEDAll.overScrollMode = 0
//        binding.ibtnEDACTVClear.setOnClickListener {
//            binding.actvEDSearch.text.clear()
//        }

        binding.ibtnEDBack.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
//                setCustomAnimations(R.anim.slide_in_right, R.anim.slide_in_left)
                replace(R.id.flMain, ExerciseFragment())
                commit()
            }
        }

        // -----! 카테고리  시작 !-----
//        val CategoryList = arrayOf("전체","목", "어깨", "팔", "척추", "하체", "몸통", "발목", "전신", "코어")
//        val adapter2 = ExerciseJointTypeRVAdapter(CategoryList,  this@ExerciseDetailFragment )
//        adapter2.category = CategoryList
//        binding.rvEcCategory.adapter = adapter2
//        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        binding.rvEcCategory.layoutManager = linearLayoutManager2
        // -----! 카테고리 끝 !-----

        lifecycleScope.launch {
            // TODO 여기서 CATEGORY에 맞게도 필터링 되야 함.

            filteredDataList  = fetchCategoryAndSearch(getString(R.string.IP_ADDRESS_t_Exercise_Description), categoryId!!, searchId!!)
            // ------! 자동완성 시작 !------
            val keywordList = mutableListOf<String>()
            for (i in 0 until filteredDataList.size) {
                keywordList.add(filteredDataList[i].exerciseName.toString())
            }
            val adapterActv = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, keywordList)
//            binding.actvEDSearch.setAdapter(adapterActv)
//
//            binding.actvEDSearch.setOnItemClickListener { parent, view, position, id ->
//                val selectedItem = parent.getItemAtPosition(position) as String
//                val filterList = verticalDataList.filter { item ->
//                    item.exerciseName == selectedItem
//                }.toMutableList()
//                val adapter = ExerciseRVAdapter(this@ExerciseFragment, filterList, "mainCategory")
//                binding.rvEcAll.adapter = adapter
//                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//                binding.rvEcAll.layoutManager = linearLayoutManager
//                adapter.notifyDataSetChanged()
//            } // ------! 자동완성 끝 !------


            try {
                binding.sflED.stopShimmer()
                binding.sflED.visibility= View.GONE
                filteredDataList = filteredDataList.toMutableList()
                val adapter = ExerciseRVAdapter(this@ExerciseDetailFragment, filteredDataList, "main")
                adapter.exerciseList = filteredDataList
                binding.rvEDAll.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvEDAll.layoutManager = linearLayoutManager
//                // ------! rv vertical 끝 !------

                val filterList = arrayListOf<String>()
                filterList.add("필터")
                filterList.add("최신순")
                filterList.add("인기순")
                filterList.add("추천순")
                binding.spnEDFilter.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, filterList)
                binding.spnEDFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
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
//                                verticalDataList.sortBy { it.exerciseId }
//                                adapter.notifyDataSetChanged()
                            }
                            2 -> {
//                                verticalDataList.sortByDescending { it.videoDuration }
//                                adapter.notifyDataSetChanged()
                            }
                            3 -> {
//                                verticalDataList.sortBy { it.exerciseIntensity }
//                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            } // ------! rv all rv 끝 !------






        }
        // ------! rv all rv 시작 !------



//        binding.ibtnEDBLEConnect.setOnClickListener {
//            if (!isReceiverRegistered) {
//                if (!mBtAdapter?.isEnabled()!!) {
//                    Toast.makeText(requireContext(), "블루투스가 켜지지 않았습니다.", Toast.LENGTH_SHORT).show()
//                    Log.i(ContentValues.TAG, "onResume() - BT not enabled yet")
//                    val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
//                }
//            }
//            clearDevice()
//            scanLeDevice(false)
//            scanLeDevice(true)
//        }
    }

    override fun onCategoryClick(category: String) {
        var filterList = mutableListOf<ExerciseVO>()
        if (category == "전체") {
            filterList = filteredDataList
        } else {
            filterList = filteredDataList.filter { item ->
                item.relatedSymptom!!.contains(category)
            }.toMutableList()
        }
        val adapter = ExerciseRVAdapter(this@ExerciseDetailFragment, filterList, "main")
        binding.rvEDAll.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvEDAll.layoutManager = linearLayoutManager
        adapter.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.setOptiLayout(requireActivity().findViewById(R.id.flMain), requireActivity().findViewById(R.id.clMain), requireActivity().findViewById(R.id.cvCl))
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as? MainActivity)?.setOptiLayout(requireActivity().findViewById(R.id.flMain), requireActivity().findViewById(R.id.clMain), requireActivity().findViewById(R.id.cvCl))
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            requireActivity().supportFragmentManager.beginTransaction().apply {
//                setCustomAnimations(R.anim.slide_in_right, R.anim.slide_in_left)
                replace(R.id.flMain, ExerciseFragment())
                commit()
            }
        }
    }
}
