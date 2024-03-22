package com.example.mhg

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.Room.ExerciseDatabase
import com.example.mhg.Room.ExerciseRepository
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentPickAddBinding
import com.example.mhg.databinding.FragmentPickBasketBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickBasketFragment : Fragment(), BasketItemTouchListener {
    lateinit var binding: FragmentPickBasketBinding
    lateinit var ExerciseList : List<HomeRVBeginnerDataClass>
    val viewModel : UserViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickBasketBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val db = ExerciseDatabase.getInstance(requireContext())
        lifecycleScope.launch {
            ExerciseList = getExerciseData(db)
            val verticalDataList = ArrayList<HomeRVBeginnerDataClass>()
            for (i in 0 until ExerciseList.size) {
                verticalDataList.add(ExerciseList[i])
//                Log.w(TAG, "$verticalDataList")
            }
            val adapter = HomeVerticalRecyclerViewAdapter(verticalDataList, "basket")
            adapter.verticalList = verticalDataList
            adapter.basketListener = this@PickBasketFragment
            // -----! 클릭 시 현재 layout viewmodel에 담기게 하는 listener !-----
            binding.rvPickBasket.adapter = adapter
            val linearLayoutManager2 =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvPickBasket.layoutManager = linearLayoutManager2
        }


    }
    suspend fun getExerciseData(db: ExerciseDatabase) : List<HomeRVBeginnerDataClass> {
        return withContext(Dispatchers.IO) {
            ExerciseRepository(db.ExerciseDao()).getHomeRVBeginnerData()
        }
    }
    override fun onBasketItemClick(item: HomeRVBeginnerDataClass) {
        viewModel.addItem(item)
        Toast.makeText(requireContext(), "${item.exerciseName}, 추가됐습니다!", Toast.LENGTH_SHORT).show()
        Log.w("장바구니viewmodel", "${viewModel.UserBasket.value}")
    }
}