package com.example.mhg

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.Room.ExerciseDatabase
import com.example.mhg.Room.ExerciseRepository
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentHomeRoutineDetailBinding
import kotlinx.coroutines.launch

class HomeRoutineDetailFragment : Fragment() {
    lateinit var binding: FragmentHomeRoutineDetailBinding
    lateinit var ExerciseList : List<HomeRVBeginnerDataClass> // 각 key값을 통해 map으로 가져온 데이터
    val viewModel : UserViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeRoutineDetailBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = ExerciseDatabase.getInstance(requireContext())

        val bundle = arguments
        val type : String? = bundle?.getString("type")
        Log.w(TAG+"타입", "$type")

        lifecycleScope.launch {

            // -----! db에서 받아서 뿌려주기 시작 !-----
            ExerciseList = getExerciseDataByType(db, type.toString()) // 각 key값을 통해 map으로 가져온 데이터
            Log.w(TAG+"db에서 가져옴", "$ExerciseList")
            binding.tvExerciseAmount.text = ExerciseList.size.toString()

            val linearlayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvHomeRoutineDetail.layoutManager = linearlayoutManager
            val adapter = HomeVerticalRecyclerViewAdapter(ExerciseList, "type")
            adapter.verticalList = ExerciseList
            binding.rvHomeRoutineDetail.adapter = adapter


        }
    }
//    suspend fun getExerciseDataByTypeList(db: ExerciseDatabase, exerciseTypeList: List<String>): Map<String, List<HomeRVBeginnerDataClass>> {
//        val exerciseDataMap = mutableMapOf<String, List<HomeRVBeginnerDataClass>>()
//        for (type in exerciseTypeList) {
//            exerciseDataMap[type] = ExerciseRepository(db.ExerciseDao()).getExerciseDataByType(type)
//        }
//        return exerciseDataMap
//    }
    suspend fun getExerciseDataByType(db: ExerciseDatabase, exerciseType: String): List<HomeRVBeginnerDataClass> {
        return ExerciseRepository(db.ExerciseDao()).getExerciseDataByType(exerciseType)
    }

}
