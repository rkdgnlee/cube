package com.example.mhg

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.util.SparseBooleanArray
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
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
   private lateinit var adapter: HomeVerticalRecyclerViewAdapter

    val viewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPickAddBinding.inflate(inflater)
        binding.clPickAddUnlisted.visibility = View.GONE
        binding.clPickAddPrivate.visibility = View.GONE




        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nsvPickAdd.isNestedScrollingEnabled = true
        binding.rvPickadd.isNestedScrollingEnabled = false
        binding.rvPickadd.overScrollMode = View.OVER_SCROLL_NEVER
        binding.btnPickAddGoBasket.setOnClickListener{
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flPick, PickBasketFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }



        // -----! viewmodel의 추가되는 값 관찰하기 !-----
        viewModel.UserBasket.observe(viewLifecycleOwner) { basketItems ->
            val adapter = HomeVerticalRecyclerViewAdapter(basketItems, "add")
            adapter.verticalList = basketItems

            val linearLayoutManager2 =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvPickadd.layoutManager = linearLayoutManager2

            // -----! item에 swipe 및 dragNdrop 연결 !-----
            val callback = ItemTouchCallback(adapter)
            val touchHelper = ItemTouchHelper(callback)
            touchHelper.attachToRecyclerView(binding.rvPickadd)
            binding.rvPickadd.adapter = adapter

            adapter.startDrag(object: HomeVerticalRecyclerViewAdapter.OnStartDragListener {
                override fun onStartDrag(viewHolder: HomeVerticalRecyclerViewAdapter) {
                    TODO("Not yet implemented")
                }
            })

            adapter.notifyDataSetChanged()



            // -----! swipe, drag 연동 !-----

        }


        // -----! 운동 추가에 대한 recyclerview 연결 및 가져오기 시작 !-----
//        val db = ExerciseDatabase.getInstance(requireContext())
//        lifecycleScope.launch{
//            ExerciseList = getExerciseData(db)
//            val verticalDataList = ArrayList<HomeRVBeginnerDataClass>()
//            for(i in 0 until ExerciseList.size) {
//                verticalDataList.add(ExerciseList[i])
////                Log.w(TAG, "$verticalDataList")
//            }
//            val adapter = HomeVerticalRecyclerViewAdapter(verticalDataList, "add")
//            adapter.verticalList = verticalDataList
//            binding.rvPickadd.adapter = adapter
//            val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//            binding.rvPickadd.layoutManager = linearLayoutManager2
//
//
//
//            // -----! 운동 만들기 버튼 클릭 시작 !-----
//            binding.btnPickAddExercise.setOnClickListener{
//                // TODO 운동 저장 테이블 만들어졌을 시 해당 JSON KEY값들에 맞게 다시 바꿔야 함
//                viewModel.User.value?.put("routine_name", "${binding.etPickAddName.text}")
//                viewModel.User.value?.put("routine_title", "${binding.etPickAddTitle.text}")
//                viewModel.User.value?.put("routine_explain", "${binding.etPickAddExplain.text}")
//
//                if (binding.clPickAddPublic.visibility == View.VISIBLE) {
//                    viewModel.User.value?.put("share_range", "Public")
//                } else if (binding.clPickAddUnlisted.visibility == View.VISIBLE) {
//                    viewModel.User.value?.put("share_range", "Unlisted")
//                } else if (binding.clPickAddPrivate.visibility == View.VISIBLE) {
//                    viewModel.User.value?.put("share_range", "Private")
//                }
//                viewModel.User.value?.put("selected_exercise","${adapter.getCheckedItems()}")
//                Log.w("$TAG 즐겨찾기추가", "${viewModel.User.value!!.optString("routine_name")}, ${viewModel.User.value!!.optString("share_range")}, ${viewModel.User.value!!.optString("routine_explain")},  ${viewModel.User.value!!.optString("selected_exercise")}")
//
//            } // -----! 운동 만들기 버튼 클릭 끝 !-----
//
//        }
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



    }
    suspend fun getExerciseData(db: ExerciseDatabase) : List<HomeRVBeginnerDataClass> {
        return withContext(Dispatchers.IO) {
            ExerciseRepository(db.ExerciseDao()).getHomeRVBeginnerData()
        }
    }


}