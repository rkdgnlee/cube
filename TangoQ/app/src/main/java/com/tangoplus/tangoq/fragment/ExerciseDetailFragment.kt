package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseCategoryRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.adapter.etc.SpinnerAdapter
import com.tangoplus.tangoq.vo.ExerciseVO
import com.tangoplus.tangoq.viewmodel.ExerciseViewModel
import com.tangoplus.tangoq.databinding.FragmentExerciseDetailBinding
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.ExerciseSearchDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.listener.OnDialogClosedListener
import kotlinx.coroutines.launch


class ExerciseDetailFragment : Fragment(), OnCategoryClickListener, OnDialogClosedListener {
    lateinit var binding : FragmentExerciseDetailBinding
    private var filteredDataList = mutableListOf<ExerciseVO>()
    private var currentCateExercises = mutableListOf<ExerciseVO>()
    private var categoryId : Int? = null
    private val evm : ExerciseViewModel by activityViewModels()

    private lateinit var categoryList : List<String>
    private lateinit var categoryMap : Map<String, Int>
    private lateinit var prefs : PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExerciseDetailBinding.inflate(inflater)
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


        // ------# 선택 카테고리 & 타입 가져오기 시작 #------
        categoryId = arguments?.getInt(ARG_CATEGORY_ID)
        val categoryName = arguments?.getString(ARG_CATEGORY_NAME)
        val sn = arguments?.getInt(ARG_SN)
        prefs = PreferencesManager(requireContext())
        // ------# 선택 카테고리 & 타입 가져오기  #------

        binding.sflED.startShimmer()
        binding.ibtnEDAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }
        binding.ibtnEDQRCode.setOnClickListener{
            val dialog = QRCodeDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "LoginScanDialogFragment")
        }


        when (categoryId) {
            1 -> binding.tvEDMainCategoryName.text = "기본 밸런스"
            2 -> binding.tvEDMainCategoryName.text = "스트레칭"
            3 -> binding.tvEDMainCategoryName.text = "근골격 개선 운동"
            4 -> binding.tvEDMainCategoryName.text = "근골격 스트레칭"
            5 -> binding.tvEDMainCategoryName.text = "기구활용 운동"
        }
        binding.tvEDMainCategoryName.textSize = 24f

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

        val adapter2 = ExerciseCategoryRVAdapter(mutableListOf(), categoryList, this@ExerciseDetailFragment,  sn!! ,"subCategory" )
        adapter2.onCategoryClickListener = this
        binding.rvEDCategory.adapter = adapter2
        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvEDCategory.layoutManager = linearLayoutManager2
        // -----! 카테고리 끝 !-----

        lifecycleScope.launch {
            filteredDataList  = evm.allExercises.filter { it.exerciseCategoryId == categoryId.toString() }.toMutableList()

            // ------! 자동완성 시작 !------
            val exerciseNames = filteredDataList.map { it.exerciseName }.distinct()
            binding.linearLayout3.setOnClickListener{
                val dialog = ExerciseSearchDialogFragment()
                dialog.show(requireActivity().supportFragmentManager, "ExerciseSearchDialogFragment")
            }
            try {
                binding.sflED.stopShimmer()
                binding.sflED.visibility= View.GONE
                currentCateExercises = filteredDataList.filter {  it.exerciseTypeId == categoryMap.get("목관절").toString() }.sortedBy { it.exerciseId }.toMutableList()
                filteredDataList = filteredDataList.toMutableList()

                updateRecyclerView(currentCateExercises.toMutableList())
            // ------! rv vertical 끝 !------
            } catch (e: IndexOutOfBoundsException) {
                Log.e("EDetailIndex", "${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e("EDetailIllegal", "${e.message}")
            } catch (e: IllegalStateException) {
                Log.e("EDetailIllegal", "${e.message}")
            } catch (e: NullPointerException) {
                Log.e("EDetailNull", "${e.message}")
            } catch (e: java.lang.Exception) {
                Log.e("EDetailException", "${e.message}")
            } // ------! rv all rv 끝 !------
        }

        // -----! spinner 연결 시작 !-----
        val filterList = arrayListOf<String>()
        filterList.add("최신순")
        filterList.add("인기순")
        filterList.add("추천순")
        binding.spnrED.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, filterList, 1)
        binding.spnrED.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                when (position) {
                    0 -> updateRecyclerView(currentCateExercises.sortedByDescending { it.exerciseId }.toMutableList())
                    1 -> updateRecyclerView(currentCateExercises.sortedByDescending { it.relatedSymptom }.toMutableList())
                    2 -> updateRecyclerView(currentCateExercises.sortedByDescending { it.duration }.toMutableList())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        // ------! spinner 연결 끝 !------
    }

    private fun updateRecyclerView(exercises : MutableList<ExerciseVO>) {
        val adapter = ExerciseRVAdapter(this@ExerciseDetailFragment, exercises, null, null, null,"main")
        adapter.dialogClosedListener = this@ExerciseDetailFragment
        adapter.exerciseList = exercises
        binding.rvEDAll.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvEDAll.layoutManager = linearLayoutManager

        if (exercises.isEmpty()) {
            binding.tvGuideNull.visibility = View.VISIBLE
        } else {
            binding.tvGuideNull.visibility = View.INVISIBLE
        }
        binding.tvEDTotalCount.text = "전체: ${exercises.size}개"
        adapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCategoryClick(category: String) {
        Log.v("category,search", "1categoryId: ${categoryId}, searchId: ${categoryMap[category]}")
        try {
            currentCateExercises = filteredDataList.filter { it.exerciseTypeId == categoryMap.get(category).toString() }.sortedBy { it.exerciseId }.toMutableList()
            val filterIndex = binding.spnrED.selectedItemPosition
            when (filterIndex) {
                0 -> updateRecyclerView(currentCateExercises.sortedByDescending { it.exerciseId }.toMutableList())
                1 -> updateRecyclerView(currentCateExercises.sortedByDescending { it.relatedSymptom }.toMutableList())
                2 -> updateRecyclerView(currentCateExercises.sortedByDescending { it.duration }.toMutableList())
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("EDetailIndex", "${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("EDetailIllegal", "${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("EDetailIllegal", "${e.message}")
        } catch (e: NullPointerException) {
            Log.e("EDetailNull", "${e.message}")
        } catch (e: java.lang.Exception) {
            Log.e("EDetailException", "${e.message}")
        }
    }

    // ------# 좋아요 누르고 PlayThumbnail 에서 종료 #------
    override fun onDialogClosed() {
        val filterIndex = binding.spnrED.selectedItemPosition
        when (filterIndex) {
            0 -> updateRecyclerView(currentCateExercises.sortedByDescending { it.exerciseId }.toMutableList())
            1 -> updateRecyclerView(currentCateExercises.sortedByDescending { it.relatedSymptom }.toMutableList())
            2 -> updateRecyclerView(currentCateExercises.sortedByDescending { it.duration }.toMutableList())
        }
    }
}
