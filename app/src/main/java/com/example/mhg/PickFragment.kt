package com.example.mhg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.PickRecyclerViewAdapter
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.databinding.FragmentPickBinding


class PickFragment : Fragment(), onPickDetailClickListener {
    lateinit var binding : FragmentPickBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickBinding.inflate(inflater)

        val pickList = mutableListOf<String>()
        val jsonArray = viewModel.exerciseList.value
        // -----! viewmodel의 list관리 시작 !-----
        // TODO 추후에 데이터 받아와서 여기다가 넣어야 함.
        viewModel.exerciseList.observe(viewLifecycleOwner) {basketList ->
            for (i in 0 until jsonArray!!.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val exerciseName = jsonObject.getString("exercise_name")
                pickList.add(exerciseName)
            }
        }
        // -----! viewmodel의 list관리 끝 !-----


        val PickRecyclerViewAdapter = PickRecyclerViewAdapter(pickList, this)
        binding.rvPick.adapter = PickRecyclerViewAdapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvPick.layoutManager = linearLayoutManager


        binding.btnPickAdd.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flPick, PickAddFragment())
                addToBackStack(null)
                commit()
            }
        }

        return binding.root
    }
    override fun onPickClick(title: String) {
        requireActivity().supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, PickDetailFragment.newInstance(title))
            addToBackStack(null)
            commit()

        }
    }

}