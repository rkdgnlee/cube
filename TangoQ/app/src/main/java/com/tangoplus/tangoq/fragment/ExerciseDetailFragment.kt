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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseCategoryRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.databinding.FragmentExerciseDetailBinding
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchCategoryAndSearch
import com.tangoplus.tangoq.`object`.Singleton_t_history
import kotlinx.coroutines.launch


class ExerciseDetailFragment : Fragment(), OnCategoryClickListener{
    lateinit var binding : FragmentExerciseDetailBinding
    private var filteredDataList = mutableListOf<ExerciseVO>()
    private lateinit var singletonInstance: Singleton_t_history
    private var categoryId : Int? = null
    private lateinit var categoryList : List<String>
    private lateinit var categoryMap : Map<String, Int>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExerciseDetailBinding.inflate(inflater)
//        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), onBackPressedCallback)

        return binding.root
    }

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_CATEGORY_NAME = "cagetory_name"
        private const val ARG_SN = "SN"

        fun newInstance(category: Pair<Int, String>, sn: Int): ExerciseDetailFragment {
            val fragment = ExerciseDetailFragment()
            val args = Bundle()
            args.putInt(ARG_CATEGORY_ID, category.first)
            args.putString(ARG_CATEGORY_NAME, category.second)
            args.putInt(ARG_SN, sn)

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

        categoryId = arguments?.getInt(ARG_CATEGORY_ID)
        val categoryName = arguments?.getString(ARG_CATEGORY_NAME)
        val sn = arguments?.getInt(ARG_SN)
        // ------! 선택 카테고리 & 타입 가져오기  !------

        binding.sflED.startShimmer()
//        binding.nsvED.isNestedScrollingEnabled = false
//        binding.rvEDAll.isNestedScrollingEnabled = false
//        binding.rvEDAll.overScrollMode = 0

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
        categoryList = listOf("목관절", "어깨", "팔꿉", "손목", "척추", "복부", "엉덩", "무릎", "발목")
        categoryMap = mapOf(
            "목관절" to 1,
            "어깨" to 2,
            "팔꿉" to 3,
            "손목" to 4,
            "척추" to 5,
            "복부" to 6,
            "엉덩" to 7,
            "무릎" to 8,
            "발목" to 9
        )
        val adapter2 = ExerciseCategoryRVAdapter(mutableListOf(), categoryList, this@ExerciseDetailFragment, this@ExerciseDetailFragment, sn!! ,"subCategory" )
        binding.rvEDCategory.adapter = adapter2
        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvEDCategory.layoutManager = linearLayoutManager2
        // -----! 카테고리 끝 !-----

        lifecycleScope.launch {
            filteredDataList  = fetchCategoryAndSearch(getString(R.string.IP_ADDRESS_t_exercise_description), categoryId!!, 1)

            // ------! 자동완성 시작 !------
            val exerciseNames = filteredDataList.map { it.exerciseName }.distinct()
            val adapterActv = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exerciseNames)
            binding.actvEDSearch.setAdapter(adapterActv)

            binding.ibtnEDACTVClear.setOnClickListener {
                binding.actvEDSearch.text.clear()

                updateRecyclerView(sn, filteredDataList)
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
                        updateRecyclerView(sn, filteredExercises)
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
            } // ------! 자동완성 끝 !------

            // ------! 시청 기록 시작 !------

            // ------! 시청 기록 끝 !------

            try {
                binding.sflED.stopShimmer()
                binding.sflED.visibility= View.GONE
                filteredDataList = filteredDataList.toMutableList()
                Log.v("EDsn", "$sn")
                // ------! 즐겨찾기 추가에서 왔을 때 !------
                updateRecyclerView(sn, filteredDataList)

                if (filteredDataList.isEmpty()) {
                    binding.tvGuideNull.visibility = View.VISIBLE
                } else {
                    binding.tvGuideNull.visibility = View.INVISIBLE
                }

//                // ------! rv vertical 끝 !------
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
    private fun updateRecyclerView(sn : Int, exercises : MutableList<ExerciseVO>) {
        if (sn == -1 ) {
            val adapter = ExerciseRVAdapter(this@ExerciseDetailFragment, exercises, singletonInstance.viewingHistory?.toList() ?: listOf(), "main")
            adapter.exerciseList = exercises
            binding.rvEDAll.adapter = adapter
            val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvEDAll.layoutManager = linearLayoutManager
            binding.btnEDFinish.visibility = View.GONE
        } else {
            val adapter = ExerciseRVAdapter(this@ExerciseDetailFragment, exercises, singletonInstance.viewingHistory?.toList() ?: listOf(), "basket")
            adapter.exerciseList = exercises


            binding.rvEDAll.adapter = adapter
            val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvEDAll.layoutManager = linearLayoutManager
            binding.btnEDFinish.visibility = View.VISIBLE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCategoryClick(sn : Int , category: String) {
        Log.v("category,search", "1categoryId: ${categoryId}, searchId: ${categoryMap[category]}")
        lifecycleScope.launch {
            try {
                val searchId = categoryMap[category] ?: return@launch
                Log.v("category,search", "2categoryId: ${categoryId}, searchId: ${searchId}")
                filteredDataList = fetchCategoryAndSearch(getString(R.string.IP_ADDRESS_t_exercise_description),
                    categoryId!!, searchId)
                updateRecyclerView(sn, filteredDataList)
            } catch (e: Exception) {
                Log.e("Exercise>filter", "$e")
            }
        }

//        val filterList: MutableList<ExerciseVO> = filteredDataList.filter { item -> item.relatedJoint!!.contains(category) }.toMutableList()

    }




//    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
//        override fun handleOnBackPressed() {
//            requireActivity().supportFragmentManager.beginTransaction().apply {
////                setCustomAnimations(R.anim.slide_in_right, R.anim.slide_in_left)
//                replace(R.id.flMain, ExerciseFragment())
//                commit()
//            }
//        }
//    }

}
