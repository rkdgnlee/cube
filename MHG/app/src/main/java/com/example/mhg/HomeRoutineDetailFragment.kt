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
import com.example.mhg.VO.ExerciseVO
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentHomeRoutineDetailBinding
import com.example.mhg.`object`.NetworkExerciseService.fetchExerciseJsonByType

import kotlinx.coroutines.launch

class HomeRoutineDetailFragment : Fragment() {
    lateinit var binding: FragmentHomeRoutineDetailBinding
    lateinit var exerciseList : MutableList<ExerciseVO> // 각 key값을 통해 map으로 가져온 데이터
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


        val bundle = arguments
        val type : String? = bundle?.getString("type")
        Log.w(TAG+"타입", "$type")

        lifecycleScope.launch {

            // -----! db에서 받아서 뿌려주기 시작 !-----
            exerciseList = fetchExerciseJsonByType(getString(R.string.IP_ADDRESS_t_Exercise_Description), getTypeIndex(type).toString()) // 각 key값을 통해 map으로 가져온 데이터
            Log.w("$TAG+조건", "${exerciseList.size}")
            binding.tvExerciseAmount.text = exerciseList.size.toString()

            val linearlayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvHomeRoutineDetail.layoutManager = linearlayoutManager
            val adapter = HomeVerticalRecyclerViewAdapter(exerciseList,"home")
            adapter.verticalList = exerciseList
            binding.rvHomeRoutineDetail.adapter = adapter


        }
    }

    fun getTypeIndex(type: String?) : Int? {
        val exerciseTypeList = listOf("목관절", "어깨", "팔꿉", "손목", "몸통전면(복부)", "몸통 후면(척추)", "몸통 코어", "엉덩", "무릎", "발목", "유산소")
        return type?.let { exerciseTypeList.indexOf(it) + 1 }
    }
}
