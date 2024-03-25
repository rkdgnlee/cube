package com.example.mhg

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.ExerciseItemVO
import com.example.mhg.databinding.FragmentPickBasketBinding
import com.example.mhg.`object`.NetworkService.fetchExerciseJson
import kotlinx.coroutines.launch


class PickBasketFragment : Fragment(), BasketItemTouchListener {
    lateinit var binding: FragmentPickBasketBinding
    lateinit var ExerciseList : List<ExerciseItemVO>
    val viewModel : ExerciseViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickBasketBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            val responseArrayList = fetchExerciseJson(getString(R.string.IP_ADDRESS_t_Exercise_Description))
            Log.w(ContentValues.TAG, "jsonArr: $responseArrayList")
            try {
                val verticalDataList = responseArrayList.toMutableList()

                val adapter = HomeVerticalRecyclerViewAdapter(verticalDataList, "basket")
                adapter.verticalList = verticalDataList
                adapter.basketListener = this@PickBasketFragment
                // -----! 클릭 시 현재 layout viewmodel에 담기게 하는 listener !-----
                binding.rvPickBasket.adapter = adapter
                val linearLayoutManager2 =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvPickBasket.layoutManager = linearLayoutManager2


            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            }

        }

        binding.btnPickBasket.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flPick, PickAddFragment())
                    .commit()
            }
        }

    }

    override fun onBasketItemClick(item: ExerciseItemVO) {
        viewModel.addExercise(item)
        Toast.makeText(requireContext(), "${item.exerciseName}, 추가됐습니다!", Toast.LENGTH_SHORT).show()
        Log.w("장바구니viewmodel", "${viewModel.exerciseUnits}")
    }
}