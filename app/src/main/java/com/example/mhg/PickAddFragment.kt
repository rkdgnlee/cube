package com.example.mhg

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ExerciseViewModel

import com.example.mhg.databinding.FragmentPickAddBinding
import com.google.gson.Gson
import com.google.gson.JsonArray
import org.json.JSONObject


class PickAddFragment : Fragment() {
   lateinit var binding : FragmentPickAddBinding
    val viewModel: ExerciseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flPick, PickBasketFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
        // -----! viewmodel의 추가되는 값 관찰하기 !-----
        viewModel.exerciseUnits.observe(viewLifecycleOwner) { basketUnits ->
            val adapter = HomeVerticalRecyclerViewAdapter(basketUnits, "add")
            adapter.verticalList = basketUnits

            val linearLayoutManager2 =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvPickadd.layoutManager = linearLayoutManager2

            // -----! item에 swipe 및 dragNdrop 연결 !-----
            val callback = ItemTouchCallback(adapter).apply {
//                setClamp(260f)
//                removePreviousClamp(binding.rvPickadd)
            }

            val touchHelper = ItemTouchHelper(callback)
            touchHelper.attachToRecyclerView(binding.rvPickadd)
            binding.rvPickadd.adapter = adapter
            adapter.startDrag(object: HomeVerticalRecyclerViewAdapter.OnStartDragListener {
                override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
                    touchHelper.startDrag(viewHolder)
                }
            })
            adapter.notifyDataSetChanged()
            // -----! swipe, drag 연동 !-----
        }

        binding.btnPickAddExercise.setOnClickListener {

            // -----! 즐겨찾기 하나 만들기 시작 !-----
            viewModel.exerciseItem.value?.put("basket_name", binding.etPickAddName.text)
            viewModel.exerciseItem.value?.put("basket_explain_title", binding.etPickAddName.text)
            viewModel.exerciseItem.value?.put("basket_explain", binding.etPickAddName.text)

            if (binding.clPickAddPublic.visibility == View.VISIBLE) {
                viewModel.exerciseItem.value?.put("basket_disclosure", "public")
            } else if (binding.clPickAddUnlisted.visibility == View.VISIBLE) {
                viewModel.exerciseItem.value?.put("basket_disclosure", "unlisted")
            } else {
                viewModel.exerciseItem.value?.put("basket_disclosure", "private")
            }
            viewModel.exerciseItem.value?.put("basket_exercises", "${viewModel.exerciseUnits}")
            val jsonObj = JSONObject()

            // -----! 즐겨찾기 하나 넣을 때, key값 = basket_name으로 !-----
            jsonObj.put("${viewModel.exerciseItem.value?.optString("basket_name")}", viewModel.exerciseItem)
            Log.w("즐겨찾기 하나", "${jsonObj}")
            // TODO: JSON으로 만든 것 보내기
            // -----! 즐겨찾기 하나 만들기 끝 !-----

            // -----! 즐겨찾기 목록에 업데이트 !-----
            // TODO -----! 즐겨찾기가 추가되면서 응답으로 갱신된 업데이트 목록 추가(select를 해서)

            viewModel.exerciseList.value?.put(jsonObj)



            // -----! 운동 만들기 버튼 클릭 끝 !-----
        }

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
}