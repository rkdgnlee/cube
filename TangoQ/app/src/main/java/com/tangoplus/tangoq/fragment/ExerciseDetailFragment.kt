package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseCategoryRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.databinding.FragmentExerciseDetailBinding
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchExerciseByCategory
import com.tangoplus.tangoq.`object`.Singleton_t_history
import kotlinx.coroutines.launch


class ExerciseDetailFragment : Fragment(), OnCategoryClickListener {
    lateinit var binding : FragmentExerciseDetailBinding
    private var filteredDataList = mutableListOf<ExerciseVO>()
    val viewModel : FavoriteViewModel by activityViewModels()
    private lateinit var singletonInstance: Singleton_t_history

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExerciseDetailBinding.inflate(inflater)
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), onBackPressedCallback)

        return binding.root
    }
    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_CATEGORY_NAME = "cagetory_name"


        fun newInstance(category: Pair<Int, String>): ExerciseDetailFragment {
            val fragment = ExerciseDetailFragment()
            val args = Bundle()
            args.putInt(ARG_CATEGORY_ID, category.first)
            args.putString(ARG_CATEGORY_NAME, category.second)
            fragment.arguments = args
            return fragment
        }
    }
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------! 뒤로가기 시작 !------
        singletonInstance = Singleton_t_history.getInstance(requireContext())
        // ------! 선택 카테고리 & 타입 가져오기 시작 !------

        val categoryId = arguments?.getInt(ARG_CATEGORY_ID)
        val categoryName = arguments?.getString(ARG_CATEGORY_NAME)
        // ------! 선택 카테고리 & 타입 가져오기  !------

        binding.sflED.startShimmer()
        binding.nsvED.isNestedScrollingEnabled = false
        binding.rvEDAll.isNestedScrollingEnabled = false
        binding.rvEDAll.overScrollMode = 0

        when (categoryId) {
            1 -> binding.tvEDMainCategoryName.text = "기본 밸런스"
            2 -> binding.tvEDMainCategoryName.text = "스트레칭"
            3 -> binding.tvEDMainCategoryName.text = "근골격 개선 운동"
            4 -> binding.tvEDMainCategoryName.text = "근골격 스트레칭"
            5 -> binding.tvEDMainCategoryName.text = "기구활용 운동"
        }
        binding.tvEDMainCategoryName.textSize = 24f
        binding.ibtnEDBack.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                //            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, ExerciseFragment())
                commit()
            }
        }

        // -----! 카테고리  시작 !-----
        val categoryList = listOf("전체","목관절", "어깨", "팔꿉", "손목", "척추", "복부", "엉덩", "무릎", "발목")
        val adapter2 = ExerciseCategoryRVAdapter(mutableListOf(), categoryList, this@ExerciseDetailFragment, this@ExerciseDetailFragment, "subCategory" )
        binding.rvEDCategory.adapter = adapter2
        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvEDCategory.layoutManager = linearLayoutManager2
        // -----! 카테고리 끝 !-----

        lifecycleScope.launch {
            filteredDataList  = fetchExerciseByCategory(getString(R.string.IP_ADDRESS_t_exercise_description), categoryId!!)

            // ------! 자동완성 시작 !------
            val exerciseNames = filteredDataList.map { it.exerciseName }.distinct()
            val adapterActv = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exerciseNames)
            binding.actvEDSearch.setAdapter(adapterActv)

            binding.ibtnEDACTVClear.setOnClickListener {
                binding.actvEDSearch.text.clear()

                updateRecyclerView(filteredDataList)
            }
            binding.actvEDSearch.setOnEditorActionListener{ _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_NEXT || event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {

                    // ------! enter 클릭시 동작 시작 !------
                    val inputText = binding.actvEDSearch.text.toString()
                    if (inputText.isNotEmpty()) {
                        val regex = Regex(".*$inputText.*", RegexOption.IGNORE_CASE) // 대소문자 구분하지 않는 정규 표현식
                        val filteredExercises = filteredDataList.filter {
                            it.exerciseName!!.matches(regex)
                        }.distinct().toMutableList()
                        updateRecyclerView(filteredExercises)
                    }
                    binding.actvEDSearch.dismissDropDown()
                    true
                    // ------! enter 클릭시 동작 끝 !------
                } else {
                    false
                }
            }
            // 사용자가 항목을 선택했을 때 필터링된 결과를 리사이클러뷰에 표시
            binding.actvEDSearch.setOnItemClickListener { parent, _, position, _ ->
                val selectedItem = parent.getItemAtPosition(position) as String
                val filterList = filteredDataList.filter { item ->
                    item.exerciseName == selectedItem
                }.toMutableList()
                val adapter = ExerciseRVAdapter(
                    this@ExerciseDetailFragment,
                    filterList,
                    singletonInstance.viewingHistory?.toList() ?: listOf(),
                    "main")
                binding.rvEDAll.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvEDAll.layoutManager = linearLayoutManager
                adapter.notifyDataSetChanged()
            }
            // ------! 자동완성 끝 !------

            // ------! 시청 기록 시작 !------




            // ------! 시청 기록 끝 !------

            try {
                binding.sflED.stopShimmer()
                binding.sflED.visibility= View.GONE
                filteredDataList = filteredDataList.toMutableList()
                val adapter = ExerciseRVAdapter(
                    this@ExerciseDetailFragment,
                    filteredDataList,
                    singletonInstance.viewingHistory?.toList() ?: listOf(),
                    "main")
                adapter.exerciseList = filteredDataList
                binding.rvEDAll.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvEDAll.layoutManager = linearLayoutManager

                if (filteredDataList.isEmpty()) {
                    binding.tvGuideNull.visibility = View.VISIBLE
                } else {
                    binding.tvGuideNull.visibility = View.INVISIBLE
                }

//                // ------! rv vertical 끝 !------

//                val filterList = arrayListOf<String>()
//                filterList.add("필터")
//                filterList.add("최신순")
//                filterList.add("인기순")
//                filterList.add("추천순")
//                binding.spnEDFilter.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, filterList)
//                binding.spnEDFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
//                    @SuppressLint("NotifyDataSetChanged")
//                    override fun onItemSelected(
//                        parent: AdapterView<*>?,
//                        view: View?,
//                        position: Int,
//                        id: Long
//                    ) {
//                        when (position) {
//                            0 -> {}
//                            1 -> {
////                                verticalDataList.sortBy { it.exerciseId }
////                                adapter.notifyDataSetChanged()
//                            }
//                            2 -> {
////                                verticalDataList.sortByDescending { it.videoDuration }
////                                adapter.notifyDataSetChanged()
//                            }
//                            3 -> {
////                                verticalDataList.sortBy { it.exerciseIntensity }
////                                adapter.notifyDataSetChanged()
//                            }
//                        }
//                    }
//                    override fun onNothingSelected(parent: AdapterView<*>?) {}
//                }

            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            } // ------! rv all rv 끝 !------
        }
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
    private fun updateRecyclerView(exercises : MutableList<ExerciseVO>) {
        val adapter = binding.rvEDAll.adapter as ExerciseRVAdapter
        adapter.exerciseList = exercises.toMutableList()
        adapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCategoryClick(category: String) {
        val filterList: MutableList<ExerciseVO> = if (category == "전체") {
            filteredDataList
        } else {
            filteredDataList.filter { item ->
                item.relatedJoint!!.contains(category)
            }.toMutableList()
        }
        val adapter = ExerciseRVAdapter(this@ExerciseDetailFragment, filterList, singletonInstance.viewingHistory?.toList() ?: listOf(),"main")
        binding.rvEDAll.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvEDAll.layoutManager = linearLayoutManager
        adapter.notifyDataSetChanged()
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
