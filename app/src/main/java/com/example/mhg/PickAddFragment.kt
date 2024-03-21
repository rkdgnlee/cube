package com.example.mhg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.Room.ExerciseDatabase
import com.example.mhg.Room.ExerciseRepository
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentPickAddBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


class PickAddFragment : Fragment() {
   lateinit var binding : FragmentPickAddBinding
   lateinit var ExerciseList : List<HomeRVBeginnerDataClass>
    val viewModel : UserViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPickAddBinding.inflate(inflater)
        binding.clPickAddUnlisted.visibility = View.GONE
        binding.clPickAddPrivate.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nsvPickAdd.isNestedScrollingEnabled = true
        binding.rvPickadd.isNestedScrollingEnabled = false
        binding.rvPickadd.overScrollMode = View.OVER_SCROLL_NEVER

        // -----! 운동 추가에 대한 recyclerview 연결 및 가져오기 시작 !-----
        val db = ExerciseDatabase.getInstance(requireContext())
        lifecycleScope.launch{
            ExerciseList = getExerciseData(db)
            val verticalDataList = ArrayList<HomeRVBeginnerDataClass>()
            for(i in 0 until ExerciseList.size) {
                verticalDataList.add(ExerciseList[i])
//                Log.w(TAG, "$verticalDataList")
            }
            val adapter = HomeVerticalRecyclerViewAdapter(verticalDataList, "add")
            adapter.verticalList = verticalDataList
            binding.rvPickadd.adapter = adapter
            val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvPickadd.layoutManager = linearLayoutManager2
        }
        // -----! 운동 추가에 대한 recyclerview 연결 및 가져오기 시작 !-----

        // -----! 공개 설정 코드 시작 !-----
        var rangeExpanded = false
        binding.clPickAddPublic.setOnClickListener{
            if (!rangeExpanded) {
                binding.clPickAddUnlisted.visibility = View.VISIBLE
                binding.clPickAddPrivate.visibility = View.VISIBLE
                rangeExpanded = true
            } else {
                binding.clPickAddUnlisted.visibility = View.GONE
                binding.clPickAddPrivate.visibility = View.GONE
                rangeExpanded = false
            }
        }

        binding.clPickAddUnlisted.setOnClickListener{
            if (!rangeExpanded) {
                binding.clPickAddPublic.visibility = View.VISIBLE
                binding.clPickAddPrivate.visibility = View.VISIBLE
                rangeExpanded = true
            } else {
                binding.clPickAddPublic.visibility = View.GONE
                binding.clPickAddPrivate.visibility = View.GONE
                rangeExpanded = false
            }
        }
        binding.clPickAddPrivate.setOnClickListener{
            if (!rangeExpanded) {
                binding.clPickAddPublic.visibility = View.VISIBLE
                binding.clPickAddUnlisted.visibility = View.VISIBLE
                rangeExpanded = true
            } else {
                binding.clPickAddPublic.visibility = View.GONE
                binding.clPickAddUnlisted.visibility = View.GONE
                rangeExpanded = false
            }
        }
        // -----! 공개 설정 코드 끝 !-----

        // -----! 운동 만들기 버튼 클릭 시작 !-----
        binding.btnPickAddExercise.setOnClickListener{
            // TODO 운동 저장 테이블 만들어졌을 시 해당 JSON KEY값들에 맞게 다시 바꿔야 함
            viewModel.User.value?.put("routine_name", "${binding.etPickAddName.text}")
            viewModel.User.value?.put("routine_title", "${binding.etPickAddTitle.text}")
            viewModel.User.value?.put("routine_name", "${binding.etPickAddExplain.text}")

            if (binding.clPickAddPublic.visibility == View.VISIBLE) {
                viewModel.User.value?.put("share_range", "Public")
            } else if (binding.clPickAddUnlisted.visibility == View.VISIBLE) {
                viewModel.User.value?.put("share_range", "Unlisted")
            } else if (binding.clPickAddPrivate.visibility == View.VISIBLE) {
                viewModel.User.value?.put("share_range", "Private")
            }
            val checkedData = mutableListOf<JSONObject>()
            for (i in 0 until checkedItems.size()) {
                val checkedItem = originalData[checkedItems.key]
            }
        }
        // -----! 운동 만들기 버튼 클릭 시작 !-----

    }
    suspend fun getExerciseData(db: ExerciseDatabase) : List<HomeRVBeginnerDataClass> {
        return withContext(Dispatchers.IO) {
            ExerciseRepository(db.ExerciseDao()).getHomeRVBeginnerData()
        }
    }


}